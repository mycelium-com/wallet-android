#!/bin/sh

# create directory ../trezor-common by running
# git clone https://github.com/trezor/trezor-common

set -euf

CURDIR="$(pwd)"

if ! cd "$CURDIR/../trezor-common/protob"; then
    printf "Cannot cd to %s\n" "$CURDIR/../trezor-common/protob"
    exit 1
fi

for i in messages types ; do
    printf "%s\n" "$i"
    protoc --java_out="$CURDIR/src/" "$i.proto" ||
        printf "Command %s failed\n" "protoc --java_out=\"$CURDIR/src/\" \"$i.proto\""
done
