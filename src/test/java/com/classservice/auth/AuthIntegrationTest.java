package com.classservice.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration tests for the auth endpoints against a real database.
 * Requires Docker to be available for Testcontainers PostgreSQL.
 *
 * Tests the full request lifecycle:
 *   POST /api/auth/register-tenant  → 201, creates tenant + admin user
 *   POST /api/auth/login            → 200, returns JWT on valid creds
 *   POST /api/auth/login            → 401, rejects wrong password
 *   GET  /api/auth/me               → 200, returns user profile with valid JWT
 *   GET  /api/auth/me               → 401, rejects missing JWT
 */
@Disabled("Requires Docker for Testcontainers PostgreSQL")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    // Shared state across tests in this class (order-dependent by design)
    private static String registeredTenantId;
    private static String adminJwt;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth";
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/register-tenant → 201
    // -----------------------------------------------------------------------

    @Test
    void registerTenant_withValidData_returns201AndTenantIdAndAdminUser() {
        Map<String, String> body = Map.of(
            "centerName",    "Green Apple Tutoring",
            "adminEmail",    "admin+" + UUID.randomUUID() + "@greenapple.vn",
            "adminPassword", "Secure1234!",
            "adminFullName", "Nguyen Van Admin"
        );

        ResponseEntity<Map> response = postJson("/register-tenant", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        // The response wraps LoginResponse (accessToken + user object)
        assertThat(response.getBody()).containsKey("accessToken");
        Map<?, ?> user = (Map<?, ?>) response.getBody().get("user");
        assertThat(user).isNotNull();
        assertThat(user.get("email")).asString().contains("@greenapple.vn");
        assertThat(user.get("role")).isEqualTo("ADMIN");
        assertThat(user.get("tenantId")).isNotNull();

        // Store for subsequent tests
        registeredTenantId = user.get("tenantId").toString();
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/login → 200 with valid credentials
    // -----------------------------------------------------------------------

    @Test
    void login_withValidCredentials_returns200AndJwt() {
        // First register a fresh tenant so this test is self-contained
        String uniqueEmail = "login.test+" + UUID.randomUUID() + "@example.com";
        Map<String, String> regBody = Map.of(
            "centerName",    "Login Test Center",
            "adminEmail",    uniqueEmail,
            "adminPassword", "Password99!",
            "adminFullName", "Login Tester"
        );
        ResponseEntity<Map> regResp = postJson("/register-tenant", regBody, Map.class);
        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tenantId = ((Map<?, ?>) regResp.getBody().get("user")).get("tenantId").toString();

        // Now login
        Map<String, String> loginBody = Map.of(
            "email",    uniqueEmail,
            "password", "Password99!",
            "tenantId", tenantId
        );

        ResponseEntity<Map> response = postJson("/login", loginBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getBody().get("accessToken")).asString().isNotBlank();

        // Store JWT for the /me test
        adminJwt = response.getBody().get("accessToken").toString();
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/login → 401 with wrong password
    // -----------------------------------------------------------------------

    @Test
    void login_withWrongPassword_returns401() {
        // Register a fresh tenant
        String uniqueEmail = "wrongpwd+" + UUID.randomUUID() + "@example.com";
        Map<String, String> regBody = Map.of(
            "centerName",    "Wrong Pwd Center",
            "adminEmail",    uniqueEmail,
            "adminPassword", "Correct1234!",
            "adminFullName", "Test Admin"
        );
        ResponseEntity<Map> regResp = postJson("/register-tenant", regBody, Map.class);
        String tenantId = ((Map<?, ?>) regResp.getBody().get("user")).get("tenantId").toString();

        Map<String, String> loginBody = Map.of(
            "email",    uniqueEmail,
            "password", "WrongPassword!",
            "tenantId", tenantId
        );

        ResponseEntity<Map> response = postJson("/login", loginBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // -----------------------------------------------------------------------
    // GET /api/auth/me → 200 with valid JWT
    // -----------------------------------------------------------------------

    @Test
    void getMe_withValidJwt_returns200AndUserProfile() {
        // Register + login to obtain a JWT
        String uniqueEmail = "me.test+" + UUID.randomUUID() + "@example.com";
        Map<String, String> regBody = Map.of(
            "centerName",    "Me Test Center",
            "adminEmail",    uniqueEmail,
            "adminPassword", "Password99!",
            "adminFullName", "Me Tester"
        );
        ResponseEntity<Map> regResp = postJson("/register-tenant", regBody, Map.class);
        String jwt = regResp.getBody().get("accessToken").toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt);

        ResponseEntity<Map> response = restTemplate.exchange(
            baseUrl + "/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();

        // The controller wraps UserDto in ApiResponse<UserDto>
        // Check top-level or data envelope depending on ApiResponse shape
        Object data = body.containsKey("data") ? ((Map<?, ?>) body.get("data")) : body;
        Map<?, ?> dataMap = (Map<?, ?>) data;
        assertThat(dataMap.get("email")).isEqualTo(uniqueEmail);
        assertThat(dataMap.get("role")).isEqualTo("ADMIN");
    }

    // -----------------------------------------------------------------------
    // GET /api/auth/me → 401 without JWT
    // -----------------------------------------------------------------------

    @Test
    void getMe_withoutJwt_returns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/me", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private <T> ResponseEntity<T> postJson(String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(baseUrl + path, new HttpEntity<>(body, headers), responseType);
    }
}
