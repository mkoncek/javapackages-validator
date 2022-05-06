package org.fedoraproject.javapackages.validator.config;

public interface RpmAttribute {
    boolean allowedProvides(String packageName, String rpmName, String value);
    boolean allowedRequires(String packageName, String rpmName, String value);
    boolean allowedConflicts(String packageName, String rpmName, String value);
    boolean allowedObsoletes(String packageName, String rpmName, String value);
    boolean allowedRecommends(String packageName, String rpmName, String value);
    boolean allowedSuggests(String packageName, String rpmName, String value);
    boolean allowedSupplements(String packageName, String rpmName, String value);
    boolean allowedEnhances(String packageName, String rpmName, String value);
    boolean allowedOrderWithRequires(String packageName, String rpmName, String value);
}
