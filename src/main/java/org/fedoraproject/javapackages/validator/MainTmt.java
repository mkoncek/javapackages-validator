package org.fedoraproject.javapackages.validator;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

    @SuppressFBWarnings({"REC_CATCH_EXCEPTION"})
    @Override
    void report(List<Validator> validators) {
        for (var validator : validators) {
            var logFile = testDataDir.resolve(validator.getClass().getCanonicalName());
            try {
                try (var osFail = new FileOutputStream(logFile.toString() + "-fail");
                        var osFull = new FileOutputStream(logFile.toString() + "-full");
                        var psFail = new PrintStream(osFail, false, StandardCharsets.UTF_8);
                        var psFull = new PrintStream(osFull, false, StandardCharsets.UTF_8);) {
                    for (var p : validator.getMessages()) {
                        psFull.println(p.getValue());
                        if (LogEvent.fail.equals(p.getKey())) {
                            psFail.println(p.getValue());
                        }
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
                    ps.println("   - " + validator.getClass().getCanonicalName() + "-fail.log");
                    ps.println("   - " + validator.getClass().getCanonicalName() + "-full.log");
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        var main = new MainTmt();
        main.report(main.execute(args));
    }
}
