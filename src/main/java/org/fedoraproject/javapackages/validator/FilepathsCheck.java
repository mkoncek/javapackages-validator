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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
                    throw Common.INCOMPLETE_READ;
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

    static Collection<String> checkSymlinks(String packageName, Filepaths config, Path envRoot, Iterable<Path> paths) throws IOException {
        var result = new ArrayList<String>(0);

        // The union of file paths present in all RPM files mapped to the RPM file names they are present in
        var files = new TreeMap<String, ArrayList<String>>();

        // The map of symbolic link names to their targets present in all RPM files
        var symlinks = new TreeMap<String, Path>();

        for (var path : paths) {
            var filename = path.getFileName();
            if (filename == null) {
                throw new IllegalArgumentException("Invalid RPM path: " + path);
            }

            String rpmName = path.toString();

            for (var pair : filesAndSymlinks(path).entrySet()) {
                files.computeIfAbsent(pair.getKey().getName().substring(1), key -> new ArrayList<String>()).add(rpmName);

                if (pair.getValue() != null) {
                    symlinks.put(pair.getKey().getName().substring(1),
                            envRoot.resolve(Paths.get("." + pair.getValue())).toAbsolutePath().normalize());
                }
            }
        }

        for (var pair : files.entrySet()) {
            if (pair.getValue().size() > 1) {
                if (config != null && config.allowedDuplicateFile(packageName, pair.getKey(), pair.getValue().stream().map(
                        s -> Paths.get(s).getFileName().toString()).collect(Collectors.toUnmodifiableList()))) {
                    System.err.println(MessageFormat.format("[INFO] Allowed duplicate file {0} provided by multiple RPMs: {1}",
                            pair.getKey(), pair.getValue()));
                } else {
                    result.add(MessageFormat.format("[FAIL] File {0} provided by multiple RPMs: {1}",
                            pair.getKey(), pair.getValue()));
                }
            }
        }

        for (var pair : symlinks.entrySet()) {
            if (!Files.exists(pair.getValue())) {
                result.add(MessageFormat.format("[FAIL] {0}: Link {1} points to {2} which is not present on the filesystem",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue()));
            } else {
                System.err.println(MessageFormat.format("[INFO] {0}: Link {1} points to file {2} which is present on the filesystem",
                        files.get(pair.getKey()), pair.getKey(), pair.getValue()));
            }
        }

        return result;
    }

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        int argsBegin = 0;

        Path envRoot = Paths.get("/");

        for (int i = 0; i != args.length; ++i) {
            if (args[i].equals("--envroot")) {
                envRoot = Paths.get(args[i + 1]).resolve(".").toAbsolutePath().normalize();
                argsBegin += 2;
            }
        }

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.FilepathsConfig");
        var config = (Filepaths) configClass.getConstructor().newInstance();

        // A map of package name -> collection of all binary rpms that were built from it
        var packages = new TreeMap<String, Collection<Path>>();

        for (int i = argsBegin; i != args.length; ++i) {
            packages.computeIfAbsent(Common.getPackageName(Paths.get(args[i])), k -> new ArrayList<>())
                .add(Paths.get(args[i]).resolve(".").toAbsolutePath().normalize());
        }

        var messages = new ArrayList<String>(0);

        for (var pair : packages.entrySet()) {
            messages.addAll(checkSymlinks(pair.getKey(), config, envRoot, pair.getValue()));
        }

        for (var message : messages) {
            exitcode = 1;
            System.out.println(message);
        }

        System.exit(exitcode);
    }
}
