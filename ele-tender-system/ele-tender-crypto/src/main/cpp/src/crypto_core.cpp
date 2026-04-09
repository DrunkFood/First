#include "crypto_core.hpp"
#include "crypto_error.hpp"

#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/sha.h>

#include <algorithm>
#include <array>
#include <cctype>
#include <cstring>
#include <iomanip>
#include <memory>
#include <sstream>
#include <string_view>

namespace eletender::crypto {
namespace {

constexpr std::string_view kSegmentDelimiter = "|||";
constexpr std::string_view kProjectInfoBegin = "{{{";
constexpr std::string_view kProjectInfoEnd = "}}}";
constexpr std::string_view kVersionBegin = "[[";
constexpr std::string_view kVersionEnd = "]]";
constexpr std::size_t kNonceLength = 12;
constexpr std::size_t kTagLength = 16;

using EvpCipherCtxPtr = std::unique_ptr<EVP_CIPHER_CTX, decltype(&EVP_CIPHER_CTX_free)>;
using EvpPkeyPtr = std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)>;
using EvpPkeyCtxPtr = std::unique_ptr<EVP_PKEY_CTX, decltype(&EVP_PKEY_CTX_free)>;

std::string join_strings(const std::vector<std::string>& parts, std::string_view delimiter) {
    std::ostringstream out;
    for (std::size_t i = 0; i < parts.size(); ++i) {
        if (i > 0) {
            out << delimiter;
        }
        out << parts[i];
    }
    return out.str();
}

std::string base64_encode(const std::vector<std::uint8_t>& data) {
    if (data.empty()) {
        return "";
    }
    std::string encoded;
    encoded.resize(4 * ((data.size() + 2) / 3));
    int length = EVP_EncodeBlock(reinterpret_cast<unsigned char*>(&encoded[0]), data.data(),
                                 static_cast<int>(data.size()));
    if (length < 0) {
        throw CryptoException("BASE64_ENCODE_FAILED", "Base64 encoding failed");
    }
    encoded.resize(static_cast<std::size_t>(length));
    return encoded;
}

std::vector<std::uint8_t> base64_decode(const std::string& data) {
    if (data.empty()) {
        return {};
    }
    std::size_t padding = 0;
    if (!data.empty() && data.back() == '=') {
        padding++;
    }
    if (data.size() > 1 && data[data.size() - 2] == '=') {
        padding++;
    }
    std::vector<std::uint8_t> decoded(3 * (data.size() / 4) + 3);
    int length = EVP_DecodeBlock(decoded.data(),
                                 reinterpret_cast<const unsigned char*>(data.data()),
                                 static_cast<int>(data.size()));
    if (length < 0) {
        throw CryptoException("INVALID_BASE64_SEGMENT", "Base64 decoding failed");
    }
    decoded.resize(static_cast<std::size_t>(length) - padding);
    return decoded;
}

std::string trim_copy(const std::string& value) {
    auto begin = std::find_if_not(value.begin(), value.end(), [](unsigned char ch) { return std::isspace(ch); });
    auto end = std::find_if_not(value.rbegin(), value.rend(), [](unsigned char ch) { return std::isspace(ch); }).base();
    if (begin >= end) {
        return "";
    }
    return std::string(begin, end);
}

std::string normalize_key_text(const std::string& key) {
    std::string normalized = key;
    auto erase_all = [&](std::string_view token) {
        std::size_t pos = 0;
        while ((pos = normalized.find(token.data(), pos, token.size())) != std::string::npos) {
            normalized.erase(pos, token.size());
        }
    };
    erase_all("-----BEGIN PUBLIC KEY-----");
    erase_all("-----END PUBLIC KEY-----");
    erase_all("-----BEGIN PRIVATE KEY-----");
    erase_all("-----END PRIVATE KEY-----");
    normalized.erase(std::remove_if(normalized.begin(), normalized.end(),
                                    [](unsigned char ch) { return std::isspace(ch); }),
                     normalized.end());
    return normalized;
}

EvpPkeyPtr parse_public_key(const std::string& public_key) {
    std::vector<std::uint8_t> der = base64_decode(normalize_key_text(public_key));
    const unsigned char* ptr = der.data();
    EVP_PKEY* key = d2i_PUBKEY(nullptr, &ptr, static_cast<long>(der.size()));
    if (key == nullptr) {
        throw CryptoException("RSA_INVALID_PUBLIC_KEY", "Unable to parse RSA public key");
    }
    return EvpPkeyPtr(key, EVP_PKEY_free);
}

EvpPkeyPtr parse_private_key(const std::string& private_key) {
    std::vector<std::uint8_t> der = base64_decode(normalize_key_text(private_key));
    const unsigned char* ptr = der.data();
    EVP_PKEY* key = d2i_AutoPrivateKey(nullptr, &ptr, static_cast<long>(der.size()));
    if (key == nullptr) {
        throw CryptoException("RSA_INVALID_PRIVATE_KEY", "Unable to parse RSA private key");
    }
    return EvpPkeyPtr(key, EVP_PKEY_free);
}

std::size_t rsa_key_size_bytes(EVP_PKEY* key) {
    int size = EVP_PKEY_get_size(key);
    if (size <= 0) {
        throw CryptoException("RSA_KEY_SIZE_FAILED", "Unable to resolve RSA key size");
    }
    return static_cast<std::size_t>(size);
}

std::vector<std::uint8_t> rsa_process(const std::vector<std::uint8_t>& input,
                                      EVP_PKEY* key,
                                      bool encrypt_mode) {
    EvpPkeyCtxPtr ctx(EVP_PKEY_CTX_new(key, nullptr), EVP_PKEY_CTX_free);
    if (!ctx) {
        throw CryptoException("RSA_CONTEXT_FAILED", "Unable to create RSA context");
    }
    int init_result = encrypt_mode ? EVP_PKEY_encrypt_init(ctx.get()) : EVP_PKEY_decrypt_init(ctx.get());
    if (init_result <= 0) {
        throw CryptoException(encrypt_mode ? "RSA_ENCRYPT_INIT_FAILED" : "RSA_DECRYPT_INIT_FAILED",
                              "Unable to initialize RSA context");
    }
    if (EVP_PKEY_CTX_set_rsa_padding(ctx.get(), RSA_PKCS1_PADDING) <= 0) {
        throw CryptoException("RSA_PADDING_FAILED", "Unable to configure RSA PKCS1 padding");
    }

    std::size_t key_size = rsa_key_size_bytes(key);
    std::size_t input_block_size = encrypt_mode ? key_size - 11 : key_size;
    std::vector<std::uint8_t> output;
    for (std::size_t offset = 0; offset < input.size(); offset += input_block_size) {
        std::size_t chunk_size = std::min(input_block_size, input.size() - offset);
        std::size_t out_len = 0;
        const unsigned char* chunk = input.data() + offset;
        int size_result = encrypt_mode
            ? EVP_PKEY_encrypt(ctx.get(), nullptr, &out_len, chunk, chunk_size)
            : EVP_PKEY_decrypt(ctx.get(), nullptr, &out_len, chunk, chunk_size);
        if (size_result <= 0) {
            throw CryptoException(encrypt_mode ? "RSA_ENCRYPT_FAILED" : "RSA_DECRYPT_FAILED",
                                  "Unable to determine RSA output size");
        }
        std::vector<std::uint8_t> block(out_len);
        int process_result = encrypt_mode
            ? EVP_PKEY_encrypt(ctx.get(), block.data(), &out_len, chunk, chunk_size)
            : EVP_PKEY_decrypt(ctx.get(), block.data(), &out_len, chunk, chunk_size);
        if (process_result <= 0) {
            throw CryptoException(encrypt_mode ? "RSA_ENCRYPT_FAILED" : "RSA_DECRYPT_FAILED",
                                  "RSA processing failed");
        }
        block.resize(out_len);
        output.insert(output.end(), block.begin(), block.end());
    }
    return output;
}

std::array<std::uint8_t, 32> ensure_key_length(const std::array<std::uint8_t, 32>& key_bytes) {
    return key_bytes;
}

std::vector<std::string> split_segments(const std::string& raw) {
    std::vector<std::string> parts;
    std::size_t start = 0;
    while (start <= raw.size()) {
        std::size_t pos = raw.find(kSegmentDelimiter, start);
        if (pos == std::string::npos) {
            std::string token = raw.substr(start);
            if (!trim_copy(token).empty()) {
                parts.push_back(token);
            }
            break;
        }
        std::string token = raw.substr(start, pos - start);
        if (!trim_copy(token).empty()) {
            parts.push_back(token);
        }
        start = pos + kSegmentDelimiter.size();
    }
    return parts;
}

std::string to_hex(const unsigned char* bytes, std::size_t length) {
    std::ostringstream out;
    out << std::hex << std::setfill('0');
    for (std::size_t i = 0; i < length; ++i) {
        out << std::setw(2) << static_cast<int>(bytes[i]);
    }
    return out.str();
}

std::vector<std::uint8_t> aes_gcm_encrypt_segment(const std::vector<std::uint8_t>& plain_segment,
                                                  const std::array<std::uint8_t, 32>& key_bytes,
                                                  std::uint64_t segment_index) {
    EvpCipherCtxPtr ctx(EVP_CIPHER_CTX_new(), EVP_CIPHER_CTX_free);
    if (!ctx) {
        throw CryptoException("AES_CONTEXT_FAILED", "Unable to create AES context");
    }
    std::array<std::uint8_t, kNonceLength> nonce{};
    for (std::size_t i = 0; i < kNonceLength; ++i) {
        nonce[i] = static_cast<std::uint8_t>((segment_index >> (8 * i)) & 0xffu);
    }

    if (EVP_EncryptInit_ex(ctx.get(), EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1 ||
        EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_IVLEN, static_cast<int>(kNonceLength), nullptr) != 1 ||
        EVP_EncryptInit_ex(ctx.get(), nullptr, nullptr, key_bytes.data(), nonce.data()) != 1) {
        throw CryptoException("AES_ENCRYPT_INIT_FAILED", "Unable to initialize AES-GCM encrypt context");
    }

    std::vector<std::uint8_t> ciphertext(plain_segment.size());
    int out_len = 0;
    if (!plain_segment.empty() &&
        EVP_EncryptUpdate(ctx.get(), ciphertext.data(), &out_len, plain_segment.data(),
                          static_cast<int>(plain_segment.size())) != 1) {
        throw CryptoException("AES_ENCRYPT_FAILED", "AES-GCM encrypt update failed");
    }
    int total_out = out_len;
    if (EVP_EncryptFinal_ex(ctx.get(), ciphertext.data() + total_out, &out_len) != 1) {
        throw CryptoException("AES_ENCRYPT_FAILED", "AES-GCM encrypt final failed");
    }
    total_out += out_len;
    ciphertext.resize(total_out);

    std::array<std::uint8_t, kTagLength> tag{};
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_GET_TAG, static_cast<int>(kTagLength), tag.data()) != 1) {
        throw CryptoException("AES_TAG_FAILED", "Unable to extract AES-GCM tag");
    }

    std::vector<std::uint8_t> encoded;
    encoded.reserve(kNonceLength + ciphertext.size() + kTagLength);
    encoded.insert(encoded.end(), nonce.begin(), nonce.end());
    encoded.insert(encoded.end(), ciphertext.begin(), ciphertext.end());
    encoded.insert(encoded.end(), tag.begin(), tag.end());
    return encoded;
}

