package com.veltro.inventory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requiere PostgreSQL real, no compatible con H2 en tests")
@SpringBootTest
class VeltroApplicationTests {

    @Test
    void contextLoads() {
    }

}
