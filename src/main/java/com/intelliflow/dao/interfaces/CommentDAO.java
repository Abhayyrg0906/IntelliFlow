package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentDAO {
    Optional<Comment> findById(int id) throws DatabaseException;
    List<Comment> findByTaskId(int taskId) throws DatabaseException;
    List<Comment> findAll() throws DatabaseException;
    Comment create(Comment comment) throws DatabaseException;
    void delete(int id) throws DatabaseException;
}
