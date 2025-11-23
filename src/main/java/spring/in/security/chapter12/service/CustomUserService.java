package spring.in.security.chapter12.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spring.in.security.chapter12.entity.CustomUser;
import spring.in.security.chapter12.repository.CustomUserRepository;

@Service
public class CustomUserService implements UserDetailsService {

    @Autowired
    private CustomUserRepository userRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check if account is locked due to too many failed login attempts
        if (loginAttemptService.isBlocked(username)) {
            throw new LockedException("Your account has been locked due to too many failed login attempts. Please try again after 24 hours.");
        }

        CustomUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .roles("USER")
                .build();
    }
}
