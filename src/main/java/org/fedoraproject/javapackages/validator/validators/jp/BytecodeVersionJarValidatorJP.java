package org.fedoraproject.javapackages.validator.validators.jp;

import java.util.Map;
import java.util.Set;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.validators.BytecodeVersionJarValidator;

public class BytecodeVersionJarValidatorJP extends BytecodeVersionJarValidator {
    private Set<String> packagesReqVersion8 = Set.of(new String[] {
            "maven",
            "apache-commons-cli",
            "apache-commons-codec",
            "apache-commons-io",
            "apache-commons-lang3",
            "atinject cdi-api",
            "google-guice guava",
            "httpcomponents-client",
            "httpcomponents-core",
            "jakarta-annotations",
            "jansi",
            "jsr-305",
            "maven-resolver",
            "maven-shared-utils",
            "maven-wagon",
            "plexus-cipher",
            "plexus-classworlds",
            "plexus-containers",
            "plexus-interpolation",
            "plexus-sec-dispatcher",
            "plexus-utils",
            "sisu",
            "slf4j",

            "ant",
            "antlr",
            "apache-commons-net",
            "bcel",
            "bsf",
            "jakarta-activation1",
            "jakarta-mail",
            "jakarta-oro",
            "jdepend",
            "jsch",
            "jzlib",
            "regexp",
            "xalan-j2",
            "xerces-j2",
            "xml-commons-apis",
            "xml-commons-resolver",
    });

    @Override
    public void validate(RpmInfoURI rpm, String jarName, Map<String, Integer> classVersions) {
        if (classVersions.remove("module-info.class") != null) {
            info("{0}: {1}: ignoring {2}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarName, DECORATION_JAR),
                    Decorated.custom("module-info.class", DECORATION_CLASS));
        }

        for (var entry : classVersions.entrySet()) {
            String className = entry.getKey();
            int version = entry.getValue();

            if (version > 44 + 11) {
                fail("{0}: {1}: {2}: class bytecode version is {3} which is larger than {4}",
                        Decorated.rpm(rpm),
                        Decorated.custom(jarName, DECORATION_JAR),
                        Decorated.custom(className, DECORATION_CLASS),
                        Decorated.actual(version),
                        Decorated.expected(44 + 11));
            } else if (packagesReqVersion8.contains(rpm.getPackageName()) && version > 44 + 8) {
                fail("{0}: {1}: {2}: class bytecode version is {3} which is larger than {4} (package is a runtime dependency of ant or maven)",
                        Decorated.rpm(rpm),
                        Decorated.custom(jarName, DECORATION_JAR),
                        Decorated.custom(className, DECORATION_CLASS),
                        Decorated.actual(version),
                        Decorated.expected(44 + 8));
            } else {
                pass("{0}: {1}: {2}: class bytecode version is {3} which is less or equal than {4}",
                        Decorated.rpm(rpm),
                        Decorated.custom(jarName, DECORATION_JAR),
                        Decorated.custom(className, DECORATION_CLASS),
                        Decorated.actual(version),
                        Decorated.expected(44 + 8));
            }
        }
    }
}
