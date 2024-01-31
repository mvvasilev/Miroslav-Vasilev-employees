package dev.mvvasilev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.mvvasilev.exception.EmployeeProjectParserException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class EmployeeProjectService {

    public static final String EXPECTED_HEADER = "empid,projectid,datefrom,dateto";

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Parse the provided json file into a list of employee project records
     *
     * @param jsonPath the path to the json file
     * @return the parsed list
     * @throws EmployeeProjectParserException If the file could not be opened for parsing
     * @throws JsonProcessingException See {@link JsonMapper#readValue(String, TypeReference)}
     * @throws JsonMappingException See {@link JsonMapper#readValue(String, TypeReference)}
     */
    public List<EmployeeProject> parseJson(Path jsonPath) throws EmployeeProjectParserException {
        var jsonMapper = JsonMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .build();

        try {
            return jsonMapper.readValue(Files.readString(jsonPath), new TypeReference<>() {});
        } catch (IOException e) {
            throw new EmployeeProjectParserException("Unable to open json file for parsing.", e);
        }
    }

    /**
     * Parse the provided xml file into a list of employee project records
     *
     * @param xmlPath the path to the xml file
     * @return the parsed list
     * @throws EmployeeProjectParserException If the file could not be opened for parsing
     * @throws JsonProcessingException See {@link XmlMapper#readValue(String, TypeReference)}
     * @throws JsonMappingException See {@link XmlMapper#readValue(String, TypeReference)}
     */
    public List<EmployeeProject> parseXml(Path xmlPath) throws EmployeeProjectParserException {
        var xmlMapper = XmlMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .build();

        try {
            return xmlMapper.readValue(Files.readString(xmlPath), new TypeReference<>() {});
        } catch (IOException e) {
            throw new EmployeeProjectParserException("Unable to open xml file for parsing.", e);
        }
    }

    /**
     * Parses a provided file as a csv, expecting it to contain valid EmployeeProject data.
     *
     * @param csvPath The path to the csv file
     * @return The parsed EmployeeProject objects
     * @throws EmployeeProjectParserException If the file could not be read, or its contents were invalid
     */
    public List<EmployeeProject> parseCsv(Path csvPath) throws EmployeeProjectParserException {

        List<String> lines;

        try {
            lines = Files.lines(csvPath).toList();
        } catch (IOException e) {
            throw new EmployeeProjectParserException("Unable to read provided csv path.", e);
        }

        if (lines.isEmpty()) {
            throw new EmployeeProjectParserException("Nothing to parse. The provided csv was a valid file, but its contents are empty.");
        }

        if (!isCsvHeaderValid(lines.get(0))) {
            throw new EmployeeProjectParserException(
                    String.format(
                            "Nothing to parse. The provided csv was a valid, non-empty file, but its header was invalid. Expected %s, found %s",
                            EXPECTED_HEADER,
                            lines.get(0)
                    )
            );
        }

        try {
            return lines.stream()
                    .skip(1) // Skip the header ( 1st line )
                    .map(line -> {
                        var data = line.replaceAll("\\s+", "").split(",");

                        // Account for NULL in toDate, meaning the project is still ongoing
                        Optional<LocalDate> toDate = "NULL".equals(data[3]) ?
                                Optional.empty() :
                                Optional.of(LocalDate.parse(data[3], CSV_DATE_FORMAT));

                        return new EmployeeProject(
                                Integer.parseInt(data[0]),
                                Integer.parseInt(data[1]),
                                LocalDate.parse(data[2], CSV_DATE_FORMAT),
                                toDate
                        );
                    })
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new EmployeeProjectParserException("Unable to parse csv: invalid employee id or project id provided", e);
        } catch (DateTimeParseException e) {
            throw new EmployeeProjectParserException("Unable to parse csv: invalid datetime format provided", e);
        }
    }

    private boolean isCsvHeaderValid(String header) {
        // Validate header, ignoring whitespace and case
        return EXPECTED_HEADER.equalsIgnoreCase(header.replaceAll("\\s+",""));
    }

    private record EmployeePairSingleProject(int emp1, int emp2, long days, int projectId ) {}

    public record EmployeePairWithProject(int firstEmployeeId, int secondEmployeeId, int projectId) {}

    /**
     * This method retrieves the 2 employees who have spent the most amount of time working together
     * on a single project
     * @param dataset The dataset of employees and projects worked on
     * @return An optional result. If no employees have ever worked on any project together, this will be empty.
     */
    public Optional<EmployeePairWithProject> findPairOfEmployeesWorkedLongestTimeTogetherOnSingleProject(Collection<EmployeeProject> dataset) {
        return dataset.stream()
                // Group all records by project, resulting in a dataset of projects, with a list of employees having ever worked on said project
                .collect(Collectors.groupingBy(EmployeeProject::projectId))
                .entrySet()
                .stream()
                .map(e -> {
                    var employees = e.getValue();

                    // Sort the employees by dateFrom
                    // This means that later we don't have to worry about which of the 2 employees started on the project first when comparing
                    employees.sort(Comparator.comparing(EmployeeProject::dateFrom));

                    var currentLongest = new EmployeePairSingleProject(0, 0, 0, 0);

                    for (var i = 0; i < employees.size(); i++) {
                        for (var j = (i + 1); j < employees.size(); j++) {
                            var timeWorkedTogetherOnProject = calculateTimeSpentInDaysByEmployeesWorkingTogether(
                                    employees.get(i).dateFrom(),
                                    employees.get(i).dateTo().orElse(LocalDate.now()),
                                    employees.get(j).dateFrom(),
                                    employees.get(j).dateTo().orElse(LocalDate.now())
                            );

                            // If these employees have spent longer together on this project than the current longest,
                            // make them the current longest
                            if (currentLongest.days() < timeWorkedTogetherOnProject) {
                                currentLongest = new EmployeePairSingleProject(
                                        employees.get(i).empId(),
                                        employees.get(j).empId(),
                                        timeWorkedTogetherOnProject,
                                        e.getKey()
                                );
                            }
                        }
                    }

                    return currentLongest;
                })
                .filter(r -> r.days() > 0)
                .max(Comparator.comparing(EmployeePairSingleProject::days))
                .map(r -> new EmployeePairWithProject(r.emp1(), r.emp2(), r.projectId()));
    }

    public record EmployeePairAnyProject(int emp1, int emp2, long duration) {};

    /**
     * This method retrieves the 2 employees who have spent the most amount of time working together
     * on any project
     * @param dataset The dataset of employees and projects worked on
     * @return An optional result. If no employees have ever worked on any project together, this will be empty.
     */
    public Optional<EmployeePairAnyProject> findPairOfEmployeesWorkedLongestTimeTogetherOnAnyProject(List<EmployeeProject> dataset) {
        var map = new HashMap<EmployeePairAnyProject, Long>();

        // Sort the data by dateFrom
        // This means that later we don't have to worry about which of the 2 employees started on the project first when comparing
        dataset.sort(Comparator.comparing(EmployeeProject::dateFrom));

        for (var i = 0; i < dataset.size(); i++) {

            var emp1Id = dataset.get(i).empId();
            var emp1ProjectId = dataset.get(i).projectId();

            for (var j = (i + 1); j < dataset.size(); j++) {

                var emp2Id = dataset.get(j).empId();
                var emp2ProjectId = dataset.get(j).projectId();

                // Working with yourself on a project doesn't count
                if (emp1Id == emp2Id) {
                    continue;
                }

                // Only interested in common projects
                if (emp1ProjectId != emp2ProjectId) {
                    continue;
                }

                var timeWorkedTogetherOnProject = calculateTimeSpentInDaysByEmployeesWorkingTogether(
                        dataset.get(i).dateFrom(),
                        dataset.get(i).dateTo().orElse(LocalDate.now()),
                        dataset.get(j).dateFrom(),
                        dataset.get(j).dateTo().orElse(LocalDate.now())
                );

                var employeePair = new EmployeePairAnyProject(emp1Id, emp2Id, 0L);

                map.putIfAbsent(employeePair, 0L);
                map.merge(employeePair, timeWorkedTogetherOnProject, Long::sum);
            }
        }

        return map.entrySet()
                .stream()
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(e -> new EmployeePairAnyProject(e.getKey().emp1(), e.getKey().emp2(), e.getValue()));
    }

    private long calculateTimeSpentInDaysByEmployeesWorkingTogether(LocalDate emp1From, LocalDate emp1To, LocalDate emp2From, LocalDate emp2To) {

        // The employee from dates are assumed to be in order, first being before the second

        var timeSpentWorkingTogether = 0L;

        // Timeline visualization:
        //
        // 1970-01-01 |----|/////////////////|-------|//////////////|---> now
        //                 \------ emp1 -----/       \----- emp2 ---/
        //                from              to      from           to
        // Skip if the second employee started after the first had already finished with the project
        if (emp1To.isBefore(emp2From)) {
            return timeSpentWorkingTogether;
        }

        // Timeline visualization:
        //
        //                           from                         to
        //                            /--------- emp2 -------------\
        // 1970-01-01 |----|//////////|#################|///////////|---> now
        //                 \------- emp1 ---------------/
        //                from                         to
        if (emp1To.isBefore(emp2To)) {
            timeSpentWorkingTogether = ChronoUnit.DAYS.between(emp2From, emp1To);
        } else {

            // Timeline visualization:
            //
            //                           from             to
            //                            /----- emp2 -----\
            // 1970-01-01 |----|//////////|#################|///////////|---> now
            //                 \------- emp1 ---------------------------/
            //                from                                     to

            timeSpentWorkingTogether = ChronoUnit.DAYS.between(emp2From, emp2To);
        }

        return timeSpentWorkingTogether;
    }

}
