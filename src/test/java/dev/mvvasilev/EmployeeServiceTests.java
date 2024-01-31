package dev.mvvasilev;

import dev.mvvasilev.exception.EmployeeProjectParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

public class EmployeeServiceTests {

    private static final String EMPTY_CSV_PATH = "src/test/resources/empty_csv.csv";

    private static final String INVALID_HEADER_CSV_PATH = "src/test/resources/invalid_header_csv.csv";

    private static final String INVALID_EMPID_CSV_PATH = "src/test/resources/invalid_empid_csv.csv";

    private static final String INVALID_DATEFROM_CSV_PATH = "src/test/resources/invalid_datefrom_csv.csv";

    private static final String EMPLOYEES_XML_PATH = "src/test/resources/employees.xml";

    private static final String EMPLOYEES_JSON_PATH = "src/test/resources/employees.json";

    private static final String EMPLOYEES_10_CSV_PATH = "src/test/resources/employees_10.csv";

    private static final String EMPLOYEES_100_CSV_PATH = "src/test/resources/employees_100.csv";

    private static final String EMPLOYEES_1000_CSV_PATH = "src/test/resources/employees_1000.csv";

    private final EmployeeProjectService service = new EmployeeProjectService();

    @Test
    public void throwsExceptionWhenCsvIsEmptyOrInvalid() {
        var emptyCsvPath = Path.of(EMPTY_CSV_PATH);
        var invalidCsvPath = Path.of(INVALID_HEADER_CSV_PATH);
        var invalidEmpIdCsvPath = Path.of(INVALID_EMPID_CSV_PATH);
        var invalidDateFromCsvPath = Path.of(INVALID_DATEFROM_CSV_PATH);

        Assertions.assertThrows(
                EmployeeProjectParserException.class,
                () -> service.parseCsv(Path.of("/a-non-existant-path-89f721qfvgf97")),
                "Unable to read provided csv path."
        );

        Assertions.assertThrows(
                EmployeeProjectParserException.class,
                () -> service.parseCsv(emptyCsvPath),
                "Nothing to parse. The provided csv was a valid file, but its contents are empty."
        );

        Assertions.assertThrows(
                EmployeeProjectParserException.class,
                () -> service.parseCsv(invalidCsvPath),
                String.format(
                        "Nothing to parse. The provided csv was a valid, non-empty file, but its header was invalid. Expected %s, found %s",
                        EmployeeProjectService.EXPECTED_HEADER,
                        "EmpID,ProjectID,SomeOtherUnexpectedColumn,DateFrom,DateTo"
                )
        );

        Assertions.assertThrows(
                EmployeeProjectParserException.class,
                () -> service.parseCsv(invalidEmpIdCsvPath),
                "Unable to parse csv: invalid employee id or project id provided"
        );

        Assertions.assertThrows(
                EmployeeProjectParserException.class,
                () -> service.parseCsv(invalidDateFromCsvPath),
                "Unable to parse csv: invalid datetime format provided"
        );
    }

    @Test
    public void csvParsedValid() {
        var parsed10Csv = service.parseCsv(Path.of(EMPLOYEES_10_CSV_PATH));

        Assertions.assertFalse(parsed10Csv.isEmpty());
        Assertions.assertEquals(766, parsed10Csv.get(0).empId());
        Assertions.assertEquals(15, parsed10Csv.get(0).projectId());
        Assertions.assertEquals(LocalDate.of(2016, 9, 23), parsed10Csv.get(0).dateFrom());
        Assertions.assertEquals(Optional.of(LocalDate.of(2022, 12, 27)), parsed10Csv.get(0).dateTo());

        // NULL dates parsed as empty
        Assertions.assertEquals(Optional.empty(), parsed10Csv.get(5).dateTo());
    }

