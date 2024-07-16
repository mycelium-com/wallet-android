#!/bin/bash

set -euf -o pipefail

die () { echo "Error: $*"; exit 1; }

sizeNames=(ldpi mdpi hdpi xhdpi xxhdpi)
sizes=(360 480 720 960 1800)
fileIn=localTraderLocalOnly.png
fileOut=lt_local_only_warning.png

conv_function () {
    for ((n=0; n <= 4; n++))
    do
        fOut="../src/main/res/drawable-${sizeNames[n]}/${fileOut}"
        convert -background none -resize "${sizes[n]}x" "$fileIn" - | \
            pngquant --force 64 > "$fOut" || die "Command failed."
    done
}

conv_function || die "Command failed."

sizes=(100 133 200 267 400)
fileIn=creditCard.png
fileOut=credit_card_buy.png

conv_function || die "Command failed."

sizes=(100 133 200 267 400)
fileIn=mycelium_logo_transp.png
fileOut=mycelium_logo_transp.png

conv_function || die "Command failed."
