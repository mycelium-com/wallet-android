
#include "jni.h"
#include <string>

#include "abieos.h"

extern "C"
{
    const char *empty = "";

    abieos_context * getContext(JNIEnv *env, jobject context_buffer) {
        if (nullptr == context_buffer) return nullptr;
        return (abieos_context *)env->GetDirectBufferAddress(context_buffer);
    }

    JNIEXPORT jobject JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_create(JNIEnv *env, jobject /* this */) {
        abieos_context* context = abieos_create();
        return env->NewDirectByteBuffer((void *)context, sizeof(context));
    }

    JNIEXPORT void JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_destroy(JNIEnv *env, jobject /* this */, jobject context_direct_byte_buffer) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        if (nullptr == context) return;

        abieos_destroy(context);
    }

    JNIEXPORT jstring JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_getError(JNIEnv *env, jobject /* this */, jobject context_direct_byte_buffer) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        return env->NewStringUTF(abieos_get_error(context));
    }

    JNIEXPORT jstring JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_getBinHex(JNIEnv *env, jobject /* this */, jobject context_direct_byte_buffer) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        const char *hex = abieos_get_bin_hex(context);

        return (nullptr == hex) ? nullptr : env->NewStringUTF(hex);
    }

    JNIEXPORT jlong JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_stringToName(JNIEnv *env, jobject /* this */,
            jobject context_direct_byte_buffer,
            jstring str) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        jboolean isCopy;
        const char *nameStr = (str == nullptr) ? nullptr : env->GetStringUTFChars(str, &isCopy);
        jlong name = abieos_string_to_name(context, nameStr);
        if (nameStr != nullptr)
            env->ReleaseStringUTFChars(str, nameStr);
        return name;
    }

    JNIEXPORT jboolean JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_setAbi(JNIEnv *env, jobject /* this */,
            jobject context_direct_byte_buffer,
            jlong contract,
            jstring abi) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        jboolean isCopy;
        const char *abiStr = (abi == nullptr) ? nullptr : env->GetStringUTFChars(abi, &isCopy);
        abieos_bool ret = abieos_set_abi(context, contract, abiStr);
        if (abiStr != nullptr)
            env->ReleaseStringUTFChars(abi, abiStr);
        return (jboolean)ret;
    }

    JNIEXPORT jboolean JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_jsonToBin(JNIEnv *env, jobject /* this */,
            jobject context_direct_byte_buffer,
            jlong contract,
            jstring type,
            jstring json,
            jboolean reorderable) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        jboolean isTypeCopy;
        const char *typeStr = (type == nullptr) ? nullptr : env->GetStringUTFChars(type, &isTypeCopy);
        jboolean isJsonCopy;
        const char *jsonStr = (json == nullptr) ? nullptr : env->GetStringUTFChars(json, &isJsonCopy);

        abieos_bool ret = 0;
        if (reorderable) {
            ret = abieos_json_to_bin_reorderable(context, contract, typeStr, jsonStr);
        } else {
            ret = abieos_json_to_bin(context, contract, typeStr, jsonStr);
        }
        if (typeStr != nullptr)
            env->ReleaseStringUTFChars(type, typeStr);
        if (jsonStr != nullptr)
            env->ReleaseStringUTFChars(json, jsonStr);
        return (jboolean)ret;
    }

    JNIEXPORT jstring JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_hexToJson(JNIEnv *env, jobject /* this */,
              jobject context_direct_byte_buffer,
              jlong contract,
              jstring type,
              jstring hex) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        jboolean isTypeCopy;
        const char *typeStr = (type == nullptr) ? nullptr : env->GetStringUTFChars(type, &isTypeCopy);
        jboolean isHexCopy;
        const char *hexStr = (hex == nullptr) ? nullptr : env->GetStringUTFChars(hex, &isHexCopy);

        const char *jsonStr = abieos_hex_to_json(context, contract, typeStr, hexStr);

        if (typeStr != nullptr)
            env->ReleaseStringUTFChars(type, typeStr);
        if (hexStr != nullptr)
            env->ReleaseStringUTFChars(hex, hexStr);

        return jsonStr == nullptr ? nullptr : env->NewStringUTF(jsonStr);
    }

    JNIEXPORT jstring JNICALL
    Java_fiofoundation_io_javaserializationprovider_AbiFIOSerializationProvider_getTypeForAction(JNIEnv *env, jobject /* this */,
              jobject context_direct_byte_buffer,
              jlong contract,
              jlong action) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        const char *typeStr = abieos_get_type_for_action(context, (uint64_t)contract, (uint64_t)action);
        return typeStr == nullptr ? nullptr : env->NewStringUTF(typeStr);
    }
}