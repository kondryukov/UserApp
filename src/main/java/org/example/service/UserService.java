package org.example.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.dao.UserDaoImpl;
import org.example.domain.User;
import org.hibernate.*;
import java.util.Set;

public class UserService {
    private static final Logger log = LogManager.getLogger(UserService.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final UserDaoImpl userDao;
    public UserService(UserDaoImpl userDao) {
        this.userDao = userDao;
    }
    public void saveUser(String name, String email, Integer age) {
        try {
            email = email.trim().toLowerCase();
            mailValidAndUnique(email);
            User user = new User(name, email, age);
            userDao.create(user);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (HibernateException e) {
            log.debug("DB error on saveUser", e);
            throw dbError("creating", e);
        }
    }
    public User readUser(Long id) {
        try {
            return userDao.read(id);
        }  catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (HibernateException e) {
            log.debug("DB error on readUser id={}", id, e);
            throw dbError("reading", e);
        }
    }
    public void updateUser(Long id, String name, String email, Integer age) {
        User user = userDao.read(id);
        if (name != null) user.setName(name);
        if (email != null) {
            email = email.trim().toLowerCase();
            mailValidAndUnique(email);
            user.setEmail(email);
        }
        if (age != null) user.setAge(age);

        try {
            userDao.update(user);
        }  catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (HibernateException e) {
            log.debug("DB error on updateUser id={}", id, e);
            throw dbError("updating", e);
        }
    }
    public void removeUserById(Long id) {
        try {
            userDao.deleteById(id);
        }  catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (HibernateException e) {
            log.debug("DB error on removeUserById id={}", id, e);
            throw dbError("deleting", e);
        }
    }
    public void removeUserByEmail(String email) {
        try {
            email = email.trim().toLowerCase();
            mailValid(email);
            userDao.deleteByEmail(email);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (HibernateException e) {
            log.debug("DB error on removeUserByEmail email={}", email, e);
            throw dbError("deleting", e);
        }
    }
    public void mailValid(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Set<ConstraintViolation<User>> v =
                validator.validateValue(User.class, "email", normalizedEmail);
        if (!v.isEmpty()) {
            throw new IllegalArgumentException("Not valid email");
        }
        if (!userDao.mailUniqueCheck(normalizedEmail)) {
            throw new IllegalArgumentException("User with " + email + " already created");
        }
    }
    public void mailValidAndUnique(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Set<ConstraintViolation<User>> v =
                validator.validateValue(User.class, "email", normalizedEmail);
        if (!v.isEmpty()) {
            throw new IllegalArgumentException("Not valid email");
        }
        if (!userDao.mailUniqueCheck(normalizedEmail)) {
            throw new IllegalArgumentException("User with " + email + " already created");
        }
    }

    private IllegalStateException dbError(String operation, Exception cause) {
        return new IllegalStateException("Database error while " + operation + " user. Try again later.", cause);
    }
}
