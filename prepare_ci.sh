#!/bin/bash

set -e

for component in $(curl -S 'https://mbi-artifacts.s3.eu-central-1.amazonaws.com/66023ada-9115-4311-b33e-c453150afd51/subject.xml'\
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
    koji download-build --arch noarch --arch x86_64 --arch src --debuginfo "${nevra}"
    popd
) & done
wait
