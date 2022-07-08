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

if [ ! "$(ls -A '/mnt/packages/')" ]; then
    echo "No packages found in /mnt/packages/"
else
    for package_dir in '/mnt/packages/'*; do
        package_name="${package_dir##*/}"
        if [ ! "$(ls -A ${package_dir})" ]; then
            echo "No rpms found in ${package_dir}"
        else
            rpms="${package_dir}/"*
            echo $package_name
            echo $rpms
            ${java_bin}/java -cp "${classpath}" "org.fedoraproject.javapackages.validator.${1}" "${package_name}" ${rpms} || exitcode=1
        fi
    done
fi
