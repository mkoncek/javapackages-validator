package org.fedoraproject.javapackages.validator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
<script src="filter.js"></script>
<link rel="stylesheet" href="style.css">
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
            println("    <td style=\"text-align:center;\">" + entry.getKey().getDecoratedText() + "</td>");
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
        for (var test : discoverTests()) {
            parameters.validatorArgs.put(test, Optional.empty());
        }

        var planFile = Files.find(TMT_TREE, 1, (p, a) -> a.isRegularFile() && p.toString().endsWith(".fmf")).findFirst().get();

        Map<String, Object> plan;
        try (var is = new FileInputStream(planFile.toFile())) {
            plan = new Yaml().load(is);
        }

        var content = (Map<?, ?>) plan.entrySet().iterator().next().getValue();
        var context = (Map<?, ?>) content.get("context");

        // TODO think about allowing specifying additional validators in addition to discovered ones

        if (context != null) {
            for (var entry : context.entrySet()) {
                var key = String.class.cast(entry.getKey());
                if (key.startsWith("/")) {
                    String[] args;
                    try {
                        args = ((List<?>) entry.getValue()).toArray(String[]::new);
                    } catch (ClassCastException ex) {
                        for (var validator : validators) {
                            if (validator.getTestName().equals(key)) {
                                validator.error("{0}", Decorated.plain("Wrong format of validator arguments in tmt plan Yaml, must be a list of strings"));
                            }
                        }
                        parameters.validatorArgs.remove(key);
                        continue;
                    }

                    parameters.validatorArgs.computeIfPresent(key, (k, v) -> {
                        if (v.isPresent()) {
                            for (var validator : validators) {
                                if (validator.getTestName().equals(key)) {
                                    validator.error("{0}", Decorated.plain("Test plan contains duplicate validator argument fields"));
                                }
                            }

                            return null;
                        }
                        return Optional.of(args);
                    });
                }
            }
        }

        return super.select(validators);
    }

    @Override
    void report(List<Validator> validators) throws Exception {
        try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("filter.js").toFile());
                var is = MainTmt.class.getResourceAsStream("/tmt_html/filter.js")) {
            is.transferTo(os);
        }
        try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("style.css").toFile());
                var is = MainTmt.class.getResourceAsStream("/tmt_html/style.css")) {
            is.transferTo(os);
        }

        try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("result.log").toFile())) {
        }
        try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("result.html").toFile());
                var ps = new HtmlTablePrintStream(os)) {
            for (var validator : validators) {
                for (var p : validator.getMessages()) {
                    ps.printRow(p);
                }
            }
        }

        for (var validator : validators) {
            var result = new StringBuilder();
            result.append("- name: ");
            result.append(validator.getTestName());
            result.append(System.lineSeparator());
            result.append("  result: ");
            result.append(validator.getResult());
            result.append(System.lineSeparator());
            result.append("  log: ");
            result.append(System.lineSeparator());
            result.append("   - 'result.log'");
            result.append(System.lineSeparator());
            result.append("   - 'result.html'");
            result.append(System.lineSeparator());

            try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("results.yaml").toFile(), true)) {
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

        try (var is = new FileInputStream(testsFile.toFile())) {
            testsYaml = new Yaml().load(is);
        }

        return testsYaml.stream().map(m -> String.class.cast(m.get("name"))).collect(Collectors.toSet());
    }

    public static void main(String[] args) throws Exception {
        Main.DECORATOR = HtmlDecorator.INSTANCE;
        new MainTmt().run(args);
    }
}
