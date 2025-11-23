package spring.in.security.chapter12.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int ATTEMPT_EXPIRY_HOURS = 24;

    private final LoadingCache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(ATTEMPT_EXPIRY_HOURS, TimeUnit.HOURS)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    /**
     * Record a failed login attempt for the given username
     */
    public void loginFailed(String username) {
        int attempts = 0;
        try {
            attempts = attemptsCache.get(username);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(username, attempts);
    }

    /**
     * Clear login attempts for the given username (called on successful login)
     */
    public void loginSucceeded(String username) {
        attemptsCache.invalidate(username);
    }

    /**
     * Check if the account is locked (has exceeded max attempts)
     */
    public boolean isBlocked(String username) {
        try {
            return attemptsCache.get(username) >= MAX_ATTEMPTS;
        } catch (ExecutionException e) {
            return false;
        }
    }

    /**
     * Get the number of failed attempts for a username
     */
    public int getAttempts(String username) {
        try {
            return attemptsCache.get(username);
        } catch (ExecutionException e) {
            return 0;
        }
    }

    /**
     * Get remaining attempts before account is locked
     */
    public int getRemainingAttempts(String username) {
        int attempts = getAttempts(username);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}
