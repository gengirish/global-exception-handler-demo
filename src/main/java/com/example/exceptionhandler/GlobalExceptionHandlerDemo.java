package com.example.exceptionhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler Demo - Single File Java Project
 * 
 * This project demonstrates:
 * - Custom exception classes
 * - Global exception handler using @ControllerAdvice
 * - Consistent JSON error responses
 * - REST API endpoints that trigger exceptions
 * - Proper HTTP status codes for different exception types
 * 
 * Skills Tested:
 * - Centralized exception handling
 * - Spring AOP concepts (@ControllerAdvice)
 * - Custom API error response design
 * - RESTful API best practices
 */

// ==================== SPRING BOOT APPLICATION ====================

@SpringBootApplication
public class GlobalExceptionHandlerDemo {
    public static void main(String[] args) {
        SpringApplication.run(GlobalExceptionHandlerDemo.class, args);
        System.out.println("\n=== Global Exception Handler Demo Started ===");
        System.out.println("Available endpoints:");
        System.out.println("GET  /api/users/{id} - Get user by ID (triggers ResourceNotFoundException for non-existent users)");
        System.out.println("POST /api/users - Create user (triggers ValidationException for invalid data)");
        System.out.println("GET  /api/users/{id}/permissions - Get user permissions (triggers UnauthorizedException)");
        System.out.println("GET  /api/error - Trigger generic exception");
        System.out.println("=====================================\n");
    }
}

// ==================== CUSTOM EXCEPTION CLASSES ====================

/**
 * Base custom exception class for application-specific exceptions
 */
abstract class BaseApplicationException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public BaseApplicationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BaseApplicationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

/**
 * Exception thrown when a requested resource is not found
 */
class ResourceNotFoundException extends BaseApplicationException {
    public ResourceNotFoundException(String resourceName, String resourceId) {
        super(
            String.format("%s with ID '%s' not found", resourceName, resourceId),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

/**
 * Exception thrown when validation fails
 */
class ValidationException extends BaseApplicationException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

/**
 * Exception thrown when user is not authorized to access a resource
 */
class UnauthorizedException extends BaseApplicationException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
    }

    public UnauthorizedException() {
        super("Access denied. Insufficient permissions.", "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
    }
}

/**
 * Exception thrown for business logic violations
 */
class BusinessLogicException extends BaseApplicationException {
    public BusinessLogicException(String message) {
        super(message, "BUSINESS_LOGIC_ERROR", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessLogicException(String message, Throwable cause) {
        super(message, "BUSINESS_LOGIC_ERROR", HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}

// ==================== ERROR RESPONSE MODELS ====================

/**
 * Standardized error response structure
 */
class ErrorResponse {
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("path")
    private String path;

    @JsonProperty("field_errors")
    private Map<String, String> fieldErrors;

    // Constructors
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(HttpStatus httpStatus, String errorCode, String message, String path) {
        this();
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
}

// ==================== GLOBAL EXCEPTION HANDLER ====================

/**
 * Global Exception Handler using @ControllerAdvice
 * 
 * This class centralizes exception handling across the entire application.
 * It catches specific custom exceptions and returns consistent JSON error responses
 * with appropriate HTTP status codes.
 */
@ControllerAdvice
@RestController
class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException
     * Returns 404 Not Found with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getHttpStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request)
        );

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle ValidationException
     * Returns 400 Bad Request with validation error details
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getHttpStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request)
        );
        
        // Include field-specific validation errors if available
        if (!ex.getFieldErrors().isEmpty()) {
            errorResponse.setFieldErrors(ex.getFieldErrors());
        }

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle UnauthorizedException
     * Returns 403 Forbidden with access denied details
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getHttpStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request)
        );

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle BusinessLogicException
     * Returns 422 Unprocessable Entity with business logic error details
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(
            BusinessLogicException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getHttpStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request)
        );

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle IllegalArgumentException
     * Returns 400 Bad Request for invalid arguments
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_ARGUMENT",
            ex.getMessage(),
            getPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     * Returns 500 Internal Server Error for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            getPath(request)
        );

        // Log the actual exception for debugging (in real applications)
        System.err.println("Unexpected exception: " + ex.getMessage());
        ex.printStackTrace();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Extract the request path from WebRequest
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}

