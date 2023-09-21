package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TmtTest;
import org.fedoraproject.xmvn.metadata.PackageMetadata;
import org.fedoraproject.xmvn.metadata.io.stax.MetadataStaxReader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Validator which checks that maven metadata XML file references files
 * present on the file system relative to the environment root.
 *
 * Ignores source rpms.
 */
@SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
@TmtTest("/java/maven_metadata")
public class MavenMetadataValidator extends ElementwiseValidator {
    public MavenMetadataValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Incorrect claim that Exception is never thrown")
    @Override
    public void validate(RpmFile rpm) throws Exception {
        var metadataXmls = new ArrayList<Pair<CpioArchiveEntry, byte[]>>();
        var foundFiles = new TreeSet<String>();
        try (var is = new RpmArchiveInputStream(rpm)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                foundFiles.add(Common.getEntryPath(rpmEntry).toString());
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

        for (var pair : metadataXmls) {
            PackageMetadata packageMetadata = null;

            try (var is = new ByteArrayInputStream(pair.getValue())) {
                packageMetadata = new MetadataStaxReader().read(is, true);
            } catch (XMLStreamException ex) {
                fail("{0}: metadata validation failed: {1}", Decorated.rpm(rpm), Decorated.plain(ex.getMessage()));
                continue;
            }

            for (var artifact : packageMetadata.getArtifacts()) {
                var artifactPath = Paths.get(artifact.getPath());
                var metadataXml = Common.getEntryPath(pair.getKey());
                if (foundFiles.contains(artifactPath.toString())) {
                    pass("{0}: {1}: artifact {2} is present in the RPM",
                            Decorated.rpm(rpm),
                            Decorated.outer(metadataXml),
                            Decorated.actual(artifactPath));
                } else {
                    fail("{0}: {1}: artifact {2} is not present in the RPM",
                            Decorated.rpm(rpm),
                            Decorated.outer(metadataXml),
                            Decorated.expected(artifactPath));
                }
            }
        }
    }
}
