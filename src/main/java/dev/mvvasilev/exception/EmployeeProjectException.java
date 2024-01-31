package dev.mvvasilev.exception;

public class EmployeeProjectException extends RuntimeException {

    public EmployeeProjectException(String message) {
        super(message);
    }

    public EmployeeProjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
