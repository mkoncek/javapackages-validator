package org.fedoraproject.javapackages.validator.compiler;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

public class InMemoryFileManager implements JavaFileManager {
    private JavaFileManager delegate;
    private Map<String, JavaFileObject> classOutputs = new TreeMap<>();

    public InMemoryFileManager(JavaFileManager delegate) {
        super();
        this.delegate = delegate;
    }

    public Map<String, JavaFileObject> getOutputs() {
        var result = classOutputs;
        classOutputs = new TreeMap<>();
        return result;
    }

    @Override
    public Location getLocationForModule(Location location, String moduleName) throws IOException {
        var result = delegate.getLocationForModule(location, moduleName);
        return result;
    }

    @Override
    public Location getLocationForModule(Location location, JavaFileObject fo)
            throws IOException {
        var result = delegate.getLocationForModule(location, fo);
        return result;
    }

    @Override
    public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service)
            throws IOException {
        var result = delegate.getServiceLoader(location, service);
        return result;
    }

    @Override
    public String inferModuleName(Location location) throws IOException {
        var result = delegate.inferModuleName(location);
        return result;
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        var result = delegate.listLocationsForModules(location);
        return result;
    }

    @Override
    public boolean contains(Location location, FileObject fo) throws IOException {
        var result = delegate.contains(location, fo);
        return result;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public int isSupportedOption(String option) {
        var result = delegate.isSupportedOption(option);
        return result;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        var result = delegate.isSameFile(a, b);
        return result;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        var result = delegate.getClassLoader(location);
        return result;
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName,
            String relativeName) throws IOException {
        var result = delegate.getFileForInput(location, packageName, relativeName);
        return result;
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName,
            String relativeName, FileObject sibling) throws IOException {
        var result = delegate.getFileForOutput(location, packageName, relativeName, sibling);
        return result;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location,
            String className, Kind kind) throws IOException {
        var result = delegate.getJavaFileForInput(location, className, kind);
        return result;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location,
            String className, Kind kind, FileObject sibling) throws IOException {
        if (kind.equals(Kind.CLASS) && location.equals(StandardLocation.CLASS_OUTPUT)) {
            var result = new InMemoryJavaClassFileObject(className);
            classOutputs.put(className, result);
            return result;
        }
        var result = delegate.getJavaFileForOutput(location, className, kind, sibling);
        return result;
    }

    @Override
    public boolean hasLocation(Location location) {
        var result = delegate.hasLocation(location);
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        var result = delegate.inferBinaryName(location, file);
        return result;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<Kind> kinds, boolean recurse) throws IOException {
        var result = delegate.list(location, packageName, kinds, recurse);
        return result;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        var result = delegate.handleOption(current, remaining);
        return result;
    }
}
