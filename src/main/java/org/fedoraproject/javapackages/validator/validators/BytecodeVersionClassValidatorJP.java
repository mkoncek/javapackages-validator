package org.fedoraproject.javapackages.validator.validators;

import java.util.Set;

import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.validators.BytecodeVersionJarValidator.BytecodeVersionClassValidator;

public class BytecodeVersionClassValidatorJP extends BytecodeVersionClassValidator {
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
            "jakarta-activation",
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
    public void validate(RpmPathInfo rpm, String jarName, String className, int version) {
        if (className.equals("module-info.class")) {
            info("{0}: {1}: ignoring module-info.class",
                    textDecorate(rpm.getPath(), DECORATION_RPM),
                    textDecorate(jarName, DECORATION_JAR));
            return;
        }

        if (version > 44 + 11) {
            fail("{0}: {1}: {2}: class bytecode version is {3} which is larger than {4}",
                    textDecorate(rpm.getPath(), DECORATION_RPM),
                    textDecorate(jarName, DECORATION_JAR),
                    textDecorate(className, DECORATION_CLASS),
                    textDecorate(version, DECORATION_ACTUAL),
                    textDecorate(44 + 11));
            return;
        }

        if (packagesReqVersion8.contains(rpm.getPackageName()) && version > 44 + 8) {
            fail("{0}: {1}: {2}: class bytecode version is {3} which is larger than {4} (package is a runtime dependency of ant or maven)",
                    textDecorate(rpm.getPath(), DECORATION_RPM),
                    textDecorate(jarName, DECORATION_JAR),
                    textDecorate(className, DECORATION_CLASS),
                    textDecorate(version, DECORATION_ACTUAL),
                    textDecorate(44 + 8));
            return;
        }

        pass("{0}: {1}: {2}: class bytecode version is {3} which is less or equal than {4}",
                textDecorate(rpm.getPath(), DECORATION_RPM),
                textDecorate(jarName, DECORATION_JAR),
                textDecorate(className, DECORATION_CLASS),
                textDecorate(version, DECORATION_ACTUAL),
                textDecorate(44 + 8), DECORATION_EXPECTED);
    }
}
