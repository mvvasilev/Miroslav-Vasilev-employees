# The Employee-Project CLI

## Building
For ease-of-use, this project provides a `farjar` gradle task which
can be run using `gradle build fatjar`. This compiles the project into
a jar file, located at `/build/libs/employee-cli-{version}.jar`

## Running

### Usage:

```bash
java -jar employee-cli-1.0-SNAPSHOT.jar 
 -c,--csv          Parse CSV File ( Default )
 -f,--file <arg>   The file to parse ( must be of same type as provided
                   parsing option (x, j, c) )
 -h,--help         Print this message
 -j,--xml          Parse XML File
 -s,--single       Find pair of employees who have spent longest amount of
                   time together on a single project
 -x,--json         Parse JSON File
```

### Examples:

Parse CSV file called `employees.csv` and find pair of employees who have worked together the
longest on any project:
```bash
java -jar employee-cli-1.0-SNAPSHOT.jar -f employees.csv
```

Parse JSON file called `employees.json` and find pair of employees who have worked together the
longest on any project:
```bash
java -jar employee-cli-1.0-SNAPSHOT.jar -jf employees.json
```

Parse XML file called `employees.xml` and find pair of employees who have worked together the
longest on any project:
```bash
java -jar employee-cli-1.0-SNAPSHOT.jar -xf employees.xml
```

### Single Project Mode

It is also possible to find the pair of employees who have worked for the longest
time together on a single project by passing the `s` flag:

```bash
java -jar employee-cli-1.0-SNAPSHOT.jar -sf employees.csv
```

```bash
java -jar employee-cli-1.0-SNAPSHOT.jar -sxf employees.xml
```

### Note on JSON & XML
`DateTo`, like in the csv variant, can be empty to imply that the employee is still
working on a project, but unlike in CSV, its value in XML and JSON cannot be `NULL`.
If `DateTo` is provided, please ensure it is a valid date pattern, matching `yyyy-MM-dd`.
If the desire is to not have a `DateTo` at all, simply omit the field from the json/xml.