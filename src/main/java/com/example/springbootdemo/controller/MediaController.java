package com.example.springbootdemo.controller;

import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.repo.ProductRepository;
import com.example.springbootdemo.service.ProductImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商品图片对外暴露：/media/products/p<商品id>-<时间戳>.<ext>
 * 只允许读 product-images 目录下的普通文件名，挡掉路径穿越。
 *
 * 图片文件丢了（比如换了台机器、或者运行时数据被清掉）时按文件名里的商品 id
 * 自动补抓一张，DB 里的 image_url 同步更新——这样数据库里存的地址永远是"能用"的。
 */
@RestController
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);
    private static final Path BASE = Paths.get("product-images");
    private static final Pattern PRODUCT_FILE = Pattern.compile("^p(\\d+)-\\d+\\.(png|jpg|jpeg|webp)$");

    private final ProductRepository products;
    private final ProductImageService images;
    /** 防止同一个商品被并发重复抓图 */
    private final Set<Long> healing = ConcurrentHashMap.newKeySet();

    public MediaController(ProductRepository products, ProductImageService images) {
        this.products = products;
        this.images = images;
    }

    @GetMapping("/media/products/{filename}")
    public ResponseEntity<Resource> productImage(@PathVariable String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path base = BASE.toAbsolutePath().normalize();
        Path file = base.resolve(filename).normalize();
        if (!file.startsWith(base)) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.isRegularFile(file)) {
            file = heal(filename);
            if (file == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }
        String name = file.getFileName().toString().toLowerCase();
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG
                : name.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header("Cache-Control", "public, max-age=86400")
                .body(new FileSystemResource(file));
    }

    /** 文件名里带着商品 id，照着再抓一张；抓不到就算了 */
    private Path heal(String filename) {
        Matcher m = PRODUCT_FILE.matcher(filename);
        if (!m.matches()) {
            return null;
        }
        long id = Long.parseLong(m.group(1));
        if (!healing.add(id)) {
            return null;
        }
        try {
            Product p = products.findById(id).orElse(null);
            if (p == null) {
                return null;
            }
            String url = images.refetch(p);
            p.setImageUrl(url);
            products.save(p);
            log.info("商品 {} 的配图缺失，已自动补抓：{}", p.getName(), url);
            Path path = BASE.resolve(url.substring(url.lastIndexOf('/') + 1)).toAbsolutePath().normalize();
            return Files.isRegularFile(path) ? path : null;
        } catch (Exception e) {
            log.warn("商品 {} 补图失败：{}", id, e.getMessage());
            return null;
        } finally {
            healing.remove(id);
        }
    }
}