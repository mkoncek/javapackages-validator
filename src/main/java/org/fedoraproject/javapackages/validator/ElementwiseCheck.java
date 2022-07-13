package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public abstract class ElementwiseCheck<Config> extends Check<Config> {
    abstract protected Collection<String> check(Path rpmPath, RpmInfo rpmInfo, Config config) throws IOException;

    @Override
    protected Collection<String> check(List<Path> testRpms, Config config) throws IOException {
        var result = new ArrayList<String>(0);

        for (Path rpmPath : testRpms) {
            var rpmInfo = new RpmInfo(rpmPath);
            result.addAll(check(rpmPath, rpmInfo, config));
        }

        return result;
    }
}
