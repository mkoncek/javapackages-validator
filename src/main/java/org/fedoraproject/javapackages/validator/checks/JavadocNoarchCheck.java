package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.fedoraproject.javapackages.validator.Check;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.Main;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public class JavadocNoarchCheck extends ElementwiseCheck<Check.NoConfig> {
    public JavadocNoarchCheck() {
        super(Check.NoConfig.class);
        setFilter((rpm) -> {
            if (rpm.isSourcePackage()) {
                return false;
            }

            String rpmName = rpm.getNEVRA().name();

            if (rpmName.endsWith("-javadocs")) {
                rpmName = rpmName.substring(0, rpmName.length() - 1);
            }

            return rpmName.equals(Common.getPackageName(rpm) + "-javadoc");
        });
    }

    @Override
    public Collection<String> check(RpmPathInfo rpm) throws IOException {
        var result = new ArrayList<String>(0);

        if (!"noarch".equals(rpm.getNEVRA().arch())) {
            result.add(failMessage("{0} is a javadoc package but its architecture is not noarch",
                    Main.getDecorator().decorate(rpm.getPath(), Decoration.bright_red)));
        } else {
            getLogger().pass("{0} is a javadoc package and its architecture is noarch",
                    Main.getDecorator().decorate(rpm.getPath(), Decoration.bright_red));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new JavadocNoarchCheck().executeCheck(args));
    }
}
