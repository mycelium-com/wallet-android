#!/bin/bash

if [[ $# -eq 0 ]] ; then
  printf "No arguments supplied.\nUsage: ./checkBuild.sh FILE REVISION\n\n \
FILE       The apk you want to verify, without extension. mbw-prodnet-release for example\n \
REVISION   The git revision, tag or branch. v2.12.0.19 for example.\n"
  exit 1
fi

releaseFile=$1
revision=$2

if [[ ! -f "./${releaseFile}.apk" ]]
then
	echo "./${releaseFile}.apk not found. Put the file you want to test into this folder and reference it omitting the \".apk\"."
	exit 1
fi

if [[ ! -f "apktool.jar" ]]
then
	echo "apktool.jar not found. Put the file into this folder. You can get it from https://ibotpeaches.github.io/Apktool/ or https://github.com/iBotPeaches/Apktool"
	exit 1
fi

workdir=/tmp/mbwDeterministicBuild/
# If the build tools are slightly non-deterministic and produce randomly one of two different
# versions of each file for example, you can try to rebuild several times, to eventually verify each
# file individually. The script will maintain a list of unverified files that should get shorter
# with every iteration. Pick how many retries you want to run:
retries=${3:-0}
mkdir -p $workdir

printf "Checking $releaseFile against revision $revision with $retries retries if verification fails.\n \
You can monitor progress in ${workdir}progress.log\n" | tee ${workdir}progress.log

cp -r . $workdir
cd $workdir

java -jar apktool.jar d --output original $releaseFile.apk

git stash
git checkout $revision || die ""
git submodule update --init --recursive

# these files are either irrelevant (apktool.yml is created by the akp extractor) or not reproducible signature/meta data (CERT, MANIFEST)
ignoreFiles="apktool.yml\|original/META-INF/CERT.RSA\|original/META-INF/CERT.SF\|original/META-INF/MANIFEST.MF"

./gradlew clean :mbw:assProdRel
cp ./mbw/build/outputs/apk/prodnet/release/mbw-prodnet-release.apk candidate.apk
java -jar apktool.jar d candidate.apk
diff --brief --recursive original/ candidate/ > 0.diff
mv candidate/ 0/
# store the list of relevant differing files in remaining.diff
cat 0.diff | grep -v "$ignoreFiles" > remaining.diff

checkFinished() {
  # if remaining.diff is empty, we are done.
  if [ ! -s remaining.diff ]
  then
    printf "$releaseFile matches $revision!\n" >> progress.log
    cat progress.log
    exit 0
  else
    printf "$(cat remaining.diff | wc -l) files differ. Trying harder to find matches.\n \
        Remaining files:\n$(cat remaining.diff)\n\n" >> progress.log
  fi
}

checkFinished

for i in $( seq 1 $retries ) ; do
  printf "Deterministic build failed. Attempt $i to still confirm all files individually under not totally deterministic conditions.\n" >> progress.log
  ./gradlew clean :mbw:assProdRel
  cp ./mbw/build/outputs/apk/prodnet/release/mbw-prodnet-release.apk candidate.apk
  java -jar apktool.jar d candidate.apk
  # join remaining filenames to a grep filter. Only these file names are relevant.
  relevantFilesFilter=$(cat remaining.diff | paste -sd "|" - | sed 's/|/\\\|/g')
  # diff, only listing files that differ
  diff --brief --recursive original/ candidate/ > $i.diff
  mv candidate/ $i/
  # diff lines that were present in all prior diffs get stored in remaining.diff
  cat $i.diff | grep "$relevantFilesFilter" > remaining.diff
  checkFinished
done

printf "Error: The following files could not be verified:\n \
$( cat remaining.diff ) \
Error: Giving up after 10 compilations. Please manually check if the differing files are acceptable using meld or any other folder diff tool.\n \
Not every tiny diff is a red flag. Maybe this script is broken. Maybe a tool compiles in a non-deterministic way. Both happened before." \
    | tee -a progress.log

exit 1
