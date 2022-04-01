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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javapackages.validator.config.Filepaths;

public class FilepathsCheck {
    /**
     * @param path
     * @return A map of file paths mapped to either the target of the symlink
     * or null, if the file pat is not a symlink.
     * @throws IOException
     */
    static SortedMap<CpioArchiveEntry, String> filesAndSymlinks(Path path) throws IOException {
        var result = new TreeMap<CpioArchiveEntry, String>((lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));

        try (var is = new RpmArchiveInputStream(path)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                if (rpmEntry.isDirectory()) {
                    continue;
                }

                var content = new byte[(int) rpmEntry.getSize()];

                if (is.read(content) != content.length) {
                    throw new IOException("Incomplete read in RPM stream");
                }

                String target = null;

                if (rpmEntry.isSymbolicLink()) {
                    target = new String(content, StandardCharsets.UTF_8);
                    Path parent = Paths.get(rpmEntry.getName().substring(1)).getParent();

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

    static Collection<String> checkSymlinks(String packageName, Filepaths config, Iterable<Path> paths) throws IOException {
        var result = new ArrayList<String>(0);

        // The union of file paths present in all RPM files mapped to the RPM file names they are present in
        var files = new TreeMap<String, ArrayList<String>>();

        // The map of symbolic link names to their targets present in all RPM files
        var symlinks = new TreeMap<String, String>();

        for (var path : paths) {
            var filePath = path.getFileName();
            if (filePath == null) {
                throw new IllegalArgumentException("Invalid RPM path: " + path);
            }

            String rpmName = path.toString();

            for (var pair : filesAndSymlinks(path).entrySet()) {
                files.computeIfAbsent(pair.getKey().getName().substring(1), key -> new ArrayList<String>()).add(rpmName);

                if (pair.getValue() != null) {
                    symlinks.put(pair.getKey().getName().substring(1), pair.getValue());
                }
            }
        }

        for (var pair : files.entrySet()) {
            if (pair.getValue().size() > 1) {
                if (!config.allowedDuplicateFile(packageName, pair.getKey(), pair.getValue().stream().map(
                        s -> Paths.get(s).getFileName().toString()).collect(Collectors.toUnmodifiableList()))) {
                    result.add(MessageFormat.format("[FAIL] File {0} provided by multiple RPMs: {1}",
                            pair.getKey(), pair.getValue()));
                } else {
                    System.err.println(MessageFormat.format("[INFO] Allowed duplicate file {0} provided by multiple RPMs: {1}",
                            pair.getKey(), pair.getValue()));
                }
            }
        }

        for (var pair : symlinks.entrySet()) {
            if (!Files.exists(Paths.get(pair.getValue()))) {
                result.add(MessageFormat.format("[FAIL] {0}: Link {1} points to {2} which is not present on the filesystem",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue()));
            } else {
                System.err.println(MessageFormat.format("[INFO] {0}: Link {1} points to file {2} which is present on the filesystem",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue()));
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.FilepathsConfig");
        var config = (Filepaths) configClass.getDeclaredField("INSTANCE").get(null);

        var paths = new ArrayList<Path>(args.length - 1);

        for (int i = 1; i != args.length; ++i) {
            paths.add(Paths.get(args[i]).resolve(".").toAbsolutePath().normalize());
        }

        var messages = checkSymlinks(args[0], config, paths);

        for (var message : messages) {
            exitcode = 1;
            System.out.println(message);
        }

        System.exit(exitcode);
    }
}
