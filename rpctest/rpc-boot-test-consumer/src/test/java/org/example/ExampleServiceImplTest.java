package org.example;

import jakarta.annotation.Resource;
import org.example.ExampleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 单元测试
 */
@SpringBootTest
class ExampleServiceImplTest {

    @Resource
    private ExampleServiceImpl exampleService;

    @Test
    void test1() {
        exampleService.test();
    }
}