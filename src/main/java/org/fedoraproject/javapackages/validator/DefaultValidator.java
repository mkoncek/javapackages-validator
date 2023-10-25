package org.fedoraproject.javapackages.validator;

import org.fedoraproject.javadeptools.rpm.RpmFile;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class DefaultValidator extends DefaultResult implements Validator {
    private String[] args = null;

    @Override
    public String getTestName() {
        var fqn = getClass().getCanonicalName();
        var prefix = "org.fedoraproject.javapackages.validator.validators.";
        if (fqn != null && fqn.startsWith(prefix)) {
            return "/" + fqn.substring(prefix.length()).replace('.', '/');
        }
        return null;
    }

    @Override
    @SuppressFBWarnings({"EI_EXPOSE_REP2"})
    public Result validate(Iterable<RpmFile> rpms, String[] args) {
        this.args = args;
        try {
            validate(rpms);
        } catch (Exception ex) {
            error();
            addLog(Common.logException(ex));
        }

        return this;
    }

    protected String[] getArgs() {
        return args;
    }

    public abstract void validate(Iterable<RpmFile> rpms) throws Exception;
}
