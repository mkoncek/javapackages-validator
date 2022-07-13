package org.fedoraproject.javapackages.validator;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class RpmAttributeCheck<Config> extends ElementwiseCheck<Config> {
    @Override
    protected Collection<String> check(Path rpmPath, RpmInfo rpmInfo, Config config) throws IOException {
        var result = new ArrayList<String>(0);

        String attributeName = config.getClass().getSimpleName();
        // Remove "Config" suffix
        attributeName = attributeName.substring(0, attributeName.length() - 6);

        try {
            Method getter = RpmInfo.class.getMethod("get" + attributeName);
            Method filter = config.getClass().getMethod("allowed" + attributeName, String.class, String.class, String.class);

            for (Object attributeObject : List.class.cast(getter.invoke(rpmInfo))) {
                var attributeValue = String.class.cast(attributeObject);
                boolean ok = true;

                if (!Boolean.class.cast(filter.invoke(config, new RpmPackageImpl(rpmInfo), attributeValue))) {
                    ok = false;
                    result.add(MessageFormat.format("[FAIL] {0}: Attribute [{1}] with invalid value: {2}",
                            rpmPath, attributeName, attributeValue));
                }

                if (ok) {
                    System.err.println(MessageFormat.format("[INFO] {0}: Attribute [{1}]: ok",
                            rpmPath, attributeName));
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }
}
