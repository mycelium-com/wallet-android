#!/bin/bash

sizeNames=(ldpi mdpi hdpi xhdpi xxhdpi)
sizes=(360 480 720 960 1800)
fileIn=localTraderLocalOnly.png
fileOut=lt_local_only_warning.png

for ((n=0; n <= 4; n++))
do
    fOut="../src/main/res/drawable-"${sizeNames[n]}"/"$fileOut
    convert -background none -resize ${sizes[n]}"x" $fileIn - | \
        pngquant --force 64 > $fOut
done


sizes=(100 133 200 267 400)
fileIn=creditCard.png
fileOut=credit_card_buy.png

for ((n=0; n <= 4; n++))
do
    fOut="../src/main/res/drawable-"${sizeNames[n]}"/"$fileOut
    convert -background none -resize ${sizes[n]}"x" $fileIn - | \
        pngquant --force 64 > $fOut
done

sizes=(100 133 200 267 400)
fileIn=mycelium_logo_transp.png
fileOut=mycelium_logo_transp.png

for ((n=0; n <= 4; n++))
do
    fOut="../src/main/res/drawable-"${sizeNames[n]}"/"$fileOut
    convert -background none -resize ${sizes[n]}"x" $fileIn - | \
        pngquant --force 64 > $fOut
done