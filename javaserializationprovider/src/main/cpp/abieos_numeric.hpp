// copyright defined in abieos/LICENSE.txt

#pragma once

#include <algorithm>
#include <array>
#include <stdint.h>
#include <string>
#include <string_view>

#include "abieos_ripemd160.hpp"

#define ABIEOS_NODISCARD [[nodiscard]]

namespace abieos {

template <typename State>
ABIEOS_NODISCARD bool set_error(State& state, std::string error) {
    state.error = std::move(error);
    return false;
}

ABIEOS_NODISCARD inline bool set_error(std::string& state, std::string error) {
    state = std::move(error);
    return false;
}

inline constexpr char base58_chars[] = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

inline constexpr auto create_base58_map() {
    std::array<int8_t, 256> base58_map{{0}};
    for (unsigned i = 0; i < base58_map.size(); ++i)
        base58_map[i] = -1;
    for (unsigned i = 0; i < sizeof(base58_chars); ++i)
        base58_map[base58_chars[i]] = i;
    return base58_map;
}

inline constexpr auto base58_map = create_base58_map();

template <auto size>
bool is_negative(const std::array<uint8_t, size>& a) {
    return a[size - 1] & 0x80;
}

template <auto size>
void negate(std::array<uint8_t, size>& a) {
    uint8_t carry = 1;
    for (auto& byte : a) {
        int x = uint8_t(~byte) + carry;
        byte = x;
        carry = x >> 8;
    }
}

template <auto size>
ABIEOS_NODISCARD inline bool decimal_to_binary(std::array<uint8_t, size>& result, std::string& error,
                                               std::string_view s) {
    memset(result.begin(), 0, result.size());
    for (auto& src_digit : s) {
        if (src_digit < '0' || src_digit > '9')
            return set_error(error, "invalid number");
        uint8_t carry = src_digit - '0';
        for (auto& result_byte : result) {
            int x = result_byte * 10 + carry;
            result_byte = x;
            carry = x >> 8;
        }
        if (carry)
            return set_error(error, "number is out of range");
    }
    return true;
}

template <typename T>
ABIEOS_NODISCARD inline auto decimal_to_binary(T& result, std::string& error, std::string_view s)
    -> std::enable_if_t<std::is_unsigned_v<T>, bool> {
    result = 0;
    if (s.empty())
        return set_error(error, "expected number");
    if (s[0] == '-')
        return set_error(error, "expected non-negative number");
    for (auto& src_digit : s) {
        if (src_digit < '0' || src_digit > '9')
            return set_error(error, "invalid number");
        T x = result * 10 + src_digit - '0';
        if (x < result)
            return set_error(error, "number is out of range");
        result = x;
    }
    return true;
}

template <typename T>
ABIEOS_NODISCARD inline auto decimal_to_binary(T& result, std::string& error, std::string_view s)
    -> std::enable_if_t<std::is_signed_v<T>, bool> {
    bool neg = false;
    if (!s.empty() && s[0] == '-') {
        neg = true;
        s.remove_prefix(1);
        if (s.empty() || s[0] == '-')
            return set_error(error, "invalid number");
    }
    std::make_unsigned_t<T> u;
    if (!decimal_to_binary(u, error, s))
        return false;
    if (neg) {
        result = -u;
        if (result > 0)
            return set_error(error, "number is out of range");
    } else {
        result = u;
        if (result < 0)
            return set_error(error, "number is out of range");
    }
    return true;
}

template <auto size>
std::string binary_to_decimal(const std::array<uint8_t, size>& bin) {
    std::string result("0");
    for (auto byte_it = bin.rbegin(); byte_it != bin.rend(); ++byte_it) {
        int carry = *byte_it;
        for (auto& result_digit : result) {
            int x = ((result_digit - '0') << 8) + carry;
            result_digit = '0' + x % 10;
            carry = x / 10;
        }
        while (carry) {
            result.push_back('0' + carry % 10);
            carry = carry / 10;
        }
    }
    std::reverse(result.begin(), result.end());
    return result;
}

template <auto size>
ABIEOS_NODISCARD inline bool base58_to_binary(std::array<uint8_t, size>& result, std::string& error,
                                              std::string_view s) {
    memset(result.begin(), 0, result.size());
    for (auto& src_digit : s) {
        int carry = base58_map[src_digit];
        if (carry < 0)
            return set_error(error, "invalid base-58 value");
        for (auto& result_byte : result) {
            int x = result_byte * 58 + carry;
            result_byte = x;
            carry = x >> 8;
        }
        if (carry)
            return set_error(error, "base-58 value is out of range");
    }
    std::reverse(result.begin(), result.end());
    return true;
}

template <auto size>
std::string binary_to_base58(const std::array<uint8_t, size>& bin) {
    std::string result("");
    for (auto byte : bin) {
        int carry = byte;
        for (auto& result_digit : result) {
            int x = (base58_map[result_digit] << 8) + carry;
            result_digit = base58_chars[x % 58];
            carry = x / 58;
        }
        while (carry) {
            result.push_back(base58_chars[carry % 58]);
            carry = carry / 58;
        }
    }
    for (auto byte : bin)
        if (byte)
            break;
        else
            result.push_back('1');
    std::reverse(result.begin(), result.end());
    return result;
}

enum class key_type : uint8_t {
    k1 = 0,
    r1 = 1,
};

struct public_key {
    key_type type{};
    std::array<uint8_t, 33> data{};
};

struct private_key {
    key_type type{};
    std::array<uint8_t, 32> data{};
};

struct signature {
    key_type type{};
    std::array<uint8_t, 65> data{};
};

ABIEOS_NODISCARD inline bool digest_message_ripemd160(std::array<unsigned char, 20>& digest, std::string& error,
                                                      const unsigned char* message, size_t message_len) {
    abieos_ripemd160::ripemd160_state self;
    abieos_ripemd160::ripemd160_init(&self);
    abieos_ripemd160::ripemd160_update(&self, message, message_len);
    if (!abieos_ripemd160::ripemd160_digest(&self, digest.data()))
        return set_error(error, "ripemd failed");
    return true;
}

template <size_t size, int suffix_size>
ABIEOS_NODISCARD inline bool digest_suffix_ripemd160(std::array<unsigned char, 20>& digest, std::string& error,
                                                     const std::array<uint8_t, size>& data,
                                                     const char (&suffix)[suffix_size]) {
    abieos_ripemd160::ripemd160_state self;
    abieos_ripemd160::ripemd160_init(&self);
    abieos_ripemd160::ripemd160_update(&self, data.data(), data.size());
    abieos_ripemd160::ripemd160_update(&self, (uint8_t*)suffix, suffix_size - 1);
    if (!abieos_ripemd160::ripemd160_digest(&self, digest.data()))
        return set_error(error, "ripemd failed");
    return true;
}

template <typename Key, int suffix_size>
ABIEOS_NODISCARD bool string_to_key(Key& result, std::string& error, std::string_view s, key_type type,
                                    const char (&suffix)[suffix_size]) {
    static constexpr auto size = std::tuple_size_v<decltype(Key::data)>;
    std::array<uint8_t, size + 4> whole;
    if (!base58_to_binary(whole, error, s))
        return false;
    result.type = type;
    memcpy(result.data.data(), whole.data(), result.data.size());
    std::array<unsigned char, 20> ripe_digest;
    if (!digest_suffix_ripemd160(ripe_digest, error, result.data, suffix))
        return false;
    if (memcmp(ripe_digest.data(), whole.data() + result.data.size(), 4))
        return set_error(error, "checksum doesn't match");
    return true;
}

template <typename Key, int suffix_size>
ABIEOS_NODISCARD bool key_to_string(std::string& dest, std::string& error, const Key& key,
                                    const char (&suffix)[suffix_size], const char* prefix) {
    static constexpr auto size = std::tuple_size_v<decltype(Key::data)>;
    std::array<unsigned char, 20> ripe_digest;
    if (!digest_suffix_ripemd160(ripe_digest, error, key.data, suffix))
        return false;
    std::array<uint8_t, size + 4> whole;
    memcpy(whole.data(), key.data.data(), size);
    memcpy(whole.data() + size, ripe_digest.data(), 4);
    dest = prefix + binary_to_base58(whole);
    return true;
}

ABIEOS_NODISCARD inline bool string_to_public_key(public_key& dest, std::string& error, std::string_view s) {
    if (s.size() >= 3 && s.substr(0, 3) == "EOS") {
        std::array<uint8_t, 37> whole;
        if (!base58_to_binary(whole, error, s.substr(3)))
            return false;
        public_key key{key_type::k1};
        static_assert(whole.size() == key.data.size() + 4);
        memcpy(key.data.data(), whole.data(), key.data.size());
        std::array<unsigned char, 20> ripe_digest;
        if (!digest_message_ripemd160(ripe_digest, error, key.data.data(), key.data.size()))
            return false;
        if (memcmp(ripe_digest.data(), whole.data() + key.data.size(), 4))
            return set_error(error, "Key checksum doesn't match");
        dest = key;
        return true;
    } else if (s.size() >= 7 && s.substr(0, 7) == "PUB_K1_") {
        return string_to_key(dest, error, s.substr(7), key_type::k1, "K1");
    } else if (s.size() >= 7 && s.substr(0, 7) == "PUB_R1_") {
        return string_to_key(dest, error, s.substr(7), key_type::r1, "R1");
    } else {
        return set_error(error, "unrecognized public key format");
    }
}

ABIEOS_NODISCARD inline bool public_key_to_string(std::string& dest, std::string& error, const public_key& key) {
    if (key.type == key_type::k1) {
        return key_to_string(dest, error, key, "K1", "PUB_K1_");
    } else if (key.type == key_type::r1) {
        return key_to_string(dest, error, key, "R1", "PUB_R1_");
    } else {
        return set_error(error, "unrecognized public key format");
    }
}

ABIEOS_NODISCARD inline bool string_to_private_key(private_key& dest, std::string& error, std::string_view s) {
    if (s.size() >= 7 && s.substr(0, 7) == "PVT_R1_")
        return string_to_key(dest, error, s.substr(7), key_type::r1, "R1");
    else
        return set_error(error, "unrecognized private key format");
}

ABIEOS_NODISCARD inline bool private_key_to_string(std::string& dest, std::string& error,
                                                   const private_key& private_key) {
    if (private_key.type == key_type::r1)
        return key_to_string(dest, error, private_key, "R1", "PVT_R1_");
    else
        return set_error(error, "unrecognized private key format");
}

ABIEOS_NODISCARD inline bool string_to_signature(signature& dest, std::string& error, std::string_view s) {
    if (s.size() >= 7 && s.substr(0, 7) == "SIG_K1_")
        return string_to_key(dest, error, s.substr(7), key_type::k1, "K1");
    else if (s.size() >= 7 && s.substr(0, 7) == "SIG_R1_")
        return string_to_key(dest, error, s.substr(7), key_type::r1, "R1");
    else
        return set_error(error, "unrecognized signature format");
}

ABIEOS_NODISCARD inline bool signature_to_string(std::string& dest, std::string& error, const signature& signature) {
    if (signature.type == key_type::k1)
        return key_to_string(dest, error, signature, "K1", "SIG_K1_");
    else if (signature.type == key_type::r1)
        return key_to_string(dest, error, signature, "R1", "SIG_R1_");
    else
        return set_error(error, "unrecognized signature format");
}

} // namespace abieos
