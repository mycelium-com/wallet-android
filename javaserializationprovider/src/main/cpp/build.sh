SDK=$ANDROID_SDK_HOME
NDK_VERSION=$NDK_VERSION
SELF=$(basename $0)
BASE=$(cd $(dirname $0); pwd -P)
ARCHES="arm64-v8a x86_64 x86"

for ARCH in $ARCHES; do
  bash -c "$SDK/cmake/3.6.4111459/bin/cmake -H$BASE -B$BASE/target/build/$ARCH -DCMAKE_TOOLCHAIN_FILE=$SDK/ndk/$NDK_VERSION/build/cmake/android.toolchain.cmake -DCMAKE_LIBRARY_OUTPUT_DIRECTORY=$BASE/target/binaries/$ARCH -DANDROID_NDK=$SDK/ndk/$NDK_VERSION -DANDROID_ABI=$ARCH -DCMAKE_MAKE_PROGRAM=$SDK/cmake/3.6.4111459/bin/ninja -DANDROID_NATIVE_API_LEVEL=21 -DANDROID_PLATFORM=android-21 -DCMAKE_BUILD_TYPE=Release -G'Android Gradle - Ninja'"
  R=$?
  if [[ $R != 0 ]]; then
    echo "$T-$B make failed"
    exit $R
  fi
done

for ARCH in $ARCHES; do
  bash -c "$SDK/cmake/3.6.4111459/bin/cmake --build $BASE/target/build/$ARCH"
  R=$?
  if [[ $R != 0 ]]; then
    echo "$T-$B build failed"
    exit $R
  fi
done