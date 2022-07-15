#!/bin/bash

set -e

if [ -z "${1}" ]; then
    echo "error: no argument provided"
    echo "Usage: run.sh <simple class name of the check> [optional flags] <RPM files or directories to test...>"
    echo "Optional flags:"
    echo "    --config-src [/mnt/config/src] - directory containing configuration sources"
    echo "    --config-bin [/mnt/config/bin] - directory where compiled class files will be put"
    exit 1
fi

java_bin="/usr/lib/jvm/jre-17/bin"

classpath="$(echo target/*.jar)"
for dependency in target/dependency/*; do
    classpath+=":${dependency}"
done

check_name="${1}"
shift 1

exec ${java_bin}/java --enable-preview --add-modules jdk.incubator.foreign --enable-native-access ALL-UNNAMED -cp "${classpath}" "org.fedoraproject.javapackages.validator.checks.${check_name}" ${@}
