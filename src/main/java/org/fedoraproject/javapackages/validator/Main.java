package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;
import org.fedoraproject.javapackages.validator.compiler.InMemoryClassLoader;
import org.fedoraproject.javapackages.validator.compiler.InMemoryFileManager;
import org.fedoraproject.javapackages.validator.compiler.URIJavaFileObject;
import org.fedoraproject.javapackages.validator.validators.Validator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Main {
    private static TextDecorator DECORATOR = TextDecorator.NO_DECORATOR;
    private static PrintStream debugOutputStream = new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);

    public static TextDecorator getDecorator() {
        return DECORATOR;
    }

    @SuppressFBWarnings({"MS_EXPOSE_REP"})
    public static PrintStream getDebugOutputStream() {
        return debugOutputStream;
    }

    static record Flag(String... options) {
        static final Flag HELP = new Flag("-h", "--help");
        static final Flag CLASS_FILE = new Flag("-cf", "--class-file");
        static final Flag CLASS_URI = new Flag("-cu", "--class-uri");
        static final Flag SOURCE_FILE = new Flag("-sf", "--source-file");
        static final Flag SOURCE_URI = new Flag("-su", "--source-uri");
        static final Flag COLOR = new Flag("-r", "--color");
        static final Flag DEBUG = new Flag("-x", "--debug");

        public boolean equals(String arg) {
            return Stream.of(options()).anyMatch(arg::equals);
        }

        @Override
        public String toString() {
            return Stream.of(options()).collect(Collectors.joining(", "));
        }
    }

    static void printHelp() {
        System.out.println("Usage: Main [optional flags] <validator flags> <RPM files or directories to test...>");
        System.out.println("Options for specifying validators, can be specified multiple times");
        System.out.println("    " + Flag.SOURCE_FILE.toString() + " - File path of a source file");
        System.out.println("    " + Flag.SOURCE_URI.toString() + " - URI of a source file");
        System.out.println("    " + Flag.CLASS_FILE.toString() + " - File path of a class file");
        System.out.println("    " + Flag.CLASS_URI.toString() + " - URI of a class file");
        System.out.println("Optional flags:");
        System.out.println("    " + Flag.HELP.toString() + " - Print help message");
        System.out.println("    " + Flag.DEBUG.toString() + " - Display debugging output");
        System.out.println("    " + Flag.COLOR.toString() + " - Display colored output");
    }

    public static Map<String, ? extends JavaFileObject> compileFiles(Iterable<URI> sourceURIs, Iterable<String> compilerOptions, Logger logger) throws IOException {
        var compilationUnits = new ArrayList<JavaFileObject>();
        for (URI sourceURI : sourceURIs) {
            if (sourceURI.getScheme().equalsIgnoreCase("file")) {
                try (var stream = Files.find(Paths.get(sourceURI), Integer.MAX_VALUE, (path, attributes) ->
                        !attributes.isDirectory() && path.toString().endsWith(".java"), FileVisitOption.FOLLOW_LINKS)) {
                    stream.map((path) -> new URIJavaFileObject(path.toUri(), Kind.SOURCE)).forEach(compilationUnits::add);
                }
            } else {
                compilationUnits.add(new URIJavaFileObject(sourceURI, Kind.SOURCE));
            }
        }

        if (compilationUnits.isEmpty()) {
            logger.debug("No source files found");
            return Collections.emptyMap();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var fileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null));

        logger.debug("Compiling source configuration files: {0}", Decorated.list(compilationUnits));

        try {
            if (!compiler.getTask(null, fileManager, null, compilerOptions, null, compilationUnits).call()) {
                throw new RuntimeException("Failed to compile configuration sources");
            }
        } finally {
            fileManager.close();
        }

        return fileManager.getOutputs();
    }

    @SuppressFBWarnings({"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    protected static Collection<Validator> instantiateValidators(Map<String, ? extends JavaFileObject> classes, Logger logger) throws IOException {
        var result = new ArrayList<Validator>();

        logger.debug("Compiled class files: {0}", Decorated.list(List.copyOf(classes.keySet())));

        try {
            ClassLoader cl = new InMemoryClassLoader(classes);
            for (var className : classes.keySet()) {
                Class<?> cls = cl.loadClass(className);
                if (Validator.class.isAssignableFrom(cls)) {
                    var validator = Validator.class.cast(cls.getConstructor().newInstance());
                    validator.setLogger(logger);
                    result.add(validator);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        logger.debug("Instantiated validators: {0}", Decorated.list(result.stream().map(o -> o.getClass().getSimpleName()).toList()));

        return result;
    }

    static class ClassNameVisitor extends ClassVisitor {
        private String name = null;

        ClassNameVisitor() {
            super(Opcodes.ASM9);
        }

        public String getClassName() {
            return name;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            this.name = name.replace('/', '.');
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("error: no arguments provided");
            printHelp();
            System.exit(1);
        } else if (Flag.HELP.equals(args[0])) {
            printHelp();
            System.exit(0);
        }

        var sourceUris = new ArrayList<URI>();
        var classUris = new ArrayList<URI>();
        var argList = new ArrayList<String>(args.length);

        for (int i = 0; i != args.length; ++i) {
            if (Flag.COLOR.equals(args[i])) {
                DECORATOR = AnsiDecorator.INSTANCE;
                continue;
            } else if (Flag.DEBUG.equals(args[i])) {
                debugOutputStream = System.err;
                continue;
            } else if (Flag.SOURCE_URI.equals(args[i])) {
                ++i;
                try {
                    sourceUris.add(new URI(args[i]));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException(args[i], ex);
                }
            } else if (Flag.SOURCE_FILE.equals(args[i])) {
                ++i;
                sourceUris.add(Paths.get(args[i]).toUri());
            } else if (Flag.CLASS_URI.equals(args[i])) {
                ++i;
                try {
                    classUris.add(new URI(args[i]));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException(args[i], ex);
                }
            } else if (Flag.CLASS_FILE.equals(args[i])) {
                ++i;
                classUris.add(Paths.get(args[i]).toUri());
            } else if (args[i].startsWith("-")) {
                throw new RuntimeException("Unrecognized option: " + args[i]);
            } else {
                argList.add(args[i]);
            }
        }

        Logger logger = new Logger();
        logger.setStream(LogEvent.pass, System.out);
        logger.setStream(LogEvent.fail, System.out);

        logger.debug("Source URIs: {0}", Decorated.list(sourceUris));
        logger.debug("Class URIs: {0}", Decorated.list(classUris));
        logger.debug("Arguments: {0}", Decorated.list(argList));

        var classes = new TreeMap<String, JavaFileObject>(compileFiles(sourceUris, Arrays.asList(), logger));
        for (var classUri : classUris) {
            var uri = new URIJavaFileObject(classUri, Kind.CLASS);
            try (var is = uri.openInputStream()) {
                var classNameVisitor = new ClassNameVisitor();
                new ClassReader(is.readAllBytes()).accept(classNameVisitor, 0);
                classes.put(classNameVisitor.getClassName(), uri);
            }
        }

        var failMessages = new ArrayList<String>();
        for (Validator validator : instantiateValidators(classes, logger)) {
            validator.validate(new ArgFileIterator(argList));
            validator.getFailMessages().forEach(failMessages::add);
            for (String passMessage : validator.getPassMessages()) {
                System.out.println(passMessage);
            }
        }

        int exitCode = 0;
        if (failMessages.isEmpty()) {
            logger.info("Summary: all checks {0}", Decorated.custom("passed", Decoration.green, Decoration.bold));
            exitCode = 0;
        } else {
            logger.info("Summary: {0} checks {1}", Decorated.plain(failMessages.size()), Decorated.custom("failed", Decoration.red, Decoration.bold));
            exitCode = 1;
        }

        System.exit(exitCode);
    }
}
