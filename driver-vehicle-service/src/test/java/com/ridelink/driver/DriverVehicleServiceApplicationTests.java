package com.ridelink.driver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb-driver;DB_CLOSE_DELAY=-1",
    "jwt.secret=dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVDI1Ng==",
    "internal.service-key=test-internal-key",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DriverVehicleServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring Boot application context loads cleanly
    }
}
