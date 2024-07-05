#!/bin/sh

set -euf

die () { echo "Error: $*"; exit 1; }

if [ $# -eq 0 ]; then
  printf "No arguments supplied.\nUsage: ./checkBuild.sh FILE REVISION [RETRIES]\n\n \
FILE       The apk you want to verify, without extension. mbw-prodnet-release for example\n \
REVISION   The git revision, tag or branch. v2.12.0.19 for example.\n \
RETRIES    The number of times compilation should be retried if differences remain."
  exit 1
fi

releaseFile=$1
revision=$2
# If the build tools are slightly non-deterministic and produce randomly one of two different
# versions of each file for example, you can try to rebuild several times, to eventually verify each
# file individually. The script will maintain a list of unverified files that should get shorter
# with every iteration. Pick how many retries you want to run:
retries=${3:-0}

if [ ! -f "./${releaseFile}.apk" ]
then
	die "./${releaseFile}.apk not found. Put the file you want to test into this folder and reference it omitting the \".apk\"."
fi

if [ ! -f "apktool.jar" ]
then
	die "apktool.jar not found. Put the file into this folder. You can get it from https://ibotpeaches.github.io/Apktool/ or https://github.com/iBotPeaches/Apktool"
fi

workdir=/tmp/mbwDeterministicBuild/
mkdir -p -- "$workdir" || die "Cannot create directory $workdir"

printf "%s\n%s\n" "Checking $releaseFile against revision $revision with $retries retries if verification fails." \
"You can monitor progress in ${workdir}progress.log" | tee "${workdir}progress.log"

if ! cp -r -- . "$workdir" || ! cd -- "$workdir"
then
        die "Error: Cannot cp and cd to $workdir (maybe no disk space?)"
fi

rm -f -- local.properties || true # this doesn't work in docker and shouldn't be needed outside of docker.

java -jar apktool.jar d --output original "$releaseFile.apk"

git config user.email "only@thistmprepo.com"
git config user.name "only temporary"
git stash
git checkout -- "$revision" || die "Invalid commit."
sed -i 's/git@github.com:/https:\/\/github.com\//' .gitmodules
git submodule update --init --recursive || die "Cannot initialize submodules."

# these files are either irrelevant (apktool.yml is created by the akp extractor) or not reproducible signature/meta data (CERT, MANIFEST)
ignoreFiles="apktool.yml\|original/META-INF/CERT.RSA\|original/META-INF/CERT.SF\|original/META-INF/MANIFEST.MF"

./gradlew clean :mbw:assProdRel
cp ./mbw/build/outputs/apk/prodnet/release/mbw-prodnet-release.apk candidate.apk ||
        die "Cannot copy a file (maybe no disk space?)"

if [ ! -e candidate.apk ]
then
	die "Unexpected error: candidate.apk doesn't exist. Possibly the build failed. Check and try again."
fi

java -jar apktool.jar d candidate.apk
set +x
diff --brief --recursive original/ candidate/ > 0.diff
set -x
mv candidate/ 0/
# store the list of relevant differing files in remaining.diff
set +x
grep -v -- "$ignoreFiles" 0.diff > remaining.diff
set -x

checkFinished() {
  # if remaining.diff is empty, we are done.
  if [ ! -s remaining.diff ]
  then
    printf "%s\n" "$releaseFile matches $revision!" >> progress.log
    cat progress.log
    exit 0
  else
    printf "%s\n%s\n%s\n\n" "$(wc -l remaining.diff) files differ. Trying harder to find matches." \
        "Remaining files:" "$(cat remaining.diff)" >> progress.log
  fi
}

checkFinished

for i in $( seq 1 "$retries" ) ; do
  printf "%s\n" "Deterministic build failed. Attempt $i to still confirm all files individually under not totally deterministic conditions." >> progress.log
  ./gradlew clean :mbw:assProdRel
  cp ./mbw/build/outputs/apk/prodnet/release/mbw-prodnet-release.apk candidate.apk ||
        die "Cannot copy a file (maybe no disk space?)"
  java -jar apktool.jar d candidate.apk
  # join remaining filenames to a grep filter. Only these file names are relevant.
  relevantFilesFilter="$(paste -sd "|" remaining.diff | sed 's/|/\\\|/g')"
  # diff, only listing files that differ
  set +x
  diff --brief --recursive original/ candidate/ > "$i.diff"
  set -x
  mv -- candidate/ "$i/"
  # diff lines that were present in all prior diffs get stored in remaining.diff
  set +x
  grep -- "$relevantFilesFilter" "$i.diff" > remaining.diff
  set -x
  checkFinished
done

printf "%s\n%s%s\n%s" "Error: The following files could not be verified:" \
"$(cat remaining.diff)" \
"Error: Giving up after $retries retries. Please manually check if the differing files are acceptable using meld or any other folder diff tool." \
"Not every tiny diff is a red flag. Maybe this script is broken. Maybe a tool compiles in a non-deterministic way. Both happened before." \
    | tee -a progress.log

exit 1
