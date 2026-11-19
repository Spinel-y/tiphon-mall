package com.example.springbootdemo.repo;

import com.example.springbootdemo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByIdAsc();

    List<Product> findByCatOrderByIdAsc(String cat);

    List<Product> findByNameContainingOrderByIdAsc(String keyword);
}