std::vector<std::uint8_t> aes_gcm_decrypt_segment(const std::vector<std::uint8_t>& encrypted_segment,
                                                  const std::array<std::uint8_t, 32>& key_bytes) {
    if (encrypted_segment.size() < kNonceLength + kTagLength) {
        throw CryptoException("AES_DECRYPT_FAILED", "Encrypted segment is too short");
    }
    const unsigned char* nonce = encrypted_segment.data();
    const unsigned char* tag = encrypted_segment.data() + encrypted_segment.size() - kTagLength;
    const unsigned char* ciphertext = encrypted_segment.data() + kNonceLength;
    std::size_t ciphertext_len = encrypted_segment.size() - kNonceLength - kTagLength;

    EvpCipherCtxPtr ctx(EVP_CIPHER_CTX_new(), EVP_CIPHER_CTX_free);
    if (!ctx) {
        throw CryptoException("AES_CONTEXT_FAILED", "Unable to create AES context");
    }
    if (EVP_DecryptInit_ex(ctx.get(), EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1 ||
        EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_IVLEN, static_cast<int>(kNonceLength), nullptr) != 1 ||
        EVP_DecryptInit_ex(ctx.get(), nullptr, nullptr, key_bytes.data(), nonce) != 1) {
        throw CryptoException("AES_DECRYPT_INIT_FAILED", "Unable to initialize AES-GCM decrypt context");
    }

    std::vector<std::uint8_t> plain(ciphertext_len);
    int out_len = 0;
    if (ciphertext_len > 0 &&
        EVP_DecryptUpdate(ctx.get(), plain.data(), &out_len, ciphertext, static_cast<int>(ciphertext_len)) != 1) {
        throw CryptoException("AES_DECRYPT_FAILED", "AES-GCM decrypt update failed");
    }
    int total_out = out_len;
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_TAG, static_cast<int>(kTagLength),
                            const_cast<unsigned char*>(tag)) != 1) {
        throw CryptoException("AES_DECRYPT_FAILED", "Unable to set AES-GCM tag");
    }
    if (EVP_DecryptFinal_ex(ctx.get(), plain.data() + total_out, &out_len) != 1) {
        throw CryptoException("AES_DECRYPT_FAILED", "AES-GCM auth tag verification failed");
    }
    total_out += out_len;
    plain.resize(total_out);
    return plain;
}

}

