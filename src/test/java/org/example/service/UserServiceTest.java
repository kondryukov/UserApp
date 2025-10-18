package org.example.service;

import org.example.dao.UserDaoImpl;
import org.example.domain.User;
import org.example.dao.UserDao;

import org.hibernate.HibernateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    UserDao userDao;
    UserService service;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDaoImpl.class);
        service = new UserService((UserDaoImpl) userDao);
    }

    @Test
    void saveUserTest() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(true);
        User u = service.saveUser("name", "asdf@mail.ru", 12);
        assertThat(u.getName()).isEqualTo("name");
        assertThat(u.getEmail()).isEqualTo("asdf@mail.ru");
        assertThat(u.getAge()).isEqualTo(12);
    }

    @Test
    void saveUserDuplicateEmailTest() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.saveUser("name", "asdf@mail.ru", 12));
    }

    @Test
    void saveUserIllegalArgumentExceptionTest() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(true);
        when(userDao.create(any())).thenThrow(IllegalStateException.class);
        Assertions.assertThrows(IllegalStateException.class, () -> service.saveUser("name", "asdf@mail.ru", 12));
    }

    @Test
    void saveUserHibernateExceptionTest() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(true);
        when(userDao.create(any(User.class))).thenThrow(new HibernateException("Hibernate exception"));
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> service.saveUser("name", "asdf@mail.ru", 12));
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof HibernateException);
    }

    @Test
    void saveUserInvalidEmailThrows() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.saveUser("name", "exampleOfInvalidEmail", 12));
        verify(userDao, never()).create(any());
    }

    @Test
    void saveUserNormalizedEmail() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(true);
        User u = service.saveUser("name", "ASDF@mail.ru", 12);
        assertThat(u.getName()).isEqualTo("name");
        assertThat(u.getEmail()).isEqualTo("asdf@mail.ru");
        assertThat(u.getAge()).isEqualTo(12);
    }

    @Test
    void saveUserWithoutEmailTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.saveUser("name", "", 12));
    }

    @Test
    void updateUser_changesOnlyProvidedFields_andChecksEmailUniq() {
        User existing = new User("Old", "old@ex.com", 25);
        existing.setId(2L);
        when(userDao.read(2L)).thenReturn(existing);
        when(userDao.mailUniqueCheck("new@ex.com")).thenReturn(true);
        service.updateUser(2L, "New", "new@ex.com", 26);
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userDao).update(cap.capture());
        User u = cap.getValue();
        assertThat(u.getId()).isEqualTo(2L);
        assertThat(u.getName()).isEqualTo("New");
        assertThat(u.getEmail()).isEqualTo("new@ex.com");
        assertThat(u.getAge()).isEqualTo(26);
    }
}
