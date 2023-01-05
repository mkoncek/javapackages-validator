package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Validator which checks that maven metadata XML file references files
 * present on the file system relative to the environment root.
 *
 * Optional arguments:
 *     -e <envroot> -- Environment root (default is /)
 *
 * Ignores source rpms.
 */
@SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
public class MavenMetadataValidator extends ElementwiseValidator {
    private Path envroot = Paths.get("/");

    @Override
    public void arguments(String[] args) {
        if (args.length > 0 && args[0].equals("-e")) {
            envroot = Paths.get(args[1]);
        }
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        CpioArchiveEntry metadataXml = null;
        byte[] content = null;
        try (var is = new RpmArchiveInputStream(rpm.getURI().toURL())) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                if (rpmEntry.getName().equals("./usr/share/maven-metadata/" + rpm.getPackageName() + ".xml")) {
                    content = new byte[(int) rpmEntry.getSize()];
                    if (is.read(content) != content.length) {
                        throw Common.INCOMPLETE_READ;
                    }
                }
                metadataXml = rpmEntry;
            }
        }

        if (metadataXml == null) {
            info("{0}: maven metadata XML file not found", Decorated.rpm(rpm));
            return;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // TODO factory settings (validation, namespace, schema)

        Document metadata = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (var is = new ByteArrayInputStream(content)) {
                metadata = builder.parse(is);
            }
            var xpath = XPathFactory.newInstance().newXPath();
            var nodes = NodeList.class.cast(xpath.evaluate("//*[local-name()=\"artifact\"]/*[local-name()=\"path\"]", metadata, XPathConstants.NODESET));
            for (int i = 0; i != nodes.getLength(); ++i) {
                var node = nodes.item(i);
                var artifactPath = Paths.get(node.getTextContent());
                var resolvedArtifactPath = envroot.resolve(Paths.get("/").relativize(artifactPath));
                if (Files.exists(resolvedArtifactPath, LinkOption.NOFOLLOW_LINKS)) {
                    pass("{0}: artifact {1} is present on filesystem as {2}",
                            Decorated.rpm(rpm),
                            Decorated.actual(artifactPath),
                            Decorated.outer(resolvedArtifactPath));
                } else {
                    fail("{0}: artifact {1} is not present on filesystem as {2}",
                            Decorated.rpm(rpm),
                            Decorated.expected(artifactPath),
                            Decorated.outer(resolvedArtifactPath));
                }
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
