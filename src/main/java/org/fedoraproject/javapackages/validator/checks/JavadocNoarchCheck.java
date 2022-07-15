package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Check;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;

public class JavadocNoarchCheck extends ElementwiseCheck<Check.NoConfig> {
    @Override
    public Collection<String> check(Path rpmPath, RpmInfo rpmInfo) throws IOException {
        var result = new ArrayList<String>(0);

        String rpmName = rpmInfo.getName();

        if (rpmName.endsWith("-javadocs")) {
            rpmName = rpmName.substring(0, rpmName.length() - 1);
        }

        if (!rpmInfo.isSourcePackage() && rpmName.equals(Common.getPackageName(rpmInfo.getSourceRPM()) + "-javadoc")) {
            if (!"noarch".equals(rpmInfo.getArch())) {
                result.add(MessageFormat.format(
                        "[FAIL] {0} is a javadoc package but its architecture is not noarch", rpmPath));
            } else {
                System.err.println(MessageFormat.format(
                        "[INFO] {0} is a javadoc package and its architecture is noarch", rpmPath));
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new JavadocNoarchCheck().executeCheck(Check.NoConfig.class, args));
    }
}
