#!/bin/sh

set -euf

die () { echo "Error: $*"; exit 1; }

APIKEY="$(cat ~/.mycelium_crowdin_api)"

cwd="$(pwd)"
dir=/tmp/translation
rm -rf -- "$dir" || die "Cannot remove $dir"
mkdir -p -- "$dir" || die "Cannot create directory $dir"
echo "    <string-array name=\"languages\">" > "$dir/1.xml"
echo "    <string-array name=\"languages_desc\">" > "$dir/2.xml"
cd -- "$dir" || die "Cannot cd to $dir"
rm -f mycelium-bitcoin-wallet.zip || die "Cannot remove mycelium-bitcoin-wallet.zip"
url="https://api.crowdin.com/api/project/mycelium-bitcoin-wallet/download/all.zip?key=$APIKEY"
if command -v curl; then
curl -LfS --tlsv1.2 --output all.zip -- "$url" || die "Cannot curl url $url"
elif command -v wget; then
  wget --secure-protocol=TLSv1_2 -O all.zip -- "$url" || die "Cannot wget url $url"
else
  die "Please install either curl or wget."
fi
unzip -q all.zip -d "$dir" || die "Cannot unzip all.zip"

UpdateOne () {
  # arg 1: source folder as in ../${1}/strings.xml
  # arg 2: destination folder as in ../res/values-${2}
  # arg 3: Language name in language itself
  echo "Updating language ${1}"
  if [ "$1" != "en" ]; then
    if ! mkdir -p -- "${cwd}/src/main/res/values-${2}" ||
       ! cp -f -- "${dir}/${1}/strings.xml" "${cwd}/src/main/res/values-${2}"; then
      die "Failed to update language ${1}"
    fi
  fi
  echo "        <item>${2}</item>" >> "$dir/1.xml"
  echo "        <item>${3}</item>" >> "$dir/2.xml"
}

mkdir -p  -- "${cwd}/res/values" || die "Cannot create directory ${cwd}/res/values"

UpdateOne bg bg        Български     Bulgarian-Bulgaria
UpdateOne cs cs        čeština       Czech
UpdateOne da da        Dansk         Danish-Danmark
UpdateOne de de        Deutsch       German-Germany
UpdateOne el el        Ελληνικά      Greek-Greece
UpdateOne en en        English       just here for the complete list rendering
UpdateOne es-ES es     Español       Spanish-Spain
UpdateOne fil fil      Filipino      Philipino
UpdateOne fr fr        Français      French-France
UpdateOne he iw        עברית         Hebrew-Israel
UpdateOne id in        Indonesia     Indonesian-Indonesia
UpdateOne it it        Italiano      Italian-Italy
UpdateOne ja ja        日本語         Japanese-Japan
UpdateOne ko ko        한국의         Korean-Korea
UpdateOne nl nl        Nederlands    Dutch/Netherlands
UpdateOne pl pl        Polski        Polish-Poland
UpdateOne pt-PT pt     Português     Portuguese-Portugal
UpdateOne ro ro        Română        Romanian/Moldavian-Romania
UpdateOne ru ru        Русский       Russian-Russia
UpdateOne sk sk        Slovensky     Slovak-Slovakia
UpdateOne sl sl        Slovenščina   Slovenia
UpdateOne sq sq        Shqip         Albania
UpdateOne sv-SE sv     Svenska       Swedish-Sweden
UpdateOne vi vi        "Tiếng Việt"  Vietnamese-Vietnam
UpdateOne zh-CN zh     简体中文       Chinese-China
UpdateOne zh-TW zh-rTW 繁体中文       Chinese-Taiwan

echo "    </string-array>" >> "$dir/1.xml"
echo "    </string-array>" >> "$dir/2.xml"

echo "Add or update the following in your strings_nolocale.xml. The zh-rTW might have to be zh-TW though:"
cat -- "$dir/1.xml" "$dir/2.xml"
