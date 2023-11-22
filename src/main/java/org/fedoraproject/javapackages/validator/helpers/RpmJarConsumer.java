package org.fedoraproject.javapackages.validator.helpers;

import java.util.function.Consumer;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javapackages.validator.Common;

import io.kojan.javadeptools.rpm.RpmArchiveInputStream;
import io.kojan.javadeptools.rpm.RpmPackage;

public interface RpmJarConsumer extends Consumer<RpmPackage> {
    @Override
    default void accept(RpmPackage rpm) {
        try (var is = new RpmArchiveInputStream(rpm.getPath())) {
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                if (!rpmEntry.isSymbolicLink() && rpmEntry.getName().endsWith(".jar")) {
                    var content = new byte[(int) rpmEntry.getSize()];

                    if (is.read(content) != content.length) {
                        throw Common.INCOMPLETE_READ;
                    }

                    acceptJarEntry(rpm, rpmEntry, content);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void acceptJarEntry(RpmPackage rpm, CpioArchiveEntry rpmEntry, byte[] content) throws Exception;
}
