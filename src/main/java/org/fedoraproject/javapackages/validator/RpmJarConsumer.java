package org.fedoraproject.javapackages.validator;

import java.util.function.Consumer;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmFile;

public interface RpmJarConsumer extends Consumer<RpmFile> {
    @Override
    default void accept(RpmFile rpm) {
        try (var is = new RpmArchiveInputStream(rpm)) {
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

    void acceptJarEntry(RpmFile rpm, CpioArchiveEntry rpmEntry, byte[] content) throws Exception;
}
