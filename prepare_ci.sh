#!/bin/bash

for component in $(curl -S 'https://mbi-artifacts.s3.eu-central-1.amazonaws.com/3406f152-0ceb-4291-8f27-6db7db011c16/subject.xml'\
    | xmllint --xpath '/subject/component/name/text()' -); do (
    nevra="$(koji latest-pkg rawhide "${component}" | tail -n +3)"
    nevra="${nevra%%[[:space:]]*}"
    echo "${nevra}"
    dist_git_url="$(koji buildinfo "${nevra}" | grep Extra | sed "s/.*git+\(.*\)'.*/\1/")"
    echo "${dist_git_url}"
    git clone "${dist_git_url%%#*}"
    cd "${component}"
    git checkout -q "${dist_git_url##*#}"
    
    mkdir rpms
    pushd rpms
    koji download-build --debuginfo "${nevra}"
    popd
) & done
wait

git clone 'https://pagure.io/javapackages-validator-tests.git'
