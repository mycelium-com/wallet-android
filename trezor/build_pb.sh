#!/bin/bash

# create directory ../trezor-common by running
# git clone https://github.com/trezor/trezor-common

CURDIR=$(pwd)

cd $CURDIR/../trezor-common/protob

for i in messages types ; do
    echo $i
    protoc --java_out=$CURDIR/src/ $i.proto
done
