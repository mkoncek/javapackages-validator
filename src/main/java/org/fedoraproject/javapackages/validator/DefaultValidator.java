package org.fedoraproject.javapackages.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmFile;

public abstract class DefaultValidator extends DefaultResult implements Validator {
    private List<String> args = null;

    @Override
    public Result validate(Iterable<RpmFile> rpms, List<String> args) {
        if (args != null) {
            this.args = new ArrayList<>(args);
        }
        try {
            validate(rpms);
        } catch (Exception ex) {
            error();
            addLog(Common.logException(ex));
        }

        return this;
    }

    protected List<String> getArgs() {
        if (args != null) {
            return Collections.unmodifiableList(args);
        }
        return null;
    }

    public abstract void validate(Iterable<RpmFile> rpms) throws Exception;
}
