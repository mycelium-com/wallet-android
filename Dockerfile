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

ENV ANDROID_SDK_FILENAME android-sdk_r27.0.3-linux.tgz
ENV ANDROID_SDK_URL https://dl.google.com/android/${ANDROID_SDK_FILENAME}
ENV ANDROID_API_LEVELS android-19,android-26
ENV ANDROID_BUILD_TOOLS_VERSION 27.0.3
ENV ANDROID_HOME /usr/local/android-sdk-linux
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools
RUN cd /usr/local/ && \
    wget -q ${ANDROID_SDK_URL} && \
    tar -xzf ${ANDROID_SDK_FILENAME} && \
    rm ${ANDROID_SDK_FILENAME}
RUN echo y | android update sdk --no-ui -a --filter ${ANDROID_API_LEVELS}
RUN echo y | android update sdk --no-ui -a --filter extra-android-m2repository,extra-android-support,extra-google-google_play_services,extra-google-m2repository
RUN echo y | android update sdk --no-ui -a --filter tools,platform-tools,build-tools-${ANDROID_BUILD_TOOLS_VERSION}
RUN rm -rf ${ANDROID_HOME}/tools

