package org.fedoraproject.javapackages.validator.compiler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FilesystemClassLoader extends ClassLoader {
    private List<Path> roots = new ArrayList<>(0);

    public FilesystemClassLoader(List<Path> roots, ClassLoader parent) {
        super(parent);
        this.roots.addAll(roots);
    }

    public FilesystemClassLoader(List<Path> roots) {
        this(roots, null);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (var root : roots) {
            var path = root.resolve(Paths.get(name.replace(".", File.separator) + ".class"));

            if (Files.isRegularFile(path)) {
                try (var is = Files.newInputStream(path)) {
                    var bytes = is.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        throw new ClassNotFoundException(name);
    }
}
