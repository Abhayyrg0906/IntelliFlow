package com.intelliflow.service.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.Comment;
import java.util.List;

public interface CommentService {
    Comment addComment(int taskId, String content) throws ValidationException, DatabaseException, UnauthorizedException;
    List<Comment> getCommentsByTaskId(int taskId) throws ValidationException, DatabaseException, UnauthorizedException;
    void deleteComment(int commentId) throws ValidationException, DatabaseException, UnauthorizedException;
}
