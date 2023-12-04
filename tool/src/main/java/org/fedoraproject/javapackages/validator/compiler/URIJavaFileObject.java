package org.fedoraproject.javapackages.validator.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.tools.SimpleJavaFileObject;

public class URIJavaFileObject extends SimpleJavaFileObject {
    private ByteArrayOutputStream content = null;

    public URIJavaFileObject(URI uri, Kind kind) {
        super(uri, kind);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        try (var is = openInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        if (content == null) {
            content = new ByteArrayOutputStream();
            try (var is = uri.toURL().openStream()) {
                is.transferTo(content);
            }
        }

        return new ByteArrayInputStream(content.toByteArray());
    }
}
