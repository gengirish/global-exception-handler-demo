# Global Exception Handler Demo - Maven Project

## Overview

This Maven project demonstrates a comprehensive global exception handling implementation for a REST API using Spring Boot. It showcases centralized exception handling with `@ControllerAdvice`, custom exception classes, and consistent JSON error responses.

## Project Structure

```
global-exception-handler-demo/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── exceptionhandler/
                        └── GlobalExceptionHandlerDemo.java
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **curl** (for testing endpoints)

## How to Run

### 1. Clone/Download the Project

Ensure you have all the project files in your directory.

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

You should see output like:

```
=== Global Exception Handler Demo Started ===
Available endpoints:
GET  /api/users/{id} - Get user by ID (triggers ResourceNotFoundException for non-existent users)
POST /api/users - Create user (triggers ValidationException for invalid data)
GET  /api/users/{id}/permissions - Get user permissions (triggers UnauthorizedException)
GET  /api/error - Trigger generic exception
=====================================
```

### 4. Alternative: Run as JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/global-exception-handler-demo-1.0.0.jar
```

## Testing the Exception Handling

### Sample Data

The application comes pre-loaded with sample users:

- **User ID 1**: John Doe (john@example.com) - Role: USER
- **User ID 2**: Jane Smith (jane@example.com) - Role: ADMIN
- **User ID 3**: Bob Johnson (bob@example.com) - Role: USER

### 1. Test Success Cases

```bash
# Get all users
curl -X GET http://localhost:8080/api/users

# Get existing user
curl -X GET http://localhost:8080/api/users/1

# Get admin user permissions
curl -X GET http://localhost:8080/api/users/2/permissions

# Create valid user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Cooper", "email": "alice@example.com", "role": "USER"}'
```

### 2. Test ResourceNotFoundException (404)

```bash
# Try to get a non-existent user
curl -X GET http://localhost:8080/api/users/999
```

**Expected Response:**

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "status": 404,
  "error": "Not Found",
  "error_code": "RESOURCE_NOT_FOUND",
  "message": "User with ID '999' not found",
  "path": "/api/users/999"
}
```

### 3. Test ValidationException (400)

```bash
# Try to create user with invalid data
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "", "email": "invalid-email", "role": ""}'
```

**Expected Response:**

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "status": 400,
  "error": "Bad Request",
  "error_code": "VALIDATION_ERROR",
  "message": "Validation failed for user creation",
  "path": "/api/users",
  "field_errors": {
    "name": "Name is required and cannot be empty",
    "email": "Email must be a valid email address",
    "role": "Role is required and cannot be empty"
  }
}
```

### 4. Test UnauthorizedException (403)

```bash
# Try to get permissions for a non-admin user
curl -X GET http://localhost:8080/api/users/1/permissions
```

**Expected Response:**

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "status": 403,
  "error": "Forbidden",
  "error_code": "UNAUTHORIZED_ACCESS",
  "message": "Only administrators can view user permissions",
  "path": "/api/users/1/permissions"
}
```

### 5. Test BusinessLogicException (422)

```bash
# Try to delete an admin user
curl -X DELETE http://localhost:8080/api/users/2
```

**Expected Response:**

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "status": 422,
  "error": "Unprocessable Entity",
  "error_code": "BUSINESS_LOGIC_ERROR",
  "message": "Cannot delete admin users. Please change the user role first.",
  "path": "/api/users/2"
}
```

### 6. Test IllegalArgumentException (400)

```bash
# Try to get user with invalid ID
curl -X GET http://localhost:8080/api/users/-1
```

**Expected Response:**

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "status": 400,
  "error": "Bad Request",
  "error_code": "INVALID_ARGUMENT",
  "message": "User ID must be a positive number",
  "path": "/api/users/-1"
}
```

### 7. Test Generic Exception (500)

```bash
# Trigger a generic exception
curl -X GET http://localhost:8080/api/error
```

**Expected Response:**

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "status": 500,
  "error": "Internal Server Error",
  "error_code": "INTERNAL_SERVER_ERROR",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/error"
}
```

