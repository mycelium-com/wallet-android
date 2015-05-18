#!/bin/sh
cwd=`pwd`
pushd `pwd`
dir=/tmp/translation
rm -r ${dir}
mkdir -p ${dir} || exit
cd ${dir} || exit
rm -f mycelium-bitcoin-wallet.zip
wget https://crowdin.net/download/project/mycelium-bitcoin-wallet.zip || exit
unzip -q mycelium-bitcoin-wallet.zip -d ${dir} || exit

UpdateOne () {
 # arg 1: source folder as in ../${1}/strings.xml
 # arg 2: destination folder as in ../res/values-${2}
 echo "Updating language ${1}"
 mkdir -p ${cwd}/src/main/res/values-${2} && cp -f ${dir}/${1}/strings.xml ${cwd}/src/main/res/values-${2} || echo “Failed to update language ${1}”
}

mkdir -p ${cwd}/res/values || exit

UpdateOne da da
UpdateOne de de
UpdateOne el el
UpdateOne es-ES es
UpdateOne fr fr
UpdateOne he iw
UpdateOne it it
UpdateOne ja ja
UpdateOne ko ko
UpdateOne nl nl
UpdateOne pl pl
UpdateOne pt-PT pt
UpdateOne ru ru
UpdateOne sk sk
UpdateOne sl sl
UpdateOne sv-SE sv
UpdateOne zh-CN zh
UpdateOne zh-TW zh-rTW
UpdateOne vi vi
UpdateOne sq sq

