/*-
 * Copyright (c) 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;

public class FilepathsCheck {
    static SortedMap<CpioArchiveEntry, String> filesAndSymlinks(Path path) throws IOException {
        var result = new TreeMap<CpioArchiveEntry, String>((lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));

        try (var is = new RpmArchiveInputStream(path)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                var content = new byte[(int) rpmEntry.getSize()];
                if (is.read(content) != content.length) {
                    throw new IOException("Incomplete read in RPM stream");
                }

                String target = null;

                if (rpmEntry.isSymbolicLink()) {
                    target = new String(content, StandardCharsets.UTF_8);
                    Path parent = Paths.get(rpmEntry.getName()).getParent();
                    if (parent == null) {
                        throw new IllegalStateException("Path::getParent of " + rpmEntry.getName() + " returned null");
                    }
                    target = parent.resolve(Paths.get(target)).normalize().toString();
                }

                result.put(rpmEntry, target);
            }
        }

        return result;
    }

    static Collection<String> checkSymlinks(Iterable<Path> paths) throws IOException {
        var result = new ArrayList<String>(0);

        // The union of file paths present in all RPM files mapped to the RPM file names they are present in
        var files = new TreeMap<String, ArrayList<String>>();

        // The map of symbolic link names to their targets present in all RPM files
        var symlinks = new TreeMap<String, String>();

        for (var path : paths) {
            for (var pair : filesAndSymlinks(path).entrySet()) {
                var providers = files.computeIfAbsent(pair.getKey().getName().substring(1), key -> new ArrayList<String>());
                providers.add(path.toString());
                if (providers.size() != 1) {
                    if (!pair.getKey().isDirectory() && !pair.getKey().getName().startsWith("./usr/share/licenses/")) {
                        result.add(MessageFormat.format("[FAIL] {0}: File {1} provided by multiple packages: {2}",
                                path, pair.getKey().getName().substring(1), providers));
                    } else {
                        System.err.println(MessageFormat.format("[INFO] {0}: File {1} provided by multiple packages - allowed",
                                path, pair.getKey().getName().substring(1)));
                    }
                }
                if (pair.getValue() != null) {
                    symlinks.put(pair.getKey().getName().substring(1), pair.getValue());
                }
            }
        }

        for (var pair : symlinks.entrySet()) {
            if (!files.containsKey(pair.getValue())) {
                result.add(MessageFormat.format("[FAIL] {0}: Link {1} points to {2} which is not present in the RPM set",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue()));
            } else {
                System.err.println(MessageFormat.format("[INFO] {0}: Link {1} points to file {2} provided by {3}",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue(), files.get(pair.getValue())));
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        int returnCode = 0;

        var result = checkSymlinks(Stream.of(args)
                .map(arg -> Paths.get(arg).resolve(".").toAbsolutePath().normalize())
                .collect(Collectors.toUnmodifiableList()));

        for (var message : result) {
            returnCode = 1;
            System.out.println(message);
        }

        System.exit(returnCode);
    }
}
