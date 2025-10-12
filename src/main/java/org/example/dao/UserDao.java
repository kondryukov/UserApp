package org.example.dao;

import org.example.domain.User;

public interface UserDao {
    void create(User user);
    User read(Long id);
    void update(User user);
    void deleteById(Long id);
    void deleteByEmail(String email);
    boolean mailUniqueCheck(String email);
}
