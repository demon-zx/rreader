package ru.terentjev.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.repository.UserRepository;
import ru.terentjev.repository.model.UserEntity;
import ru.terentjev.service.mapper.UserMapper;
import ru.terentjev.service.model.User;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements FirstRunService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public static final String TYPE_NAME = User.class.getSimpleName();

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(UserMapper.mapper::from);
    }

    public User findById(UUID id) throws InvalidArgumentException {
        return repository.findById(id)
                .map(UserMapper.mapper::from)
                .orElseThrow(() -> new InvalidArgumentException(
                        MessageFormat.format("Not found {1} with id: {0}", id, TYPE_NAME)
                ));
    }

    @Transactional
    public User save(User user) {
        UserEntity entity = UserMapper.mapper.to(user);
        return UserMapper.mapper.from(repository.save(entity));
    }

    @Transactional
    public void changePassword(String username, String newPassword) throws InvalidArgumentException {
        UserEntity user = repository.findByUsername(username)
                .orElseThrow(() -> new InvalidArgumentException(
                        MessageFormat.format("User not found: {0}", username)
                ));

        UserEntity updatedUser = new UserEntity(
                user.id(),
                user.username(),
                passwordEncoder.encode(newPassword),
                user.admin(),
                user.enabled(),
                false
        );

        repository.save(updatedUser);
    }

    public boolean isCompromised(String username) {
        return repository.findByUsername(username)
                .map(UserEntity::compromised)
                .orElse(false);
    }

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    @Override
    @Transactional
    public void firstRun() {
        long userCount = repository.count();
        if (userCount == 0) {
            log.info("No users found. Creating default admin user...");

            UserEntity adminUser = new UserEntity(
                    null,
                    DEFAULT_ADMIN_USERNAME,
                    passwordEncoder.encode("admin"),
                    true,
                    true,
                    true
            );

            repository.save(adminUser);
            log.info("Default admin user created with username: '{}'", DEFAULT_ADMIN_USERNAME);
        } else {
            log.debug("Users already exist in database, skipping admin creation");
        }
    }
}
