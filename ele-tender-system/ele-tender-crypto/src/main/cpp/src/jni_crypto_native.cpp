#include "crypto_core.hpp"
#include "crypto_error.hpp"

#include <jni.h>

#include <array>
#include <string>
#include <vector>

using eletender::crypto::CryptoException;
using eletender::crypto::ParsedEncryptedFile;

namespace {

std::string to_string(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return "";
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        throw CryptoException("JNI_STRING_FAILED", "Unable to access Java string");
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::vector<std::uint8_t> to_bytes(JNIEnv* env, jbyteArray array) {
    if (array == nullptr) {
        return {};
    }
    jsize length = env->GetArrayLength(array);
    std::vector<std::uint8_t> result(static_cast<std::size_t>(length));
    env->GetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte*>(result.data()));
    return result;
}

jbyteArray to_jbyte_array(JNIEnv* env, const std::vector<std::uint8_t>& bytes) {
    jbyteArray array = env->NewByteArray(static_cast<jsize>(bytes.size()));
    if (array == nullptr) {
        return nullptr;
    }
    if (!bytes.empty()) {
        env->SetByteArrayRegion(array, 0, static_cast<jsize>(bytes.size()),
                                reinterpret_cast<const jbyte*>(bytes.data()));
    }
    return array;
}

jobject to_unpack_result(JNIEnv* env, const ParsedEncryptedFile& parsed) {
    jclass result_class = env->FindClass("com/jy/eletender/crypto/native_bridge/UnpackResult");
    jmethodID ctor = env->GetMethodID(result_class, "<init>", "()V");
    jobject result = env->NewObject(result_class, ctor);

    jmethodID get_segments = env->GetMethodID(result_class, "getEncryptedSegments", "()Ljava/util/List;");
    jobject segments = env->CallObjectMethod(result, get_segments);
    jclass list_class = env->FindClass("java/util/List");
    jmethodID add = env->GetMethodID(list_class, "add", "(Ljava/lang/Object;)Z");
    for (const auto& segment : parsed.encrypted_segments) {
        env->CallBooleanMethod(segments, add, to_jbyte_array(env, segment));
    }

    jmethodID set_project = env->GetMethodID(result_class, "setEncryptedProjectInfoRsa", "([B)V");
    env->CallVoidMethod(result, set_project, to_jbyte_array(env, parsed.encrypted_project_info_rsa));

    jmethodID set_version = env->GetMethodID(result_class, "setFormatVersion", "(Ljava/lang/String;)V");
    env->CallVoidMethod(result, set_version, env->NewStringUTF(parsed.format_version.c_str()));

    jmethodID set_raw = env->GetMethodID(result_class, "setRawFileBytes", "([B)V");
    env->CallVoidMethod(result, set_raw, to_jbyte_array(env, parsed.raw_file_bytes));
    return result;
}

std::vector<std::vector<std::uint8_t>> to_byte_list(JNIEnv* env, jobject list_object) {
    std::vector<std::vector<std::uint8_t>> result;
    if (list_object == nullptr) {
        return result;
    }
    jclass list_class = env->FindClass("java/util/List");
    jmethodID size_method = env->GetMethodID(list_class, "size", "()I");
    jmethodID get_method = env->GetMethodID(list_class, "get", "(I)Ljava/lang/Object;");
    jint size = env->CallIntMethod(list_object, size_method);
    result.reserve(static_cast<std::size_t>(size));
    for (jint i = 0; i < size; ++i) {
        auto* bytes = reinterpret_cast<jbyteArray>(env->CallObjectMethod(list_object, get_method, i));
        result.push_back(to_bytes(env, bytes));
    }
    return result;
}

jobject to_java_list(JNIEnv* env, const std::vector<std::vector<std::uint8_t>>& values) {
    jclass array_list_class = env->FindClass("java/util/ArrayList");
    jmethodID ctor = env->GetMethodID(array_list_class, "<init>", "()V");
    jmethodID add = env->GetMethodID(array_list_class, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(array_list_class, ctor);
    for (const auto& value : values) {
        env->CallBooleanMethod(list, add, to_jbyte_array(env, value));
    }
    return list;
}

std::array<std::uint8_t, 32> to_key(JNIEnv* env, jbyteArray array) {
    std::vector<std::uint8_t> bytes = to_bytes(env, array);
    if (bytes.size() != 32) {
        throw CryptoException("INVALID_AES_KEY_LENGTH", "keyBytes must be exactly 32 bytes");
    }
    std::array<std::uint8_t, 32> key{};
    std::copy(bytes.begin(), bytes.end(), key.begin());
    return key;
}

void throw_java_exception(JNIEnv* env, const CryptoException& ex) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    std::string message = ex.code() + ": " + ex.what();
    env->ThrowNew(exception_class, message.c_str());
}

void throw_java_exception(JNIEnv* env, const std::exception& ex) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    env->ThrowNew(exception_class, ex.what());
}

}

extern "C" {

JNIEXPORT jobject JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativeUnpackFile
  (JNIEnv* env, jobject, jbyteArray encrypted_file_bytes) {
    try {
        ParsedEncryptedFile parsed = eletender::crypto::unpack_file(to_bytes(env, encrypted_file_bytes));
        return to_unpack_result(env, parsed);
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativePackFile
  (JNIEnv* env, jobject, jobject encrypted_segments, jbyteArray encrypted_project_info_rsa, jstring format_version) {
    try {
        auto packed = eletender::crypto::pack_file(
            to_byte_list(env, encrypted_segments),
            to_bytes(env, encrypted_project_info_rsa),
            to_string(env, format_version)
        );
        return to_jbyte_array(env, packed);
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativeEncryptSegments
  (JNIEnv* env, jobject, jbyteArray plain_bytes, jbyteArray key_bytes, jint segment_size) {
    try {
        auto segments = eletender::crypto::encrypt_segments(
            to_bytes(env, plain_bytes),
            to_key(env, key_bytes),
            static_cast<std::size_t>(segment_size)
        );
        return to_java_list(env, segments);
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativeDecryptSegments
  (JNIEnv* env, jobject, jobject encrypted_segments, jbyteArray key_bytes) {
    try {
        auto plain = eletender::crypto::decrypt_segments(
            to_byte_list(env, encrypted_segments),
            to_key(env, key_bytes)
        );
        return to_jbyte_array(env, plain);
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativeRsaEncrypt
  (JNIEnv* env, jobject, jbyteArray plain_bytes, jstring public_key) {
    try {
        auto bytes = eletender::crypto::rsa_encrypt(to_bytes(env, plain_bytes), to_string(env, public_key));
        return to_jbyte_array(env, bytes);
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativeRsaDecrypt
  (JNIEnv* env, jobject, jbyteArray cipher_bytes, jstring private_key) {
    try {
        auto bytes = eletender::crypto::rsa_decrypt(to_bytes(env, cipher_bytes), to_string(env, private_key));
        return to_jbyte_array(env, bytes);
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

JNIEXPORT jstring JNICALL Java_com_jy_eletender_crypto_native_1bridge_JniCryptoNative_nativeSha256
  (JNIEnv* env, jobject, jbyteArray data) {
    try {
        std::string digest = eletender::crypto::sha256_hex(to_bytes(env, data));
        return env->NewStringUTF(digest.c_str());
    } catch (const CryptoException& ex) {
        throw_java_exception(env, ex);
    } catch (const std::exception& ex) {
        throw_java_exception(env, ex);
    }
    return nullptr;
}

}
