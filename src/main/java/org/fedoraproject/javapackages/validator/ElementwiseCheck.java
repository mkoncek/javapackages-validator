package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public abstract class ElementwiseCheck<Config> extends Check<Config> {
    abstract protected Collection<String> check(Path rpmPath, RpmInfo rpmInfo, Config config) throws IOException;

    private Collection<String> checkNonNull(Path rpmPath, RpmInfo rpmInfo, Config config) throws IOException {
        if (config == null) {
            System.err.println("[INFO] Configuration class not found, ignoring the test");
            return Collections.emptyList();
        }

        return check(rpmPath, rpmInfo, config);
    }

    @Override
    protected Collection<String> check(List<Path> testRpms, Config config) throws IOException {
        var result = new ArrayList<String>(0);

        for (Path rpmPath : testRpms) {
            var rpmInfo = new RpmInfo(rpmPath);
            result.addAll(checkNonNull(rpmPath, rpmInfo, config));
        }

        return result;
    }
}
