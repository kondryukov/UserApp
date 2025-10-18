package org.example.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.User;
import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;

import java.sql.SQLException;

public class UserDaoImpl implements UserDao {
    private static final Logger log = LogManager.getLogger(UserDaoImpl.class);
    private final SessionFactory sessionFactory;

    public UserDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public User create(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            log.info("User created {}", user);
            return user;
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e);
        } catch (JDBCException e) {
            handleJdbcException("create", e);
        } catch (HibernateException e) {
            log.error("Hibernate error in create()", e);
            throw e;
        } finally {
            safeRollback(transaction);
        }
        return null;
    }

    @Override
    public User read(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            session.setDefaultReadOnly(true);
            transaction = session.beginTransaction();
            User user = session.get(User.class, id);
            if (user == null) {
                log.info("User with id={} not existed", id);
                transaction.commit();
                throw new IllegalArgumentException("User with id={} is not existed");
            } else {
                log.debug("User is load: {}", user);
            }
            transaction.commit();
            return user;
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e);
        } catch (JDBCException e) {
            handleJdbcException("read", e);
        } catch (HibernateException e) {
            log.error("Hibernate error in read(id={})", id, e);
            throw e;
        } finally {
            safeRollback(transaction);
        }
        return null;
    }


    @Override
    public User update(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
            log.info("User is updated {}", user);
            return user;
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e);
        } catch (JDBCException e) {
            handleJdbcException("update", e);
        } catch (HibernateException e) {
            log.error("Hibernate error in update()", e);
            throw e;
        } finally {
            safeRollback(transaction);
        }
        return null;
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User user = session.get(User.class, id);
            if (user == null) {
                log.info("User with id={} is not existed", id);
                transaction.commit();
                throw new IllegalArgumentException("User with id={" + id + "} is not existed");
            } else {
                session.remove(user);
                transaction.commit();
                log.info("User {} is deleted", user);
            }
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e);
        } catch (JDBCException e) {
            handleJdbcException("deleteById", e);
        } catch (HibernateException e) {
            log.error("Hibernate error in deleteById(id={})", id, e);
            throw e;
        } finally {
            safeRollback(transaction);
        }
    }

    @Override
    public void deleteByEmail(String email) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User user = session.createQuery(
                            "from User u where lower(u.email) = :e", User.class)
                    .setParameter("e", email)
                    .setMaxResults(1)
                    .uniqueResult();
            if (user == null) {
                log.info("User with email={} is not existed", email);
            } else {
                session.remove(user);
                transaction.commit();
                log.info("User {} is deleted", user);
            }
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e);
        } catch (JDBCException e) {
            handleJdbcException("deleteByEmail", e);
        } catch (HibernateException e) {
            log.error("Hibernate error in deleteByEmail(email={})", email, e);
            throw e;
        } finally {
            safeRollback(transaction);
        }
    }

    public boolean mailUniqueCheck(String email) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            session.setDefaultReadOnly(true);
            transaction = session.beginTransaction();
            Integer search = session.createQuery(
                            "select 1 from User u where lower(u.email) = :e", Integer.class)
                    .setParameter("e", email.trim().toLowerCase())
                    .setMaxResults(1)
                    .uniqueResult();
            if (search == null) {
                log.info("User with mail={} not existed", email);
            } else {
                log.debug("User with mail={} exists", email);
            }
            transaction.commit();
            return search == null;
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e);
            throw e;
        } catch (JDBCException e) {
            handleJdbcException("mailUniqueCheck", e);
            throw e;
        } catch (HibernateException e) {
            log.error("Hibernate error in mailUniqueCheck(email={})", email, e);
            throw e;
        } finally {
            safeRollback(transaction);
        }
    }

    private void safeRollback(Transaction transaction) {
        try {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
                log.debug("Transaction rollback");
            }
        } catch (RuntimeException re) {
            log.warn("Error in transaction rollback", re);
        }
    }

    private void handleConstraintViolation(ConstraintViolationException e) {
        SQLException sqlException = e.getSQLException();
        String state = sqlException != null ? sqlException.getSQLState() : null;

        if ("23505".equals(state)) {
            log.info("Unique violation (23505): {}", safeSqlMessage(sqlException));
            throw new IllegalStateException("That email is already used");
        } else if ("23502".equals(state)) {
            log.info("NOT NULL violation (23502): {}", safeSqlMessage(sqlException));
            throw new IllegalArgumentException("That field can't be empty");
        } else {
            log.error("ConstraintViolation SQLState={} : {}", state, safeSqlMessage(sqlException), e);
            throw new IllegalStateException("Constraint violation in db");
        }
    }

    private void handleJdbcException(String operation, JDBCException exception) {
        SQLException sqlException = exception.getSQLException();
        String state = sqlException != null ? sqlException.getSQLState() : null;

        if ("22P02".equals(state)) {
            log.info("Wrong data (22P02) в {}: {}", operation, safeSqlMessage(sqlException));
            throw new IllegalArgumentException("Wrong data");
        }

        log.error("JDBCException в {} (SQLState={}): {}", operation, state, safeSqlMessage(sqlException), exception);
        throw exception;
    }

    private String safeSqlMessage(SQLException exception) {
        return exception == null ? "<no-sql-exception>" : exception.getMessage();
    }
}
