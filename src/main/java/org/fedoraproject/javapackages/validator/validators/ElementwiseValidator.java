package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class ElementwiseValidator extends Validator {
    private Predicate<RpmInfoURI> filter;

    protected ElementwiseValidator() {
        this(rpm -> true);
    }

    protected ElementwiseValidator(Predicate<RpmInfoURI> filter) {
        super();
        this.filter = filter;
    }

    @Override
    public final void validate(Iterator<RpmInfoURI> rpmIt) throws IOException {
        while (rpmIt.hasNext()) {
            RpmInfoURI rpm = rpmIt.next();
            if (filter.test(rpm)) {
                validate(rpm);
            } else {
                debug("{0}: filtered out {1}",
                        Decorated.custom(getClass().getSimpleName(), Decoration.bright_yellow),
                        Decorated.rpm(rpm));
            }
        }
    }

    public abstract void validate(RpmInfoURI rpm) throws IOException;
}
