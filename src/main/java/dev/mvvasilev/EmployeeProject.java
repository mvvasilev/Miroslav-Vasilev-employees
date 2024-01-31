package dev.mvvasilev;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public record EmployeeProject (
        int empId,
        int projectId,
        LocalDate dateFrom,
        Optional<LocalDate> dateTo
) {}
