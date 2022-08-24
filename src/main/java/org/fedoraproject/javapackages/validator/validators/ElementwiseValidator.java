package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

import org.fedoraproject.javapackages.validator.RpmPathInfo;

public abstract class ElementwiseValidator extends Validator {
    private Predicate<RpmPathInfo> filter;

    protected ElementwiseValidator() {
        this(rpm -> true);
    }

    protected ElementwiseValidator(Predicate<RpmPathInfo> filter) {
        super();
        this.filter = filter;
    }

    @Override
    public void validate(Iterator<RpmPathInfo> rpmIt) throws IOException {
        while (rpmIt.hasNext()) {
            RpmPathInfo rpm = rpmIt.next();
            if (filter.test(rpm)) {
                validate(rpm);
            }
        }
    }

    public abstract void validate(RpmPathInfo rpm) throws IOException;
}
