package ru.terentjev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.repository.UserRepository;
import ru.terentjev.repository.model.UserEntity;
import ru.terentjev.service.model.User;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder);
    }

    @Nested
    class FindByUsername {
        // находит пользователя и возвращает отображённую модель
        @Test
        void returnsUserWhenFound() {
            when(repository.findByUsername("alice")).thenReturn(Optional.of(entity("alice")));
            // act
            Optional<User> result = service.findByUsername("alice");
            // assert
            assertTrue(result.isPresent());
            assertEquals(
                    USER_ID,
                    result.get()
                            .getId()
            );
            assertEquals(
                    "alice",
                    result.get()
                            .getUsername()
            );
        }

        // не находит пользователя и возвращает пустой Optional
        @Test
        void returnsEmptyWhenNotFound() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
            // act
            Optional<User> result = service.findByUsername("ghost");
            // assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class FindById {
        // находит пользователя по id
        @Test
        void returnsUserWhenFound() throws InvalidArgumentException {
            when(repository.findById(USER_ID)).thenReturn(Optional.of(entity("alice")));
            // act
            User result = service.findById(USER_ID);
            // assert
            assertEquals(USER_ID, result.getId());
            assertEquals("alice", result.getUsername());
        }

        // бросает InvalidArgumentException, если пользователь не найден
        @Test
        void throwsWhenNotFound() {
            when(repository.findById(USER_ID)).thenReturn(Optional.empty());
            // act
            String message = assertThrows(
                    InvalidArgumentException.class,
                    () -> service.findById(USER_ID)
            ).getMessage();
            // assert
            assertTrue(message.contains("User"));
            assertTrue(message.contains(USER_ID.toString()));
        }
    }

    @Nested
    class Save {
        // сохраняет пользователя и возвращает сохранённую сущность
        @Test
        void persistsAndReturnsSavedUser() {
            when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity stored = invocation.getArgument(0);
                return new UserEntity(
                        USER_ID,
                        stored.username(),
                        stored.password(),
                        stored.admin(),
                        stored.enabled(),
                        stored.compromised()
                );
            });
            // act
            User result = service.save(user("bob", false));
            // assert
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(repository).save(captor.capture());
            UserEntity actual = captor.getValue();
            assertEquals("bob", actual.username());
            assertFalse(actual.admin());
            assertEquals(USER_ID, result.getId());
            assertEquals("bob", result.getUsername());
        }
    }

    @Nested
    class ChangePassword {
        // заменяет пароль на закодированный и сбрасывает признак скомпрометированности
        @Test
        void savesEncodedPasswordAndResetsCompromised() throws InvalidArgumentException {
            UserEntity existing = new UserEntity(
                    USER_ID,
                    "alice",
                    "old-encrypted",
                    true,
                    true,
                    true
            );
            when(repository.findByUsername("alice")).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
            // act
            service.changePassword("alice", "new-password");
            // assert
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(repository).save(captor.capture());
            UserEntity saved = captor.getValue();
            assertEquals(USER_ID, saved.id());
            assertEquals(existing.username(), saved.username());
            assertEquals("encoded-password", saved.password());
            assertEquals(existing.admin(), saved.admin());
            assertEquals(existing.enabled(), saved.enabled());
            assertFalse(saved.compromised());
        }

        // бросает InvalidArgumentException, если пользователь не найден
        @Test
        void throwsWhenUserNotFound() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
            // act
            InvalidArgumentException e = assertThrows(
                    InvalidArgumentException.class,
                    () -> service.changePassword("ghost", "new-password")
            );
            // assert
            assertTrue(
                    e.getMessage()
                            .contains("ghost")
            );
            verify(repository, never()).save(any(UserEntity.class));
        }
    }

    @Nested
    class IsCompromised {
        // возвращает true, если пользователь скомпрометирован
        @Test
        void returnsTrueWhenCompromised() {
            when(repository.findByUsername("alice")).thenReturn(Optional.of(entity("alice")));
            // act
            boolean result = service.isCompromised("alice");
            // assert
            assertTrue(result);
        }

        // возвращает false, если пользователь не скомпрометирован
        @Test
        void returnsFalseWhenNotCompromised() {
            UserEntity clean = new UserEntity(USER_ID, "bob", "p", false, true, false);
            when(repository.findByUsername("bob")).thenReturn(Optional.of(clean));
            // act
            boolean result = service.isCompromised("bob");
            // assert
            assertFalse(result);
        }

        // возвращает false, если пользователь не найден
        @Test
        void returnsFalseWhenUserNotFound() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
            // act
            boolean result = service.isCompromised("ghost");
            // assert
            assertFalse(result);
        }
    }

    @Nested
    class FirstRun {
        // создаёт администратора по умолчанию при пустой базе
        @Test
        void createsDefaultAdminWhenNoUsers() {
            when(repository.count()).thenReturn(0L);
            when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");
            // act
            service.firstRun();
            // assert
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(repository).save(captor.capture());
            UserEntity saved = captor.getValue();
            assertNull(saved.id());
            assertEquals("admin", saved.username());
            assertEquals("encoded-admin", saved.password());
            assertTrue(saved.admin());
            assertTrue(saved.enabled());
            assertTrue(saved.compromised());
        }

        // не создаёт пользователя, если пользователи уже существуют
        @Test
        void skipsCreationWhenUsersExist() {
            when(repository.count()).thenReturn(1L);
            // act
            service.firstRun();
            // assert
            verify(repository, never()).save(any(UserEntity.class));
        }
    }

    private UserEntity entity(String username) {
        return new UserEntity(
                USER_ID,
                username,
                "encrypted",
                false,
                true,
                true
        );
    }

    private User user(String username, boolean admin) {
        return new User(
                null,
                username,
                "raw-password",
                admin,
                true,
                false
        );
    }
}
