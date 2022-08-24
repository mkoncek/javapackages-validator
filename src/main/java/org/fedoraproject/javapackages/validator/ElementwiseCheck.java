package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class ElementwiseCheck<Config> extends Check<Config> {
    private final Predicate<RpmPathInfo> filter;

    protected ElementwiseCheck(Class<Config> configClass) {
        this(configClass, rpm -> true);
    }

    protected ElementwiseCheck(Class<Config> configClass, Predicate<RpmPathInfo> filter) {
        super(configClass);
        this.filter = filter;
    }

    abstract protected Collection<String> check(Config config, RpmPathInfo rpm) throws IOException;

    @Override
    public final Collection<String> check(Config config, Iterator<RpmPathInfo> rpmIt) throws IOException {
        var result = new ArrayList<String>(0);

        while (rpmIt.hasNext()) {
            RpmPathInfo rpm = rpmIt.next();
            if (filter.test(rpm)) {
                result.addAll(check(config, rpm));
            } else {
                getLogger().debug("{0}: filtered out {1}",
                        textDecorate(getClass().getSimpleName(), Decoration.bright_yellow),
                        textDecorate(rpm.getPath(), DECORATION_RPM));
            }
        }

        return result;
    }
}
