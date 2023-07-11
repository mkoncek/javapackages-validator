#!/bin/bash

set -eux

for component in $(curl -S 'https://mbi-artifacts.s3.eu-central-1.amazonaws.com/f8e6c83c-94ac-43a2-ad7f-9a01aa453c1b/subject.xml'\
    | xmllint --xpath '/subject/component/name/text()' -); do (
    nevra="$(koji latest-pkg rawhide "${component}" | tail -n +3)"
    nevra="${nevra%%[[:space:]]*}"
    dist_git_url="$(koji buildinfo "${nevra}" | grep Extra | sed "s/.*git+\(.*\)'.*/\1/")"
    git clone "${dist_git_url%%#*}"
    pushd "${component}"
    git checkout -q "${dist_git_url##*#}"
    
    mkdir rpms
    pushd rpms
    koji download-build --arch noarch --arch x86_64 --arch src --debuginfo "${nevra}"
    popd
    popd
) & done
wait
