package com.classservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical tenant-isolation integration tests.
 * Verifies that Teacher A from Tenant A cannot see or access Tenant B's data.
 *
 * Setup:
 *   - Two tenants registered independently
 *   - Each tenant creates one class
 * Assertions:
 *   - Tenant A's token cannot GET Tenant B's class (expects 404, not 403)
 *   - Tenant A's token cannot list students for Tenant B's class
 *
 * Requires Docker for Testcontainers PostgreSQL.
 */
@Disabled("Requires Docker for Testcontainers PostgreSQL")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TenantIsolationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String apiBase;

    // Tenant A credentials
    private String tokenA;
    private String tenantAId;
    private String teacherAId;

    // Tenant B credentials
    private String tokenB;
    private String classBId;

    @BeforeEach
    void setUp() {
        apiBase = "http://localhost:" + port + "/api";
        setUpTenants();
    }

    private void setUpTenants() {
        // Register Tenant A
        String emailA = "admin.a." + UUID.randomUUID() + "@tenanta.com";
        Map<String, String> regA = Map.of(
            "centerName",    "Tenant A Center",
            "adminEmail",    emailA,
            "adminPassword", "Password123!",
            "adminFullName", "Admin A"
        );
        ResponseEntity<Map> respA = postJson("/auth/register-tenant", regA, null, Map.class);
        assertThat(respA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        tokenA   = respA.getBody().get("accessToken").toString();
        Map<?, ?> userA = (Map<?, ?>) respA.getBody().get("user");
        tenantAId   = userA.get("tenantId").toString();
        teacherAId  = userA.get("id").toString();

        // Register Tenant B
        String emailB = "admin.b." + UUID.randomUUID() + "@tenantb.com";
        Map<String, String> regB = Map.of(
            "centerName",    "Tenant B Center",
            "adminEmail",    emailB,
            "adminPassword", "Password123!",
            "adminFullName", "Admin B"
        );
        ResponseEntity<Map> respB = postJson("/auth/register-tenant", regB, null, Map.class);
        assertThat(respB.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        tokenB = respB.getBody().get("accessToken").toString();
        Map<?, ?> userB = (Map<?, ?>) respB.getBody().get("user");
        String teacherBId = userB.get("id").toString();

        // Create a class in Tenant B (using Tenant B's token)
        Map<String, Object> classBody = Map.of(
            "name",           "Tenant B Math",
            "teacherId",      teacherBId,
            "ratePerSession", 100000
        );
        ResponseEntity<Map> classResp = postJson("/classes", classBody, tokenB, Map.class);
        assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // POST /api/classes returns ApiResponse<ClassDto>; id is under "data"
        Map<?, ?> classBData = classResp.getBody().containsKey("data")
            ? (Map<?, ?>) classResp.getBody().get("data")
            : classResp.getBody();
        classBId = classBData.get("id").toString();
    }

    // -----------------------------------------------------------------------
    // Tenant A cannot GET Tenant B's class — expects 404 (not 403)
    // -----------------------------------------------------------------------

    @Test
    void tenantA_cannotGetTenantBClass_receives404() {
        ResponseEntity<Map> response = getWithAuth("/classes/" + classBId, tokenA, Map.class);

        // The system should return 404 (not found in A's tenant scope),
        // NOT 403 (forbidden) — because cross-tenant leakage even of the 403
        // error code reveals the resource exists, which is a security risk.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -----------------------------------------------------------------------
    // Tenant A cannot list students in Tenant B's class
    // -----------------------------------------------------------------------

    @Test
    void tenantA_cannotListStudentsInTenantBClass_receives404() {
        ResponseEntity<Map> response = getWithAuth(
            "/classes/" + classBId + "/students", tokenA, Map.class);

        // Either 404 (class not found in A's tenant scope) or empty — must never
        // return Tenant B's student data.
        assertThat(response.getStatusCode())
            .isIn(HttpStatus.NOT_FOUND, HttpStatus.OK);

        // If it somehow returned 200, the data must be empty
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object data = response.getBody().get("data");
            if (data instanceof Iterable<?> list) {
                assertThat(list).isEmpty();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Tenant A can still access its own resources
    // -----------------------------------------------------------------------

    @Test
    void tenantA_canListItsOwnClasses_receives200() {
        // Create a class for Tenant A
        Map<String, Object> classBody = Map.of(
            "name",           "Tenant A Math",
            "teacherId",      teacherAId,
            "ratePerSession", 120000
        );
        ResponseEntity<Map> classResp = postJson("/classes", classBody, tokenA, Map.class);
        assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> classAData = classResp.getBody().containsKey("data")
            ? (Map<?, ?>) classResp.getBody().get("data")
            : classResp.getBody();
        String classAId = classAData.get("id").toString();

        // Tenant A can retrieve its own class
        ResponseEntity<Map> response = getWithAuth("/classes/" + classAId, tokenA, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // GET /api/classes/{id} wraps ClassDto in ApiResponse — check the data envelope
        Map<?, ?> data = response.getBody().containsKey("data")
            ? (Map<?, ?>) response.getBody().get("data")
            : response.getBody();
        assertThat(data.get("id").toString()).isEqualTo(classAId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private <T> ResponseEntity<T> postJson(String path, Object body, String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return restTemplate.postForEntity(apiBase + path, new HttpEntity<>(body, headers), responseType);
    }

    private <T> ResponseEntity<T> getWithAuth(String path, String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange(apiBase + path, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }
}
