package org.fedoraproject.javapackages.validator.validators.jp;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.validators.FilesValidator;

public class FilesValidatorJP extends FilesValidator {
    private Map<String, Pattern> patternsCache = new TreeMap<>();

    private static String substitute(RpmInfo rpm, String text) {
        return text
                .replace("${package}", rpm.getPackageName())
                .replace("${rpm.name}", rpm.getName())
        ;
    }

    private static class Prefix {
        static final String ETC = "/etc/${package}";
        static final String USR_BIN = "/usr/bin/";
        static final String USR_LIB_BUILD_ID = "/usr/lib/.build-id";
    }

    private static Map<String, List<String>> nameAliases = new TreeMap<>();
    static {
        nameAliases.put("maven-resolver", List.of("aether"));
        nameAliases.put("google-guice", List.of("guice"));
        nameAliases.put("google-guice-javadoc", List.of("guice-parent"));
        nameAliases.put("jakarta-mail", List.of("javamail", "javax.mail"));
    }

    private static boolean isAlias(String rpmName, String alias) {
        return nameAliases.getOrDefault(rpmName, Collections.<String>emptyList()).stream().anyMatch(a -> alias.equals(a));
    }

    private static Map<String, List<String>> prefixes = new TreeMap<>();
    static {
        prefixes.put("ant", List.of(Prefix.ETC, Prefix.USR_BIN));
        prefixes.put("antlr", List.of(Prefix.USR_BIN));
        prefixes.put("aqute-bnd", List.of(Prefix.USR_BIN));
        prefixes.put("byaccj", List.of(Prefix.USR_BIN, Prefix.USR_LIB_BUILD_ID));
        prefixes.put("jansi", List.of(Prefix.USR_LIB_BUILD_ID));
        prefixes.put("java_cup", List.of(Prefix.USR_BIN));
        prefixes.put("javapackages-bootstrap", List.of("/usr/share/java/javapackages-bootstrap/", "/usr/lib/rpm/macros.d", "/usr/libexec/javapackages-bootstrap/"));
        prefixes.put("javapackages-tools", List.of(Prefix.USR_BIN, "/usr/share/xmvn/conf", "/usr/lib/rpm", "/usr/share/java-utils/", "/usr/lib/eclipse", "/usr/share/eclipse", "/etc/java", "/etc/jvm", "/usr/lib/java", "/usr/lib/jvm", "/etc/ivy", "/etc/ant.d"));
        prefixes.put("jflex", List.of(Prefix.USR_BIN));
        prefixes.put("maven", List.of(Prefix.ETC));
        prefixes.put("modello", List.of(Prefix.USR_BIN));
        prefixes.put("objectweb-asm", List.of(Prefix.USR_BIN));
        prefixes.put("xerces-j2", List.of(Prefix.USR_BIN));
        prefixes.put("xml-commons-resolver", List.of(Prefix.USR_BIN));
        prefixes.put("xmvn", List.of(Prefix.USR_BIN));
    }

    private static Map<String, List<String>> exceptionalFiles = new TreeMap<>();
    static {
        exceptionalFiles.put("aqute-bnd", List.of("/etc/ant.d/aqute-bnd"));
        exceptionalFiles.put("maven", List.of("/etc/m2.conf", "/etc/java/maven.conf"));
        exceptionalFiles.put("jansi", List.of("/usr/lib/jansi", "/usr/lib/jansi/libjansi.so", "/usr/lib/java/jansi"));
        exceptionalFiles.put("javapackages-tools", List.of("/usr/share/java-utils", "/usr/share/xmvn", "/usr/share/ivy-xmls", "/usr/share/java", "/usr/share/javadoc", "/usr/share/jvm", "/usr/share/jvm-common", "/usr/share/maven-metadata", "/usr/share/maven-poms"));
    }

    private static boolean namesRelated(String lhs, String rhs) {
        int index = 0;

        while (index != lhs.length() && index != rhs.length() && lhs.charAt(index) == rhs.charAt(index)) {
            ++index;
        }

        if (index == lhs.length() && index == rhs.length()) {
            return true;
        }

        if (lhs.length() > rhs.length()) {
            String temp = lhs;
            lhs = rhs;
            rhs = temp;
        }

        if (index == lhs.length() && rhs.charAt(index) == '-') {
            return true;
        }

        if (index > 0 && lhs.charAt(index - 1) == '-' && rhs.charAt(index - 1) == '-') {
            return true;
        }

        return false;
    }

    private static boolean startsWithOrEquals(String string, String prefix) {
        if (string.startsWith(prefix) && (string.length() == prefix.length() || string.charAt(prefix.length()) == '/')) {
            return true;
        }

        return false;
    }

    private static final Pattern DOC_LICENSE_PATTERN = Pattern.compile("/usr/share/(doc|licenses)/([^/]*)(.*)");

