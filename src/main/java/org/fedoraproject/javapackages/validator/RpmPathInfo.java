package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Path;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class RpmPathInfo extends RpmInfo {
    private final Path path;

    public RpmPathInfo(Path path) throws IOException {
        super(path);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
