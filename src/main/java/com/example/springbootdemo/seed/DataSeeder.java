package com.example.springbootdemo.seed;

import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.repo.ProductRepository;
import com.example.springbootdemo.repo.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * 启动时补数据：商品表空就从 classpath:products.json 灌 35 条，
 * 管理员账号不存在就建一个（密码来自 application-secret.properties）。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ProductRepository products;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${tiphon.admin.password:}")
    private String adminPassword;

    public DataSeeder(ProductRepository products, UserRepository users, PasswordEncoder encoder) {
        this.products = products;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedProducts();
        seedAdmin();
    }

    private void seedProducts() throws Exception {
        if (products.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("products.json").getInputStream()) {
            List<Map<String, Object>> rows = new ObjectMapper().readValue(in, new TypeReference<>() {
            });
            for (Map<String, Object> row : rows) {
                Product product = new Product();
                product.setId(((Number) row.get("id")).longValue());
                product.setName(String.valueOf(row.get("name")));
                product.setPrice(((Number) row.get("price")).doubleValue());
                product.setOriginalPrice(((Number) row.getOrDefault("original", 0)).doubleValue());
                product.setSold(((Number) row.getOrDefault("sold", 0)).intValue());
                product.setRating(((Number) row.getOrDefault("rating", 0)).doubleValue());
                product.setCat(String.valueOf(row.getOrDefault("cat", "other")));
                product.setTag(String.valueOf(row.getOrDefault("tag", "")));
                product.setDescription(String.valueOf(row.getOrDefault("desc", "")));
                products.save(product);
            }
            log.info("商品种子数据已写入：{} 条", rows.size());
        }
    }

    private void seedAdmin() {
        if (users.existsByUsername("admin")) {
            return;
        }
        boolean generated = adminPassword == null || adminPassword.isBlank();
        String password = generated ? randomPassword() : adminPassword;
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode(password));
        admin.setNickname("店主");
        admin.setRole("ADMIN");
        users.save(admin);
        if (generated) {
            log.warn("没有配置 tiphon.admin.password，已为 admin 生成临时密码：{}（重启后想保留就把它写进配置文件）", password);
        } else {
            log.info("管理员账号 admin 已创建（密码来自 tiphon.admin.password）");
        }
    }

    /** 12 位随机密码，够用就行，不追求可读性 */
    private String randomPassword() {
        String chars = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}