ParsedEncryptedFile unpack_file(const std::vector<std::uint8_t>& file_bytes) {
    ParsedEncryptedFile parsed;
    parsed.raw_file_bytes = file_bytes;
    std::string raw(file_bytes.begin(), file_bytes.end());

    std::size_t version_begin = raw.rfind(kVersionBegin);
    std::size_t version_end = raw.rfind(kVersionEnd);
    if (version_begin == std::string::npos || version_end == std::string::npos || version_end <= version_begin) {
        throw CryptoException("MISSING_VERSION", "Encrypted file is missing format version");
    }
    parsed.format_version = raw.substr(version_begin + kVersionBegin.size(),
                                       version_end - version_begin - kVersionBegin.size());
    raw.erase(version_begin, version_end + kVersionEnd.size() - version_begin);

    std::size_t project_begin = raw.rfind(kProjectInfoBegin);
    std::size_t project_end = raw.rfind(kProjectInfoEnd);
    if (project_begin != std::string::npos && project_end != std::string::npos && project_end > project_begin) {
        std::string project_base64 = raw.substr(project_begin + kProjectInfoBegin.size(),
                                                project_end - project_begin - kProjectInfoBegin.size());
        parsed.encrypted_project_info_rsa = base64_decode(project_base64);
        raw.erase(project_begin, project_end + kProjectInfoEnd.size() - project_begin);
    }

    for (const std::string& segment : split_segments(raw)) {
        parsed.encrypted_segments.push_back(base64_decode(trim_copy(segment)));
    }

    if (parsed.encrypted_segments.empty()) {
        throw CryptoException("INVALID_FILE_FORMAT", "Encrypted file contains no segments");
    }
    return parsed;
}

