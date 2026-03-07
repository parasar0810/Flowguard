package com.flowguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class FlowGuardApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
        // with all beans, autoconfiguration, and profile-specific config
    }
}
