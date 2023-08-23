package org.fedoraproject.javapackages.validator;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.yaml.snakeyaml.Yaml;

public class MainTmt extends Main {
    private Path TMT_TEST_DATA = null;
    private Path TMT_TREE = null;

    public MainTmt() {
        TMT_TEST_DATA = Paths.get(System.getenv("TMT_TEST_DATA"));
        TMT_TREE = Paths.get(System.getenv("TMT_TREE"));
    }

    private static class HtmlTablePrintStream extends PrintStream {
        public HtmlTablePrintStream(OutputStream os) {
            super(os, false, StandardCharsets.UTF_8);
            super.print("""
<!DOCTYPE html>
<html>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.3/jquery.min.js"></script>
<script src="../filter.js"></script>
<link rel="stylesheet" href="../style.css">
<table>
<tr>
    <th>Filter:</th>
    <th><input type="checkbox" class="filter_checkbox" id="checkbox_debug" value="debug"><label for="checkbox_debug">Debug</label></th>
    <th><input type="checkbox" class="filter_checkbox" id="checkbox_info" value="info"><label for="checkbox_info">Info</label></th>
    <th><input type="checkbox" class="filter_checkbox" id="checkbox_pass" value="pass"><label for="checkbox_pass">Pass</label></th>
    <th><input type="checkbox" class="filter_checkbox" id="checkbox_fail" value="fail"><label for="checkbox_fail">Fail</label></th>
    <th><input type="checkbox" class="filter_checkbox" id="checkbox_error" value="error"><label for="checkbox_error">Error</label></th>
</tr>
</table>

<table>
""");
        }

        public void printRow(Pair<LogEvent, String> entry) {
            println("  <tr class=\"" + entry.getKey() + "\">");
            println("    <td style=\"text-align:center;\">" + entry.getKey().getDecoratedText().toString(HtmlDecorator.INSTANCE) + "</td>");
            println("    <td>" + entry.getValue() + "</td>");
            println("  </tr>");
        }

        @Override
        public void close() {
            super.println("</table>");
            super.println("</html>");
        }
    }

    @Override
    List<Validator> select(List<Validator> validators) throws Exception {
        var discoveredTests = discoverTests();
        var validatorTests = new TreeMap<String, Validator>();
        validators = new ArrayList<>(validators);
        validators.sort((lhs, rhs) -> lhs.getTestName().compareTo(rhs.getTestName()));
        for (var validator : validators) {
            var testName = validator.getTestName();
            if (!discoveredTests.contains(testName)) {
                continue;
            }

            logger.debug("Test {0} is implemented by {1}",
                    Decorated.actual(testName),
                    Decorated.struct(validator.getClass().getCanonicalName()));

            parameters.validatorArgs.put(validator.getClass().getCanonicalName(), Optional.empty());

            if (!validatorTests.containsKey(testName)) {
                validatorTests.put(testName, validator);
            } else {
                Consumer<Validator> print = v -> v.error("Test is implemented by multiple validators: {0}",
                        Decorated.struct(v.getClass().getCanonicalName()));
                Optional.ofNullable(validatorTests.put(testName, null)).ifPresent(print);
                print.accept(validator);
            }
        }

        for (var entry : validatorTests.entrySet()) {
            if (entry.getValue() == null) {
                logger.debug("Test {0} implemented by multiple validators",
                        Decorated.actual(entry.getKey()));
            }
        }

        for (var validator : validatorTests.values()) {
            if (validator != null) {
                parameters.validatorArgs.put(validator.getClass().getCanonicalName(), Optional.empty());
            }
        }

        var optConfigFile = Optional.<Path>of(TMT_TREE.resolve("plans").resolve("javapackages-validator.yaml"));
        if (!Files.isRegularFile(optConfigFile.get())) {
            optConfigFile = Optional.<Path>of(TMT_TREE.resolve("javapackages-validator.yaml"));
            if (!Files.isRegularFile(optConfigFile.get())) {
                optConfigFile = Optional.empty();
            }
        }

        if (optConfigFile.isPresent()) {
            var configPath = optConfigFile.get();
            var configuration = Collections.<String, Object>emptyMap();;
            try (var is = Files.newInputStream(configPath)) {
                try {
                    configuration = new Yaml().load(is);
                } catch (Exception ex) {
                    var os = new ByteArrayOutputStream();
                    var ps = new PrintStream(os, false, StandardCharsets.UTF_8);
                    ex.printStackTrace(ps);
                    logger.debug("An exception occured when attempting to read yaml file {0}: {1}",
                            Decorated.actual(configPath),
                            Decorated.plain(new String(os.toByteArray(), StandardCharsets.UTF_8)));
                }
            }

            for (var entry : configuration.entrySet()) {
                var key = String.class.cast(entry.getKey());
                Validator validator;

                if (key.startsWith("/") && (validator = validatorTests.get(key)) != null) {
                    String[] args;
                    try {
                        args = ((List<?>) entry.getValue()).toArray(String[]::new);
                    } catch (ClassCastException ex) {
                        validator.error("{0}", Decorated.plain(
                                "Wrong format of validator arguments in tmt plan Yaml, must be a list of strings"));
                        parameters.validatorArgs.remove(validator.getClass().getCanonicalName());
                        continue;
                    }

                    parameters.validatorArgs.put(validator.getClass().getCanonicalName(), Optional.of(args));
                }
            }

            var exclusions = Collections.<Pattern>emptyList();
            var discover = configuration.get("discover");
            if (discover != null) {
                var exclude = (List<?>) ((Map<?, ?>) discover).get("exclude");
                if (exclude != null) {
                    exclusions = exclude.stream().map(pattern -> Pattern.compile(String.class.cast(pattern))).toList();
                    for (var validator : validators) {
                        if (exclusions.stream().anyMatch(pattern -> pattern.matcher(validator.getTestName()).matches())) {
                            parameters.validatorArgs.remove(validator.getClass().getCanonicalName());
                        }
                    }
                }
            }
        }

        return super.select(validators);
    }

