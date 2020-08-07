// copyright defined in abieos/LICENSE.txt

#pragma once

#include "abieos.hpp"

#include <exception>

namespace abieos {

struct error : std::exception {
    std::string message;

    error(std::string message) : message(std::move(message)) {}
    virtual const char* what() const noexcept { return message.c_str(); }
};

inline std::string public_key_to_string(const public_key& key) {
    std::string dest, error;
    if (!public_key_to_string(dest, error, key))
        throw abieos::error(error);
    return dest;
}

inline time_point_sec string_to_time_point_sec(const char* s) {
    time_point_sec result;
    std::string error;
    if (!string_to_time_point_sec(result, error, s))
        throw abieos::error(error);
    return result;
}

inline time_point string_to_time_point(const std::string& s) {
    time_point result;
    std::string error;
    if (!string_to_time_point(result, error, s))
        throw abieos::error(error);
    return result;
}

inline uint64_t string_to_symbol_code(const char* s) {
    uint64_t result;
    std::string error;
    if (!string_to_symbol_code(result, error, s))
        throw abieos::error(error);
    return result;
}

inline uint64_t string_to_symbol(const char* s) {
    uint64_t result;
    std::string error;
    if (!string_to_symbol(result, error, s))
        throw abieos::error(error);
    return result;
}

inline asset string_to_asset(const char* s) {
    asset result;
    std::string error;
    if (!string_to_asset(result, error, s))
        throw abieos::error(error);
    return result;
}

template <typename T>
T read_raw(input_buffer& bin) {
    std::string error;
    T x;
    if (!read_raw(bin, error, x))
        throw abieos::error(error);
    return x;
}

inline uint32_t read_varuint32(input_buffer& bin) {
    std::string error;
    uint32_t x;
    if (!read_varuint32(bin, error, x))
        throw abieos::error(error);
    return x;
}

inline std::string read_string(input_buffer& bin) {
    std::string error;
    std::string x;
    if (!read_string(bin, error, x))
        throw abieos::error(error);
    return x;
}

template <typename T>
void bin_to_native(T& obj, input_buffer& bin) {
    std::string error;
    if (!bin_to_native(obj, error, bin))
        throw abieos::error(error);
}

template <typename T>
T bin_to_native(input_buffer& bin) {
    T obj;
    bin_to_native(obj, bin);
    return obj;
}

template <typename T>
void json_to_native(T& obj, std::string_view json) {
    std::string error;
    if (!json_to_native(obj, error, json))
        throw abieos::error(error);
}

inline void check_abi_version(const std::string& s) {
    std::string error;
    if (!check_abi_version(s, error))
        throw abieos::error(error);
}

inline contract create_contract(const abi_def& abi) {
    std::string error;
    contract c;
    if (!fill_contract(c, error, abi))
        throw abieos::error(error);
    return c;
}

inline void json_to_bin(std::vector<char>& bin, const abi_type* type, const jvalue& value) {
    std::string error;
    if (!json_to_bin(bin, error, type, value))
        throw abieos::error(error);
}

} // namespace abieos
