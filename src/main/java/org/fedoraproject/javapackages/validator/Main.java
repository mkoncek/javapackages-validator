package org.fedoraproject.javapackages.validator;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fedoraproject.javapackages.validator.Logger.LogEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Main {
    private static TextDecorator DECORATOR = TextDecorator.NO_DECORATOR;
    private static PrintStream debugOutputStream = new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    private static boolean alwaysRecompileConfig = false;
    private static Collection<RpmPathInfo> TEST_RPMS = Collections.emptyList();

    public static TextDecorator getDecorator() {
        return DECORATOR;
    }

    @SuppressFBWarnings({"MS_EXPOSE_REP"})
    public static PrintStream getDebugOutputStream() {
        return debugOutputStream;
    }

    public static boolean alwaysRecompileConfig() {
        return alwaysRecompileConfig;
    }

    public static void readTestRpmArgs(Iterable<String> args) {
        Main.TEST_RPMS = new ArrayList<>();
        for (var rpmIt = new ArgFileIterator(args); rpmIt.hasNext();) {
            Main.TEST_RPMS.add(rpmIt.next());
        }
    }

    public static Collection<RpmPathInfo> getTestRpms() {
        return Collections.unmodifiableCollection(TEST_RPMS);
    }

    static record Flag(String... options) {
        static final Flag CONFIG_FILE = new Flag("-c", "--config-file");
        static final Flag CONFIG_URI = new Flag("-u", "--config-uri");
        static final Flag CONFIG_DIRECTORY = new Flag("-d", "--directory");
        static final Flag COLOR = new Flag("-r", "--color");
        static final Flag DEBUG = new Flag("-x", "--debug");
        static final Flag RECOMPILE_CONFIG = new Flag("-p", "--recompile");

        public boolean equals(String arg) {
            return Stream.of(options()).anyMatch(arg::equals);
        }

        @Override
        public String toString() {
            return Stream.of(options()).collect(Collectors.joining(", "));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("error: no arguments provided");
            System.out.println("Usage: Main <simple class name of the check> [optional flags] <RPM files or directories to test...>");
            System.out.println("Optional flags:");
            System.out.println("    " + Flag.CONFIG_FILE.toString() + " - File path of a configuration source, can be specified multiple times");
            System.out.println("    " + Flag.CONFIG_URI.toString() + " - URI of a configuration source, can be specified multiple times");
            System.out.println("    " + Flag.CONFIG_DIRECTORY.toString() + " - Directory where compiled configuration class files will be put");
            System.out.println("    " + Flag.RECOMPILE_CONFIG.toString() + " - Force recompilation of configuration files");
            System.out.println("    " + Flag.DEBUG.toString() + " - Display debugging output");
            System.out.println("    " + Flag.COLOR.toString() + " - Display colored output");
            System.exit(1);
        }

        var argList = new ArrayList<>(args.length);

        for (String arg : args) {
            if (Flag.COLOR.equals(arg)) {
                DECORATOR = AnsiDecorator.INSTANCE;
                continue;
            } else if (Flag.DEBUG.equals(arg)) {
                debugOutputStream = System.err;
                continue;
            } else if (Flag.RECOMPILE_CONFIG.equals(arg)) {
                alwaysRecompileConfig = true;
            }

            argList.add(arg);
        }

        Check<?> check = Check.class.cast(Class.forName("org.fedoraproject.javapackages.validator.checks." + argList.get(0)).getConstructor().newInstance());
        check.getLogger().setStream(LogEvent.pass, System.out);
        System.exit(check.executeCheck(Arrays.copyOfRange(argList.toArray(String[]::new), 1, argList.size())));
    }
}
