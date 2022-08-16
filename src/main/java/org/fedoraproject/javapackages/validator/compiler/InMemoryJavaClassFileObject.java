package org.fedoraproject.javapackages.validator.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class InMemoryJavaClassFileObject extends SimpleJavaFileObject {
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "it is intended to share the same stream with the writer")
    private ByteArrayOutputStream content = new ByteArrayOutputStream();

    public InMemoryJavaClassFileObject(String name) {
        super(URI.create("class:///" + name), Kind.CLASS);
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(content.toByteArray());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return content;
    }
}
