#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace eletender::crypto {

struct ParsedEncryptedFile {
    std::vector<std::vector<std::uint8_t>> encrypted_segments;
    std::vector<std::uint8_t> encrypted_project_info_rsa;
    std::string format_version;
    std::vector<std::uint8_t> raw_file_bytes;
};

ParsedEncryptedFile unpack_file(const std::vector<std::uint8_t>& file_bytes);

std::vector<std::uint8_t> pack_file(
    const std::vector<std::vector<std::uint8_t>>& encrypted_segments,
    const std::vector<std::uint8_t>& encrypted_project_info_rsa,
    const std::string& format_version
);

std::vector<std::vector<std::uint8_t>> encrypt_segments(
    const std::vector<std::uint8_t>& plain_bytes,
    const std::array<std::uint8_t, 32>& key_bytes,
    std::size_t segment_size
);

std::vector<std::uint8_t> decrypt_segments(
    const std::vector<std::vector<std::uint8_t>>& encrypted_segments,
    const std::array<std::uint8_t, 32>& key_bytes
);

std::vector<std::uint8_t> rsa_encrypt(
    const std::vector<std::uint8_t>& plain_bytes,
    const std::string& public_key
);

std::vector<std::uint8_t> rsa_decrypt(
    const std::vector<std::uint8_t>& cipher_bytes,
    const std::string& private_key
);

std::string sha256_hex(const std::vector<std::uint8_t>& data);

}
