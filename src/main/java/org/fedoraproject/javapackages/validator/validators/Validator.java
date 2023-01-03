package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.LogEvent;
import org.fedoraproject.javapackages.validator.Logger;
import org.fedoraproject.javapackages.validator.RpmInfoURI;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class Validator {
    protected List<String> failMessages = new ArrayList<>(0);
    protected List<String> passMessages = new ArrayList<>(0);
    private Logger logger = new Logger();

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Logger getLogger() {
        return logger;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public List<String> getFailMessages() {
        return Collections.unmodifiableList(failMessages);
    }

    public List<String> getPassMessages() {
        return Collections.unmodifiableList(passMessages);
    }

    public boolean failed() {
        return !failMessages.isEmpty();
    }

    protected final void fail(String pattern, Decorated... arguments) {
        failMessages.add(LogEvent.fail.withFormat(pattern, arguments));
    }

    protected final void pass(String pattern, Decorated... arguments) {
        passMessages.add(LogEvent.pass.withFormat(pattern, arguments));
    }

    protected final void debug(String pattern, Decorated... arguments) {
        getLogger().debug(pattern, arguments);
    }

    protected final void info(String pattern, Decorated... arguments) {
        getLogger().info(pattern, arguments);
    }

    abstract public void validate(Iterator<RpmInfoURI> rpmIt) throws IOException;
}
