package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Check;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public class JavaExclusiveArchCheck extends ElementwiseCheck<Check.NoConfig> {
    /**
     * The expanded value of `rpm -E '%{java_arches}'` as of 18. 8. 2022 on Fedora 37
     */
    private static final Set<String> JAVA_ARCHES = Set.of(new String[] {
            "aarch64",
            "ppc64le",
            "s390x",
            "x86_64",
    });

    public JavaExclusiveArchCheck() {
        super(Check.NoConfig.class);
        setFilter(RpmInfo::isSourcePackage);
    }

    @Override
    protected Collection<String> check(Check.NoConfig config, RpmPathInfo rpm) throws IOException {
        String decoratedJavaArches = listDecorate(JAVA_ARCHES, Decoration.bright_yellow);
        getLogger().debug("%java_arches: {0}", decoratedJavaArches);

        var result = new ArrayList<String>(0);

        boolean buildNoarch = rpm.getBuildArchs().contains("noarch");
        Set<String> exclusiveArches = new TreeSet<>(rpm.getExclusiveArch());
        boolean exclusiveNoarch = exclusiveArches.remove("noarch");

        String decoratedRpm = textDecorate(rpm.getPath(), Decoration.bright_red);

        if (buildNoarch && !exclusiveNoarch) {
            result.add(failMessage("{0}: has BuildArch noarch but noarch is not present in its ExclusiveArch field"));
        }

        if (!buildNoarch && exclusiveNoarch) {
            result.add(failMessage("{0}: does not have BuildArch noarch but noarch is present in its ExclusiveArch field"));
        }

        Set<String> tempSet;

        tempSet = new TreeSet<>(JAVA_ARCHES);
        tempSet.removeAll(exclusiveArches);
        if (!tempSet.isEmpty()) {
            result.add(failMessage("{0}: ExclusiveArch field does not contain "
                    + "all the expected values; missing values are: {1}; "
                    + "the values of ExclusiveArch are: {2}",
                    decoratedRpm, listDecorate(tempSet, Decoration.cyan),
                    listDecorate(rpm.getExclusiveArch(), Decoration.bright_magenta)));
        }

        tempSet = new TreeSet<>(exclusiveArches);
        tempSet.removeAll(JAVA_ARCHES);
        if (!tempSet.isEmpty()) {
            result.add(failMessage("{0}: ExclusiveArch contains more entries than "
                    + "expected; superfluous values are: {0}; "
                    + "the values of ExclusiveArch are: {1}",
                    decoratedRpm, listDecorate(tempSet, Decoration.cyan),
                    listDecorate(rpm.getExclusiveArch(), Decoration.bright_magenta)));
        }

        if (result.isEmpty()) {
            getLogger().pass("{0}: ExclusiveArch with %java_arches - ok", decoratedRpm);
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new JavaExclusiveArchCheck().executeCheck(args));
    }
}
