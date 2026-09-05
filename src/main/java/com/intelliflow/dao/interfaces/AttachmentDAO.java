package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Attachment;
import java.util.List;
import java.util.Optional;

public interface AttachmentDAO {
    Optional<Attachment> findById(int id) throws DatabaseException;
    List<Attachment> findByTaskId(int taskId) throws DatabaseException;
    List<Attachment> findAll() throws DatabaseException;
    Attachment create(Attachment attachment) throws DatabaseException;
    void delete(int id) throws DatabaseException;
}
