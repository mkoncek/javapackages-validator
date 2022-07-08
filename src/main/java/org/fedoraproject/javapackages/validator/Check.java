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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

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
            compiler.getTask(null, fileManager, null, compilerOptions, null, compilationUnits).call();
        } finally {
            fileManager.close();
        }
    }

    protected Config getConfig(Class<Config> configClass) throws IOException {
        final Path config_bin_dir = Paths.get("/mnt/config/bin");

        if (Files.notExists(config_bin_dir)) {
            Files.createDirectories(config_bin_dir);
            compileFiles(Paths.get("/mnt/config/src"), Arrays.asList("-d", config_bin_dir.toString()));
        }

        if (configurations == null) {
            var classes = (String[]) Files.find(config_bin_dir, Integer.MAX_VALUE,
                    (path, attributes) -> attributes.isRegularFile() && path.toString().endsWith(".class")).map(Path::toString).toArray();
            var urls = new URL[classes.length];

            for (int i = 0; i != classes.length; ++i) {
                urls[i] = Paths.get(classes[i]).toUri().toURL();
                classes[i] = classes[i].substring(config_bin_dir.toString().length() + 1);
                classes[i] = classes[i].substring(0, classes[i].length() - 6);
                classes[i] = classes[i].replace('/', '.');
            }

            try (URLClassLoader cl = new URLClassLoader(urls)) {
                configurations = new TreeMap<>();
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
