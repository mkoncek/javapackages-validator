package org.fedoraproject.javapackages.validator.helpers;

import java.util.function.Predicate;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class JarValidator extends ElementwiseValidator implements RpmJarConsumer {
    public static final Decoration DECORATION_JAR = Decoration.bright_blue;

    protected JarValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    protected JarValidator(Predicate<RpmInfo> filter) {
        super(filter);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        accept(rpm);
    }
}
