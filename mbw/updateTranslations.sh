#!/bin/sh

APIKEY=`cat ~/.mycelium_crowdin_api`

cwd=`pwd`
dir=/tmp/translation
rm -r ${dir}
mkdir -p ${dir} || exit
cd ${dir} || exit
rm -f mycelium-bitcoin-wallet.zip
wget -O all.zip https://api.crowdin.com/api/project/mycelium-bitcoin-wallet/download/all.zip?key=$APIKEY || exit
unzip -q all.zip -d ${dir} || exit

UpdateOne () {
 # arg 1: source folder as in ../${1}/strings.xml
 # arg 2: destination folder as in ../res/values-${2}
 echo "Updating language ${1}"
 mkdir -p ${cwd}/src/main/res/values-${2} && cp -f ${dir}/${1}/strings.xml ${cwd}/src/main/res/values-${2} || echo "Failed to update language ${1}"
}

mkdir -p ${cwd}/res/values || exit

UpdateOne bg bg        Bulgarian-Bulgaria
UpdateOne cs cs        Czech
UpdateOne da da        Danish-Danmark
UpdateOne de de        German-Germany
UpdateOne el el        Greek-Greece
UpdateOne es-ES es     Spanish-Spain
UpdateOne fil fil      Philipino
UpdateOne fr fr        French-France
UpdateOne he iw        Hebrew-Israel
UpdateOne id id        Indonesian-Indonesia
UpdateOne it it        Italian-Italy
UpdateOne ja ja        Japanese-Japan
UpdateOne ko ko        Korean-Korea
UpdateOne nl nl        Dutch/Netherlands
UpdateOne pl pl        Polish-Poland
UpdateOne pt-PT pt     Portuguese-Portugal
UpdateOne ro ro        Romanian/Moldavian-Romania
UpdateOne ru ru        Russian-Russia
UpdateOne sk sk        Slovak-Slovakia
UpdateOne sl sl        Slovenia
UpdateOne sq sq        Albania
UpdateOne sv-SE sv     Swedish-Sweden
UpdateOne vi vi        Vietnamese-Vietnam
UpdateOne zh-CN zh     Chinese-China
UpdateOne zh-TW zh-rTW Chinese-Taiwan
