package org.fedoraproject.javapackages.validator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class Check<Config> {
    private Map<Class<?>, Object> configurations = null;

    private static void compileFiles(Path sourceDir, Iterable<String> compilerOptions) throws IOException {
        List<File> inputFiles = Files.find(sourceDir, Integer.MAX_VALUE,
                (path, attributes) -> attributes.isRegularFile() && path.toString().endsWith(".java"))
                .map(Path::toFile).toList();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        try {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(inputFiles);
            if (!compiler.getTask(null, fileManager, null, compilerOptions, null, compilationUnits).call()) {
                throw new RuntimeException("Failed to compile configuration sources");
            }
        } finally {
            fileManager.close();
        }
    }

    @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    protected Config getConfig(Class<Config> configClass) throws IOException {
        final Path config_bin_dir = Paths.get("/mnt/config/bin");

        if (Files.notExists(config_bin_dir)) {
            compileFiles(Paths.get("/mnt/config/src"), Arrays.asList("-d", config_bin_dir.toString()));
        }

        if (configurations == null) {
            var classes = Files.find(config_bin_dir, Integer.MAX_VALUE,
                    (path, attributes) -> attributes.isRegularFile() && path.toString().endsWith(".class")).map(Path::toString).toArray(String[]::new);

            for (int i = 0; i != classes.length; ++i) {
                classes[i] = classes[i].substring(config_bin_dir.toString().length() + 1);
                classes[i] = classes[i].substring(0, classes[i].length() - 6);
                classes[i] = classes[i].replace('/', '.');
            }

            try (URLClassLoader cl = new URLClassLoader(new URL[] {config_bin_dir.toUri().toURL()})) {
                configurations = new HashMap<>();
                for (var className : classes) {
                    Class<?> cls = cl.loadClass(className);
                    for (var intrfc : cls.getInterfaces()) {
                        Object instance = cls.getConstructor().newInstance();
                        configurations.computeIfAbsent(intrfc, (i) -> instance);
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }

        return configClass.cast(configurations.get(configClass));
    }

    protected abstract Collection<String> check(String packageName, Path rpmPath, Config config) throws IOException;

    protected int executeCheck(Class<Config> configClass, String... args) throws IOException {
        int result = 0;

        var config = getConfig(configClass);
        String packageName = args[0];

        for (int i = 1; i != args.length; ++i) {
            for (var message : check(packageName, Paths.get(args[i]).resolve(".").toAbsolutePath().normalize(), config)) {
                result = 1;
                System.out.println(message);
            }
        }

        return result;
    }
}
