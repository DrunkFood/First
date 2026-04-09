#include "crypto_core.hpp"

#include <array>
#include <cassert>
#include <cstdint>
#include <iostream>
#include <string>
#include <vector>

using namespace eletender::crypto;

namespace {

const std::string kPrivateKey = R"(-----BEGIN PRIVATE KEY-----
MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCcXlbzrBT7yc+7
a2Vrtf/HCs0DO5vGSUFHW7M8CW1hDcYcWBFPJaPgGW/NgdzU1brm7nad0IvB7tvE
72/1VFxmhUTu+rWirzyZ7+PRe/jJrnN9RwrhyeK3FdAkIEt/f1NF8CIakYkWkBdk
8fZ5XCaI2JSwd5ZugUr7f0r5tii0IkA/YFtia6Ih4/0oJLwd5JarK7G2+GASU/Pr
4PKdowRuTcm2hSUkKUVk0TUOQzDrxb4aq6Aozbuj77qUXtFwnlqDxSoPE8rblx9k
xiMJokw5Qs1GP3X6LkmrEiTeDyUAkhtpySkpJdBPYRIrPi7TKmJ3ls7WSit7AWZi
CbOyaW4HAgMBAAECggEAEklQEYgGyNlK2Kf7FjO/A+0bQ5ewJGoDCdAgr6ffPucy
9glF4O54cdKCX3332tCx3k7LLV0q8uo25kV6tNiBmSBYYDrj4fDB/V3vU7C+oVYB
kzLV2XOCzBbEyWzDbNdxisTF4Dsz1SEoXeQ+sZDuwA3VTvvKlQVmxO7tJYhsIys4
DMSbY7gc+sdjGUrHdEZbl3nfWUkn0rqrQE2NmdsISLkcoLpL6s1lSZd5TrW9Bwe+
+6rhY9r4fT+BwIpL7Yx8cFz127uR2quCTlwHI/LJmoKcw10P9KpVI+n27u+yhGUd
8Z0gTlFTpbnmaOyT1HOqE7fitG/VJbwhIJ4QXM51IQKBgQDSr8RAvJz+wqrwwZC/
js3xO/bTKbep4Q4Xqa3McETDBtM6b8pRA/sb+QrSxew3erBqotZkG5bHx1J6GvRc
vyNTZ6ckaf3E1y7+jIQSHA33KHHFcjhtL0wCsHshYo7bUaKpR+mrpIP0GMFl281x
RhCiu7JwQVHxvz362bF7jj07IQKBgQC9/90fLIYSIbYqgFAfLEY7a9+BNxYK1PjE
zj1Ofbwn0K60mmCCgnvRo/RLBHLAYyrv06ByME7t7l+L/r00jK4HG/tBDL8f8i3l
3zvJWRHx8LcifsB+tK8fareyetcAYQ8nzJVvS9PQee7hwQ7yb9+0w+d+wRCZylQf
eosJLTPsJwKBgHUSiGo0pMSH5bcMyGM5dkSjPn+OQembDlqlxdbBV+RLaZqiPfkQ
zjt4AsSmiKE3gspum9Va40k2ACWrzreu2nFhOqZoY0Q7EnkOGeF6R2RczAOcebBq
RMGF0ZX2j01dqpaISFdBfrVoACeaoSlddqcGx5vLID7GNymqSA5RNsMhAoGARE8b
JrwRL6+jGMCtDagTUAXGg2RUrmxHTCqB7BhUb1Qdm5ztGb7j2UlC6T2eLAD7TOIf
Cy7HEc/j1ictyxjQ8Ilk2cxFYqzlR4HsssUtKHjMvsAnYOaBF6B8jtSPO/mpQzvQ
dgUjEA7mjY+lWhBSs2DDd9TdrQ0LFY4vMotn4X0CgYAURn1Kbhu21I+A/gv4VGaG
BKut8g3Cp9AIQJcNvl4wVIlbKlcxxpBGqSJS5xdjIwzmpGjhAEosCutuFTr+idGK
jKepjvuoocWKreeskTupT/lbLT3uMibIoyLRgG+oH4ZLaImouhpt9hfKCnLQFRlr
VF0ppiv5CPXM/YJU3uZJ4Q==
-----END PRIVATE KEY-----)";

const std::string kPublicKey = R"(-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnF5W86wU+8nPu2tla7X/
xwrNAzubxklBR1uzPAltYQ3GHFgRTyWj4BlvzYHc1NW65u52ndCLwe7bxO9v9VRc
ZoVE7vq1oq88me/j0Xv4ya5zfUcK4cnitxXQJCBLf39TRfAiGpGJFpAXZPH2eVwm
iNiUsHeWboFK+39K+bYotCJAP2BbYmuiIeP9KCS8HeSWqyuxtvhgElPz6+DynaME
bk3JtoUlJClFZNE1DkMw68W+GqugKM27o++6lF7RcJ5ag8UqDxPK25cfZMYjCaJM
OULNRj91+i5JqxIk3g8lAJIbackpKSXQT2ESKz4u0ypid5bO1korewFmYgmzsmlu
BwIDAQAB
-----END PUBLIC KEY-----)";

std::array<std::uint8_t, 32> key() {
    return {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
            0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f};
}

std::vector<std::uint8_t> bytes(const std::string& text) {
    return std::vector<std::uint8_t>(text.begin(), text.end());
}

void should_pack_and_unpack_round_trip() {
    std::vector<std::vector<std::uint8_t>> segments = {bytes("segment-1"), bytes("segment-2")};
    std::vector<std::uint8_t> project_info = bytes("{\"projectId\":\"P1\"}");
    std::vector<std::uint8_t> packed = pack_file(segments, project_info, "2.0.0");
    ParsedEncryptedFile parsed = unpack_file(packed);

    assert(parsed.format_version == "2.0.0");
    assert(parsed.encrypted_segments.size() == 2);
    assert(parsed.encrypted_segments[0] == segments[0]);
    assert(parsed.encrypted_segments[1] == segments[1]);
    assert(parsed.encrypted_project_info_rsa == project_info);
}

void should_encrypt_and_decrypt_segments() {
    std::string text = "hello crypto native world";
    auto encrypted = encrypt_segments(bytes(text), key(), 8);
    auto plain = decrypt_segments(encrypted, key());
    assert(std::string(plain.begin(), plain.end()) == text);
}

void should_encrypt_and_decrypt_rsa_payload() {
    std::string text(512, 'A');
    auto encrypted = rsa_encrypt(bytes(text), kPublicKey);
    auto plain = rsa_decrypt(encrypted, kPrivateKey);
    assert(std::string(plain.begin(), plain.end()) == text);
}

void should_hash_sha256() {
    assert(sha256_hex(bytes("abc")) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
}

}

int main() {
    should_pack_and_unpack_round_trip();
    should_encrypt_and_decrypt_segments();
    should_encrypt_and_decrypt_rsa_payload();
    should_hash_sha256();
    std::cout << "crypto_core_test passed" << std::endl;
    return 0;
}
