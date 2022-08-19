package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;
import org.fedoraproject.javapackages.validator.config.JavadocNoarchConfig;

public class JavadocNoarchCheck extends ElementwiseCheck<JavadocNoarchConfig> {
    public JavadocNoarchCheck() {
        super(JavadocNoarchConfig.class);
        setFilter(Predicate.not(RpmInfo::isSourcePackage));
    }

    @Override
    public Collection<String> check(JavadocNoarchConfig config, RpmPathInfo rpm) throws IOException {
        var result = new ArrayList<String>(0);

        if (!"noarch".equals(rpm.getArch())) {
            result.add(failMessage("{0} is a javadoc package but its architecture is {1}",
                    textDecorate(rpm.getPath(), Decoration.bright_red),
                    textDecorate(rpm.getArch(), Decoration.bright_magenta)));
        } else {
            getLogger().pass("{0} is a javadoc package and its architecture is {1}",
                    textDecorate(rpm.getPath(), Decoration.bright_red),
                    textDecorate("noarch", Decoration.bright_cyan));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new JavadocNoarchCheck().executeCheck(args));
    }
}
