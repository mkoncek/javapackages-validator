package org.fedoraproject.javapackages.validator;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.config.RpmAttribute;

public class RpmAttributeCheck {
    static Collection<String> checkAttributes(Path path, RpmAttribute config) throws Exception {
        var result = new ArrayList<String>(0);

        var filename = path.getFileName();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file path: " + path);
        }

        var info = new RpmInfo(path);

        for (String attributeName : new String[] {"Provides", "Requires", "Conflicts", "Obsoletes",
                "Recommends", "Suggests", "Supplements", "Enhances", "OrderWithRequires"}) {
            Method getter = info.getClass().getMethod("get" + attributeName);
            Method filter = config.getClass().getMethod("allowed" + attributeName, String.class, String.class, String.class);

            for (Object attributeObject : List.class.cast(getter.invoke(info))) {
                var attributeValue = String.class.cast(attributeObject);
                boolean ok = true;

                if (!Boolean.class.cast(filter.invoke(config, Common.getPackageName(path), filename, attributeValue))) {
                    ok = false;
                    result.add(MessageFormat.format("[FAIL] {0}: Attribute [{1}] with invalid value: {2}",
                            path, attributeName, attributeValue));
                }

                if (ok) {
                    System.err.println(MessageFormat.format("[INFO] {0}: Attribute [{1}]: ok",
                            path, attributeName));
                }
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.RpmAttributeConfig");
        var config = (RpmAttribute) configClass.getConstructor().newInstance();

        for (int i = 0; i != args.length; ++i) {
            for (var message : checkAttributes(Paths.get(args[i]).resolve(".").toAbsolutePath().normalize(), config)) {
                exitcode = 1;
                System.out.println(message);
            }
        }

        System.exit(exitcode);
    }
}