std::vector<std::uint8_t> pack_file(
    const std::vector<std::vector<std::uint8_t>>& encrypted_segments,
    const std::vector<std::uint8_t>& encrypted_project_info_rsa,
    const std::string& format_version
) {
    if (encrypted_segments.empty()) {
        throw CryptoException("INVALID_FILE_FORMAT", "Cannot pack file without encrypted segments");
    }
    if (format_version.empty()) {
        throw CryptoException("MISSING_VERSION", "Format version is required");
    }

    std::vector<std::string> encoded_segments;
    encoded_segments.reserve(encrypted_segments.size());
    for (const auto& segment : encrypted_segments) {
        encoded_segments.push_back(base64_encode(segment));
    }

    std::string packed = join_strings(encoded_segments, kSegmentDelimiter);
    if (!encrypted_project_info_rsa.empty()) {
        packed.append(kSegmentDelimiter)
              .append(kProjectInfoBegin)
              .append(base64_encode(encrypted_project_info_rsa))
              .append(kProjectInfoEnd);
    }
    packed.append(kVersionBegin).append(format_version).append(kVersionEnd);
    return std::vector<std::uint8_t>(packed.begin(), packed.end());
}

std::vector<std::vector<std::uint8_t>> encrypt_segments(
    const std::vector<std::uint8_t>& plain_bytes,
    const std::array<std::uint8_t, 32>& key_bytes,
    std::size_t segment_size
) {
    ensure_key_length(key_bytes);
    if (segment_size == 0) {
        throw CryptoException("INVALID_AES_KEY_LENGTH", "segment_size must be greater than zero");
    }
    std::vector<std::vector<std::uint8_t>> segments;
    for (std::size_t offset = 0, index = 0; offset < plain_bytes.size(); offset += segment_size, ++index) {
        std::size_t length = std::min(segment_size, plain_bytes.size() - offset);
        std::vector<std::uint8_t> chunk(plain_bytes.begin() + static_cast<long>(offset),
                                        plain_bytes.begin() + static_cast<long>(offset + length));
        segments.push_back(aes_gcm_encrypt_segment(chunk, key_bytes, index));
    }
    if (segments.empty()) {
        segments.push_back(aes_gcm_encrypt_segment({}, key_bytes, 0));
    }
    return segments;
}