    @Override
    void report(List<Validator> validators) throws Exception {
        try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve("filter.js"));
                var is = MainTmt.class.getResourceAsStream("/tmt_html/filter.js")) {
            is.transferTo(os);
        }
        try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve("style.css"));
                var is = MainTmt.class.getResourceAsStream("/tmt_html/style.css")) {
            is.transferTo(os);
        }

        Files.createDirectories(TMT_TEST_DATA.resolve("results"));

        var testReports = new TreeMap<String, ArrayList<Validator>>();
        for (var validator : validators) {
            if (parameters.validatorArgs.containsKey(validator.getClass().getCanonicalName())) {
                testReports.computeIfAbsent(validator.getTestName(), k -> new ArrayList<>()).add(validator);
            }
        }

        for (var entry : testReports.entrySet()) {
            var resultFile = "results/";
            resultFile += entry.getKey().substring(1).replace('/', '#');

            try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve(resultFile + ".log"))) {
            }
            try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve(resultFile + ".html"));
                var ps = new HtmlTablePrintStream(os)) {
                for (var validator : entry.getValue()) {
                    for (var p : validator.getMessages()) {
                        ps.printRow(p);
                    }
                }
            }

            var result = new StringBuilder();
            result.append("- name: '");
            result.append(entry.getKey());
            result.append("'");
            result.append(System.lineSeparator());
            result.append("  result: ");

            if (entry.getValue().size() == 1) {
                var validator = entry.getValue().get(0);
                result.append(validator.getResult());
                result.append(System.lineSeparator());
                result.append("  starttime: '");
                result.append(validator.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME));
                result.append("'");
                result.append(System.lineSeparator());
                result.append("  endtime: '");
                result.append(validator.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME));
                result.append("'");
                result.append(System.lineSeparator());
                result.append("  duration: ");
                result.append(validator.getFormattedDuration());
            } else {
                result.append("error");
            }

            result.append(System.lineSeparator());
            result.append("  log: ");
            result.append(System.lineSeparator());
            result.append("   - '");
            result.append(resultFile);
            result.append(".log'");
            result.append(System.lineSeparator());
            result.append("   - '");
            result.append(resultFile);
            result.append(".html'");
            result.append(System.lineSeparator());

            try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve("results.yaml"),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                os.write(result.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    protected Path resolveRelativePath(Path path) {
        return TMT_TEST_DATA.resolve(path);
    }

    private Set<String> discoverTests() throws Exception {
        var testsFile = TMT_TREE.getParent();

        if (testsFile == null) {
            throw new IllegalStateException("The parent of tmt plan data does not exist");
        }

        testsFile = testsFile.resolve("discover").resolve("tests.yaml");

        List<Map<String, Object>> testsYaml;

        try (var is = Files.newInputStream(testsFile)) {
            testsYaml = new Yaml().load(is);
        }

        var result = testsYaml.stream().map(m -> String.class.cast(m.get("name"))).collect(Collectors.toSet());

        logger.debug("Discovered tests:{0}", Decorated.plain(result.stream().map(t ->
            System.lineSeparator() + Decorated.struct(t)).collect(Collectors.joining())));

        return result;
    }

    public static void main(String[] args) throws Exception {
        Validator.DECORATOR = HtmlDecorator.INSTANCE;
        new MainTmt().run(args);
    }
}
