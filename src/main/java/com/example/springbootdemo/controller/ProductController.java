package com.example.springbootdemo.controller;

import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.repo.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String cat,
                                          @RequestParam(required = false) String q) {
        List<Product> list;
        if (q != null && !q.isBlank()) {
            list = products.findByNameContainingOrderByIdAsc(q.trim());
        } else if (cat != null && !cat.isBlank() && !"all".equals(cat)) {
            list = products.findByCatOrderByIdAsc(cat);
        } else {
            list = products.findAllByOrderByIdAsc();
        }
        return list.stream().map(ProductController::view).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        return view(product);
    }

    static Map<String, Object> view(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("price", product.getPrice());
        map.put("original", product.getOriginalPrice());
        map.put("sold", product.getSold());
        map.put("rating", product.getRating());
        map.put("cat", product.getCat());
        map.put("tag", product.getTag());
        map.put("desc", product.getDescription());
        // 还没抓过图的商品给一个"按需补图"的地址：前端一请求，MediaController 就会去抓一张
        map.put("image", product.getImageUrl() != null ? product.getImageUrl()
                : "/media/products/p" + product.getId() + "-0.jpg");
        return map;
    }
}