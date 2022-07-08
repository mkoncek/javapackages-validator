#!/bin/bash

set -e

if [ -z "${1}" ]; then
    echo "error: no argument provided"
    echo "Usage: run.sh <simple class name of the check>"
    exit 1
fi

java_bin="/usr/lib/jvm/java-18/bin"

classpath=$(echo target/*.jar)
for dependency in target/dependency/*; do
    classpath+=":${dependency}"
done

readonly package_dir="$(echo /mnt/package/*)"

if [ ! "$(ls -A ${package_dir})" ]; then
    echo "No rpms found in ${package_dir}" >&2
else
    rpms="$(echo ${package_dir}/*)"
    ${java_bin}/java -cp "${classpath}" "org.fedoraproject.javapackages.validator.${1}" "${package_name}" ${rpms}
fi