    @Test
    public void xmlParsedValid() {
        var parsedXml = service.parseXml(Path.of(EMPLOYEES_XML_PATH));

        Assertions.assertFalse(parsedXml.isEmpty());
        Assertions.assertEquals(1, parsedXml.get(0).empId());
        Assertions.assertEquals(2, parsedXml.get(0).projectId());
        Assertions.assertEquals(LocalDate.of(2020, 1, 1), parsedXml.get(0).dateFrom());
        Assertions.assertEquals(Optional.of(LocalDate.of(2023, 6, 1)), parsedXml.get(0).dateTo());

        // NULL dates parsed as empty
        Assertions.assertEquals(Optional.empty(), parsedXml.get(3).dateTo());
    }

    @Test
    public void jsonParsedValid() {
        var parsedJson = service.parseJson(Path.of(EMPLOYEES_JSON_PATH));

        Assertions.assertFalse(parsedJson.isEmpty());
        Assertions.assertEquals(1, parsedJson.get(0).empId());
        Assertions.assertEquals(2, parsedJson.get(0).projectId());
        Assertions.assertEquals(LocalDate.of(2020, 1, 1), parsedJson.get(0).dateFrom());
        Assertions.assertEquals(Optional.of(LocalDate.of(2021, 1, 5)), parsedJson.get(0).dateTo());

        // NULL dates parsed as empty
        Assertions.assertEquals(Optional.empty(), parsedJson.get(3).dateTo());
    }

    @Test
    public void successfullyFindPairOfEmployeesWhoSpentLongestAmountOfTimeTogetherOnSingleProject() {
        var parsed10Csv = service.parseCsv(Path.of(EMPLOYEES_10_CSV_PATH));
        var parsed100Csv = service.parseCsv(Path.of(EMPLOYEES_100_CSV_PATH));
        var parsed1000Csv = service.parseCsv(Path.of(EMPLOYEES_1000_CSV_PATH));

        var result1 = service.findPairOfEmployeesWorkedLongestTimeTogetherOnSingleProject(parsed10Csv);
        var result2 = service.findPairOfEmployeesWorkedLongestTimeTogetherOnSingleProject(parsed100Csv);
        var result3 = service.findPairOfEmployeesWorkedLongestTimeTogetherOnSingleProject(parsed1000Csv);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(854, result1.get().firstEmployeeId());
        Assertions.assertEquals(731, result1.get().secondEmployeeId());
        Assertions.assertEquals(18, result1.get().projectId());

        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(928, result2.get().firstEmployeeId());
        Assertions.assertEquals(258, result2.get().secondEmployeeId());
        Assertions.assertEquals(4, result2.get().projectId());

        Assertions.assertTrue(result3.isPresent());
        Assertions.assertEquals(50, result3.get().firstEmployeeId());
        Assertions.assertEquals(34, result3.get().secondEmployeeId());
        Assertions.assertEquals(33, result3.get().projectId());
    }

    @Test
    public void successfullyFindPairOfEmployeesWhoSpentLongestAmountOfTimeTogetherOnAnyProject() {
        var parsed10Csv = service.parseCsv(Path.of(EMPLOYEES_10_CSV_PATH));
        var parsed100Csv = service.parseCsv(Path.of(EMPLOYEES_100_CSV_PATH));
        var parsed1000Csv = service.parseCsv(Path.of(EMPLOYEES_1000_CSV_PATH));

        var result1 = service.findPairOfEmployeesWorkedLongestTimeTogetherOnAnyProject(parsed10Csv);
        var result2 = service.findPairOfEmployeesWorkedLongestTimeTogetherOnAnyProject(parsed100Csv);
        var result3 = service.findPairOfEmployeesWorkedLongestTimeTogetherOnAnyProject(parsed1000Csv);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(854, result1.get().emp1());
        Assertions.assertEquals(731, result1.get().emp2());

        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(928, result2.get().emp1());
        Assertions.assertEquals(258, result2.get().emp2());

        Assertions.assertTrue(result3.isPresent());
        Assertions.assertEquals(50, result3.get().emp1());
        Assertions.assertEquals(34, result3.get().emp2());
    }

}
