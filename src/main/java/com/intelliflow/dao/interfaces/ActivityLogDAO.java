package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.ActivityLog;
import java.util.List;

public interface ActivityLogDAO {
    List<ActivityLog> findAll() throws DatabaseException;
    List<ActivityLog> findByUserId(int userId) throws DatabaseException;
    ActivityLog create(ActivityLog log) throws DatabaseException;
}
