package org.fedoraproject.javapackages.validator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javapackages.validator.validators.Validator;

public class MainTmt extends Main {
    private Path TMT_TEST_DATA = null;
    private String TMT_TEST_NAME = null;

    public MainTmt() {
        TMT_TEST_DATA = Paths.get(System.getenv("TMT_TEST_DATA"));
        TMT_TEST_NAME = System.getenv("TMT_TEST_NAME");
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
    void report(List<Validator> validators) {
        try {
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

            var result = new StringBuilder();
            result.append("- name: ");
            result.append(TMT_TEST_NAME);
            result.append(System.lineSeparator());
            result.append("  result: ");
            result.append(validators.stream().map(Validator::getResult).max(Comparator.comparing(Enum::ordinal)).get());
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
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    protected Path resolveRelativePath(Path path) {
        return TMT_TEST_DATA.resolve(path);
    }

    public static void main(String[] args) throws Exception {
        Main.DECORATOR = HtmlDecorator.INSTANCE;
        final var TMT_TEST_DATA = Paths.get(System.getenv("TMT_TEST_DATA"));

        try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("filter.js").toFile());
                var is = MainTmt.class.getResourceAsStream("/tmt_html/filter.js")) {
            is.transferTo(os);
        }
        try (var os = new FileOutputStream(TMT_TEST_DATA.resolve("style.css").toFile());
                var is = MainTmt.class.getResourceAsStream("/tmt_html/style.css")) {
            is.transferTo(os);
        }

        var main = new MainTmt();

        main.report(main.execute(args));
    }
}
