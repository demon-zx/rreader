package ru.terentjev.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtil {
    private SecurityUtil() {
    }

    /**
     * Возвращает имя текущего аутентифицированного пользователя.
     */
    public static Optional<String> user() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }

    public static String userRequired() {
        return user().orElseThrow(() -> new BadCredentialsException("User not found"));
    }
}
