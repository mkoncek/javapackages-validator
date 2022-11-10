package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class RpmInfoURI extends RpmInfo {
    private final URI uri;

    public RpmInfoURI(URI uri) throws IOException {
        super(uri.toURL());
        this.uri = uri;
    }

    public static RpmInfoURI create(URI uri) {
        try {
            return new RpmInfoURI(uri);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public URI getURI() {
        return uri;
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
