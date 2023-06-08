FROM ubuntu:18.04

RUN dpkg --add-architecture i386 && \
    apt-get update -y && \
    apt-get install -y software-properties-common && \
    apt-get update -y && \
    apt-get install -y wget \
            openjdk-11-jdk \
            git unzip disorderfs && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get autoremove -y && \
    apt-get clean

ARG ANDROID_SDK_VERSION=9123335
ARG ANDROID_BUILD_TOOLS_VERSION=30.0.3
ARG ANDROID_NDK_VERSION=25.1.8937393
ARG ANDROID_PLATFORM_VERSION=31
ARG CMAKE_VERSION=3.10.2.4988404

ENV ANDROID_HOME /opt/android-sdk
ENV ANDROID_SDK_HOME  ${ANDROID_HOME}
ENV ANDROID_SDK_ROOT  ${ANDROID_HOME}
ENV ANDROID_SDK       ${ANDROID_HOME}

ENV PATH "${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin"
ENV PATH "${PATH}:${ANDROID_HOME}/tools/bin"
ENV PATH "${PATH}:${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS_VERSION}"
ENV PATH "${PATH}:${ANDROID_HOME}/platform-tools"
ENV PATH "${PATH}:${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}"
ENV PATH "${PATH}:${ANDROID_HOME}/emulator"

# download and install Android SDK
RUN mkdir -p $ANDROID_HOME && cd $ANDROID_HOME && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip && \
    unzip *commandlinetools*linux*.zip -d cmdline-tools/ && \
    rm *commandlinetools*linux*.zip && \
    mv cmdline-tools/cmdline-tools/ cmdline-tools/latest/ && \
    yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools"
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-${ANDROID_PLATFORM_VERSION}"
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "system-images;android-${ANDROID_PLATFORM_VERSION};google_apis_playstore;x86_64"
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS_VERSION}"
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "ndk;${ANDROID_NDK_VERSION}"
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "cmake;${CMAKE_VERSION}"
