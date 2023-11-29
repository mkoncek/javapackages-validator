package org.fedoraproject.javapackages.validator.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fedoraproject.javapackages.validator.spi.Result;
import org.fedoraproject.javapackages.validator.spi.Validator;

import io.kojan.javadeptools.rpm.RpmPackage;

public abstract class DefaultValidator extends DefaultResult implements Validator {
    private List<String> args = null;

    @Override
    public Result validate(Iterable<RpmPackage> rpms, List<String> args) {
        if (args != null) {
            this.args = new ArrayList<>(args);
        }
        try {
            validate(rpms);
        } catch (Exception ex) {
            error();
            addLog(logException(ex));
        }

        return this;
    }

    protected List<String> getArgs() {
        if (args != null) {
            return Collections.unmodifiableList(args);
        }
        return null;
    }

    public abstract void validate(Iterable<RpmPackage> rpms) throws Exception;
}
