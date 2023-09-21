package org.fedoraproject.javapackages.validator.validators;

import java.util.function.Predicate;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.Validator;

public abstract class ElementwiseValidator extends Validator {
    private Predicate<RpmInfo> filter;

    protected ElementwiseValidator() {
        this(rpm -> true);
    }

    protected ElementwiseValidator(Predicate<RpmInfo> filter) {
        super();
        this.filter = filter;
    }

    @Override
    public final void validate(Iterable<RpmFile> rpms) throws Exception {
        for (var rpm : rpms) {
            if (filter.test(rpm.getInfo())) {
                validate(rpm);
            } else {
                debug("{0} filtered out {1}",
                        Decorated.struct(getClass().getCanonicalName()),
                        Decorated.rpm(rpm));
            }
        }
    }

    public abstract void validate(RpmFile rpm) throws Exception;
}
