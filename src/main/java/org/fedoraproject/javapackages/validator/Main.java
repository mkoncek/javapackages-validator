package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.ToolProvider;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;
import org.fedoraproject.javapackages.validator.compiler.FilesystemClassLoader;
import org.fedoraproject.javapackages.validator.compiler.InMemoryClassLoader;
import org.fedoraproject.javapackages.validator.compiler.InMemoryFileManager;
import org.fedoraproject.javapackages.validator.compiler.URIJavaFileObject;
import org.fedoraproject.javapackages.validator.validators.Validator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Main {
    static TextDecorator DECORATOR = TextDecorator.NO_DECORATOR;
    static PrintStream debugOutputStream = new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);

    public static TextDecorator getDecorator() {
        return DECORATOR;
    }

    @SuppressFBWarnings({"MS_EXPOSE_REP"})
    public static PrintStream getDebugOutputStream() {
        return debugOutputStream;
    }

    static record Flag(String... options) {
        static final Flag SOURCE_FILE = new Flag("-sf", "--source-file");
        static final Flag SOURCE_URI = new Flag("-su", "--source-uri");

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
            SOURCE_FILE, SOURCE_URI, CLASS_PATH, FILE, URI, HELP, COLOR, DEBUG,
        };
    }


    static void printHelp() {
        System.out.println("Usage: Main [optional flags] <validator class name> <validator flags> <{-f | -u} RPM files or directories to test>...");
        System.out.println("    " + Flag.HELP + " - Print help message");
        System.out.println();
        System.out.println("Options for specifying validators, can be specified multiple times:");
        System.out.println("    " + Flag.SOURCE_FILE + " - File path of a source file");
        System.out.println("    " + Flag.SOURCE_URI + " - URI of a source file");
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

    public static Map<String, JavaFileObject> compileFile(URI sourceURI, Iterable<String> compilerOptions, Logger logger) throws IOException {
        var compilationUnits = new ArrayList<JavaFileObject>();
        if (sourceURI.getScheme().equalsIgnoreCase("file")) {
            try (var stream = Files.find(Paths.get(sourceURI), Integer.MAX_VALUE, (path, attributes) ->
                    !attributes.isDirectory() && path.toString().endsWith(".java"), FileVisitOption.FOLLOW_LINKS)) {
                stream.map((path) -> new URIJavaFileObject(path.toUri(), Kind.SOURCE)).forEach(compilationUnits::add);
            }
        } else {
            compilationUnits.add(new URIJavaFileObject(sourceURI, Kind.SOURCE));
        }

        if (compilationUnits.isEmpty()) {
            synchronized(logger) {
                logger.debug("No source files found");
            }
            return Collections.emptyMap();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var fileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null));

        synchronized(logger) {
            logger.debug("Compiling source files: {0}", Decorated.list(compilationUnits));
        }

        try {
            if (!compiler.getTask(null, fileManager, null, compilerOptions, null, compilationUnits).call()) {
                throw new RuntimeException("Failed to compile sources");
            }
        } finally {
            fileManager.close();
        }

        return fileManager.getOutputs();
    }

    @SuppressFBWarnings({"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    protected static Validator instantiateValidator(ClassLoader classLoader,
            String className, String[] args) throws IOException {
        try {
            Class<?> classType = classLoader.loadClass(className);
            if (!Validator.class.isAssignableFrom(classType)) {
                throw new RuntimeException(MessageFormat.format("{0} is not derived from Validator class", classType));
            }

            if (Modifier.isAbstract(classType.getModifiers())) {
                throw new RuntimeException(MessageFormat.format("{0} is convertible to Validator but is abstract", classType));
            }

            var validator = Validator.class.cast(classType.getConstructor().newInstance());
            if (args != null) {
                validator.arguments(args);
            }
            return validator;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <T> int tryReadArgs(List<MutablePair<T, String[]>> result, String[] args, int pos) {
        if (pos + 1 < args.length && args[pos + 1].equals("[")) {
            pos += 2;
            var begin = pos;
            while (!args[pos].equals("]")) {
                ++pos;
            }
            result.get(result.size() - 1).setValue(Arrays.copyOfRange(args, begin, pos));
        }
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

    @SuppressFBWarnings({"DM_EXIT", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    List<Validator> execute(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("error: no arguments provided");
            printHelp();
            System.exit(1);
        } else if (Flag.HELP.equals(args[0])) {
            printHelp();
            System.exit(0);
        }

        var sourceUris = new ArrayList<URI>();
        var classpathRoots = new ArrayList<Path>();
        var classNames = new ArrayList<MutablePair<String, String[]>>();
        var argsPath = new ArrayList<String>();
        var argsUri = new ArrayList<URI>();

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
                classNames.add(MutablePair.of(args[i], null));
                i = tryReadArgs(classNames, args, i);
            } else if (lastFlag == Flag.COLOR) {
                DECORATOR = AnsiDecorator.INSTANCE;
                --i;
            } else if (lastFlag == Flag.DEBUG) {
                debugOutputStream = System.err;
                --i;
            } else if (lastFlag == Flag.SOURCE_FILE) {
                sourceUris.add(resolveRelativePathCommon(args[i]).toUri());
            } else if (lastFlag == Flag.SOURCE_URI) {
                sourceUris.add(new URI(args[i]));
            } else if (lastFlag == Flag.CLASS_PATH) {
                classpathRoots.add(resolveRelativePathCommon(args[i]));
            } else if (lastFlag == Flag.FILE) {
                argsPath.add(args[i]);
            } else if (lastFlag == Flag.URI) {
                argsUri.add(new URI(args[i]));
            }
        }

        Logger logger = new Logger();
        logger.setStream(LogEvent.pass, System.out);
        logger.setStream(LogEvent.fail, System.out);

        logger.debug("Source URIs: {0}", Decorated.list(sourceUris));
        logger.debug("Additional classpath entries: {0}", Decorated.list(classpathRoots));
        logger.debug("File arguments: {0}", Decorated.list(argsPath));
        logger.debug("URI arguments: {0}", Decorated.list(argsUri));

        var classes = new TreeMap<String, JavaFileObject>();
        sourceUris.parallelStream().forEach(sourceUri -> {
            try {
                var result = compileFile(sourceUri, Arrays.asList(), logger);
                synchronized (classes) {
                    classes.putAll(result);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        logger.debug("Compiled class files: {0}", Decorated.list(List.copyOf(classes.keySet())));
        logger.debug("Validators: {0}", Decorated.list(classNames.stream().map(MutablePair::getKey).toList()));

        var classLoader = new InMemoryClassLoader(classes, new FilesystemClassLoader(classpathRoots, ClassLoader.getSystemClassLoader()));
        var validators = new ArrayList<Validator>();

        for (var className : classNames) {
            validators.add(instantiateValidator(classLoader, className.getKey(), className.getValue()));
        }

        logger.debug("Instantiated validators: {0}", Decorated.list(validators.stream().map(o -> o.getClass().getSimpleName()).toList()));

        validators.parallelStream().forEach(validator -> validator.pubvalidate(
                IteratorUtils.<RpmInfoURI>chainedIterator(new ArgFileIterator(argsPath),
                    argsUri.stream().map(RpmInfoURI::create).iterator()))
        );

        return validators;
    }

    protected static final String decorated(Pair<LogEvent, String> entry) {
        return "[" + entry.getKey().getDecoratedText() + "] " + entry.getValue();
    }

    @SuppressFBWarnings({"DM_EXIT"})
    void report(List<Validator> validators) {
        int passMessages = 0;
        for (Validator validator : validators) {
            for (var p : validator.getMessages()) {
                if (LogEvent.pass.equals(p.getKey()) || LogEvent.info.equals(p.getKey())) {
                    System.out.println(decorated(p));
                    ++passMessages;
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
        for (Validator validator : validators) {
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

    public static void main(String[] args) throws Exception {
        var main = new Main();
        main.report(main.execute(args));
    }
}
