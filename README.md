# Spring Security in Action - Chapter 6

This project demonstrates user registration and authentication using Spring Security 6, Thymeleaf, and Liquibase.

## Features

- User registration with validation
- Custom login page
- Spring Security 6 configuration
- Thymeleaf templates with Spring Security integration
- jQuery integration via WebJars
- Liquibase database migration with seed data
- PostgreSQL database with environment variable support
- BCrypt password encoding
- Pre-loaded test users for quick testing

## Technologies

- **Spring Boot 3.5.6**
- **Spring Security 6**
- **Thymeleaf** with Spring Security extras
- **Liquibase** for database migrations
- **PostgreSQL Database**
- **jQuery 3.7.1** via WebJars
- **Bootstrap 5.3.3** via WebJars
- **Lombok** for reducing boilerplate code
- **Jakarta Validation** for input validation

## Project Structure

```
src/main/java/com/manning/sbip/ch06/
├── config/
│   └── SecurityConfiguration.java       # Spring Security configuration
├── controller/
│   ├── HomeController.java              # Home page controller
│   ├── LoginController.java             # Login controller
│   └── RegistrationController.java      # User registration controller
├── dto/
│   └── UserDto.java                     # Data transfer object for user registration
├── entity/
│   └── ApplicationUser.java             # JPA entity for users
├── repository/
│   └── UserRepository.java              # Spring Data repository
├── service/
│   ├── CustomUserDetailsService.java    # Custom UserDetailsService implementation
│   ├── UserService.java                 # User service interface
│   └── impl/
│       └── DefaultUserService.java      # User service implementation
└── Chapter6Application.java             # Main application class

src/main/resources/
├── db/changelog/
│   ├── changelog-master.yaml            # Liquibase master changelog
│   ├── changelog-1.0.yaml               # Database schema changelog
│   ├── changelog-2.0.yaml               # Seed data changelog
│   └── users-seed-data.csv              # Test users seed data
├── templates/
│   ├── add-user.html                    # User registration page
│   ├── home.html                        # Home page
│   └── login.html                       # Login page
├── static/
│   └── css/
│       └── style.css                    # Custom CSS styles
├── application.properties                # Application configuration
└── liquibase.properties                  # Liquibase Maven plugin configuration
```

## Running the Application

### Prerequisites
Ensure you have PostgreSQL running and the database created. Set the required environment variables:

```bash
export SSO_USER_DATABASE_URL=jdbc:postgresql://localhost:5432/app
export SSO_USER_DATABASE_USERNAME=sa
export SSO_USER_DATABASE_PASSWORD=Admin@123!
export SSO_USER_DATABASE_DRIVER=org.postgresql.Driver
```

### Running Liquibase Migrations

The project includes Liquibase Maven plugin for database migrations. Run migrations using:

```bash
mvn liquibase:update -P liquibase-user
```

This will:
- Create the CT_USERS table
- Load seed data with test users

### Running the Application

1. **Build the project:**
   ```bash
   mvn clean install
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Access the application:**
   - Home page: http://localhost:8080/
   - Registration page: http://localhost:8080/adduser
   - Login page: http://localhost:8080/login

## Database Configuration

The application uses PostgreSQL database with environment variable support:

### Default Configuration
- **JDBC URL:** jdbc:postgresql://localhost:5432/app
- **Username:** sa
- **Password:** Admin@123!
- **Driver:** org.postgresql.Driver

### Environment Variables (Optional)
You can override the default configuration using these environment variables:
- `SSO_USER_DATABASE_URL` - Database JDBC URL
- `SSO_USER_DATABASE_USERNAME` - Database username
- `SSO_USER_DATABASE_PASSWORD` - Database password
- `SSO_USER_DATABASE_DRIVER` - Database driver class name

### Database Schema and Seed Data
- Database schema is managed by Liquibase and is automatically created on application startup
- Seed data with test users is automatically loaded via Liquibase

### Liquibase Maven Plugin Commands

The project includes Liquibase Maven plugin with the `liquibase-user` profile. Available commands:

```bash
# Apply database changes
mvn liquibase:update -P liquibase-user

# Rollback last change
mvn liquibase:rollback -P liquibase-user -Dliquibase.rollbackCount=1

# Check database status
mvn liquibase:status -P liquibase-user

# Generate SQL for changes (without applying)
mvn liquibase:updateSQL -P liquibase-user

# Clear checksums (use if migration fails)
mvn liquibase:clearCheckSums -P liquibase-user
```

Configuration is managed via `src/main/resources/liquibase.properties`

### Test Users (Pre-loaded)
The following test users are available for login without registration:

| Username   | Password | First Name | Last Name | Email                    |
|------------|----------|------------|-----------|--------------------------|
| testuser   | password | John       | Doe       | john.doe@example.com     |
| janesmith  | password | Jane       | Smith     | jane.smith@example.com   |
| admin      | password | Admin      | User      | admin@example.com        |

All test users use the password: **password**

## Security Configuration

- Public endpoints: `/adduser`, `/login`, `/login-error`
- Protected endpoint: `/` (requires authentication)
- Static resources (`/webjars/**`, `/css/**`, `/images/**`) are publicly accessible
- Password encoding: BCrypt

## User Registration Flow

1. User navigates to `/adduser`
2. Fills out the registration form with validation:
   - First Name (required)
   - Last Name (required)
   - Username (required)
   - Email (required, must be valid email format)
   - Password (required)
   - Confirm Password (required, must match password)
3. jQuery validates password match on the client side
4. Server-side validation using Jakarta Validation annotations
5. Password is encrypted using BCrypt before storing
6. User is redirected to login page upon successful registration

## Login Flow

1. User navigates to `/login`
2. Enters username and password
3. Spring Security authenticates against the database
4. On success, user is redirected to home page (`/`)
5. On failure, user is redirected to `/login-error` with error message

## Key Features Demonstrated

### 1. User Registration with Validation
- DTO pattern for form handling
- Jakarta Validation annotations
- Client-side validation using jQuery
- Server-side validation with BindingResult

### 2. Custom UserDetailsService
- Loads user from database
- Integrates with Spring Security authentication

### 3. Spring Security 6 Configuration
- SecurityFilterChain bean instead of WebSecurityConfigurerAdapter (deprecated)
- Custom login page
- Logout configuration
- Static resource handling

### 4. Thymeleaf Integration
- Form binding with `th:object` and `th:field`
- Error display with `th:errors`
- Spring Security namespace for authentication info
- Parameter-based conditional rendering

### 5. Liquibase Database Migration
- YAML-based changelogs
- Automatic schema creation
- Version control for database changes
- CSV-based seed data loading for test users

### 6. WebJars for Frontend Dependencies
- jQuery served via WebJars
- Bootstrap for responsive UI
- No need to manually download/manage frontend libraries

## Testing

Run tests with:
```bash
mvn test
```

## Notes

- This is a learning project based on "Spring Security in Action" Chapter 6
- Uses PostgreSQL database (ensure PostgreSQL is running on localhost:5432 or configure using environment variables)
- Passwords are securely hashed using BCrypt
- All authentication and authorization is handled by Spring Security
- Test users are pre-loaded via Liquibase for immediate testing
- Database configuration supports environment variables for flexible deployment
