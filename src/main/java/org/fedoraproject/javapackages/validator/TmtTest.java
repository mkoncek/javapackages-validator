package org.fedoraproject.javapackages.validator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TmtTest {
    /**
     * The tmt test name. Must start with a slash "/".
     */
    public String value();
}