## API Endpoints

| Method | Endpoint                      | Description               | Exception Triggered                                 |
| ------ | ----------------------------- | ------------------------- | --------------------------------------------------- |
| GET    | `/api/users`                  | Get all users             | None (success case)                                 |
| GET    | `/api/users/{id}`             | Get user by ID            | ResourceNotFoundException, IllegalArgumentException |
| POST   | `/api/users`                  | Create new user           | ValidationException, BusinessLogicException         |
| DELETE | `/api/users/{id}`             | Delete user               | ResourceNotFoundException, BusinessLogicException   |
| GET    | `/api/users/{id}/permissions` | Get user permissions      | ResourceNotFoundException, UnauthorizedException    |
| GET    | `/api/error`                  | Trigger generic exception | Generic Exception                                   |

## Key Features Demonstrated

### 1. Custom Exception Hierarchy

- **BaseApplicationException**: Abstract base class with error codes and HTTP status
- **ResourceNotFoundException**: For 404 Not Found scenarios
- **ValidationException**: For 400 Bad Request with field-level validation errors
- **UnauthorizedException**: For 403 Forbidden access control
- **BusinessLogicException**: For 422 Unprocessable Entity business rule violations

### 2. Global Exception Handler (@ControllerAdvice)

- Centralized exception handling across the entire application
- Specific handlers for each custom exception type
- Generic handler for unexpected exceptions
- Consistent JSON error response format

### 3. Error Response Structure

All error responses follow a consistent JSON structure:

```json
{
  "timestamp": "yyyy-MM-dd HH:mm:ss",
  "status": 404,
  "error": "Not Found",
  "error_code": "RESOURCE_NOT_FOUND",
  "message": "Descriptive error message",
  "path": "/api/endpoint",
  "field_errors": {
    "fieldName": "Field-specific error message"
  }
}
```

### 4. HTTP Status Codes

- **200 OK**: Successful operations
- **201 Created**: Successful user creation
- **400 Bad Request**: Validation errors, invalid arguments
- **403 Forbidden**: Authorization failures
- **404 Not Found**: Resource not found
- **422 Unprocessable Entity**: Business logic violations
- **500 Internal Server Error**: Unexpected errors

## Maven Commands

```bash
# Clean and compile
mvn clean compile

# Run tests (if any)
mvn test

# Package as JAR
mvn clean package

# Run the application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests during build
mvn clean package -DskipTests
```

## Development Tips

### 1. Hot Reload

The project includes Spring Boot DevTools for automatic restart during development.

### 2. Logging

Check console output for exception details and application logs.

### 3. Port Configuration

Default port is 8080. To change:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### 4. JSON Formatting

Use tools like `jq` for better JSON formatting:

```bash
curl -X GET http://localhost:8080/api/users/999 | jq '.'
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (Windows)
taskkill /PID <PID> /F

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Java Version Issues

Ensure Java 17+ is installed:

```bash
java -version
javac -version
```

### Maven Issues

```bash
# Clean Maven cache
mvn dependency:purge-local-repository

# Reload dependencies
mvn clean install -U
```

## Skills Tested

1. **Centralized Exception Handling**: Using `@ControllerAdvice` to handle exceptions globally
2. **Spring AOP Concepts**: Aspect-oriented programming with exception handling
3. **Custom API Error Response Design**: Consistent JSON error structure
4. **HTTP Status Code Management**: Appropriate status codes for different exception types
5. **RESTful API Best Practices**: Proper REST endpoint design and error handling

## Extension Ideas

1. Add request validation using `@Valid` and `@Validated`
2. Implement internationalization (i18n) for error messages
3. Add database integration with JPA exceptions
4. Create custom validation annotations
5. Add authentication/authorization with JWT
6. Implement audit logging for exceptions
7. Add request/response logging interceptors
8. Create integration tests with TestRestTemplate

This project serves as a comprehensive example of professional exception handling in Spring Boot applications, demonstrating industry best practices for REST API error management.
