package org.example.dao;

import org.example.domain.User;
import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDaoImplTest {
    SessionFactory sf;
    Session session;
    Transaction tx;
    UserDaoImpl dao;

    @BeforeEach
    void setUp() {
        sf = mock(SessionFactory.class);
        session = mock(Session.class);
        tx = mock(Transaction.class);

        when(sf.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);

        dao = new UserDaoImpl(sf);
    }
    @Test
    void create_success_commits_and_no_rollback() {
        User u = new User("A", "a@ex.com", 20);

        doNothing().when(session).persist(u);
        doNothing().when(tx).commit();
        when(tx.getStatus()).thenReturn(
                TransactionStatus.COMMITTED);

        dao.create(u);

        verify(tx).commit();
        verify(tx, never()).rollback();
    }
    @Test
    void create_uniqueViolation_23505_translates_to_IllegalState_and_rolls_back() {
        User u = new User("A", "dupe@ex.com", 20);
        SQLException sql = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = mock(ConstraintViolationException.class);
        when(cve.getSQLException()).thenReturn(sql);

        doThrow(cve).when(session).persist(u);
        when(tx.getStatus()).thenReturn(TransactionStatus.ACTIVE);

        assertThatThrownBy(() -> dao.create(u))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");

        verify(tx).rollback();
        verify(tx, never()).commit();
    }
    @Test
    void create_invalidText_22P02_translates_to_IllegalArgument_and_rolls_back() {
        User u = new User("A", "a@ex.com", 20);
        SQLException sql = new SQLException("invalid input syntax for type", "22P02");
        JDBCException je = mock(JDBCException.class);
        when(je.getSQLException()).thenReturn(sql);

        doThrow(je).when(session).persist(u);
        when(tx.getStatus()).thenReturn(TransactionStatus.ACTIVE);

        assertThatThrownBy(() -> dao.create(u))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wrong data");

        verify(tx).rollback();
        verify(tx, never()).commit();
    }
    @Test
    void read_success_returnsUser_and_commits() {
        User u = new User("R", "r@ex.com", 18);
        u.setId(1L);

        when(session.get(User.class, 1L)).thenReturn(u);
        doNothing().when(tx).commit();
        when(tx.getStatus()).thenReturn(TransactionStatus.COMMITTED);

        User out = dao.read(1L);

        assertThat(out).isNotNull();
        assertThat(out.getEmail()).isEqualTo("r@ex.com");
        verify(tx).commit();
        verify(tx, never()).rollback();
    }
    @Test
    void update_commitThrows_constraintViolation_23505_translates_and_rollsBack() {
        User u = new User("U", "dupe@ex.com", 21);
        u.setId(6L);

        SQLException sql = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = mock(ConstraintViolationException.class);
        when(cve.getSQLException()).thenReturn(sql);

        when(session.merge(u)).thenReturn(u);
        doThrow(cve).when(tx).commit();
        when(tx.getStatus()).thenReturn(TransactionStatus.ACTIVE);

        assertThatThrownBy(() -> dao.update(u))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");

        verify(tx).rollback();
    }
    @Test
    void deleteById_found_removes_and_commits() {
        User u = new User("D", "d@ex.com", 33);
        u.setId(10L);

        when(session.get(User.class, 10L)).thenReturn(u);
        doNothing().when(session).remove(u);
        doNothing().when(tx).commit();
        when(tx.getStatus()).thenReturn(TransactionStatus.COMMITTED);

        dao.deleteById(10L);

        verify(session).remove(u);
        verify(tx).commit();
        verify(tx, never()).rollback();
    }
}
