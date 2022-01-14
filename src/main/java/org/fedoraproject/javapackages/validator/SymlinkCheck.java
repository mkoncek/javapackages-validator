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

public class SymlinkCheck {
    static SortedMap<String, String> filesAndSymlinks(Path path) throws IOException {
        var result = new TreeMap<String, String>();

        try (var is = new RpmArchiveInputStream(path)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                String rpmEntryName = rpmEntry.getName();

                if (rpmEntryName.startsWith("./")) {
                    rpmEntryName = rpmEntryName.substring(1);
                }

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

                result.put(rpmEntryName, target);
            }
        }

        return result;
    }

    static Collection<String> checkSymlinks(Iterable<Path> paths) throws IOException {
        var result = new ArrayList<String>(0);

        // The union of file paths present in all RPM files mapped to the RPM file names they are present in
        var files = new TreeMap<String, String>();

        // The map of symbolic link names to their targets present in all RPM files
        var symlinks = new TreeMap<String, String>();

        for (var path : paths) {
            for (var pair : filesAndSymlinks(path).entrySet()) {
                String previousRPM = null;
                if ((previousRPM = files.put(pair.getKey(), path.toString())) != null) {
                    result.add("Duplicate file present in " + previousRPM + " and in " + path.toString());
                }
                if (pair.getValue() != null) {
                    symlinks.put(pair.getKey(), pair.getValue());
                }
            }
        }

        for (var pair : symlinks.entrySet()) {
            if (!files.containsKey(pair.getValue())) {
                result.add(MessageFormat.format("{0}: {1} points to {2} which is not present in the RPM set",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue()));
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
