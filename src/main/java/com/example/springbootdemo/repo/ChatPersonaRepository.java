package com.example.springbootdemo.repo;

import com.example.springbootdemo.entity.ChatPersona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPersonaRepository extends JpaRepository<ChatPersona, Long> {
}