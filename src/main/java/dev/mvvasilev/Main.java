package dev.mvvasilev;

import dev.mvvasilev.exception.EmployeeProjectException;
import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final String HELP_OPTION = "h";

    private static final String JSON_PARSE_OPTION = "j";

    private static final String XML_PARSE_OPTION = "x";

    private static final String CSV_PARSE_OPTION = "c";

    private static final String FILE_PATH_OPTION = "f";

    private static final String SINGLE_PROJECT_OPTION = "s";

    public static void main(String[] args) throws ParseException {
        var cliOptions = getOptions();

        var cliParser = new DefaultParser();

        var cmd = cliParser.parse(cliOptions, args);

        if (cmd.hasOption(HELP_OPTION)) {
            var formatter = new HelpFormatter();
            formatter.printHelp("cli", cliOptions);
            return;
        }

        runCli(cmd);
    }

    private static void runCli(CommandLine cmd) {
        var filePath = cmd.getOptionValue(FILE_PATH_OPTION);

        if (filePath == null) {
            System.out.println("Must provide file to parse ( '-f' or '--file' ). Please refer to -h or --help.");
            return;
        }

        var employeeService = new EmployeeProjectService();

        List<EmployeeProject> parsed = parseFile(employeeService, cmd, filePath);

        if (cmd.hasOption(SINGLE_PROJECT_OPTION)) {
            employeeService.findPairOfEmployeesWorkedLongestTimeTogetherOnSingleProject(parsed)
                    .ifPresentOrElse(r -> System.out.println(
                            "Result of pair of employees who have spent longest amount of time together on a single project: " +
                                    "[" + r.firstEmployeeId() + ", " + r.secondEmployeeId() + "] " +
                                    "on project " + r.projectId()
                    ), () -> System.out.println("No employees were found to have worked together on any project."));
        } else {
            employeeService.findPairOfEmployeesWorkedLongestTimeTogetherOnAnyProject(parsed)
                    .ifPresentOrElse(r -> System.out.println(
                            "Result of pair of employees who have spent longest amount of time together on any project: " +
                                    "[" + r.emp1() + ", " + r.emp2() + "] " +
                                    "for " + r.duration() + " days."
                    ), () -> System.out.println("No employees were found to have worked together on any project."));
            ;
        }
    }

    private static Options getOptions() {
        var cliOptions = new Options();
        cliOptions.addOption("h", "help", false, "Print this message");
        cliOptions.addOption(JSON_PARSE_OPTION, "xml", false, "Parse XML File");
        cliOptions.addOption(XML_PARSE_OPTION, "json", false, "Parse JSON File");
        cliOptions.addOption(CSV_PARSE_OPTION, "csv", false, "Parse CSV File ( Default )");
        cliOptions.addOption(FILE_PATH_OPTION, "file", true, "The file to parse ( must be of same type as provided parsing option (x, j, c) )");
        cliOptions.addOption(SINGLE_PROJECT_OPTION, "single", false, "Find pair of employees who have spent longest amount of time together on a single project");
        return cliOptions;
    }

    private static List<EmployeeProject> parseFile(EmployeeProjectService service, CommandLine cmd, String filePath) {
        var path = Path.of(filePath);

        if (cmd.hasOption(JSON_PARSE_OPTION)) {
            return service.parseJson(path);
        }

        if (cmd.hasOption(XML_PARSE_OPTION)) {
            return service.parseXml(path);
        }

        // Parse csv by default, no need to check if csv option was provided explicitly
        return service.parseCsv(path);
    }
}