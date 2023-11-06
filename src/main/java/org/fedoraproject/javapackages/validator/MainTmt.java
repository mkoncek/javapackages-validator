package org.fedoraproject.javapackages.validator;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.collections4.IterableUtils;
import org.yaml.snakeyaml.Yaml;

public class MainTmt extends Main {
    private Path TMT_TEST_DATA = null;
    private Path TMT_TREE = null;
    private Map<String, List<LogEntry>> additionalLogs = new TreeMap<>();

    private static String getenv(String key) {
        var result = System.getenv(key);
        if (result == null) {
            throw new RuntimeException("Environment variable " + Objects.toString(key) + " not set");
        }
        return result;
    }

    public MainTmt() {
        TMT_TEST_DATA = Paths.get(getenv("TMT_TEST_DATA"));
        TMT_TREE = Paths.get(getenv("TMT_TREE"));
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

        public void printRow(LogEntry entry) {
            println("  <tr class=\"" + entry.kind() + "\">");
            println("    <td style=\"text-align:center;\">" + HtmlDecorator.INSTANCE.decorate(entry.kind().getDecoratedText()) + "</td>");
            println("    <td>" + Main.decoratedObjects(entry, HtmlDecorator.INSTANCE) + "</td>");
            println("  </tr>");
        }

        @Override
        public void close() {
            super.println("</table>");
            super.println("</html>");
        }
    }

    @Override
    Map<String, Validator> select(Map<String, Validator> validators) throws Exception {
        var validatorTests = super.select(validators);

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
                    throw ex;
                }
            }

            for (var entry : configuration.entrySet()) {
                var key = String.class.cast(entry.getKey());
                Validator validator;

                if (key.startsWith("/") && (validator = validatorTests.get(key)) != null) {
                    List<String> args;
                    try {
                        args = ((List<?>) entry.getValue()).stream().map(o -> String.class.cast(o)).toList();
                    } catch (ClassCastException ex) {
                        var result = this.reports.computeIfAbsent(validator.getTestName(), k -> new DefaultResult());
                        result.error("{0}", Decorated.plain("Wrong format of validator arguments " +
                                "in configuration Yaml file, must be a list of strings"));
                        continue;
                    }

                    var previous = parameters.validatorArgs.put(validator.getClass().getCanonicalName(), Optional.of(args));
                    if (previous != null) {
                        additionalLogs.put(validator.getTestName(), new ArrayList<>(Collections.singletonList(
                                LogEntry.debug("Overriding arguments for {0}", Decorated.struct(validator.getClass().getCanonicalName())))));
                    }
                }
            }

            var exclusions = Collections.<Pattern>emptyList();
            List<?> exclude = List.class.cast(configuration.get("exclude-tests-matching"));
            if (exclude != null) {
                exclusions = exclude.stream().map(pattern -> Pattern.compile(String.class.cast(pattern))).toList();
                logger.debug("Found exclusion patterns: {0}", Decorated.list(exclusions));
                for (var entry : validators.entrySet()) {
                    for (var exclusionPattern : exclusions) {
                        if (exclusionPattern.matcher(entry.getKey()).matches()) {
                            var message = LogEntry.info("Exclusion pattern {0} matches test {1}",
                                    Decorated.actual(exclusionPattern),
                                    Decorated.struct(entry.getKey()));
                            logger.debug(message.pattern(), message.objects());
                            var result = this.reports.computeIfAbsent(entry.getKey(), k -> new DefaultResult());
                            result.info(message.pattern(), message.objects());
                            break;
                        }
                    }
                }
            }
        }

        for (var exclusion : this.reports.keySet()) {
            validators.remove(exclusion);
        }

        return validators;
    }

    private static String getFormattedDuration(LocalDateTime startTime, LocalDateTime endTime) {
        var duration = Duration.between(startTime, endTime);
        return String.format("%02d:%02d:%02d.%03d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());
    }

    @Override
    void report(List<NamedResult> results) throws Exception {
        try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve("filter.js"));
                var is = MainTmt.class.getResourceAsStream("/tmt_html/filter.js")) {
            is.transferTo(os);
        }
        try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve("style.css"));
                var is = MainTmt.class.getResourceAsStream("/tmt_html/style.css")) {
            is.transferTo(os);
        }

        Files.createDirectories(TMT_TEST_DATA.resolve("results"));

        var testResults = IterableUtils.chainedIterable(results, this.reports.entrySet().stream()
                .map(e -> new NamedResult(e.getValue(), e.getKey())).toList());

        for (var namedResult : testResults) {
            var resultFile = "results/";
            resultFile += namedResult.getTestName().substring(1).replace('/', '.');

            var chainedLogs = IterableUtils.chainedIterable(additionalLogs
                    .getOrDefault(namedResult.getTestName(), Collections.emptyList()), namedResult);

            try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve(resultFile + ".log"));
                    var ps = new PrintStream(os, false, StandardCharsets.UTF_8)) {
                for (var entry : chainedLogs) {
                    ps.println(Main.decorated(entry));
                }
            }
            try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve(resultFile + ".html"));
                    var ps = new HtmlTablePrintStream(os)) {
                for (var entry : chainedLogs) {
                    ps.printRow(entry);
                }
            }

            var resultYaml = new StringBuilder();
            resultYaml.append("- name: '");
            resultYaml.append(namedResult.getTestName());
            resultYaml.append("'");
            resultYaml.append(System.lineSeparator());
            resultYaml.append("  result: ");
            resultYaml.append(namedResult.getResult());
            resultYaml.append(System.lineSeparator());

            var startTime = namedResult.getStartTime();
            if (startTime != null) {
                resultYaml.append("  starttime: '");
                resultYaml.append(startTime.format(DateTimeFormatter.ISO_DATE_TIME));
                resultYaml.append("'");
                resultYaml.append(System.lineSeparator());
            }

            var endTime = namedResult.getEndTime();
            if (endTime != null) {
                resultYaml.append("  endtime: '");
                resultYaml.append(endTime.format(DateTimeFormatter.ISO_DATE_TIME));
                resultYaml.append("'");
                resultYaml.append(System.lineSeparator());
            }

            if (startTime != null && endTime != null) {
                resultYaml.append("  duration: ");
                resultYaml.append(getFormattedDuration(startTime, endTime));
                resultYaml.append(System.lineSeparator());
            }

            resultYaml.append("  log: ");
            resultYaml.append(System.lineSeparator());
            resultYaml.append("   - '");
            resultYaml.append(resultFile);
            resultYaml.append(".log'");
            resultYaml.append(System.lineSeparator());
            resultYaml.append("   - '");
            resultYaml.append(resultFile);
            resultYaml.append(".html'");
            resultYaml.append(System.lineSeparator());

            try (var os = Files.newOutputStream(TMT_TEST_DATA.resolve("results.yaml"),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                os.write(resultYaml.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    protected Path resolveRelativePath(Path path) {
        return TMT_TEST_DATA.resolve(path);
    }

    public static void main(String[] args) throws Exception {
        new MainTmt().run(args);
    }
}
