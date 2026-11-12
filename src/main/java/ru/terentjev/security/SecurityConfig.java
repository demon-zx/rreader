package ru.terentjev.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    public static final String ROLE_ADMINISTRATOR = "ADMINISTRATOR";
    public static final String ROLE_READER = "READER";

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler successHandler
    ) throws Exception {
        Set<String> any = new HashSet<>(
                Set.of(
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/webjars/**",
                        "/favicon.*",
                        "/apple-touch-icon.png"
                )
        );
        http
                .csrf(
                        b -> b.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .rememberMe(
                        remember -> remember.key("rreader-remember-me-key")
                                .rememberMeParameter("remember-me")
                                .tokenValiditySeconds(
                                        (int) Duration.ofDays(30)
                                                .toSeconds()
                                )
                )
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(any.toArray(new String[0]))
                                .permitAll()
                                .requestMatchers("/api/v1/admin/**")
                                .hasRole(ROLE_ADMINISTRATOR)
                                .anyRequest()
                                .authenticated()
                )
                .exceptionHandling(
                        e -> e.defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .formLogin(
                        form -> form.loginPage("/login")
                                .loginProcessingUrl("/login")
                                .successHandler(successHandler)
                                .failureUrl("/login?error=true")
                                .permitAll()
                )
                .logout(
                        logout -> logout.logoutSuccessUrl("/login?logout=true")
                                .permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
