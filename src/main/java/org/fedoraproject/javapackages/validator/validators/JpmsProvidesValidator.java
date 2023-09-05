package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;

public class JpmsProvidesValidator extends JarValidator {
    private Map<String, String> jarModuleNames = new TreeMap<>();

    @Override
    public void validateJarEntry(RpmInfoURI rpm, CpioArchiveEntry rpmEntry, byte[] content) throws IOException {
        var moduleName = Optional.<String>empty();

        try (var is = new JarInputStream(new ByteArrayInputStream(content))) {
            for (JarEntry entry; (entry = is.getNextJarEntry()) != null;) {
                if (entry.getName().equals("module-info.class")) {
                    var md = ModuleDescriptor.read(ByteBuffer.wrap(is.readNBytes((int) entry.getSize())));
                    moduleName = Optional.of(md.name());
                    break;
                }
            }

            if (moduleName.isEmpty()) {
                var mf = is.getManifest();
                moduleName = Optional.ofNullable(mf.getMainAttributes().getValue("Automatic-Module-Name"));
            }
        }

        if (moduleName.isPresent()) {
            debug("{0}: Module name {1} found in {2}",
                    Decorated.rpm(rpm),
                    Decorated.actual(moduleName.get()),
                    Decorated.outer(Common.getEntryPath(rpmEntry)));
            jarModuleNames.put(moduleName.get(), Common.getEntryPath(rpmEntry).toString());
        }
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        super.validate(rpm);
        var providedModuleNames = new TreeSet<String>();

        for (var reldep : rpm.getProvides()) {
            var name = reldep.getName();
            if (name.startsWith("jpms(") && name.endsWith(")")) {
                providedModuleNames.add(name);
            }
        }

        boolean ok = true;

        for (var providedModuleName : providedModuleNames) {
            if (! jarModuleNames.keySet().contains(providedModuleName)) {
                ok = false;
                fail("{0}: Module name {1} provided by this RPM was not found in any JAR file",
                        Decorated.rpm(rpm),
                        Decorated.actual(providedModuleName));
            }
        }

        for (var jarModuleNameEntry : jarModuleNames.entrySet()) {
            if (! providedModuleNames.contains(jarModuleNameEntry.getKey())) {
                ok = false;
                fail("{0}: Module name {1} of {2} is not provided by this RPM",
                        Decorated.rpm(rpm),
                        Decorated.actual(jarModuleNameEntry.getKey()),
                        Decorated.outer(jarModuleNameEntry.getValue()));
            }
        }

        if (ok) {
            pass("{0}: found module names exactly match provided jpms fields, {1}",
                    Decorated.rpm(rpm),
                    Decorated.list(providedModuleNames.stream().toList()));
        }
    }
}
