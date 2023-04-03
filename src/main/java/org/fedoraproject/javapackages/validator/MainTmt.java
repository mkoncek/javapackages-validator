package org.fedoraproject.javapackages.validator;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javapackages.validator.validators.Validator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MainTmt extends Main {
    private static final Path testDataDir = Paths.get(System.getenv("TMT_TEST_DATA"));

    private static String toDashCase(String value) {
        var result = new StringBuilder();
        for (int i = 0; i != value.length(); ++i) {
            if (Character.isUpperCase(value.charAt(i))) {
                result.append('-');
                result.append(Character.toLowerCase(value.charAt(i)));
            } else {
                result.append(value.charAt(i));
            }
        }
        if (value.length() > 0 && Character.isUpperCase(value.charAt(0)) ) {
            return result.substring(1);
        }
        return result.toString();
    }

    static private class HtmlTablePrintStream extends PrintStream {
        public HtmlTablePrintStream(OutputStream os) {
            super(os, false, StandardCharsets.UTF_8);
            super.println("""
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

    @SuppressFBWarnings({"REC_CATCH_EXCEPTION", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
    @Override
    void report(List<Validator> validators) {
        try {
            try (var os = new FileOutputStream(testDataDir.resolve("filter.js").toFile());
                    var is = this.getClass().getResourceAsStream("/tmt_html/filter.js")) {
                is.transferTo(os);
            }
            try (var os = new FileOutputStream(testDataDir.resolve("style.css").toFile());
                    var is = this.getClass().getResourceAsStream("/tmt_html/style.css")) {
                is.transferTo(os);
            }
            for (var validator : validators) {
                var logFile = testDataDir.resolve(validator.getClass().getSimpleName());
                try (var os = new FileOutputStream(logFile.toString() + ".log")) {
                }
                try (var os = new FileOutputStream(logFile.toString() + ".html");
                        var ps = new HtmlTablePrintStream(os)) {
                    for (var p : validator.getMessages()) {
                        ps.printRow(p);
                    }
                }
                try (var os = new FileOutputStream(testDataDir.resolve("results.yaml").toFile(), true);
                        var ps = new PrintStream(os, false, StandardCharsets.UTF_8);) {
                    ps.print("- name: /");
                    var name = validator.getClass().getSimpleName();
                    if (name.endsWith("Validator")) {
                        name = name.substring(0, name.length() - 9);
                    }
                    ps.println(toDashCase(name));
                    ps.print("  result: ");
                    ps.println(validator.getResult());
                    ps.println("  log:");
                    var reLativeLogPath = testDataDir.resolve(validator.getClass().getSimpleName());
                    System.out.println(reLativeLogPath);
                    // NOTE workaround for tmt relative paths
                    reLativeLogPath = reLativeLogPath.getParent().getParent().getParent().getParent().getParent().relativize(reLativeLogPath);
                    ps.println("   - " + reLativeLogPath + ".log");
                    ps.println("   - " + reLativeLogPath + ".html");
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public static void main(String[] args) throws Exception {
        var main = new MainTmt();
        Main.DECORATOR = HtmlDecorator.INSTANCE;
        main.report(main.execute(args));
    }
}
