#!/bin/sh

# this script helps wrap up the currently 8 apks into 2 zips, named containing the versionName

set -euf

releasefolder="/tmp/release_mbw/comp_$(date +%s)/"
mbwVersion="$( grep "versionName '" mbw/build.gradle | sed "s/.*versionName //g" | sed "s/'//g" )"
mkdir -p -- "$releasefolder"
if ! find . -name "*.apk" -exec echo {} \; -exec cp -- {} "$releasefolder" \; ; then
  printf "Error: Cannot copy a file (maybe no disk space?)"
  exit 1
fi

if ! zip "${releasefolder}release_mbw_${mbwVersion}.zip" "${releasefolder}mbw*.apk" >/dev/null 2>&1; then
  printf "Error: Cannot zip a file (maybe no disk space?)"
  exit 1
fi

printf "Done\n"