std::vector<std::uint8_t> decrypt_segments(
    const std::vector<std::vector<std::uint8_t>>& encrypted_segments,
    const std::array<std::uint8_t, 32>& key_bytes
) {
    ensure_key_length(key_bytes);
    std::vector<std::uint8_t> plain;
    for (const auto& segment : encrypted_segments) {
        std::vector<std::uint8_t> decrypted = aes_gcm_decrypt_segment(segment, key_bytes);
        plain.insert(plain.end(), decrypted.begin(), decrypted.end());
    }
    return plain;
}

std::vector<std::uint8_t> rsa_encrypt(
    const std::vector<std::uint8_t>& plain_bytes,
    const std::string& public_key
) {
    auto key = parse_public_key(public_key);
    return rsa_process(plain_bytes, key.get(), true);
}

std::vector<std::uint8_t> rsa_decrypt(
    const std::vector<std::uint8_t>& cipher_bytes,
    const std::string& private_key
) {
    auto key = parse_private_key(private_key);
    return rsa_process(cipher_bytes, key.get(), false);
}

std::string sha256_hex(const std::vector<std::uint8_t>& data) {
    unsigned char digest[SHA256_DIGEST_LENGTH];
    if (SHA256(data.data(), data.size(), digest) == nullptr) {
        throw CryptoException("SHA256_FAILED", "Unable to compute SHA256");
    }
    return to_hex(digest, SHA256_DIGEST_LENGTH);
}

}
