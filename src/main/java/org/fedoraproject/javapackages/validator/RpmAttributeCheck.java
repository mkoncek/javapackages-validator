package org.fedoraproject.javapackages.validator;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.config.RpmAttribute;

public class RpmAttributeCheck {
    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.RpmAttributeConfig");
        var config = (RpmAttribute) configClass.getConstructor().newInstance();

        String packageName = args[0];

        var messages = new ArrayList<String>();

        for (int i = 1; i != args.length; ++i) {
            var filepath = Paths.get(args[i]);
            var filename = filepath.getFileName();
            if (filename == null) {
                throw new IllegalArgumentException("Invalid file path: " + args[i]);
            }

            var info = new RpmInfo(filepath);

            for (String attributeName : new String[] {"Provides", "Requires", "Conflicts", "Obsoletes",
                    "Recommends", "Suggests", "Supplements", "Enhances", "OrderWithRequires"}) {
                Method getter = info.getClass().getMethod("get" + attributeName);
                Method filter = config.getClass().getMethod("allowed" + attributeName, String.class, String.class, String.class);

                for (Object attributeObject : List.class.cast(getter.invoke(info))) {
                    var attributeValue = String.class.cast(attributeObject);
                    boolean ok = true;

                    if (!Boolean.class.cast(filter.invoke(config, packageName, filename, attributeValue))) {
                        ok = false;
                        messages.add(MessageFormat.format("[FAIL] {0}: Attribute [{1}] with invalid value: {2}",
                                filepath, attributeName, attributeValue));
                    }

                    if (ok) {
                        System.err.println(MessageFormat.format("[INFO] {0}: Attribute [{1}]: ok",
                                filepath, attributeName));
                    }
                }
            }
        }

        for (var message : messages) {
            exitcode = 1;
            System.out.println(message);
        }

        System.exit(exitcode);
    }
}
