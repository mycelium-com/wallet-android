#!/bin/sh

# this script helps wrap up the currently 8 apks into 2 zips, named containing the versionName

releasefolder="/tmp/release_mbw/comp_"$(date +"%s")"/"
mbwVersion=$( grep "versionName '" mbw/build.gradle | sed "s/.*versionName //g" | sed "s/'//g" )
bchVersion=$( grep 'versionName "' modulespvbch/build.gradle | sed "s/.*versionName //g" | sed 's/"//g' )
mkdir -p $releasefolder
for f in $( find . -name *.apk ); do
  echo $f
  cp $f $releasefolder
done
zip $releasefolder"release_mbw_${mbwVersion}.zip" ${releasefolder}mbw*.apk >/dev/null 2>&1
zip $releasefolder"release_bch_${bchVersion}.zip" ${releasefolder}*bch*.apk >/dev/null 2>&1
echo done