// ==================== SAMPLE DATA MODELS ====================

/**
 * Simple User model for demonstration
 */
class User {
    private Long id;
    private String name;
    private String email;
    private String role;

    // Constructors
    public User() {}

    public User(Long id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

/**
 * User creation request model
 */
class CreateUserRequest {
    private String name;
    private String email;
    private String role;

    // Constructors
    public CreateUserRequest() {}

    public CreateUserRequest(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

// ==================== SAMPLE REST CONTROLLER ====================

/**
 * Sample REST Controller to demonstrate exception handling
 * 
 * This controller provides endpoints that intentionally trigger different
 * types of exceptions to showcase the global exception handler in action.
 */
@RestController
@RequestMapping("/api")
class UserController {

    // Sample data for demonstration
    private final Map<Long, User> users = new HashMap<>();

    public UserController() {
        // Initialize with sample data
        users.put(1L, new User(1L, "John Doe", "john@example.com", "USER"));
        users.put(2L, new User(2L, "Jane Smith", "jane@example.com", "ADMIN"));
        users.put(3L, new User(3L, "Bob Johnson", "bob@example.com", "USER"));
    }

    /**
     * Get user by ID
     * Throws ResourceNotFoundException if user doesn't exist
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }

        User user = users.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id.toString());
        }

        return ResponseEntity.ok(user);
    }

    /**
     * Create a new user
     * Throws ValidationException if request data is invalid
     */
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        // Validate request
        Map<String, String> fieldErrors = new HashMap<>();
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            fieldErrors.put("name", "Name is required and cannot be empty");
        }
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            fieldErrors.put("email", "Email is required and cannot be empty");
        } else if (!request.getEmail().matches("^(?=.{1,64}@.{4,64}$)(?=.{6,100}$)[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$")) {
            fieldErrors.put("email", "Email must be a valid email address");
        }
        
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            fieldErrors.put("role", "Role is required and cannot be empty");
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("Validation failed for user creation", fieldErrors);
        }

        // Check for duplicate email
        boolean emailExists = users.values().stream()
            .anyMatch(user -> user.getEmail().equalsIgnoreCase(request.getEmail()));
        
        if (emailExists) {
            throw new BusinessLogicException("User with email '" + request.getEmail() + "' already exists");
        }

        // Create new user
        Long newId = users.keySet().stream().max(Long::compareTo).orElse(0L) + 1;
        User newUser = new User(newId, request.getName(), request.getEmail(), request.getRole());
        users.put(newId, newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    /**
     * Get user permissions
     * Throws UnauthorizedException for non-admin users
     */
    @GetMapping("/users/{id}/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(@PathVariable Long id) {
        User user = users.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id.toString());
        }

        // Simulate authorization check
        if (!"ADMIN".equals(user.getRole())) {
            throw new UnauthorizedException("Only administrators can view user permissions");
        }

        Map<String, Object> permissions = new HashMap<>();
        permissions.put("userId", id);
        permissions.put("permissions", new String[]{"READ", "WRITE", "DELETE", "ADMIN"});
        permissions.put("role", user.getRole());

        return ResponseEntity.ok(permissions);
    }

    /**
     * Get all users
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.values());
        response.put("total", users.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to trigger a generic exception for testing
     */
    @GetMapping("/error")
    public ResponseEntity<String> triggerError() {
        throw new RuntimeException("This is a test exception to demonstrate generic error handling");
    }

    /**
     * Delete user by ID
     * Demonstrates business logic exception
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        User user = users.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id.toString());
        }

        // Business rule: Cannot delete admin users
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessLogicException("Cannot delete admin users. Please change the user role first.");
        }

        users.remove(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("deletedUserId", id.toString());
        
        return ResponseEntity.ok(response);
    }
}
