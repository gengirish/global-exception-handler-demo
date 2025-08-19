package com.example.exceptionhandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Test Suite for Global Exception Handler Demo
 * 
 * This single-file test suite covers:
 * - All custom exception types and their handling
 * - HTTP status codes and error response formats
 * - REST API endpoints functionality
 * - Integration testing with TestRestTemplate
 * - JSON response validation
 * - Edge cases and boundary conditions
 * 
 * Test Categories:
 * 1. Exception Handler Tests
 * 2. REST Controller Tests
 * 3. Integration Tests
 * 4. Error Response Format Tests
 * 5. Business Logic Tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GlobalExceptionHandlerDemoTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    // ==================== EXCEPTION HANDLER TESTS ====================

    @Nested
    @DisplayName("Exception Handler Tests")
    class ExceptionHandlerTests {

        @Test
        @DisplayName("Should handle ResourceNotFoundException with 404 status")
        void testResourceNotFoundException() throws Exception {
            // Test non-existent user
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/999", String.class);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(404, jsonResponse.get("status").asInt());
            assertEquals("Not Found", jsonResponse.get("error").asText());
            assertEquals("RESOURCE_NOT_FOUND", jsonResponse.get("error_code").asText());
            assertEquals("User with ID '999' not found", jsonResponse.get("message").asText());
            assertEquals("/api/users/999", jsonResponse.get("path").asText());
            assertNotNull(jsonResponse.get("timestamp"));
        }

        @Test
        @DisplayName("Should handle ValidationException with 400 status and field errors")
        void testValidationException() throws Exception {
            // Create user with invalid data
            String invalidUserJson = """
                {
                    "name": "",
                    "email": "invalid-email",
                    "role": ""
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(invalidUserJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(400, jsonResponse.get("status").asInt());
            assertEquals("Bad Request", jsonResponse.get("error").asText());
            assertEquals("VALIDATION_ERROR", jsonResponse.get("error_code").asText());
            assertEquals("Validation failed for user creation", jsonResponse.get("message").asText());
            
            // Check field errors
            JsonNode fieldErrors = jsonResponse.get("field_errors");
            assertNotNull(fieldErrors);
            assertTrue(fieldErrors.has("name"));
            assertTrue(fieldErrors.has("email"));
            assertTrue(fieldErrors.has("role"));
            assertEquals("Name is required and cannot be empty", fieldErrors.get("name").asText());
            assertEquals("Email must be a valid email address", fieldErrors.get("email").asText());
            assertEquals("Role is required and cannot be empty", fieldErrors.get("role").asText());
        }

        @Test
        @DisplayName("Should handle UnauthorizedException with 403 status")
        void testUnauthorizedException() throws Exception {
            // Try to get permissions for non-admin user (ID 1)
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/1/permissions", String.class);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(403, jsonResponse.get("status").asInt());
            assertEquals("Forbidden", jsonResponse.get("error").asText());
            assertEquals("UNAUTHORIZED_ACCESS", jsonResponse.get("error_code").asText());
            assertEquals("Only administrators can view user permissions", jsonResponse.get("message").asText());
        }

        @Test
        @DisplayName("Should handle BusinessLogicException with 422 status")
        void testBusinessLogicException() throws Exception {
            // Try to create user with duplicate email
            String duplicateUserJson = """
                {
                    "name": "Test User",
                    "email": "john@example.com",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(duplicateUserJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(422, jsonResponse.get("status").asInt());
            assertEquals("Unprocessable Entity", jsonResponse.get("error").asText());
            assertEquals("BUSINESS_LOGIC_ERROR", jsonResponse.get("error_code").asText());
            assertTrue(jsonResponse.get("message").asText().contains("already exists"));
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException with 400 status")
        void testIllegalArgumentException() throws Exception {
            // Try to get user with invalid ID
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/-1", String.class);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(400, jsonResponse.get("status").asInt());
            assertEquals("Bad Request", jsonResponse.get("error").asText());
            assertEquals("INVALID_ARGUMENT", jsonResponse.get("error_code").asText());
            assertEquals("User ID must be a positive number", jsonResponse.get("message").asText());
        }

        @Test
        @DisplayName("Should handle generic Exception with 500 status")
        void testGenericException() throws Exception {
            // Trigger generic exception
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/error", String.class);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(500, jsonResponse.get("status").asInt());
            assertEquals("Internal Server Error", jsonResponse.get("error").asText());
            assertEquals("INTERNAL_SERVER_ERROR", jsonResponse.get("error_code").asText());
            assertEquals("An unexpected error occurred. Please try again later.", jsonResponse.get("message").asText());
        }
    }

    // ==================== REST CONTROLLER TESTS ====================

    @Nested
    @DisplayName("REST Controller Tests")
    class RestControllerTests {

        @Test
        @DisplayName("Should get all users successfully")
        void testGetAllUsers() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertTrue(jsonResponse.has("users"));
            assertTrue(jsonResponse.has("total"));
assertEquals(5, jsonResponse.get("total").asInt());
        }

        @Test
        @DisplayName("Should get existing user by ID successfully")
        void testGetUserById() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/1", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(1, jsonResponse.get("id").asInt());
            assertEquals("John Doe", jsonResponse.get("name").asText());
            assertEquals("john@example.com", jsonResponse.get("email").asText());
            assertEquals("USER", jsonResponse.get("role").asText());
        }

        @Test
        @DisplayName("Should create valid user successfully")
        void testCreateValidUser() throws Exception {
            String validUserJson = """
                {
                    "name": "Alice Cooper",
                    "email": "alice@example.com",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(validUserJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertNotNull(jsonResponse.get("id"));
            assertEquals("Alice Cooper", jsonResponse.get("name").asText());
            assertEquals("alice@example.com", jsonResponse.get("email").asText());
            assertEquals("USER", jsonResponse.get("role").asText());
        }

        @Test
        @DisplayName("Should get admin user permissions successfully")
        void testGetAdminUserPermissions() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/2/permissions", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals(2, jsonResponse.get("userId").asInt());
            assertEquals("ADMIN", jsonResponse.get("role").asText());
            assertTrue(jsonResponse.has("permissions"));
        }

        @Test
        @DisplayName("Should delete regular user successfully")
        void testDeleteRegularUser() throws Exception {
            // First create a user to delete
            String userJson = """
                {
                    "name": "Test Delete User",
                    "email": "delete@example.com",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> createRequest = new HttpEntity<>(userJson, headers);

            ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl + "/users", createRequest, String.class);
            
            JsonNode createdUser = objectMapper.readTree(createResponse.getBody());
            Long userId = createdUser.get("id").asLong();

            // Now delete the user
            ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl + "/users/" + userId,
                HttpMethod.DELETE,
                null,
                String.class
            );

            assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(deleteResponse.getBody());
            assertEquals("User deleted successfully", jsonResponse.get("message").asText());
            assertEquals(userId.toString(), jsonResponse.get("deletedUserId").asText());
        }

        @Test
        @DisplayName("Should not delete admin user")
        void testDeleteAdminUser() throws Exception {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/users/2",
                HttpMethod.DELETE,
                null,
                String.class
            );

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals("BUSINESS_LOGIC_ERROR", jsonResponse.get("error_code").asText());
            assertTrue(jsonResponse.get("message").asText().contains("Cannot delete admin users"));
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete user lifecycle")
        void testCompleteUserLifecycle() throws Exception {
            // 1. Create a new user
            String userJson = """
                {
                    "name": "Integration Test User",
                    "email": "integration@example.com",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> createRequest = new HttpEntity<>(userJson, headers);

            ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl + "/users", createRequest, String.class);
            
            assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
            JsonNode createdUser = objectMapper.readTree(createResponse.getBody());
            Long userId = createdUser.get("id").asLong();

            // 2. Retrieve the created user
            ResponseEntity<String> getResponse = restTemplate.getForEntity(
                baseUrl + "/users/" + userId, String.class);
            
            assertEquals(HttpStatus.OK, getResponse.getStatusCode());
            JsonNode retrievedUser = objectMapper.readTree(getResponse.getBody());
            assertEquals("Integration Test User", retrievedUser.get("name").asText());

            // 3. Try to access permissions (should fail for non-admin)
            ResponseEntity<String> permissionsResponse = restTemplate.getForEntity(
                baseUrl + "/users/" + userId + "/permissions", String.class);
            
            assertEquals(HttpStatus.FORBIDDEN, permissionsResponse.getStatusCode());

            // 4. Delete the user
            ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl + "/users/" + userId,
                HttpMethod.DELETE,
                null,
                String.class
            );
            
            assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

            // 5. Verify user is deleted
            ResponseEntity<String> verifyResponse = restTemplate.getForEntity(
                baseUrl + "/users/" + userId, String.class);
            
            assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
        }

        @Test
        @DisplayName("Should handle multiple validation errors")
        void testMultipleValidationErrors() throws Exception {
            String invalidUserJson = """
                {
                    "name": null,
                    "email": "",
                    "role": null
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(invalidUserJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode fieldErrors = jsonResponse.get("field_errors");
            
            // Should have errors for all three fields
            assertTrue(fieldErrors.has("name"));
            assertTrue(fieldErrors.has("email"));
            assertTrue(fieldErrors.has("role"));
        }

        @Test
        @DisplayName("Should handle edge case user IDs")
        void testEdgeCaseUserIds() throws Exception {
            // Test zero ID
            ResponseEntity<String> zeroResponse = restTemplate.getForEntity(
                baseUrl + "/users/0", String.class);
            assertEquals(HttpStatus.BAD_REQUEST, zeroResponse.getStatusCode());

            // Test negative ID
            ResponseEntity<String> negativeResponse = restTemplate.getForEntity(
                baseUrl + "/users/-5", String.class);
            assertEquals(HttpStatus.BAD_REQUEST, negativeResponse.getStatusCode());

            // Test very large ID
            ResponseEntity<String> largeResponse = restTemplate.getForEntity(
                baseUrl + "/users/999999", String.class);
            assertEquals(HttpStatus.NOT_FOUND, largeResponse.getStatusCode());
        }
    }

    // ==================== ERROR RESPONSE FORMAT TESTS ====================

    @Nested
    @DisplayName("Error Response Format Tests")
    class ErrorResponseFormatTests {

        @Test
        @DisplayName("Should have consistent error response structure")
        void testErrorResponseStructure() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/999", String.class);

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            // Check all required fields are present
            assertTrue(jsonResponse.has("timestamp"));
            assertTrue(jsonResponse.has("status"));
            assertTrue(jsonResponse.has("error"));
            assertTrue(jsonResponse.has("error_code"));
            assertTrue(jsonResponse.has("message"));
            assertTrue(jsonResponse.has("path"));
            
            // Check field types
            assertTrue(jsonResponse.get("timestamp").isTextual());
            assertTrue(jsonResponse.get("status").isInt());
            assertTrue(jsonResponse.get("error").isTextual());
            assertTrue(jsonResponse.get("error_code").isTextual());
            assertTrue(jsonResponse.get("message").isTextual());
            assertTrue(jsonResponse.get("path").isTextual());
        }

        @Test
        @DisplayName("Should include field_errors for validation exceptions")
        void testValidationErrorResponseStructure() throws Exception {
            String invalidUserJson = """
                {
                    "name": "",
                    "email": "invalid",
                    "role": ""
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(invalidUserJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            // Should have field_errors for validation exceptions
            assertTrue(jsonResponse.has("field_errors"));
            assertTrue(jsonResponse.get("field_errors").isObject());
            assertTrue(jsonResponse.get("field_errors").size() > 0);
        }

        @Test
        @DisplayName("Should have proper timestamp format")
        void testTimestampFormat() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/users/999", String.class);

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String timestamp = jsonResponse.get("timestamp").asText();
            
            // Should match pattern: yyyy-MM-dd HH:mm:ss
            assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        }
    }

    // ==================== BUSINESS LOGIC TESTS ====================

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should prevent duplicate email addresses")
        void testDuplicateEmailPrevention() throws Exception {
            // Try to create user with existing email
            String duplicateEmailJson = """
                {
                    "name": "Duplicate Email User",
                    "email": "jane@example.com",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(duplicateEmailJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            assertEquals("BUSINESS_LOGIC_ERROR", jsonResponse.get("error_code").asText());
            assertTrue(jsonResponse.get("message").asText().contains("already exists"));
        }

        @Test
        @DisplayName("Should enforce admin-only permission access")
        void testAdminOnlyPermissionAccess() throws Exception {
            // Test with regular user (should fail)
            ResponseEntity<String> userResponse = restTemplate.getForEntity(
                baseUrl + "/users/1/permissions", String.class);
            assertEquals(HttpStatus.FORBIDDEN, userResponse.getStatusCode());

            // Test with admin user (should succeed)
            ResponseEntity<String> adminResponse = restTemplate.getForEntity(
                baseUrl + "/users/2/permissions", String.class);
            assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        }

        @Test
        @DisplayName("Should validate email format")
        void testEmailFormatValidation() throws Exception {
            String[] invalidEmails = {
                "invalid-email",
                "test@",
                "@example.com",
                "test..test@example.com"
            };

            for (String invalidEmail : invalidEmails) {
                String userJson = String.format("""
                    {
                        "name": "Test User",
                        "email": "%s",
                        "role": "USER"
                    }
                    """, invalidEmail);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> request = new HttpEntity<>(userJson, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/users", request, String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                    "Should reject invalid email: " + invalidEmail);
            }
        }

        @Test
        @DisplayName("Should handle case-insensitive email comparison")
        void testCaseInsensitiveEmailComparison() throws Exception {
            String upperCaseEmailJson = """
                {
                    "name": "Upper Case Email User",
                    "email": "JOHN@EXAMPLE.COM",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(upperCaseEmailJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/users", request, String.class);

            // Should fail because john@example.com already exists (case-insensitive)
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        }

        @Test
        @DisplayName("Should auto-generate sequential user IDs")
        void testSequentialUserIdGeneration() throws Exception {
            // Create first user
            String user1Json = """
                {
                    "name": "Sequential User 1",
                    "email": "seq1@example.com",
                    "role": "USER"
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request1 = new HttpEntity<>(user1Json, headers);

            ResponseEntity<String> response1 = restTemplate.postForEntity(
                baseUrl + "/users", request1, String.class);
            
            JsonNode user1 = objectMapper.readTree(response1.getBody());
            Long user1Id = user1.get("id").asLong();

            // Create second user
            String user2Json = """
                {
                    "name": "Sequential User 2",
                    "email": "seq2@example.com",
                    "role": "USER"
                }
                """;

            HttpEntity<String> request2 = new HttpEntity<>(user2Json, headers);

            ResponseEntity<String> response2 = restTemplate.postForEntity(
                baseUrl + "/users", request2, String.class);
            
            JsonNode user2 = objectMapper.readTree(response2.getBody());
            Long user2Id = user2.get("id").asLong();

            // Second user ID should be greater than first
            assertTrue(user2Id > user1Id, "User IDs should be sequential");
        }
    }

    // ==================== PERFORMANCE AND STRESS TESTS ====================

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle multiple concurrent requests")
        void testConcurrentRequests() throws Exception {
            int numberOfRequests = 10;
            Thread[] threads = new Thread[numberOfRequests];
            boolean[] results = new boolean[numberOfRequests];

            for (int i = 0; i < numberOfRequests; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                            baseUrl + "/users/1", String.class);
                        results[index] = response.getStatusCode() == HttpStatus.OK;
                    } catch (Exception e) {
                        results[index] = false;
                    }
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Check all requests succeeded
            for (boolean result : results) {
                assertTrue(result, "All concurrent requests should succeed");
            }
        }

        @Test
        @DisplayName("Should handle rapid exception generation")
        void testRapidExceptionGeneration() throws Exception {
            int numberOfRequests = 5;
            
            for (int i = 0; i < numberOfRequests; i++) {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/users/999" + i, String.class);
                
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                assertEquals("RESOURCE_NOT_FOUND", jsonResponse.get("error_code").asText());
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper method to create a test user
     */
    private JsonNode createTestUser(String name, String email, String role) throws Exception {
        String userJson = String.format("""
            {
                "name": "%s",
                "email": "%s",
                "role": "%s"
            }
            """, name, email, role);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(userJson, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/users", request, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            return objectMapper.readTree(response.getBody());
        }
        return null;
    }

    /**
     * Helper method to verify error response structure
     */
    private void verifyErrorResponse(JsonNode response, int expectedStatus, String expectedErrorCode) {
        assertEquals(expectedStatus, response.get("status").asInt());
        assertEquals(expectedErrorCode, response.get("error_code").asText());
        assertNotNull(response.get("timestamp"));
        assertNotNull(response.get("message"));
        assertNotNull(response.get("path"));
    }
}
