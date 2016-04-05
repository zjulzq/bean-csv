# Bean CSV
Read bean objects from a csv file or write bean objects to a csv file.

## Features
1. Read bean objects from a csv file.
1. Write bean objects to a csv file.
1. `@CsvColumn` indicates the column names, column order and column format.

## Usage

Please refer to the following examples.

1. How to use `@CsvColumn`?
<pre>
  public class Worker {
    @CsvColumn(name="id", orderKey="A")
    private String id;

    @CsvColumn(name="firstName", orderKey="B")
    private String firstName;

    @CsvColumn(name="lastName", orderKey="C")
    private String lastName;

    @CsvColumn(name="birthday", orderKey="D", format="yyyyMMdd")
    private Date birthday;

    // setters and getters
  }
</pre>
1. How to read bean objects?
<pre>
  CSVReader csvReader = new CSVReader(new FileReader("beancsv.csv"));
  List<Worker> workers = BeanCsv.parseBeans(csvReader, Worker.class, true);
  csvReader.close();
</pre>
1. How to write bean objects?
<pre>
  CSVWriter csvWriter = new CSVWriter(new FileWriter(new File("beancsv.csv")));
  BeanCsv.writeHeader(csvWriter, Worker.class);
  List<Worker> workers = new ArrayList<>();
  workers.add(new Worker());
  workers.add(new Worker());
  BeanCsv.write(csvWriter, workers);
  csvWriter.close();
</pre>

## Contributing
We accept PRs via github. There are some guidelines which will make applying PRs easier for us:

1. No tabs! Please use spaces for indentation.
1. Respect the code style.
1. Create minimal diffs - disable on save actions like reformat source code or organize imports. If you feel the source code should be reformatted create a separate PR for this change.
1. Provide JUnit tests for your changes and make sure your changes don't break any existing tests by running `mvn clean test`.

## License
Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).
