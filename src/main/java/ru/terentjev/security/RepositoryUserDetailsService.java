package ru.terentjev.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.terentjev.service.UserService;

import java.util.List;

import static ru.terentjev.security.SecurityConfig.ROLE_ADMINISTRATOR;
import static ru.terentjev.security.SecurityConfig.ROLE_READER;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public RepositoryUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = "ROLE_" +  (user.isAdmin() ? ROLE_ADMINISTRATOR : ROLE_READER);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        return new User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }

}
