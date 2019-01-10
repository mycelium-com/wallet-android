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
ARG ANDROID_SDK_VERSION=4333796
ENV ANDROID_HOME /opt/android-sdk
RUN mkdir -p /opt/android-sdk && cd /opt/android-sdk && \
    wget -q https://dl.google.com/android/repository/sdk-tools-linux-${ANDROID_SDK_VERSION}.zip && \
    unzip *tools*linux*.zip && \
    rm *tools*linux*.zip && \
    yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses