package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.Main;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;
import org.fedoraproject.javapackages.validator.config.JavadocNoarchConfig;

public class JavadocNoarchCheck extends ElementwiseCheck<JavadocNoarchConfig> {
    public JavadocNoarchCheck() {
        this(null);
    }

    public JavadocNoarchCheck(JavadocNoarchConfig config) {
        super(JavadocNoarchConfig.class, config);
        setFilter((rpm) -> {
            if (rpm.isSourcePackage()) {
                return false;
            }

            return getConfig().isJavadocRpm(rpm);
        });
    }

    @Override
    public Collection<String> check(RpmPathInfo rpm) throws IOException {
        var result = new ArrayList<String>(0);

        if (!"noarch".equals(rpm.getArch())) {
            result.add(failMessage("{0} is a javadoc package but its architecture is {1}",
                    Main.getDecorator().decorate(rpm.getPath(), Decoration.bright_red),
                    Main.getDecorator().decorate(rpm.getArch(), Decoration.bright_magenta)));
        } else {
            getLogger().pass("{0} is a javadoc package and its architecture is {1}",
                    Main.getDecorator().decorate(rpm.getPath(), Decoration.bright_red),
                    Main.getDecorator().decorate("noarch", Decoration.bright_cyan));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new JavadocNoarchCheck().executeCheck(args));
    }
}
