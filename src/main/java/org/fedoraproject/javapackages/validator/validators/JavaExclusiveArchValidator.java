package org.fedoraproject.javapackages.validator.validators;

import java.util.Collections;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TmtTest;

@TmtTest("/java/exclusive_arch")
public class JavaExclusiveArchValidator extends ElementwiseValidator {
    /**
     * The expanded value of `rpm -E '%{java_arches}'` as of 18. 8. 2022 on Fedora 37
     */
    private static final String JAVA_ARCHES = "aarch64 ppc64le s390x x86_64";

    public JavaExclusiveArchValidator() {
        super(RpmInfo::isSourcePackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        var buildArchs = rpm.getInfo().getBuildArchs();
        debug("{0}: Build archs: {1}", Decorated.rpm(rpm), Decorated.list(buildArchs));
        boolean noarch = buildArchs.equals(Collections.singletonList("noarch"));

        String expected = noarch ? JAVA_ARCHES + " noarch" : JAVA_ARCHES;
        String actual = String.join(" ", rpm.getInfo().getExclusiveArch());

        if (expected.equals(actual)) {
            pass("{0}: ExclusiveArch with %java_arches - ok",
                    Decorated.rpm(rpm));
        } else {
            fail("{0}: expected ExclusiveArch \"{1}\" but was \"{2}\"",
                    Decorated.rpm(rpm), Decorated.expected(expected),
                    Decorated.actual(actual));
        }
    }
}
