package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.lang3.tuple.Pair;
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

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Incorrect claim that Exception is never thrown")
    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        var metadataXmls = new ArrayList<Pair<CpioArchiveEntry, byte[]>>();
        try (var is = new RpmArchiveInputStream(rpm.getURI().toURL())) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                if (rpmEntry.getName().startsWith("./usr/share/maven-metadata/") && rpmEntry.getName().endsWith(".xml")) {
                    byte[] content = new byte[(int) rpmEntry.getSize()];
                    if (is.read(content) != content.length) {
                        throw Common.INCOMPLETE_READ;
                    }
                    metadataXmls.add(Pair.of(rpmEntry, content));
                }
            }
        }

        if (metadataXmls.isEmpty()) {
            info("{0}: maven metadata XML file not found", Decorated.rpm(rpm));
            return;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        // TODO factory settings (validation, namespace, schema)

        /*
        URL schemaFile = new URL("https://fedora-java.github.io/xmvn/xsd/metadata-2.0.0.xsd");
        Source xmlFile = new StreamSource(new File("/home/mkoncek/Upstream/javapackages-validator/jakarta-activation1.xml"));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(schemaFile);
            var validator = schema.newValidator();
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (SAXException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
        } catch (IOException e) {}
        */

        for (var pair : metadataXmls) {
            Document metadata = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                try (var is = new ByteArrayInputStream(pair.getValue())) {
                    metadata = builder.parse(is);
                }
                var xpath = XPathFactory.newInstance().newXPath();
                var nodes = NodeList.class.cast(xpath.evaluate("//*[local-name()=\"artifact\"]/*[local-name()=\"path\"]", metadata, XPathConstants.NODESET));
                for (int i = 0; i != nodes.getLength(); ++i) {
                    var node = nodes.item(i);
                    var artifactPath = Paths.get(node.getTextContent());
                    var resolvedArtifactPath = envroot.resolve(Paths.get("/").relativize(artifactPath));
                    if (Files.exists(resolvedArtifactPath, LinkOption.NOFOLLOW_LINKS)) {
                        pass("{0}: {1}: artifact {2} is present on filesystem as {3}",
                                Decorated.rpm(rpm),
                                Decorated.outer(pair.getKey().getName()),
                                Decorated.actual(artifactPath),
                                Decorated.outer(resolvedArtifactPath));
                    } else {
                        fail("{0}: {1}: artifact {2} is not present on filesystem as {3}",
                                Decorated.rpm(rpm),
                                Decorated.outer(pair.getKey().getName()),
                                Decorated.expected(artifactPath),
                                Decorated.outer(resolvedArtifactPath));
                    }
                }
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }
}
