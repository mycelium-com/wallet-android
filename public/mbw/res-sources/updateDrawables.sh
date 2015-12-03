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
