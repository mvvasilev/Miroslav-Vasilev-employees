package dev.mvvasilev.exception;

public class EmployeeProjectParserException extends EmployeeProjectException {
    public EmployeeProjectParserException(String message) {
        super(message);
    }

    public EmployeeProjectParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
