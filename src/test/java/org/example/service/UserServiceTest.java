package org.example.service;

import org.example.dao.UserDaoImpl;
import org.example.domain.User;
import org.example.dao.UserDao;

import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void saveUserDuplicateEmail() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.saveUser("name", "asdf@mail.ru", 12));

        assertEquals("User with asdf@mail.ru already created", exception.getMessage());

    }

    @Test
    void saveUserInvalidEmailThrows() {
        assertThrows(IllegalArgumentException.class,
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.saveUser("name", "", 12));
        assertEquals("Not valid email", exception.getMessage());
    }

    @Test
    void saveUserInvalidEmailShouldThrowAndNeverCallDao() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveUser("name", "notvalidemail", 12));
        verify(userDao, never()).mailUniqueCheck(anyString());
        verify(userDao, never()).create(any());
    }

    @Test
    void saveUserIllegalStateException() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(true);
        when(userDao.create(any())).thenThrow(IllegalStateException.class);
        assertThrows(IllegalStateException.class,
                () -> service.saveUser("name", "asdf@mail.ru", 12));
    }

    @Test
    void saveUserHibernateException() {
        when(userDao.mailUniqueCheck("asdf@mail.ru")).thenReturn(true);
        when(userDao.create(any(User.class))).thenThrow(new HibernateException("Hibernate exception"));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.saveUser("name", "asdf@mail.ru", 12));
        assertNotNull(exception.getCause());
        assertInstanceOf(HibernateException.class, exception.getCause());
    }

    @Test
    void readUserIllegalArgumentException() {
        when(userDao.read(any())).thenThrow(IllegalArgumentException.class);
        assertThrows(IllegalArgumentException.class, () -> service.readUser(12L));
    }

    @Test
    void readUserIllegalStateException() {
        when(userDao.read(any())).thenThrow(IllegalStateException.class);
        assertThrows(IllegalStateException.class, () -> service.readUser(12L));
    }

    @Test
    void readUserHibernateException() {
        when(userDao.read(any())).thenThrow(new HibernateException("Hibernate exception"));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.readUser(12L));
        assertNotNull(exception.getCause());
        assertInstanceOf(HibernateException.class, exception.getCause());
    }

    @Test
    void updateUserTest() {
        User existing = new User("Old", "old@mail.ru", 25);
        existing.setId(12L);
        when(userDao.read(12L)).thenReturn(existing);
        when(userDao.update(existing)).thenReturn(existing);
        when(userDao.mailUniqueCheck("new@mail.ru")).thenReturn(true);

        User updating = service.updateUser(12L, "New", "nEW@mail.ru", 125);

        assertThat(updating.getAge()).isEqualTo(125);
        assertThat(updating.getName()).isEqualTo("New");
        assertThat(updating.getEmail()).isEqualTo("new@mail.ru");
    }

    @Test
    void updateUserWithoutAge() {
        User existing = new User("Old", "old@mail.ru", 25);
        existing.setId(12L);
        when(userDao.read(12L)).thenReturn(existing);
        when(userDao.update(existing)).thenReturn(existing);
        when(userDao.mailUniqueCheck("new@mail.ru")).thenReturn(true);

        User updating = service.updateUser(12L, "New", "nEW@mail.ru", null);

        assertThat(updating.getAge()).isEqualTo(25);
        assertThat(updating.getName()).isEqualTo("New");
        assertThat(updating.getEmail()).isEqualTo("new@mail.ru");
    }

    @Test
    void updateUserWithoutName() {
        User existing = new User("Old", "old@mail.ru", 25);
        existing.setId(12L);
        when(userDao.read(12L)).thenReturn(existing);
        when(userDao.mailUniqueCheck("new@mail.ru")).thenReturn(true);
        when(userDao.update(existing)).thenReturn(existing);

        User updating = service.updateUser(12L, null, "nEW@mail.ru", 123);

        assertThat(updating.getAge()).isEqualTo(123);
        assertThat(updating.getName()).isEqualTo("Old");
        assertThat(updating.getEmail()).isEqualTo("new@mail.ru");
    }

    @Test
    void updateUserWithoutEmail() {
        User existing = new User("Old", "old@mail.ru", 25);
        existing.setId(12L);
        when(userDao.read(12L)).thenReturn(existing);
        when(userDao.update(existing)).thenReturn(existing);

        User updating = service.updateUser(12L, "New", null, 123);

        assertThat(updating.getAge()).isEqualTo(123);
        assertThat(updating.getName()).isEqualTo("New");
        assertThat(updating.getEmail()).isEqualTo("old@mail.ru");
    }

    @Test
    void updateUserInvalidEmailShouldThrowAndNeverUpdate() {
        User existing = new User("Old", "old@mail.ru", 25);
        existing.setId(10L);
        when(userDao.read(10L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateUser(10L, "New", "notValidEmail", 26));

        verify(userDao).read(10L);
        verify(userDao, never()).update(any());
    }

    @Test
    void updateUserIllegalArgumentException() {
        User existing = new User("Old", "old@mail.ru", 25);
        existing.setId(12L);
        when(userDao.read(12L)).thenReturn(existing);

        when(userDao.mailUniqueCheck("new@mail.ru")).thenReturn(true);
        when(userDao.update(existing)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateUser(12L, "New", "nEW@mail.ru", null));
    }

    @Test
    void updateUserIllegalStateException() {
        User existing = new User("Old", "old@mail.ru", 25);
        when(userDao.read(any())).thenReturn(existing);
        when(userDao.mailUniqueCheck("new@mail.ru")).thenReturn(true);
        when(userDao.update(any())).thenThrow(IllegalStateException.class);

        assertThrows(IllegalStateException.class,
                () -> service.updateUser(12L, "New", "nEW@mail.ru", null));
    }

    @Test
    void updateUserHibernateException() {
        User existing = new User("Old", "old@mail.ru", 25);
        when(userDao.read(any())).thenReturn(existing);
        when(userDao.mailUniqueCheck("new@mail.ru")).thenReturn(true);
        when(userDao.update(any())).thenThrow(new HibernateException(""));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.updateUser(12L, "New", "nEW@mail.ru", null));
        assertNotNull(exception.getCause());
        assertEquals("Database error while updating user. Try again later.", exception.getMessage());
        assertInstanceOf(HibernateException.class, exception.getCause());
    }

    @Test
    void mailValid() {
        String email = "wrongEmail";

        assertThrows(IllegalArgumentException.class, () -> service.mailValid(email));
    }

    @Test
    void mailValidAndUnique() {
        String email = "mail@mail.ru";
        when(userDao.mailUniqueCheck("mail@mail.ru")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.mailValidAndUnique(email));
    }

    @Test
    void removeUserById_happyPath_callsDaoOnce() {
        service.removeUserById(123L);
        verify(userDao).deleteById(123L);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    void removeUserByIdIllegalArgumentException() {

        doThrow(new IllegalArgumentException("wrong id")).when(userDao).deleteById(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.removeUserById(1L));

        assertEquals("wrong id", exception.getMessage());
        verify(userDao).deleteById(1L);
    }

    @Test
    void removeUserByIdIllegalStateException() {
        doThrow(new IllegalStateException("state")).when(userDao).deleteById(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.removeUserById(1L));

        assertEquals("state", exception.getMessage());
        verify(userDao).deleteById(1L);
    }

    @Test
    void removeUserByIdWrapsHibernateExceptionIntoIllegalState() {
        doThrow(new HibernateException("")).when(userDao).deleteById(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.removeUserById(1L));

        assertNotNull(exception.getCause());
        assertEquals("Database error while deleting user. Try again later.", exception.getMessage());
        assertInstanceOf(HibernateException.class, exception.getCause());

        verify(userDao).deleteById(1L);
    }

    @Test
    void removeUserByEmailNormalizesAndCallsDao() {
        String raw = "  MAIL@mail.RU  ";
        String normalized = "mail@mail.ru";

        service.removeUserByEmail(raw);

        verify(userDao).deleteByEmail(normalized);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    void removeUserByEmailInvalidEmailThrowsIAEAndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> service.removeUserByEmail("notValidEmail"));

        verify(userDao, never()).deleteByEmail(anyString());
    }

    @Test
    void removeUserByEmailIllegalArgumentException() {
        doThrow(new IllegalArgumentException("user not existed")).when(userDao).deleteByEmail("mail@mail.ru");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.removeUserByEmail("MAIL@mail.ru"));

        assertEquals("user not existed", exception.getMessage());
        verify(userDao).deleteByEmail("mail@mail.ru");
    }

    @Test
    void removeUserByEmailIllegalStateException() {
        doThrow(new IllegalStateException("state")).when(userDao).deleteByEmail("user@mail.ru");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.removeUserByEmail(" user@mail.ru "));

        assertEquals("state", exception.getMessage());
        verify(userDao).deleteByEmail("user@mail.ru");
    }

    @Test
    void removeUserByEmailWrapsHibernateExceptionIntoIllegalState() {
        doThrow(new org.hibernate.HibernateException("")).when(userDao).deleteByEmail("user@mail.ru");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.removeUserByEmail("user@mail.ru"));

        assertEquals("Database error while deleting user. Try again later.", exception.getMessage());
        assertInstanceOf(HibernateException.class, exception.getCause());
        verify(userDao).deleteByEmail("user@mail.ru");
    }
}
