package org.fedoraproject.javapackages.validator;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.config.RpmAttribute;

public class RpmAttributeCheck extends Check<RpmAttribute> {
    @Override
    public Collection<String> check(String packageName, Path rpmPath, RpmAttribute config) throws IOException {
        var result = new ArrayList<String>(0);

        var filename = rpmPath.getFileName();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file path: " + rpmPath);
        }

        var info = new RpmInfo(rpmPath);

        try {
            for (String attributeName : new String[] {"Provides", "Requires", "Conflicts", "Obsoletes",
                    "Recommends", "Suggests", "Supplements", "Enhances", "OrderWithRequires"}) {
                Method getter = info.getClass().getMethod("get" + attributeName);
                Method filter = config.getClass().getMethod("allowed" + attributeName, String.class, String.class, String.class);

                for (Object attributeObject : List.class.cast(getter.invoke(info))) {
                    var attributeValue = String.class.cast(attributeObject);
                    boolean ok = true;

                    if (!Boolean.class.cast(filter.invoke(config, packageName, filename, attributeValue))) {
                        ok = false;
                        result.add(MessageFormat.format("[FAIL] {0}: Attribute [{1}] with invalid value: {2}",
                                rpmPath, attributeName, attributeValue));
                    }

                    if (ok) {
                        System.err.println(MessageFormat.format("[INFO] {0}: Attribute [{1}]: ok",
                                rpmPath, attributeName));
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new RpmAttributeCheck().executeCheck(RpmAttribute.class, args));
    }
}
