package com.example.springbootdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 冒烟测试：用 H2 内存库把上下文拉起来，保证不依赖本机 MySQL。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tiphon-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("h2")
class SpringbootDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}