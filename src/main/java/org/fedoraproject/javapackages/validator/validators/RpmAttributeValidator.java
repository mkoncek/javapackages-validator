package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmPathInfo;

public abstract class RpmAttributeValidator extends ElementwiseValidator {
    private final String attributeName;

    protected RpmAttributeValidator(String attributeName) {
        this.attributeName = attributeName;
    }

    protected RpmAttributeValidator(Predicate<RpmPathInfo> filter, String attributeName) {
        super(filter);
        this.attributeName = attributeName;
    }

    public abstract boolean allowedAttribute(RpmInfo rpm, String value);

    @Override
    public void validate(RpmPathInfo rpm) throws IOException {
        try {
            Method getter = RpmInfo.class.getMethod("get" + attributeName);

            for (Object attributeObject : List.class.cast(getter.invoke(rpm))) {
                var attributeValue = String.class.cast(attributeObject);
                boolean ok = true;

                if (!Boolean.class.cast(allowedAttribute(rpm, attributeValue))) {
                    ok = false;
                    fail("{0}: Attribute {1} with invalid value: {2}",
                            Decorated.rpm(rpm),
                            Decorated.outer(attributeName),
                            Decorated.actual(attributeValue));
                }

                if (ok) {
                    pass("{0}: Attribute [{1}]: ok",
                            Decorated.rpm(rpm),
                            Decorated.outer(attributeName));
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
