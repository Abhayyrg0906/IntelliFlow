package com.intelliflow.service.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.Attachment;

import java.io.File;
import java.util.List;

public interface AttachmentService {
    Attachment uploadAttachment(int taskId, File sourceFile) throws ValidationException, DatabaseException, UnauthorizedException;
    List<Attachment> getAttachmentsByTaskId(int taskId) throws ValidationException, DatabaseException, UnauthorizedException;
    File getAttachmentFile(int attachmentId) throws ValidationException, DatabaseException, UnauthorizedException;
    void deleteAttachment(int attachmentId) throws ValidationException, DatabaseException, UnauthorizedException;
}
