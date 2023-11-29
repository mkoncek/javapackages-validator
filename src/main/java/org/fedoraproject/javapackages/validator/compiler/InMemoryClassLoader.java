package org.fedoraproject.javapackages.validator.compiler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.TreeMap;

import javax.tools.JavaFileObject;

public final class InMemoryClassLoader extends ClassLoader {
    private Map<String, Class<?>> classes = new TreeMap<>();

    public InMemoryClassLoader(Map<String, ? extends JavaFileObject> classes, ClassLoader parent) {
        super(parent);

        for (var entry : classes.entrySet()) {
            byte[] bytes;
            try (var is = entry.getValue().openInputStream()) {
                bytes = is.readAllBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            this.classes.put(entry.getKey(), defineClass(entry.getKey(), bytes, 0, bytes.length));
        }
    }

    public InMemoryClassLoader(Map<String, ? extends JavaFileObject> classes) {
        this(classes, null);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> result = classes.get(name);
        if (result != null) {
            return result;
        }
        throw new ClassNotFoundException(name);
    }
}
