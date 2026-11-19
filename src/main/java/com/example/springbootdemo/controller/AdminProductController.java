package com.example.springbootdemo.controller;

import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.repo.ProductRepository;
import com.example.springbootdemo.service.CurrentUser;
import com.example.springbootdemo.service.ProductImageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 店主后台：单个商品换图（重抓 / 上传）、整店一键重抓。
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductRepository products;
    private final ProductImageService images;
    private final CurrentUser currentUser;

    public AdminProductController(ProductRepository products, ProductImageService images, CurrentUser currentUser) {
        this.products = products;
        this.images = images;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/image/refetch")
    public Map<String, Object> refetch(@PathVariable Long id, Authentication auth) throws IOException {
        requireAdmin(auth);
        Product product = find(id);
        String url = images.refetch(product);
        product.setImageUrl(url);
        product.setImageSource("so.com");
        products.save(product);
        return Map.of("id", id, "image", url);
    }

    @PostMapping("/{id}/image/upload")
    public Map<String, Object> upload(@PathVariable Long id,
                                      @RequestParam("image") MultipartFile file,
                                      Authentication auth) throws IOException {
        requireAdmin(auth);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没收到图片");
        }
        Product product = find(id);
        String url = images.storeUploaded(id, file.getBytes());
        product.setImageUrl(url);
        product.setImageSource("upload");
        products.save(product);
        return Map.of("id", id, "image", url);
    }

    @PostMapping("/images/refetch-all")
    public List<Map<String, Object>> refetchAll(Authentication auth) throws InterruptedException {
        requireAdmin(auth);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product product : products.findAllByOrderByIdAsc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", product.getId());
            try {
                String url = images.refetch(product);
                product.setImageUrl(url);
                product.setImageSource("so.com");
                products.save(product);
                row.put("ok", true);
                row.put("image", url);
            } catch (Exception e) {
                row.put("ok", false);
                row.put("error", e.getMessage());
            }
            result.add(row);
            Thread.sleep(800);
        }
        // 抓完顺手清掉没被任何商品引用的旧图，免得 product-images 越堆越多
        images.pruneUnused(products.findAllByOrderByIdAsc());
        return result;
    }

    private Product find(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
    }

    private void requireAdmin(Authentication auth) {
        if (!currentUser.isAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有店主才能改商品喵");
        }
    }
}