package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findById(int id) throws DatabaseException;
    Optional<User> findByUsername(String username) throws DatabaseException;
    Optional<User> findByEmail(String email) throws DatabaseException;
    List<User> findAll() throws DatabaseException;
    User create(User user) throws DatabaseException;
    void update(User user) throws DatabaseException;
    void delete(int id) throws DatabaseException;
}
