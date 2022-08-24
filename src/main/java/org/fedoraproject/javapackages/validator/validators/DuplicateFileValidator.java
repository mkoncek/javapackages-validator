package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.utils.Iterators;
import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.RpmPathInfo;

public abstract class DuplicateFileValidator extends Validator {
    @Override
    public void validate(Iterator<RpmPathInfo> rpmIt) throws IOException {
        var rpms = new ArrayList<RpmPathInfo>();
        Iterators.addAll(rpms, rpmIt);

        // The union of file paths present in all RPM files mapped to the RPM file names they are present in
        var files = new TreeMap<String, ArrayList<Pair<CpioArchiveEntry, Path>>>();

        for (RpmPathInfo rpm : rpms) {
            if (!new RpmPathInfo(rpm.getPath()).isSourcePackage()) {
                for (var pair : Common.rpmFilesAndSymlinks(rpm.getPath()).entrySet()) {
                    files.computeIfAbsent(Common.getEntryPath(pair.getKey()).toString(), key -> new ArrayList<>())
                        .add(Pair.of(pair.getKey(), rpm.getPath()));
                }
            }
        }

        for (var entry : files.entrySet()) {
            if (entry.getValue().size() > 1) {
                var providers = new ArrayList<RpmInfo>(entry.getValue().size());
                for (var providerPair : entry.getValue()) {
                    providers.add(new RpmInfo(providerPair.getValue()));
                }
                var okDifferentArchs = new Boolean[] {true};
                // If all providers are of different architecture (with the
                // exception of noarch), then it is ok
                providers.sort((lhs, rhs) -> {
                    int cmp = lhs.getArch().compareTo(rhs.getArch());
                    if (cmp == 0 || lhs.getArch().equals("noarch") || rhs.getArch().equals("noarch")) {
                        okDifferentArchs[0] = false;
                    }
                    return cmp;
                });

                // If the file entry is a directory in all providers, then it is ok
                boolean okDirectory = entry.getValue().stream().map(Pair::getKey).allMatch(CpioArchiveEntry::isDirectory);

                String decoratedFile = textDecorate(entry.getKey(), DECORATION_ACTUAL);
                String decoratedProviders = entry.getValue().stream().map(pair -> textDecorate(pair.getValue(), DECORATION_RPM)).toList().toString();

                if (okDifferentArchs[0]) {
                    pass("File {0} provided by RPMs of unique architectures: {1}",
                            decoratedFile, decoratedProviders);
                    return;
                }

                if (okDirectory) {
                    pass("Directory {0} provided by multiple RPMs: {1}",
                            decoratedFile, decoratedProviders);
                    return;
                }

                validate(Paths.get(entry.getKey()), Collections.unmodifiableCollection(providers));
            }
        }
    }

    public abstract void validate(Path path, Collection<? extends RpmInfo> providerRpms) throws IOException;

    public static abstract class DuplicateFileValidatorDefault extends DuplicateFileValidator {
        @Override
        public void validate(Path path, Collection<? extends RpmInfo> providerRpms) throws IOException {
            validateDefault(path, Collections.unmodifiableCollection(providerRpms));

            String decoratedFile = textDecorate(path, DECORATION_ACTUAL);
            String decoratedProviders = providerRpms.stream().map(provider -> textDecorate(provider, DECORATION_RPM)).toList().toString();

            if (!failed()) {
                pass("Allowed duplicate file {0} provided by multiple RPMs: {1}",
                        decoratedFile, decoratedProviders);
                return;
            }

            fail("File {0} provided by multiple RPMs: {1}",
                    decoratedFile, decoratedProviders);
        }

        public abstract boolean validateDefault(Path path, Collection<? extends RpmInfo> providerRpms) throws IOException;
    }
}
