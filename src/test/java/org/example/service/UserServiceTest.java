package org.example.service;

import org.example.dao.UserDaoImpl;
import org.example.domain.User;
import org.example.dao.UserDao;

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
        service = mock(UserService.class);
        service = new UserService((UserDaoImpl) userDao);
    }
    @Test
    void saveUser_ok_normalizesEmail_and_callsDao() {
        when(userDao.mailUniqueCheck("a@b.com")).thenReturn(true);

        service.saveUser("Alice", "  A@B.COM  ", 30);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userDao).create(cap.capture());
        User u = cap.getValue();

        assertThat(u.getName()).isEqualTo("Alice");
        assertThat(u.getEmail()).isEqualTo("a@b.com");
        assertThat(u.getAge()).isEqualTo(30);
    }
    @Test
    void saveUser_blankEmail_throws() {
        try {
            service.saveUser("Alice", "   ", 20);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Not valid email");
            verifyNoInteractions(userDao);
        }
    }
    @Test
    void saveUser_duplicateEmail_throws() {
        when(userDao.mailUniqueCheck("dupe@ex.com")).thenReturn(false);

        try {
            service.saveUser("Bob", "dupe@ex.com", 20);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("already");
            verify(userDao, never()).create(any());
        }
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
