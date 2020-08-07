# Java FIO Serialization Provider

    Be sure to set the Android SDK location and NDK VERSION in build.sh before running the build.

    cd src/main/cpp
    sh build.sh

# Resulting files

The build.sh script builds the "libabieos-lib.so" file located at src/main/android-cpp/target/binaries.
There is a "libabieos-lib.so" for each supported platform.  The "libabieos-lib.so" in the root of
the "binaries" folder supports all platforms.

#Building the Serialization Provider

From the build menu in Visual Studio, select "Make Module".

The serialization provider is also built when the entire project is built.  The resulting "jar"
file is located in the build/libs folder.