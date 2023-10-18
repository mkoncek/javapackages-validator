#!/bin/bash

set -e

java_bin='/usr/lib/jvm/jre-21-openjdk/bin'

exec "${java_bin}"/java --enable-preview --enable-native-access ALL-UNNAMED -jar 'target/validator.jar' "${@}"

# exec "${java_bin}"/java --enable-preview --enable-native-access ALL-UNNAMED -cp "target/classes$(find target/dependency -type f -printf ':%p')" org.fedoraproject.javapackages.validator.Main "${@}"
