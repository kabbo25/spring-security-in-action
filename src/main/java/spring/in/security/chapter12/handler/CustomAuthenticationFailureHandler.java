package spring.in.security.chapter12.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import spring.in.security.chapter12.service.LoginAttemptService;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");

        // Handle account locked due to too many failed attempts
        if (exception instanceof LockedException) {
            redirectStrategy.sendRedirect(request, response, "/login?locked");
            return;
        }

        // Record failed login attempt for other authentication failures (e.g., bad credentials)
        if (username != null && !username.isEmpty()) {
            loginAttemptService.loginFailed(username);
        }

        redirectStrategy.sendRedirect(request, response, "/login?error");
    }
}
