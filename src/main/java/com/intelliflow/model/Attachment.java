package com.intelliflow.model;

import com.intelliflow.enums.Role;
import java.time.LocalDateTime;

public class Attachment {
    private int id;
    private int taskId;
    private int userId;
    private String filename;
    private String storedFilename;
    private long fileSize;
    private String fileType;
    private LocalDateTime createdAt;

    // Transient display fields
    private String uploaderName;
    private Role uploaderRole;

    public Attachment() {
    }

    public Attachment(int taskId, int userId, String filename, String storedFilename, long fileSize, String fileType) {
        this.taskId = taskId;
        this.userId = userId;
        this.filename = filename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public Role getUploaderRole() {
        return uploaderRole;
    }

    public void setUploaderRole(Role uploaderRole) {
        this.uploaderRole = uploaderRole;
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", userId=" + userId +
                ", filename='" + filename + '\'' +
                ", storedFilename='" + storedFilename + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
