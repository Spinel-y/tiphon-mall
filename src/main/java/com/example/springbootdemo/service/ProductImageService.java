package com.example.springbootdemo.service;

import com.example.springbootdemo.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商品配图：360 图搜拿候选直链，下载后先看文件头再落盘，
 * 存到 product-images/，对外是 /media/products/xxx.jpg。
 */
@Service
public class ProductImageService {

    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final Path DIR = Paths.get("product-images");
    private static final int MIN_BYTES = 12 * 1024;

    private final RestClient searchClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, UA)
            .defaultHeader(HttpHeaders.REFERER, "https://image.so.com/")
            .build();

    private final RestClient downloadClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, UA)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /** 抓一张新图，返回可直接给前端的 URL */
    public String refetch(Product product) throws IOException {
        List<String> urls = search(product.getName());
        if (urls.isEmpty()) {
            urls = search(product.getName() + " 商品");
        }
        if (urls.isEmpty()) {
            throw new IllegalStateException("图搜没有返回结果：" + product.getName());
        }
        for (String url : urls) {
            try {
                byte[] bytes = downloadClient.get().uri(URI.create(url)).retrieve().body(byte[].class);
                String ext = sniff(bytes);
                if (ext == null) {
                    continue;
                }
                return write(product.getId(), bytes, ext);
            } catch (Exception e) {
                log.warn("下载商品图失败 {}：{}", url, e.getMessage());
            }
        }
        throw new IllegalStateException("候选图都下载不下来，换一次试试：" + product.getName());
    }

    /** 管理员手动上传替换 */
    public String storeUploaded(Long productId, byte[] bytes) throws IOException {
        String ext = sniff(bytes);
        if (ext == null) {
            throw new IllegalStateException("只支持 JPG / PNG / WebP 图片");
        }
        return write(productId, bytes, ext);
    }

    List<String> search(String keyword) {
        try {
            String json = searchClient.get()
                    .uri("https://image.so.com/j?q={q}&src=srp&sn=0&pn=20", keyword)
                    .retrieve()
                    .body(String.class);
            if (json == null || json.isBlank()) {
                return List.of();
            }
            JsonNode list = mapper.readTree(json).path("list");
            List<String> urls = new ArrayList<>();
            for (JsonNode node : list) {
                String img = node.path("img").asText("");
                if (!img.startsWith("http")) {
                    continue;
                }
                int w = node.path("width").asInt(0);
                int h = node.path("height").asInt(0);
                if (w > 0 && w < 600) {
                    continue;
                }
                if (w > 0 && h > 0) {
                    double ratio = w / (double) h;
                    if (ratio < 0.55 || ratio > 2.0) {
                        continue;
                    }
                }
                urls.add(img);
                if (urls.size() >= 8) {
                    break;
                }
            }
            return urls;
        } catch (Exception e) {
            log.warn("图搜请求失败 {}：{}", keyword, e.getMessage());
            return List.of();
        }
    }

    /** 清掉 database 里已经没有商品引用的图片文件，返回删掉的张数 */
    public int pruneUnused(Collection<Product> all) {
        Set<String> keep = new HashSet<>();
        for (Product p : all) {
            String url = p.getImageUrl();
            if (url != null && url.contains("/")) {
                keep.add(url.substring(url.lastIndexOf('/') + 1));
            }
        }
        int removed = 0;
        try (var files = Files.list(DIR)) {
            for (Path f : files.toList()) {
                String name = f.getFileName().toString();
                if (Files.isRegularFile(f) && !keep.contains(name)) {
                    try {
                        Files.delete(f);
                        removed++;
                    } catch (IOException ignored) {
                        // 被占用就算了，下次再清
                    }
                }
            }
        } catch (IOException e) {
            log.warn("清理旧商品图失败：{}", e.getMessage());
        }
        return removed;
    }

    private String write(Long productId, byte[] bytes, String ext) throws IOException {
        Files.createDirectories(DIR);
        String filename = "p" + productId + "-" + System.currentTimeMillis() + ext;
        Files.write(DIR.resolve(filename), bytes);
        return "/media/products/" + filename;
    }

    /** 看文件头判真格式，防止把 HTML 错误页当图片存下来 */
    private static String sniff(byte[] bytes) {
        if (bytes == null || bytes.length < MIN_BYTES) {
            return null;
        }
        if (bytes.length > 4 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return ".jpg";
        }
        if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return ".png";
        }
        if (bytes.length > 12 && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}