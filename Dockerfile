FROM ubuntu:18.04

RUN dpkg --add-architecture i386 && \
    apt-get update -y && \
    apt-get install -y software-properties-common && \
    apt-get update -y && \
    apt-get install -y wget \
            openjdk-8-jre-headless=8u162-b12-1 \
            openjdk-8-jre=8u162-b12-1 \
            openjdk-8-jdk-headless=8u162-b12-1 \
            openjdk-8-jdk=8u162-b12-1 \
            git unzip && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get autoremove -y && \
    apt-get clean

# download and install Android SDK
ARG ANDROID_SDK_VERSION=9123335
ENV ANDROID_HOME /opt/android-sdk
RUN mkdir -p $ANDROID_HOME && cd $ANDROID_HOME && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip && \
    unzip *commandlinetools*linux*.zip -d cmdline-tools/ && \
    rm *commandlinetools*linux*.zip && \
    mv cmdline-tools/cmdline-tools/ cmdline-tools/latest/ && \
    yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# download and install Android NDK
ENV ANDROID_NDK_HOME /opt/android-ndk
ENV ANDROID_NDK_VERSION r21d
RUN mkdir /opt/android-ndk-tmp && \
    cd /opt/android-ndk-tmp && \
    wget -q https://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip && \
    unzip -q android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip && \
    mv ./android-ndk-${ANDROID_NDK_VERSION} ${ANDROID_NDK_HOME} && \
    cd ${ANDROID_NDK_HOME} && \
    rm -rf /opt/android-ndk-tmp
ENV PATH ${PATH}:${ANDROID_NDK_HOME}