    private boolean allowedAnyFile(RpmInfo rpm, String filename) {
        for (String prefix : prefixes.getOrDefault(rpm.getPackageName(), Collections.emptyList())) {
            if (filename.startsWith(substitute(rpm, prefix))) {
                return true;
            }
        }

        for (String filepath : exceptionalFiles.getOrDefault(rpm.getPackageName(), Collections.emptyList())) {
            if (filename.equals(filepath)) {
                return true;
            }
        }

        Matcher matcher = DOC_LICENSE_PATTERN.matcher(filename);

        if (matcher.matches()) {
            if (namesRelated(rpm.getName(), matcher.group(2))
                    || isAlias(rpm.getName(), matcher.group(2))) {
                if (matcher.group(3).isEmpty()) {
                    return true;
                }

                if (matcher.group(3).charAt(0) == '/') {
                    if (matcher.group(1).equals("licenses") &&
                            matcher.group(3).codePoints().filter(c -> c == '/').count() != 1) {
                        return false;
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private boolean allowedJavadocFile(RpmInfo rpm, String filename) {
        if (allowedAnyFile(rpm, filename)) {
            return true;
        }

        if (patternsCache.computeIfAbsent(rpm.getPackageName() + "/javadoc",
                p -> Pattern.compile("/usr/share/javadoc/" + rpm.getPackageName() + "(:?/.*)?"))
                .matcher(filename).matches()) {
            return true;
        }

        return false;
    }

    private boolean allowedDebuginfoFile(RpmInfo rpm, String filename) {
        if (allowedAnyFile(rpm, filename)) {
            return true;
        }

        if (startsWithOrEquals(filename, "/usr/lib/debug")) {
            return true;
        }

        return false;
    }

    private boolean allowedDebugsourceFile(RpmInfo rpm, String filename) {
        if (allowedAnyFile(rpm, filename)) {
            return true;
        }

        if (filename.startsWith("/usr/src/debug/")) {
            return true;
        }

        return false;
    }

    private boolean allowedCLibraryFile(RpmInfo rpm, String filename) {
        if (allowedAnyFile(rpm, filename)) {
            return true;
        }

        if (filename.startsWith("/usr/include/")) {
            return true;
        }

        if (filename.startsWith("/usr/lib64/lib") && filename.endsWith(".a")) {
            return true;
        }

        return false;
    }

    private boolean allowedPythonLibraryFile(RpmInfo rpm, String filename) {
        if (allowedAnyFile(rpm, filename)) {
            return true;
        }

        if (filename.startsWith("/usr/lib/python")) {
            return true;
        }

        return false;
    }

    private boolean allowedJavaLibraryFile(RpmInfo rpm, String filename) {
        if (allowedAnyFile(rpm, filename)) {
            return true;
        }

        String prefix;
        String rpmEntryPrefix;

        if (filename.startsWith(prefix = "/usr/share/java/")) {
            rpmEntryPrefix = filename.substring(prefix.length());
            if (filename.endsWith(".jar") && rpmEntryPrefix.codePoints().filter(c -> c == '/').count() <= 1) {
                return true;
            }

            if (namesRelated(rpm.getName(), rpmEntryPrefix)
                    || namesRelated(rpm.getPackageName(), rpmEntryPrefix)
                    || isAlias(rpm.getName(), rpmEntryPrefix)) {
                return true;
            }
        }

        if (filename.startsWith(prefix = "/usr/lib/java/") && filename.endsWith(".jar")) {
            if (filename.substring(prefix.length()).codePoints().filter(c -> c == '/').count() <= 1) {
                return true;
            }
        }

        if (filename.startsWith(prefix = "/usr/share/maven-poms/")) {
            rpmEntryPrefix = filename.substring(prefix.length());
            if (filename.endsWith(".pom") && rpmEntryPrefix.codePoints().filter(c -> c == '/').count() <= 1) {
                return true;
            }

            if (namesRelated(rpm.getName(), rpmEntryPrefix)
                    || namesRelated(rpm.getPackageName(), rpmEntryPrefix)
                    || isAlias(rpm.getName(), rpmEntryPrefix)) {
                return true;
            }
        }

        if (filename.startsWith("/usr/share/maven-metadata/") && filename.endsWith(".xml")) {
            return true;
        }

        return false;
    }

    private boolean allowedJavaApplicationFile(RpmInfo rpm, String filename) {
        if (allowedJavaLibraryFile(rpm, filename)) {
            return true;
        }

        if (filename.startsWith("/usr/bin/" + rpm.getPackageName() + "/")) {
            return true;
        }

        if (filename.startsWith("/usr/share/man/man1/") && filename.endsWith(".1.gz")) {
            return true;
        }

        if (filename.startsWith("/usr/share/man/man7/") && filename.endsWith(".7.gz")) {
            return true;
        }

        String prefix;

        if (filename.startsWith(prefix = "/usr/share/bash-completion")) {
            if (filename.length() == prefix.length()) {
                return true;
            }

            if (filename.startsWith(prefix += "/completions")) {
                if (filename.length() == prefix.length()) {
                    return true;
                }

                if (filename.charAt(prefix.length()) == '/') {
                    return true;
                }
            }
        }

        if (filename.startsWith("/usr/share/maven-metadata/") && filename.endsWith(".xml")) {
            return true;
        }

        if (filename.startsWith(prefix = "/usr/share/")) {
            String suffix = filename.substring(prefix.length());
            if (suffix.length() > 0) {
                int index = suffix.indexOf('/');
                if (index == -1) {
                    index = suffix.length();
                }
                if (namesRelated(suffix.substring(0, index), rpm.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean allowedFile(RpmInfo rpm, Path path) {
        if (rpm.isSourcePackage()) {
            // TODO
            return true;
        }

        // TODO
        String filename = path.toString();

        if (rpm.getName().equals(rpm.getPackageName() + "-javadoc") || rpm.getName().equals(rpm.getPackageName() + "-javadocs")) {
            return allowedJavadocFile(rpm, filename);
        } else if (rpm.getName().equals(rpm.getPackageName() + "-debuginfo")) {
            return allowedDebuginfoFile(rpm, filename);
        } else if (rpm.getName().equals(rpm.getPackageName() + "-debugsource")) {
            return allowedDebugsourceFile(rpm, filename);
        } else if (rpm.getName().startsWith("python3-javapackages")) {
            return allowedPythonLibraryFile(rpm, filename);
        } else if (rpm.getName().startsWith("antlr-C++")) {
            return allowedCLibraryFile(rpm, filename);
        } else {
            // TODO differentiate Java applications ad libraries
            return allowedJavaApplicationFile(rpm, filename);
        }
    }
}
