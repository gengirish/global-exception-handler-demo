@echo off
REM Global Exception Handler Demo - API Test Script (Windows)
REM This script tests all the exception handling scenarios

echo === Global Exception Handler Demo - API Tests ===
echo Make sure the application is running on http://localhost:8080
echo.

set BASE_URL=http://localhost:8080/api

REM Test 1: Get all users (should work)
echo === Test 1: Get All Users (Success Case) ===
curl -X GET "%BASE_URL%/users"
echo.
echo.

REM Test 2: Get existing user (should work)
echo === Test 2: Get Existing User (Success Case) ===
echo Getting user with ID 1...
curl -X GET "%BASE_URL%/users/1"
echo.
echo.

REM Test 3: ResourceNotFoundException - Non-existent user
echo === Test 3: ResourceNotFoundException (404 Not Found) ===
echo Trying to get user with ID 999 (non-existent)...
curl -X GET "%BASE_URL%/users/999"
echo.
echo.

REM Test 4: IllegalArgumentException - Invalid ID
echo === Test 4: IllegalArgumentException (400 Bad Request) ===
echo Trying to get user with invalid ID (-1)...
curl -X GET "%BASE_URL%/users/-1"
echo.
echo.

REM Test 5: ValidationException - Invalid user creation
echo === Test 5: ValidationException (400 Bad Request) ===
echo Trying to create user with invalid data...
curl -X POST "%BASE_URL%/users" -H "Content-Type: application/json" -d "{\"name\": \"\", \"email\": \"invalid-email\", \"role\": \"\"}"
echo.
echo.

REM Test 6: BusinessLogicException - Duplicate email
echo === Test 6: BusinessLogicException (422 Unprocessable Entity) ===
echo Trying to create user with existing email...
curl -X POST "%BASE_URL%/users" -H "Content-Type: application/json" -d "{\"name\": \"Test User\", \"email\": \"john@example.com\", \"role\": \"USER\"}"
echo.
echo.

REM Test 7: UnauthorizedException - Non-admin accessing permissions
echo === Test 7: UnauthorizedException (403 Forbidden) ===
echo Trying to get permissions for non-admin user (ID 1)...
curl -X GET "%BASE_URL%/users/1/permissions"
echo.
echo.

REM Test 8: Successful permission access for admin
echo === Test 8: Successful Permission Access (Admin User) ===
echo Getting permissions for admin user (ID 2)...
curl -X GET "%BASE_URL%/users/2/permissions"
echo.
echo.

REM Test 9: BusinessLogicException - Delete admin user
echo === Test 9: BusinessLogicException - Delete Admin (422 Unprocessable Entity) ===
echo Trying to delete admin user (ID 2)...
curl -X DELETE "%BASE_URL%/users/2"
echo.
echo.

REM Test 10: Successful user deletion
echo === Test 10: Successful User Deletion ===
echo Deleting regular user (ID 3)...
curl -X DELETE "%BASE_URL%/users/3"
echo.
echo.

REM Test 11: Generic Exception
echo === Test 11: Generic Exception (500 Internal Server Error) ===
echo Triggering generic exception...
curl -X GET "%BASE_URL%/error"
echo.
echo.

REM Test 12: Create valid user
echo === Test 12: Create Valid User (Success Case) ===
echo Creating a new valid user...
curl -X POST "%BASE_URL%/users" -H "Content-Type: application/json" -d "{\"name\": \"Alice Cooper\", \"email\": \"alice@example.com\", \"role\": \"USER\"}"
echo.
echo.

echo === All Tests Completed! ===
echo Check the responses above to see the different exception handling scenarios.
echo Each error response should have a consistent JSON structure with:
echo - timestamp
echo - status (HTTP status code)
echo - error (HTTP status text)
echo - error_code (custom error code)
echo - message (descriptive error message)
echo - path (request path)
echo - field_errors (for validation errors)
echo.
echo Exception handling demo completed successfully!
pause
