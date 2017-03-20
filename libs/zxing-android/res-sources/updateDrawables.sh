#!/bin/bash

sizeNames=(ldpi mdpi hdpi xhdpi xxhdpi)
sizes=(30 64 96 128 250)
fileIn=camera-switcher.svg
fileOut=camera_switcher.png

for ((n=0; n <= 4; n++))
do
    fOut="../src/main/res/drawable-"${sizeNames[n]}"/"$fileOut
    convert -background none -resize ${sizes[n]}"x" $fileIn - | \
        pngquant --force 64 > $fOut
done
