
package com.arovia.api.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.arovia.api.organization.application.CreateOrganizationCommand;
import com.arovia.api.organization.application.OrganizationProvisioningResult;
import com.arovia.api.organization.application.OrganizationProvisioningService;
import com.arovia.api.testing.PostgresIntegrationTestSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "arovia.scheduling.enabled=false"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
class CustomerSecurityIntegrationTest {

    private static final String CUSTOMER_ENDPOINT =
            "/api/v1/customers";

    private static final String CSRF_ENDPOINT =
            "/api/v1/identity/csrf";

    private static final String LOGIN_ENDPOINT =
            "/api/v1/identity/sessions";

    private static final String ACTIVE_ORGANIZATION_ENDPOINT =
            "/api/v1/organizations/active";

    private static final String SESSION_COOKIE_NAME =
            "AROVIA_SESSION";

    private static final String CSRF_HEADER_NAME =
            "X-CSRF-TOKEN";

    private static final String TEST_PASSWORD =
            "Integration-Test-Password-2026!";

    private static final String CUSTOMER_REQUEST = """
            {
              "customerType": "BUSINESS",
              "displayName": "Security Test Customer",
              "legalName": "Security Test Customer Private Limited",
              "email": "security-customer@example.com",
              "phone": "+919876543210",
              "notes": "Created by HTTP security integration test"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrganizationProvisioningService provisioningService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper objectMapper;

    @DynamicPropertySource
    static void registerIntegrationTestProperties(
            DynamicPropertyRegistry registry
    ) {
        PostgresIntegrationTestSupport.registerWebProperties(
                registry,
                "customer-security-integration-v1"
        );
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE organization CASCADE"
        );

        jdbcTemplate.execute(
                "TRUNCATE TABLE user_account CASCADE"
        );
    }

    @Test
    void rejectsUnauthenticatedCustomerListRequest()
            throws Exception {

        mockMvc.perform(
                        get(CUSTOMER_ENDPOINT)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresOrganizationContextForAuthenticatedUser()
            throws Exception {

        UUID userId = createVerifiedUser(
                "no-organization@example.com"
        );

        AuthenticatedSession session =
                createAuthenticatedSession(userId);

        mockMvc.perform(
                        get(CUSTOMER_ENDPOINT)
                                .cookie(session.cookie())
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("ORGANIZATION_CONTEXT_REQUIRED")
                );
    }

    @Test
    void allowsAuthenticatedMemberToListCustomers()
            throws Exception {

        UUID userId = createVerifiedUser(
                "active-member@example.com"
        );

        OrganizationProvisioningResult organization =
                createOrganization(
                        userId,
                        "Security Test Organization"
                );

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        organization.organizationId()
                );

        mockMvc.perform(
                        get(CUSTOMER_ENDPOINT)
                                .cookie(session.cookie())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsCustomerCreationWithoutCsrfToken()
            throws Exception {

        UUID userId = createVerifiedUser(
                "csrf-member@example.com"
        );

        OrganizationProvisioningResult organization =
                createOrganization(
                        userId,
                        "CSRF Test Organization"
                );

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        organization.organizationId()
                );

        mockMvc.perform(
                        post(CUSTOMER_ENDPOINT)
                                .cookie(session.cookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUSTOMER_REQUEST)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsCustomerCreationWithCsrfToken()
            throws Exception {

        UUID userId = createVerifiedUser(
                "customer-creator@example.com"
        );

        OrganizationProvisioningResult organization =
                createOrganization(
                        userId,
                        "Customer Creation Organization"
                );

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        organization.organizationId()
                );

        CsrfState csrfState = obtainCsrfToken(
                session.cookie()
        );

        mockMvc.perform(
                        post(CUSTOMER_ENDPOINT)
                                .cookie(csrfState.cookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        csrfState.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUSTOMER_REQUEST)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.customerNumber")
                                .value("CUS-000001")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Security Test Customer")
                );
    }

    @Test
    void preventsReadingCustomerFromAnotherOrganization()
            throws Exception {

        UUID userId = createVerifiedUser(
                "cross-organization-reader@example.com"
        );

        OrganizationProvisioningResult firstOrganization =
                createOrganization(
                        userId,
                        "First Security Organization"
                );

        OrganizationProvisioningResult secondOrganization =
                createOrganization(
                        userId,
                        "Second Security Organization"
                );

        UUID customerId = createCustomer(
                userId,
                firstOrganization.organizationId()
        );

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        secondOrganization.organizationId()
                );

        mockMvc.perform(
                        get(
                                CUSTOMER_ENDPOINT + "/{customerId}",
                                customerId
                        )
                                .cookie(session.cookie())
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void preventsUpdatingCustomerFromAnotherOrganization()
            throws Exception {

        UUID userId = createVerifiedUser(
                "cross-organization-updater@example.com"
        );

        OrganizationProvisioningResult firstOrganization =
                createOrganization(
                        userId,
                        "First Update Organization"
                );

        OrganizationProvisioningResult secondOrganization =
                createOrganization(
                        userId,
                        "Second Update Organization"
                );

        UUID customerId = createCustomer(
                userId,
                firstOrganization.organizationId()
        );

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        secondOrganization.organizationId()
                );

        CsrfState csrfState = obtainCsrfToken(
                session.cookie()
        );

        String updateRequest = """
                {
                  "customerType": "BUSINESS",
                  "displayName": "Unauthorized Update",
                  "legalName": null,
                  "email": null,
                  "phone": null,
                  "notes": null
                }
                """;

        mockMvc.perform(
                        put(
                                CUSTOMER_ENDPOINT + "/{customerId}",
                                customerId
                        )
                                .cookie(csrfState.cookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        csrfState.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest)
                )
                .andExpect(status().isNotFound());

        assertCustomerName(
                firstOrganization.organizationId(),
                customerId,
                "Security Test Customer"
        );
    }

    @Test
    void preventsArchivingCustomerFromAnotherOrganization()
            throws Exception {

        UUID userId = createVerifiedUser(
                "cross-organization-archiver@example.com"
        );

        OrganizationProvisioningResult firstOrganization =
                createOrganization(
                        userId,
                        "First Archive Organization"
                );

        OrganizationProvisioningResult secondOrganization =
                createOrganization(
                        userId,
                        "Second Archive Organization"
                );

        UUID customerId = createCustomer(
                userId,
                firstOrganization.organizationId()
        );

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        secondOrganization.organizationId()
                );

        CsrfState csrfState = obtainCsrfToken(
                session.cookie()
        );

        mockMvc.perform(
                        post(
                                CUSTOMER_ENDPOINT + "/{customerId}/archive",
                                customerId
                        )
                                .cookie(csrfState.cookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        csrfState.token()
                                )
                )
                .andExpect(status().isNotFound());

        assertCustomerStatus(
                firstOrganization.organizationId(),
                customerId,
                "ACTIVE"
        );
    }

    private UUID createVerifiedUser(String email) {
        UUID userId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO user_account (
                    id,
                    email,
                    normalized_email,
                    password_hash,
                    display_name,
                    status,
                    email_verified_at
                )
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                """,
                userId,
                email,
                email.toLowerCase(),
                passwordEncoder.encode(TEST_PASSWORD),
                "Customer Security Integration Test"
        );

        return userId;
    }

    private OrganizationProvisioningResult createOrganization(
            UUID ownerUserId,
            String name
    ) {
        return provisioningService.createOrganization(
                ownerUserId,
                true,
                new CreateOrganizationCommand(
                        name,
                        null,
                        null
                )
        );
    }

    private AuthenticatedSession createAuthenticatedSession(
            UUID userId
    ) throws Exception {
        return createAuthenticatedSession(userId, null);
    }

    private AuthenticatedSession createAuthenticatedSession(
            UUID userId,
            UUID organizationId
    ) throws Exception {

        /*
         * Obtain a real CSRF token and server-managed session cookie.
         */
        CsrfState initialCsrf = obtainCsrfToken(null);

        String email = jdbcTemplate.queryForObject(
                """
                SELECT email
                FROM user_account
                WHERE id = ?
                """,
                String.class,
                userId
        );

        String loginRequest = objectMapper.writeValueAsString(
                new LoginRequest(email, TEST_PASSWORD)
        );

        /*
         * Authenticate through the application's actual login endpoint.
         * Spring Session manages the session and session-fixation rotation.
         */
        MvcResult loginResult = mockMvc.perform(
                        post(LOGIN_ENDPOINT)
                                .cookie(initialCsrf.cookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        initialCsrf.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie authenticatedCookie = cookieFromResponse(
                loginResult,
                initialCsrf.cookie()
        );

        /*
         * The login endpoint invalidates the old CSRF token.
         * Obtain a fresh token using the authenticated session.
         */
        if (organizationId != null) {
            CsrfState organizationCsrf = obtainCsrfToken(
                    authenticatedCookie
            );

            String selectionRequest = objectMapper.writeValueAsString(
                    new SelectOrganizationRequest(organizationId)
            );

            MvcResult selectionResult = mockMvc.perform(
                            put(ACTIVE_ORGANIZATION_ENDPOINT)
                                    .cookie(organizationCsrf.cookie())
                                    .header(
                                            CSRF_HEADER_NAME,
                                            organizationCsrf.token()
                                    )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(selectionRequest)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            authenticatedCookie = cookieFromResponse(
                    selectionResult,
                    organizationCsrf.cookie()
            );
        }

        return new AuthenticatedSession(authenticatedCookie);
    }

    private CsrfState obtainCsrfToken(
            Cookie existingCookie
    ) throws Exception {

        var requestBuilder = get(CSRF_ENDPOINT);

        if (existingCookie != null) {
            requestBuilder.cookie(existingCookie);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String returnedHeaderName = response
                .path("headerName")
                .asText();

        if (!CSRF_HEADER_NAME.equals(returnedHeaderName)) {
            throw new IllegalStateException(
                    "Unexpected CSRF header name: "
                            + returnedHeaderName
            );
        }

        String token = response.path("token").asText();

        if (token.isBlank()) {
            throw new IllegalStateException(
                    "The CSRF endpoint returned an empty token."
            );
        }

        Cookie cookie = cookieFromResponse(
                result,
                existingCookie
        );

        return new CsrfState(cookie, token);
    }

    private Cookie cookieFromResponse(
            MvcResult result,
            Cookie existingCookie
    ) {

        List<String> setCookieHeaders =
                result.getResponse().getHeaders(
                        HttpHeaders.SET_COOKIE
                );

        for (String header : setCookieHeaders) {
            String prefix = SESSION_COOKIE_NAME + "=";

            if (!header.startsWith(prefix)) {
                continue;
            }

            String cookieValue = header.substring(
                    prefix.length(),
                    header.indexOf(';') >= 0
                            ? header.indexOf(';')
                            : header.length()
            );

            if (cookieValue.isBlank()) {
                return null;
            }

            return new Cookie(
                    SESSION_COOKIE_NAME,
                    cookieValue
            );
        }

        if (existingCookie != null) {
            return existingCookie;
        }

        throw new IllegalStateException(
                "The response did not contain the "
                        + SESSION_COOKIE_NAME
                        + " session cookie."
        );
    }

    private UUID createCustomer(
            UUID userId,
            UUID organizationId
    ) throws Exception {

        AuthenticatedSession session =
                createAuthenticatedSession(
                        userId,
                        organizationId
                );

        CsrfState csrfState = obtainCsrfToken(
                session.cookie()
        );

        MvcResult result = mockMvc.perform(
                        post(CUSTOMER_ENDPOINT)
                                .cookie(csrfState.cookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        csrfState.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUSTOMER_REQUEST)
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String customerId = response.path("id").asText();

        if (customerId.isBlank()) {
            throw new IllegalStateException(
                    "Customer creation response did not contain an id."
            );
        }

        return UUID.fromString(customerId);
    }

    private void assertCustomerName(
            UUID organizationId,
            UUID customerId,
            String expectedName
    ) {
        String actualName = jdbcTemplate.queryForObject(
                """
                SELECT display_name
                FROM customer
                WHERE organization_id = ?
                  AND id = ?
                """,
                String.class,
                organizationId,
                customerId
        );

        assertEquals(expectedName, actualName);
    }

    private void assertCustomerStatus(
            UUID organizationId,
            UUID customerId,
            String expectedStatus
    ) {
        String actualStatus = jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM customer
                WHERE organization_id = ?
                  AND id = ?
                """,
                String.class,
                organizationId,
                customerId
        );

        assertEquals(expectedStatus, actualStatus);
    }

    private record LoginRequest(
            String email,
            String password
    ) {
    }

    private record SelectOrganizationRequest(
            UUID organizationId
    ) {
    }

    private record AuthenticatedSession(
            Cookie cookie
    ) {
    }

    private record CsrfState(
            Cookie cookie,
            String token
    ) {
    }
}
