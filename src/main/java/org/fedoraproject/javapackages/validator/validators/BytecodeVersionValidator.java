package org.fedoraproject.javapackages.validator.validators;

import java.nio.file.Path;
import java.util.Map;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TmtTest;

@TmtTest("/java/bytecode_version")
public class BytecodeVersionValidator extends BytecodeVersionJarValidator {
    private short min = Short.MIN_VALUE;
    private short max = Short.MAX_VALUE;

    @Override
    public void arguments(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Wrong number of arguments, expected 1");
        }

        var arg = args[0];

        var pos = arg.indexOf(':');

        if (pos == -1) {
            var num = Short.parseShort(arg);
            min = num;
            max = num;
        } else {
            if (pos != 0) {
                min = Short.parseShort(arg.substring(0, pos));
            }
            if (pos != arg.length() - 1) {
                max = Short.parseShort(arg.substring(pos + 1));
            }

            if (min > max) {
                throw new IllegalArgumentException("Specified minimum is larger than specified maximum");
            }
        }

        debug("Limits: {0}:{1}", Decorated.plain(min), Decorated.plain(max));
    }

    @Override
    public void validate(RpmInfoURI rpm, Path jarPath, Map<Path, Short> classVersions) {
        for (var entry : classVersions.entrySet()) {
            boolean failed = false;

            if (entry.getValue() < min) {
                failed = true;
                fail("{0}: {1}: {2}: bytecode version: {3} is smaller than {4}",
                        Decorated.rpm(rpm),
                        Decorated.custom(jarPath, DECORATION_JAR),
                        Decorated.struct(entry.getKey()),
                        Decorated.actual(entry.getValue()),
                        Decorated.expected(min));
            }

            if (max < entry.getValue()) {
                failed = true;
                fail("{0}: {1}: {2}: bytecode version: {3} is larger than {4}",
                        Decorated.rpm(rpm),
                        Decorated.custom(jarPath, DECORATION_JAR),
                        Decorated.struct(entry.getKey()),
                        Decorated.actual(entry.getValue()),
                        Decorated.expected(max));
            }

            if (!failed) {
                pass();
                debug("{0}: {1}: {2}: bytecode version: {3}",
                        Decorated.rpm(rpm),
                        Decorated.custom(jarPath, DECORATION_JAR),
                        Decorated.struct(entry.getKey()),
                        Decorated.actual(entry.getValue()));
            }
        }
    }
}
