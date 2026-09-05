package com.intelliflow.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FileStorageUtil {

    public static final long MAX_FILE_SIZE = 15 * 1024 * 1024L; // 15 MB

    private static final Set<String> BLOCKED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "exe", "bat", "cmd", "sh", "vbs", "msi", "jar", "com", "ps1", "scr",
            "dll", "bin", "app", "pif", "gadget", "wsf", "cpl", "reg", "hta", "vbe", "jse"
    ));

    private static Path storageDirectory = Paths.get("data", "attachments");

    static {
        initStorageDirectory();
    }

    public static synchronized void setStorageDirectory(Path customPath) {
        storageDirectory = customPath;
        initStorageDirectory();
    }

    public static synchronized Path getStorageDirectory() {
        return storageDirectory;
    }

    private static void initStorageDirectory() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }
        } catch (IOException e) {
            System.err.println("Could not create attachments directory: " + e.getMessage());
        }
    }

    /**
     * Validates an uploaded file for existence, size, and security constraints.
     */
    public static void validateFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Selected file does not exist or is not a valid regular file.");
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("Selected file cannot be read.");
        }
        if (file.length() <= 0) {
            throw new IllegalArgumentException("File is empty (0 bytes). Cannot upload empty files.");
        }
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(String.format("File size (%s) exceeds maximum allowed size of %s.",
                    formatFileSize(file.length()), formatFileSize(MAX_FILE_SIZE)));
        }
        validateFilename(file.getName());
    }

    /**
     * Validates filename for path traversal, illegal characters, and blocked extensions.
     */
    public static void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty.");
        }
        String clean = filename.trim();
        if (clean.contains("..") || clean.contains("/") || clean.contains("\\") || clean.contains("\0")) {
            throw new IllegalArgumentException("Filename contains invalid path characters or path traversal sequences.");
        }

        String ext = getFileExtension(clean).toLowerCase(Locale.ROOT);
        if (BLOCKED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Executable and potentially harmful file type (." + ext + ") is not allowed.");
        }
    }

    /**
     * Sanitizes original filename and returns a secure unique storage filename.
     */
    public static String generateStoredFilename(String originalFilename) {
        validateFilename(originalFilename);
        String nameOnly = new File(originalFilename).getName();
        // Remove potentially unsafe characters
        String sanitized = nameOnly.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isEmpty() || sanitized.equals(".")) {
            sanitized = "attachment";
        }
        return UUID.randomUUID().toString().replace("-", "") + "_" + sanitized;
    }

    /**
     * Saves source file into the managed storage directory under the given stored filename.
     */
    public static void saveFile(File sourceFile, String storedFilename) throws IOException {
        validateFile(sourceFile);
        Path targetPath = resolveSecurePath(storedFilename);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Saves input stream data into the managed storage directory.
     */
    public static void saveFile(InputStream in, String storedFilename) throws IOException {
        Path targetPath = resolveSecurePath(storedFilename);
        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Resolves and returns the stored File, strictly preventing path traversal.
     */
    public static File getStoredFile(String storedFilename) {
        Path targetPath = resolveSecurePath(storedFilename);
        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Attachment file not found on disk.");
        }
        return file;
    }

    /**
     * Deletes stored file from disk if it exists.
     */
    public static boolean deleteStoredFile(String storedFilename) {
        try {
            Path targetPath = resolveSecurePath(storedFilename);
            return Files.deleteIfExists(targetPath);
        } catch (Exception e) {
            return false;
        }
    }

    private static Path resolveSecurePath(String storedFilename) {
        if (storedFilename == null || storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            throw new SecurityException("Illegal filename path access attempt: " + storedFilename);
        }
        Path normalizedStorage = storageDirectory.toAbsolutePath().normalize();
        Path targetPath = normalizedStorage.resolve(storedFilename).normalize();
        if (!targetPath.startsWith(normalizedStorage)) {
            throw new SecurityException("Path traversal attempt detected: " + storedFilename);
        }
        return targetPath;
    }

    /**
     * Extracts extension without leading dot.
     */
    public static String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Returns appropriate emoji icon according to file extension.
     */
    public static String getFileIcon(String filename) {
        String ext = getFileExtension(filename).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "pdf":
            case "doc":
            case "docx":
            case "txt":
            case "rtf":
            case "odt":
            case "md":
                return "📄";
            case "xls":
            case "xlsx":
            case "csv":
            case "tsv":
            case "ods":
                return "📊";
            case "png":
            case "jpg":
            case "jpeg":
            case "gif":
            case "webp":
            case "svg":
            case "bmp":
            case "ico":
                return "🖼️";
            case "zip":
            case "tar":
            case "gz":
            case "7z":
            case "rar":
                return "📦";
            case "json":
            case "xml":
            case "yaml":
            case "yml":
            case "html":
            case "css":
            case "sql":
                return "📜";
            case "mp3":
            case "wav":
            case "ogg":
                return "🎵";
            case "mp4":
            case "avi":
            case "mov":
            case "mkv":
                return "🎥";
            default:
                return "📁";
        }
    }

    /**
     * Formats bytes to human-readable size string.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.ROOT, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
