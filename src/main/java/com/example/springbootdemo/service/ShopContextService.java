package com.example.springbootdemo.service;

import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 把店铺现状拼成一段给 AI 客服看的上下文：用户是谁、货架上有哪些商品。
 */
@Service
public class ShopContextService {

    private final ProductRepository products;

    public ShopContextService(ProductRepository products) {
        this.products = products;
    }

    public String build(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("店铺：提丰商城，主营数码、美妆、服饰、家居、食品等品类，满 99 包邮，7 天无理由。\n");

        if (user == null) {
            sb.append("当前用户：未登录的游客（可以逛、可以问，但要登录才能下单和看订单）。\n");
        } else {
            sb.append("当前用户：").append(user.getNickname() == null ? user.getUsername() : user.getNickname())
                    .append("（用户名 ").append(user.getUsername()).append("）\n");
        }

        sb.append("商品目录（id｜名称｜价格｜分类｜已售）：\n");
        List<Product> list = products.findAllByOrderByIdAsc();
        for (Product p : list) {
            sb.append(p.getId()).append("｜")
                    .append(p.getName()).append("｜")
                    .append(String.format("%.0f", p.getPrice())).append("元｜")
                    .append(p.getCat()).append("｜")
                    .append(p.getSold())
                    .append('\n');
        }
        return sb.toString();
    }
}