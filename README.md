# IntelliFlow – Smart Workflow Automation Platform

IntelliFlow is a Java-based desktop workflow management platform designed to help organizations manage projects, assign tasks, enforce state-transition lifecycles, and view performance reports from a centralized application. 

Developed using a modern layered architecture, it features role-based access control, a sleek dark look-and-feel, secure password hashing, and in-memory connection pooling.

---

## Technical Stack

*   **Runtime Environment**: Java 17 LTS or newer (fully compatible with Java 21)
*   **User Interface**: Java Swing styled with the **FlatLaf (Flat Dark)** look-and-feel library
*   **Persistent Storage**: MySQL Database
*   **Database Connectivity**: JDBC with **HikariCP** connection pooling
*   **Build & Dependency Management**: Apache Maven
*   **Unit Testing Suite**: JUnit 5 (Jupiter engine)
*   **Security Hashing**: jBCrypt

---

## Architectural Blueprint

IntelliFlow uses a strict **Three-Tier Layered Architecture** pattern:

1.  **Presentation Tier (`com.intelliflow.ui`)**: Manages the visual layout, views, and components. Coordinates with the Service layer using worker threads to prevent locking the Event Dispatch Thread (EDT).
2.  **Service Tier (`com.intelliflow.service`)**: Handles the core business rules, credentials checks, authorization checks, workflow state-transition checks, dates validations, and notifications dispatch.
3.  **Data Access Object (DAO) Tier (`com.intelliflow.dao`)**: Interacts directly with the MySQL connection pool to run prepared statements. Maps SQL rows to Java models.
4.  **Utilities & Session Context (`com.intelliflow.util` & `com.intelliflow.context`)**: Singleton session storage, password hash check functions, regex matches, and database connection pooling.

---

## Database Schema Design

The relational database schema consists of 5 normalized tables located in [`db/schema.sql`](file:///c:/Projects/IntelliFlow/db/schema.sql):

*   **`users`**: Stores credentials, roles (`ADMIN`, `MANAGER`, `EMPLOYEE`), and email indexes.
*   **`projects`**: Holds project names, start/end dates, statuses (`PLANNED`, `ACTIVE`, `COMPLETED`, `ON_HOLD`, `CANCELLED`), and links to the project manager.
*   **`tasks`**: Tracks tasks with deadlines, priority levels (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), and statuses (`TO_DO`, `IN_PROGRESS`, `TESTING`, `COMPLETED`, `BLOCKED`). Enforces target deadlines to remain within project boundaries.
*   **`notifications`**: Manages unread alert messages.
*   **`activity_logs`**: System audit trails logs.

---

## Local Installation & Setup

### 1. Database Initialization
1. Ensure your local MySQL Server is running.
2. Open your MySQL client shell (or workbench) and execute the SQL script located at:
   [`db/schema.sql`](file:///c:/Projects/IntelliFlow/db/schema.sql)
   This creates the database `intelliflow` and its tables.

### 2. Configure Database Credentials
Configure database credentials via environment variables or a local untracked `db-local.properties` file in `src/main/resources/`:

Environment Variables:
```bash
export DB_USER="your_db_username"
export DB_PASS="your_db_password"
```

Or configure via `src/main/resources/db.properties` placeholder resolution:
```properties
db.url=jdbc:mysql://localhost:3306/intelliflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=${DB_USER}
db.password=${DB_PASS}
```

### 3. Bootstrap Seed Accounts
On first run with an empty database, IntelliFlow automatically bootstraps default role-based administrator, manager, and employee accounts hashed with BCrypt. All users are prompted to change their initial credentials upon setup.

---

## Build, Test & Run

All commands should target Java 17/21.

### 1. Run Unit Tests
To run the automated business logic, validators, and workflow state transition checks:
```bash
mvn test
```

### 2. Build the JAR Executable
To compile resources and packages into a single target executable JAR:
```bash
mvn clean package
```

### 3. Run the Desktop Application
To execute the application:
```bash
mvn exec:java -Dexec.mainClass="com.intelliflow.App"
```

---

## Design Highlights

*   **Modern Look and Feel**: FlatLaf makes the GUI clean and modern, eliminating the standard grey Java look.
*   **Programmatic Access Control**: Views adjust dynamically to show or hide panels (e.g. `Reports` are hidden from employees, and `System Logs`/`User Management` are restricted to Admin).
*   **Audit Logger**: Logs are written to the database for all key operations (logins, creates, updates, deletions).
*   **Notification Engine**: When managers create tasks or projects, notifications are automatically written to the database for the assigned employee and manager.
