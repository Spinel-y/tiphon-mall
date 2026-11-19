package com.example.springbootdemo.repo;

import com.example.springbootdemo.entity.CsProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsProfileRepository extends JpaRepository<CsProfile, Long> {
}