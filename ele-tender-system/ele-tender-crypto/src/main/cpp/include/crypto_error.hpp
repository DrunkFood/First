#pragma once

#include <stdexcept>
#include <string>

namespace eletender::crypto {

class CryptoException : public std::runtime_error {
public:
    CryptoException(std::string code, const std::string& message)
        : std::runtime_error(message), code_(std::move(code)) {
    }

    const std::string& code() const noexcept {
        return code_;
    }

private:
    std::string code_;
};

}
