package org.example.dao;

import org.example.domain.User;
import org.hibernate.*;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDaoImplTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18").withDatabaseName("userdb").withUsername("userapp").withPassword("password");

    private SessionFactory sessionFactory;
    private UserDaoImpl userDao;

    @BeforeAll
    void setUpAll() {
        sessionFactory = buildSessionFactory();
        userDao = new UserDaoImpl(sessionFactory);

        try (Session s = sessionFactory.openSession()) {
            s.beginTransaction();
            s.createNativeMutationQuery(
                    "CREATE UNIQUE INDEX IF NOT EXISTS users_email_ci_uidx ON users (lower(email))"
            ).executeUpdate();
            s.createNativeMutationQuery(
                    "ALTER TABLE users ALTER COLUMN email SET NOT NULL"
            ).executeUpdate();
            s.getTransaction().commit();
        }
    }

    @AfterAll
    void tearDownAll() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    void cleanDb() {
        try (Session s = sessionFactory.openSession()) {
            Transaction transaction = s.beginTransaction();
            s.createNativeMutationQuery(
                    "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
            ).executeUpdate();
            transaction.commit();
        }
    }

    @Test
    void createUser() {
        for (Long i = 1L; i < 10; i++) {
            User user = userDao.create(new User("name", i + "user@mail.ru", 20));
            assertThat(user.getId()).isEqualTo(i);
        }
    }

    @Test
    void createUserUniqueEmail() {
        User u1 = new User("name", "user@mail.ru", 20);
        User u2 = new User("name", "user@mail.ru", 20);
        userDao.create(u1);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userDao.create(u2));

        assertEquals("That email is already used", exception.getMessage());
    }

    @Test
    void createUserNullEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userDao.create(new User("name", null, 123)));

        assertEquals("That field can't be empty", exception.getMessage());
    }

    @Test
    void createAndReadUser() {
        User saved = userDao.create(new User("name", "name@mail.ru", 20));
        assertThat(saved.getId()).isEqualTo(1L);

        User found = userDao.read(saved.getId());
        assertThat(found).extracting(User::getName, User::getEmail, User::getAge).containsExactly("name", "name@mail.ru", 20);
    }

    @Test
    void readNotExistingUser() {
        User saved = userDao.create(new User("name", "name@mail.ru", 20));
        assertThat(saved.getId()).isEqualTo(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userDao.read(12L));
        assertEquals("User with id={" + 12L + "} is not existed", exception.getMessage());
    }

    @Test
    void createLongEmailJDBCException() {
        String sb = "mail".repeat(100) + "@mail.ru";
        User user = new User("name", sb, 20);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userDao.create(user));

        assertEquals("Database error while creating user", exception.getMessage());
    }

    @Test
    void updateUser() {
        User saved = userDao.create(new User("old", "old@mail.ru", 12));
        saved.setName("new");
        saved.setEmail("new@mail.ru");
        saved.setAge(123);
        User updated = userDao.update(saved);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getName()).isEqualTo("new");
        assertThat(updated.getEmail()).isEqualTo("new@mail.ru");
        assertThat(updated.getAge()).isEqualTo(123);

        User reread = userDao.read(saved.getId());
        assertThat(reread.getName()).isEqualTo("new");
        assertThat(reread.getEmail()).isEqualTo("new@mail.ru");
        assertThat(reread.getAge()).isEqualTo(123);
    }

    @Test
    void updateDuplicateEmail() {
        userDao.create(new User("old", "old@mail.ru", 12));
        User updated = userDao.create(new User("new", "new@mail.ru", 123));
        updated.setEmail("old@mail.ru");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userDao.update(updated));

        assertEquals("That email is already used", exception.getMessage());

    }

    @Test
    void deleteById() {
        User user = userDao.create(new User("name", "name@mail.ru", 12));
        userDao.deleteById(user.getId());
        assertThat(user.getId()).isEqualTo(1L);

        Long id = user.getId();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userDao.read(id));

        assertEquals("User with id={1} is not existed", exception.getMessage());
    }

    @Test
    void deleteByNotExistedId() {
        userDao.create(new User("name", "name@mail.ru", 12));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userDao.deleteById(123L));

        assertEquals("User with id={123} is not existed", exception.getMessage());
    }

    @Test
    void deleteByEmail() {
        User user1 = userDao.create(new User("name", "name@mail.ru", 12));
        User user2 = userDao.create(new User("name", "name1@mail.ru", 12));

        assertThat(userDao.read(1L).getId()).isEqualTo(user1.getId());
        userDao.deleteByEmail("name@mail.ru");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userDao.read(1L));
        assertEquals("User with id={1} is not existed", exception.getMessage());
        assertThat(userDao.read(2L).getId()).isEqualTo(user2.getId());
    }

    @Test
    void mailUniqueCheck() {
        assertThat(userDao.mailUniqueCheck("mail@mail.ru")).isTrue();
        userDao.create(new User("name", "mail@mail.ru", 12));
        assertThat(userDao.mailUniqueCheck("mail@mail.ru")).isFalse();
        assertThat(userDao.mailUniqueCheck("newmail@mail.ru")).isTrue();
    }


    private SessionFactory buildSessionFactory() {
        Configuration configuration = new Configuration();

        configuration.addAnnotatedClass(org.example.domain.User.class);
        configuration.setProperty("hibernate.connection.url", postgres.getJdbcUrl());
        configuration.setProperty("hibernate.connection.username", postgres.getUsername());
        configuration.setProperty("hibernate.connection.password", postgres.getPassword());
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setProperty("jakarta.persistence.validation.mode", "none");
        configuration.setProperty("hibernate.check_nullability", "false");

        ServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        return configuration.buildSessionFactory(registry);
    }
}
