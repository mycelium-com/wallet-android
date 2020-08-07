
#include <jni.h>
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
    create(JNIEnv *env, jobject /* this */) {
        abieos_context* context = abieos_create();
        return env->NewDirectByteBuffer((void *)context, sizeof(context));
    }

    JNIEXPORT void JNICALL
    destroy(JNIEnv *env, jobject /* this */, jobject context_direct_byte_buffer) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        if (nullptr == context) return;

        abieos_destroy(context);
    }

    JNIEXPORT jstring JNICALL
    getError(JNIEnv *env, jobject /* this */, jobject context_direct_byte_buffer) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        return env->NewStringUTF(abieos_get_error(context));
    }

    JNIEXPORT jstring JNICALL
    getBinHex(JNIEnv *env, jobject /* this */, jobject context_direct_byte_buffer) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        const char *hex = abieos_get_bin_hex(context);

        return (nullptr == hex) ? nullptr : env->NewStringUTF(hex);
    }

    JNIEXPORT jlong JNICALL
    stringToName(JNIEnv *env, jobject /* this */,
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
    setAbi(JNIEnv *env, jobject /* this */,
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
    jsonToBin(JNIEnv *env, jobject /* this */,
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
    hexToJson(JNIEnv *env, jobject /* this */,
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
    getTypeForAction(JNIEnv *env, jobject /* this */,
              jobject context_direct_byte_buffer,
              jlong contract,
              jlong action) {
        abieos_context* context = getContext(env, context_direct_byte_buffer);
        const char *typeStr = abieos_get_type_for_action(context, (uint64_t)contract, (uint64_t)action);
        return typeStr == nullptr ? nullptr : env->NewStringUTF(typeStr);
    }

    static JNINativeMethod method_table[] = {
            {"create", "()Ljava/nio/ByteBuffer;", (void *) create},
            {"destroy", "(Ljava/nio/ByteBuffer;)V", (void *) destroy},
            {"getError", "(Ljava/nio/ByteBuffer;)Ljava/lang/String;", (void *) getError},
            {"getBinHex", "(Ljava/nio/ByteBuffer;)Ljava/lang/String;", (void *) getBinHex},
            {"stringToName", "(Ljava/nio/ByteBuffer;Ljava/lang/String;)J", (void *) stringToName},
            {"setAbi", "(Ljava/nio/ByteBuffer;JLjava/lang/String;)Z", (void *) setAbi},
            {"jsonToBin", "(Ljava/nio/ByteBuffer;JLjava/lang/String;Ljava/lang/String;Z)Z", (void *) jsonToBin},
            {"hexToJson", "(Ljava/nio/ByteBuffer;JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void *) hexToJson},
            {"getTypeForAction", "(Ljava/nio/ByteBuffer;JJ)Ljava/lang/String;", (void *) getTypeForAction }
    };

    JNIEXPORT jint JNICALL
    JNI_OnLoad(JavaVM *vm, void *reserved) {

        JNIEnv *env;
        if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return JNI_ERR;
        }

        jclass clazz = env->FindClass("fiofoundation/io/androidfioserializationprovider/AbiFIOSerializationProvider");
        if (NULL == clazz) {
            return JNI_ERR;
        }

        jint ret = env->RegisterNatives(clazz, method_table,
                                        sizeof(method_table) / sizeof(method_table[0]));
        return ret == 0 ? JNI_VERSION_1_6 : JNI_ERR;
    }
}


