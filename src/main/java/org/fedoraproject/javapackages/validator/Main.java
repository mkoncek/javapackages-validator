package org.fedoraproject.javapackages.validator;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Main {
    static TextDecorator DECORATOR = TextDecorator.NO_DECORATOR;
    static PrintStream debugOutputStream = new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);

    protected Parameters parameters;
    protected Logger logger;

    public static TextDecorator getDecorator() {
        return DECORATOR;
    }

    @SuppressFBWarnings({"MS_EXPOSE_REP"})
    public static PrintStream getDebugOutputStream() {
        return debugOutputStream;
    }

    static record Flag(String... options) {
        static final Flag SOURCE_PATH = new Flag("-sp", "--source-path");
        static final Flag CLASS_PATH = new Flag("-cp", "--class-path");

        static final Flag FILE = new Flag("-f", "--file");
        static final Flag URI = new Flag("-u", "--uri");

        static final Flag HELP = new Flag("-h", "--help");
        static final Flag COLOR = new Flag("-r", "--color");
        static final Flag DEBUG = new Flag("-x", "--debug");

        public boolean equals(String arg) {
            return Stream.of(options()).anyMatch(arg::equals);
        }

        @Override
        public String toString() {
            return Stream.of(options()).collect(Collectors.joining(", "));
        }

        static final Flag[] ALL_FLAGS = new Flag[] {
            SOURCE_PATH, CLASS_PATH, FILE, URI, HELP, COLOR, DEBUG,
        };
    }

    static void printHelp() {
        System.out.println("Usage: Main [optional flags] <validator class name> <validator flags> <{-f | -u} RPM files or directories to test>...");
        System.out.println("    " + Flag.HELP + " - Print help message");
        System.out.println();
        System.out.println("Options for specifying validators, can be specified multiple times:");
        System.out.println("    " + Flag.SOURCE_PATH + " - File path of a source file");
        System.out.println("    " + Flag.CLASS_PATH + " - Additional class path entry");
        System.out.println();
        System.out.println("Validator arguments can be immediately followed by space-separated square parentheses");
        System.out.println("the contents of which will be passed as arguments to the validator.");
        System.out.println();
        System.out.println("Options for specifying tested RPM files, can be specified multiple times:");
        System.out.println("    " + Flag.FILE + " - File path of an .rpm file");
        System.out.println("    " + Flag.URI + " - URI of an .rpm file");
        System.out.println();
        System.out.println("Optional flags:");
        System.out.println("    " + Flag.DEBUG + " - Display debugging output");
        System.out.println("    " + Flag.COLOR + " - Display colored output");
    }

    private static Optional<FileTime> getRecursiveFileTime(Path path, BiPredicate<Path, BasicFileAttributes> filter) {
    	try (var stream = Files.find(path, Integer.MAX_VALUE, filter, FileVisitOption.FOLLOW_LINKS)) {
    	    return stream.map((p) -> {
    	        try {
    	            return Files.getLastModifiedTime(p);
	            } catch (IOException ex) {
	                throw new RuntimeException(ex);
                }
	        }).max((lhs, rhs) -> lhs.compareTo(rhs));
	    } catch (IOException ex) {
	        throw new RuntimeException(ex);
    	}
    }

    public static void compileFiles(Path sourcePath, Path classPath, Iterable<String> compilerOptions, Logger logger) throws IOException {
        var sourceMtime = getRecursiveFileTime(sourcePath, (p, a) -> true).get();

        if (Files.notExists(classPath)) {
            Files.createDirectories(classPath);
        }

        var classMtime = getRecursiveFileTime(classPath, (p, a) -> !a.isDirectory());
        var classDMtime = getRecursiveFileTime(classPath, (p, a) -> a.isDirectory()).get();

        boolean recompile = false;

        if (classMtime.isEmpty()) {
            recompile = true;
            logger.debug("No class files are present on the specified class path");
        } else if (sourceMtime.compareTo(classMtime.get()) > 0) {
            recompile = true;
            logger.debug("Source path is more up-to-date than the specified class path");
        } else if (classDMtime.compareTo(classMtime.get()) > 0) {
            recompile = true;
            logger.debug("Class path has possibly been modified");
        }

        if (recompile) {
            FileUtils.cleanDirectory(classPath.toFile());
            var javac = ToolProvider.getSystemJavaCompiler();
            var fileManager = javac.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);

            var sourceFiles = Files.find(sourcePath, Integer.MAX_VALUE, (path, attributes) ->
                !attributes.isDirectory() && path.toString().endsWith(".java"), FileVisitOption.FOLLOW_LINKS
            ).toList();
            var compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourceFiles);

            synchronized(logger) {
                logger.debug("Compiling source files: {0}", Decorated.list(sourceFiles));
            }

            try {
                if (!javac.getTask(null, fileManager, null, compilerOptions, null, compilationUnits).call()) {
                    throw new RuntimeException("Failed to compile sources");
                }
            } finally {
                fileManager.close();
            }
        } else {
            logger.debug("Not recompiling source files");
        }
    }

    static <T> int tryReadArgs(Map<String, Optional<String[]>> result, String[] args, int pos) {
        var origPos = pos;
        var vArgs = Optional.<String[]>empty();

        if (pos + 1 < args.length && args[pos + 1].equals("[")) {
            pos += 2;
            var begin = pos;

            while (!args[pos].equals("]")) {
                ++pos;

                if (pos == args.length) {
                    throw new RuntimeException("Missing ending ] parenthesis");
                }
            }

            vArgs = Optional.of(Arrays.copyOfRange(args, begin, pos));
        }

        result.put(args[origPos], vArgs);
        return pos;
    }

    private Path resolveRelativePathCommon(String path) {
        var result = Paths.get(path);

        if (!result.isAbsolute()) {
            result = resolveRelativePath(result);
        }

        return result;
    }

    protected Path resolveRelativePath(Path path) {
        return path;
    }

    protected static class Parameters {
        Path sourcePath = null;
        Path classPath = null;
        List<String> argPaths = new ArrayList<>(0);
        List<URI> argUris = new ArrayList<>(0);
        Map<String, Optional<String[]>> validatorArgs = new LinkedHashMap<>();
    }

    @SuppressFBWarnings({"DM_EXIT"})
    void parseArguments(String[] args) throws Exception {
        parameters = new Parameters();

        if (args.length == 0) {
            System.out.println("error: no arguments provided");
            printHelp();
            System.exit(1);
        } else if (Flag.HELP.equals(args[0])) {
            printHelp();
            System.exit(0);
        }

        Flag lastFlag = null;
        for (int i = 0; i != args.length; ++i) {
            if (args[i].startsWith("-")) {
                lastFlag = null;

                for (Flag flag : Flag.ALL_FLAGS) {
                    if (flag.equals(args[i])) {
                        lastFlag = flag;
                        ++i;
                        break;
                    }
                }

                if (lastFlag == null) {
                    throw new RuntimeException("Unrecognized option: " + args[i]);
                }
            } else {
                lastFlag = null;
            }

            if (lastFlag == null) {
                i = tryReadArgs(parameters.validatorArgs, args, i);
            } else if (lastFlag == Flag.COLOR) {
                DECORATOR = AnsiDecorator.INSTANCE;
                --i;
            } else if (lastFlag == Flag.DEBUG) {
                debugOutputStream = System.err;
                --i;
            } else if (lastFlag == Flag.SOURCE_PATH) {
                parameters.sourcePath = resolveRelativePathCommon(args[i]);
            } else if (lastFlag == Flag.CLASS_PATH) {
                parameters.classPath = resolveRelativePathCommon(args[i]);
            } else if (lastFlag == Flag.FILE) {
                parameters.argPaths.add(args[i]);
            } else if (lastFlag == Flag.URI) {
                parameters.argUris.add(new URI(args[i]));
            }
        }

        logger = new Logger();

        logger.debug("Source path: {0}", Decorated.plain(parameters.sourcePath));
        logger.debug("Class path: {0}", Decorated.plain(parameters.classPath));
        logger.debug("Path arguments: {0}", Decorated.list(parameters.argPaths));
        logger.debug("URI arguments: {0}", Decorated.list(parameters.argUris));
    }

    private List<Validator> discover() throws Exception {
        if (parameters.sourcePath != null && parameters.classPath == null) {
            throw new RuntimeException("If source path is specified then class path needs to be specified too");
        }

        if (parameters.classPath != null) {
            compileFiles(parameters.sourcePath, parameters.classPath, List.of("-d", parameters.classPath.toString()), logger);
            var serviceOutFile = Files.createDirectories(parameters.classPath.resolve("META-INF").resolve("services")).resolve(Validator.class.getCanonicalName());
            try (var os = new FileOutputStream(serviceOutFile.toFile())) {
                for (var serviceFile : Files.find(parameters.sourcePath, Integer.MAX_VALUE, (p, a) -> !a.isDirectory() && p.getFileName().equals(Paths.get(Validator.class.getCanonicalName()))).toList()) {
                    try (var is = new FileInputStream(serviceFile.toFile())) {
                        is.transferTo(os);
                    }
                }
            }
        }

        var classLoader = new URLClassLoader(parameters.classPath == null ? new URL[] {} : new URL[] {parameters.classPath.toUri().toURL()});
        var validators = ServiceLoader.<Validator>load(Validator.class, classLoader).stream().map(ServiceLoader.Provider::get).toList();

        logger.debug("Available validator services:{0}", Decorated.plain(validators.stream().map(v ->
            System.lineSeparator() + Decorated.struct(v.getClass().getCanonicalName())
        ).collect(Collectors.joining())));

        return Collections.unmodifiableList(validators);
    }

    List<Validator> select(List<Validator> validators) throws Exception {
        logger.debug("Main arguments: {0}", Decorated.plain(parameters.validatorArgs.entrySet().stream().map(e -> {
            var result = new StringBuilder();
            result.append(System.lineSeparator());
            result.append(Decorated.custom(e.getKey(), Decoration.bright_green).toString());
            if (e.getValue().isPresent()) {
                var args = e.getValue().get();
                result.append(Decorated.custom(" [ ", Decoration.bright_blue).toString());
                result.append(Stream.of(args).map(a -> Decorated.custom(a, Decoration.cyan).toString()).collect(Collectors.joining(" ")));
                result.append(Decorated.custom(" ]", Decoration.bright_blue).toString());
            }
            return result.toString();
        }).collect(Collectors.joining())));

        Predicate<Validator> selector = v -> {
            var args = parameters.validatorArgs.get(v.getClass().getCanonicalName());

            if (args != null) {
                if (args.isPresent()) {
                    try {
                        v.arguments(args.get());
                    } catch (Exception ex) {
                        var stackTrace = new ByteArrayOutputStream();
                        ex.printStackTrace(new PrintStream(stackTrace, false, StandardCharsets.UTF_8));
                        v.error("An exception occured during arugment processing in {0}:{1}{2}",
                                Decorated.struct(v.getClass().getCanonicalName()),
                                Decorated.plain(System.lineSeparator()),
                                Decorated.plain(new String(stackTrace.toByteArray(), StandardCharsets.UTF_8)));
                        return false;
                    }
                }

                return true;
            }

            return false;
        };

        if (parameters.validatorArgs.isEmpty()) {
            selector = v -> true;
        }

        return validators.stream().filter(selector).toList();
    }

    @SuppressFBWarnings({"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    List<Validator> execute(List<Validator> validators) throws Exception {
        logger.debug("Selected validators:{0}", Decorated.plain(validators.stream().map(v ->
            System.lineSeparator() + Decorated.struct(v.getClass().getCanonicalName())
        ).collect(Collectors.joining())));

        validators.parallelStream().forEach(validator -> validator.pubvalidate(
                IteratorUtils.<RpmInfoURI>chainedIterator(new ArgFileIterator(parameters.argPaths),
                        parameters.argUris.stream().map(RpmInfoURI::create).iterator())));

        return validators;
    }

    protected static final String decorated(Pair<LogEvent, String> entry) {
        return "[" + entry.getKey().getDecoratedText() + "] " + entry.getValue();
    }

    @SuppressFBWarnings({"DM_EXIT"})
    void report(List<Validator> validators) throws Exception {
        int passMessages = 0;
        for (Validator validator : validators) {
            for (var p : validator.getMessages()) {
                if (!LogEvent.fail.equals(p.getKey()) && !LogEvent.error.equals(p.getKey())) {
                    if (LogEvent.debug.equals(p.getKey())) {
                        getDebugOutputStream().println(decorated(p));
                    } else {
                        System.out.println(decorated(p));
                    }

                    if (LogEvent.pass.equals(p.getKey())) {
                        ++passMessages;
                    }
                }
            }
        }

        int failMessages = 0;
        for (Validator validator : validators) {
            for (var p : validator.getMessages()) {
                if (LogEvent.fail.equals(p.getKey())) {
                    System.out.println(decorated(p));
                    ++failMessages;
                }
            }
        }

        int errorMessages = 0;
        for (var validator : validators) {
            for (var p : validator.getMessages()) {
                if (LogEvent.error.equals(p.getKey())) {
                    System.out.println(decorated(p));
                    ++errorMessages;
                }
            }
        }

        int exitCode = 0;
        if (failMessages == 0 && errorMessages == 0) {
            if (passMessages > 0) {
                System.err.println(MessageFormat.format("Summary: all checks {0}", Decorated.custom("passed", Decoration.green, Decoration.bold)));
            } else {
                System.err.println("Summary: no output available");
            }
        } else if (errorMessages == 0) {
            System.err.println(MessageFormat.format("Summary: {0} {1}", Decorated.plain(failMessages), Decorated.custom(
                    "failed check" + (failMessages == 1 ? "" : "s"), Decoration.red, Decoration.bold)));
            exitCode = 1;
        } else if (failMessages == 0) {
            System.err.println(MessageFormat.format("Summary: {0} {1} occured", Decorated.plain(errorMessages), Decorated.custom(
                    "error" + (errorMessages == 1 ? "" : "s"), Decoration.red, Decoration.bold)));
            exitCode = 2;
        }

        System.exit(exitCode);
    }

    void run(String[] args) throws Exception {
        parseArguments(args);
        var tests = discover();
        execute(select(tests));
        report(tests);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
