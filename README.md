# Spring Security in Action - Chapter 6

This project demonstrates user registration and authentication using Spring Security 6, Thymeleaf, and Liquibase.

## Features

- User registration with validation
- Custom login page
- Spring Security 6 configuration
- Thymeleaf templates with Spring Security integration
- jQuery integration via WebJars
- Liquibase database migration
- H2 in-memory database
- BCrypt password encoding

## Technologies

- **Spring Boot 3.5.6**
- **Spring Security 6**
- **Thymeleaf** with Spring Security extras
- **Liquibase** for database migrations
- **H2 Database** (in-memory)
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
│   └── changelog-1.0.yaml               # Database schema changelog
├── templates/
│   ├── add-user.html                    # User registration page
│   ├── home.html                        # Home page
│   └── login.html                       # Login page
├── static/
│   └── css/
│       └── style.css                    # Custom CSS styles
└── application.properties                # Application configuration
```

## Running the Application

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
   - H2 Console (for debugging): http://localhost:8080/h2-console

## Database Configuration

The application uses H2 in-memory database with the following credentials:
- **JDBC URL:** jdbc:h2:mem:testdb
- **Username:** sa
- **Password:** (empty)

Database schema is managed by Liquibase and is automatically created on application startup.

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
- Uses in-memory H2 database for simplicity (not suitable for production)
- Passwords are securely hashed using BCrypt
- All authentication and authorization is handled by Spring Security
