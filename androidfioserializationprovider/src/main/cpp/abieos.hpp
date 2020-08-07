// copyright defined in abieos/LICENSE.txt

#pragma once

#include <ctime>
#include <date/date.h>
#include <map>
#include <optional>
#include <variant>
#include <vector>

#include "abieos_numeric.hpp"

#include "rapidjson/reader.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

namespace abieos {

inline constexpr bool trace_json_to_jvalue_event = false;
inline constexpr bool trace_json_to_jvalue = false;
inline constexpr bool trace_json_to_native = false;
inline constexpr bool trace_json_to_native_event = false;
inline constexpr bool trace_bin_to_native = false;
inline constexpr bool trace_jvalue_to_bin = false;
inline constexpr bool trace_json_to_bin = false;
inline constexpr bool trace_json_to_bin_event = false;
inline constexpr bool trace_bin_to_json = false;

inline constexpr size_t max_stack_size = 128;

template <typename T>
inline constexpr bool is_vector_v = false;

template <typename T>
inline constexpr bool is_vector_v<std::vector<T>> = true;

template <typename T>
inline constexpr bool is_pair_v = false;

template <typename First, typename Second>
inline constexpr bool is_pair_v<std::pair<First, Second>> = true;

template <typename T>
inline constexpr bool is_optional_v = false;

template <typename T>
inline constexpr bool is_optional_v<std::optional<T>> = true;

template <typename T>
inline constexpr bool is_string_v = false;

template <>
inline constexpr bool is_string_v<std::string> = true;

template <auto P>
struct member_ptr;

template <class C, typename M>
const C* class_from_void(M C::*, const void* v) {
    return reinterpret_cast<const C*>(v);
}

template <class C, typename M>
C* class_from_void(M C::*, void* v) {
    return reinterpret_cast<C*>(v);
}

template <auto P>
auto& member_from_void(const member_ptr<P>&, const void* p) {
    return class_from_void(P, p)->*P;
}

template <auto P>
auto& member_from_void(const member_ptr<P>&, void* p) {
    return class_from_void(P, p)->*P;
}

template <auto P>
struct member_ptr {
    using member_type = std::decay_t<decltype(member_from_void(std::declval<member_ptr<P>>(), std::declval<void*>()))>;
};

// Pseudo objects never exist, except in serialized form
struct pseudo_optional;
struct pseudo_extension;
struct pseudo_object;
struct pseudo_array;
struct pseudo_variant;

template <typename T>
struct might_not_exist {
    T value{};
};

template <typename SrcIt, typename DestIt>
void hex(SrcIt begin, SrcIt end, DestIt dest) {
    auto nibble = [&dest](uint8_t i) {
        if (i <= 9)
            *dest++ = '0' + i;
        else
            *dest++ = 'A' + i - 10;
    };
    while (begin != end) {
        nibble(((uint8_t)*begin) >> 4);
        nibble(((uint8_t)*begin) & 0xf);
        ++begin;
    }
}

template <typename SrcIt, typename DestIt>
ABIEOS_NODISCARD bool unhex(std::string& error, SrcIt begin, SrcIt end, DestIt dest) {
    auto get_digit = [&](uint8_t& nibble) {
        if (*begin >= '0' && *begin <= '9')
            nibble = *begin++ - '0';
        else if (*begin >= 'a' && *begin <= 'f')
            nibble = *begin++ - 'a' + 10;
        else if (*begin >= 'A' && *begin <= 'F')
            nibble = *begin++ - 'A' + 10;
        else
            return set_error(error, "expected hex string");
        return true;
    };
    while (begin != end) {
        uint8_t h, l;
        if (!get_digit(h) || !get_digit(l))
            return false;
        *dest++ = (h << 4) | l;
    }
    return true;
}

template <typename T>
void push_raw(std::vector<char>& bin, const T& obj) {
    static_assert(std::is_trivially_copyable_v<T>);
    bin.insert(bin.end(), reinterpret_cast<const char*>(&obj), reinterpret_cast<const char*>(&obj + 1));
}

struct input_buffer {
    const char* pos = nullptr;
    const char* end = nullptr;
};

ABIEOS_NODISCARD inline bool read_raw(input_buffer& bin, std::string& error, void* dest, ptrdiff_t size) {
    if (bin.end - bin.pos < size)
        return set_error(error, "read past end");
    if (size)
        memcpy(dest, bin.pos, size);
    bin.pos += size;
    return true;
}

template <typename T>
ABIEOS_NODISCARD bool read_raw(input_buffer& bin, std::string& error, T& dest) {
    static_assert(std::is_trivially_copyable_v<T>);
    return read_raw(bin, error, &dest, sizeof(dest));
}

ABIEOS_NODISCARD inline bool read_raw(input_buffer& bin, std::string& error, bool& dest) {
    char tmp;
    if (!read_raw(bin, error, &tmp, sizeof(tmp)))
        return false;
    dest = tmp;
    return true;
}

ABIEOS_NODISCARD bool read_varuint32(input_buffer& bin, std::string& error, uint32_t& dest);

ABIEOS_NODISCARD inline bool read_string(input_buffer& bin, std::string& error, std::string& dest) {
    uint32_t size;
    if (!read_varuint32(bin, error, size))
        return false;
    if (size > bin.end - bin.pos)
        return set_error(error, "invalid string size");
    dest.resize(size);
    return read_raw(bin, error, dest.data(), size);
}

///////////////////////////////////////////////////////////////////////////////
// stream events
///////////////////////////////////////////////////////////////////////////////

enum class event_type {
    received_null,         // 0
    received_bool,         // 1
    received_string,       // 2
    received_start_object, // 3
    received_key,          // 4
    received_end_object,   // 5
    received_start_array,  // 6
    received_end_array,    // 7
};

struct event_data {
    bool value_bool = 0;
    std::string value_string{};
    std::string key{};
};

ABIEOS_NODISCARD bool receive_event(struct json_to_native_state&, event_type, bool start);
ABIEOS_NODISCARD bool receive_event(struct json_to_bin_state&, event_type, bool start);

template <typename Derived>
struct json_reader_handler : public rapidjson::BaseReaderHandler<rapidjson::UTF8<>, Derived> {
    event_data received_data{};
    bool started = false;

    Derived& get_derived() { return *static_cast<Derived*>(this); }

    bool get_start() {
        if (started)
            return false;
        started = true;
        return true;
    }

    bool get_bool() const { return received_data.value_bool; }

    const std::string& get_string() const { return received_data.value_string; }

    bool Null() { return receive_event(get_derived(), event_type::received_null, get_start()); }
    bool Bool(bool v) {
        received_data.value_bool = v;
        return receive_event(get_derived(), event_type::received_bool, get_start());
    }
    bool RawNumber(const char* v, rapidjson::SizeType length, bool copy) { return String(v, length, copy); }
    bool Int(int v) { return false; }
    bool Uint(unsigned v) { return false; }
    bool Int64(int64_t v) { return false; }
    bool Uint64(uint64_t v) { return false; }
    bool Double(double v) { return false; }
    bool String(const char* v, rapidjson::SizeType length, bool) {
        received_data.value_string = {v, length};
        return receive_event(get_derived(), event_type::received_string, get_start());
    }
    bool StartObject() { return receive_event(get_derived(), event_type::received_start_object, get_start()); }
    bool Key(const char* v, rapidjson::SizeType length, bool) {
        received_data.key = {v, length};
        return receive_event(get_derived(), event_type::received_key, get_start());
    }
    bool EndObject(rapidjson::SizeType) {
        return receive_event(get_derived(), event_type::received_end_object, get_start());
    }
    bool StartArray() { return receive_event(get_derived(), event_type::received_start_array, get_start()); }
    bool EndArray(rapidjson::SizeType) {
        return receive_event(get_derived(), event_type::received_end_array, get_start());
    }
};

///////////////////////////////////////////////////////////////////////////////
// json model
///////////////////////////////////////////////////////////////////////////////

struct jvalue;
using jarray = std::vector<jvalue>;
using jobject = std::map<std::string, jvalue>;

struct jvalue {
    std::variant<std::nullptr_t, bool, std::string, jobject, jarray> value;
};

inline event_type get_event_type(std::nullptr_t) { return event_type::received_null; }
inline event_type get_event_type(bool b) { return event_type::received_bool; }
inline event_type get_event_type(const std::string& s) { return event_type::received_string; }
inline event_type get_event_type(const jobject&) { return event_type::received_start_object; }
inline event_type get_event_type(const jarray&) { return event_type::received_start_array; }

inline event_type get_event_type(const jvalue& value) {
    return std::visit([](const auto& x) { return get_event_type(x); }, value.value);
}

///////////////////////////////////////////////////////////////////////////////
// state and serializers
///////////////////////////////////////////////////////////////////////////////

struct size_insertion {
    size_t position = 0;
    uint32_t size = 0;
};

struct json_to_jvalue_stack_entry {
    jvalue* value = nullptr;
    std::string key = "";
};

struct native_serializer;

struct native_stack_entry {
    void* obj = nullptr;
    const native_serializer* ser = nullptr;
    int position = 0;
    int array_size = 0;
};

struct jvalue_to_bin_stack_entry {
    const struct abi_type* type = nullptr;
    bool allow_extensions = false;
    const jvalue* value = nullptr;
    int position = -1;
};

struct json_to_bin_stack_entry {
    const struct abi_type* type = nullptr;
    bool allow_extensions = false;
    int position = -1;
    size_t size_insertion_index = 0;
    size_t variant_type_index = 0;
};

struct bin_to_json_stack_entry {
    const struct abi_type* type = nullptr;
    bool allow_extensions = false;
    int position = -1;
    uint32_t array_size = 0;
};

struct json_to_jvalue_state : json_reader_handler<json_to_jvalue_state> {
    std::string& error;
    std::vector<json_to_jvalue_stack_entry> stack;
};

struct json_to_native_state : json_reader_handler<json_to_native_state> {
    std::string& error;
    std::vector<native_stack_entry> stack;
};

struct bin_to_native_state {
    std::string& error;
    input_buffer bin{};
    std::vector<native_stack_entry> stack{};
};

struct jvalue_to_bin_state {
    std::string& error;
    std::vector<char>& bin;
    const jvalue* received_value = nullptr;
    std::vector<jvalue_to_bin_stack_entry> stack{};
    bool skipped_extension = false;

    bool get_bool() const { return std::get<bool>(received_value->value); }

    const std::string& get_string() const { return std::get<std::string>(received_value->value); }
};

struct json_to_bin_state : json_reader_handler<json_to_bin_state> {
    std::string& error;
    std::vector<char> bin;
    std::vector<size_insertion> size_insertions{};
    std::vector<json_to_bin_stack_entry> stack{};
    bool skipped_extension = false;
};

struct bin_to_json_state : json_reader_handler<bin_to_json_state> {
    std::string& error;
    input_buffer& bin;
    rapidjson::Writer<rapidjson::StringBuffer>& writer;
    std::vector<bin_to_json_stack_entry> stack{};
    bool skipped_extension = false;

    bin_to_json_state(input_buffer& bin, std::string& error, rapidjson::Writer<rapidjson::StringBuffer>& writer)
        : error{error}, bin{bin}, writer{writer} {}
};

struct native_serializer {
    ABIEOS_NODISCARD virtual bool bin_to_native(void*, bin_to_native_state&, bool) const = 0;
    ABIEOS_NODISCARD virtual bool json_to_native(void*, json_to_native_state&, event_type, bool) const = 0;
};

struct native_field_serializer_methods {
    ABIEOS_NODISCARD virtual bool bin_to_native(void*, bin_to_native_state&, bool) const = 0;
    ABIEOS_NODISCARD virtual bool json_to_native(void*, json_to_native_state&, event_type, bool) const = 0;
};

struct native_field_serializer {
    std::string_view name = "<unknown>";
    const native_field_serializer_methods* methods = nullptr;
};

struct abi_serializer {
    ABIEOS_NODISCARD virtual bool json_to_bin(jvalue_to_bin_state& state, bool allow_extensions, const abi_type* type,
                                              event_type event, bool start) const = 0;
    ABIEOS_NODISCARD virtual bool json_to_bin(json_to_bin_state& state, bool allow_extensions, const abi_type* type,
                                              event_type event, bool start) const = 0;
    ABIEOS_NODISCARD virtual bool bin_to_json(bin_to_json_state& state, bool allow_extensions, const abi_type* type,
                                              bool start) const = 0;
};

///////////////////////////////////////////////////////////////////////////////
// serializer function prototypes
///////////////////////////////////////////////////////////////////////////////

template <typename T>
ABIEOS_NODISCARD auto bin_to_native(T& obj, bin_to_native_state& state, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool>;
template <typename T>
ABIEOS_NODISCARD auto bin_to_native(T& obj, bin_to_native_state& state, bool start)
    -> std::enable_if_t<std::is_class_v<T>, bool>;
template <typename T>
ABIEOS_NODISCARD bool bin_to_native(std::vector<T>& v, bin_to_native_state& state, bool start);
template <typename First, typename Second>
ABIEOS_NODISCARD bool bin_to_native(std::pair<First, Second>& obj, bin_to_native_state& state, bool start);
ABIEOS_NODISCARD bool bin_to_native(std::string& obj, bin_to_native_state& state, bool);
template <typename T>
ABIEOS_NODISCARD bool bin_to_native(std::optional<T>& v, bin_to_native_state& state, bool start);
template <typename... Ts>
ABIEOS_NODISCARD bool bin_to_native(std::variant<Ts...>& v, bin_to_native_state& state, bool start);
template <typename T>
ABIEOS_NODISCARD bool bin_to_native(might_not_exist<T>& obj, bin_to_native_state& state, bool);

template <typename T>
void native_to_bin(std::vector<char>& bin, const T& obj);
void native_to_bin(std::vector<char>& bin, const std::string& obj);
template <typename T>
void native_to_bin(std::vector<char>& bin, const std::vector<T>& obj);

template <typename T>
ABIEOS_NODISCARD auto json_to_native(T& obj, json_to_native_state& state, event_type event, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool>;
template <typename T>
ABIEOS_NODISCARD auto json_to_native(T& obj, json_to_native_state& state, event_type event, bool start)
    -> std::enable_if_t<std::is_class_v<T>, bool>;
template <typename T>
ABIEOS_NODISCARD bool json_to_native(std::vector<T>& obj, json_to_native_state& state, event_type event, bool start);
template <typename First, typename Second>
ABIEOS_NODISCARD bool json_to_native(std::pair<First, Second>& obj, json_to_native_state& state, event_type event,
                                     bool start);
ABIEOS_NODISCARD bool json_to_native(std::string& obj, json_to_native_state& state, event_type event, bool start);
template <typename T>
ABIEOS_NODISCARD bool json_to_native(std::optional<T>& obj, json_to_native_state& state, event_type event, bool start);
template <typename... Ts>
ABIEOS_NODISCARD bool json_to_native(std::variant<Ts...>& obj, json_to_native_state& state, event_type event,
                                     bool start);
template <typename T>
ABIEOS_NODISCARD bool json_to_native(might_not_exist<T>& obj, json_to_native_state& state, event_type event,
                                     bool start);

ABIEOS_NODISCARD bool json_to_bin(pseudo_optional*, jvalue_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool);
ABIEOS_NODISCARD bool json_to_bin(pseudo_extension*, jvalue_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool);
ABIEOS_NODISCARD bool json_to_bin(pseudo_object*, jvalue_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_array*, jvalue_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_variant*, jvalue_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool start);
template <typename T>
ABIEOS_NODISCARD auto json_to_bin(T*, jvalue_to_bin_state& state, bool allow_extensions, const abi_type*,
                                  event_type event, bool start) -> std::enable_if_t<std::is_arithmetic_v<T>, bool>;
ABIEOS_NODISCARD bool json_to_bin(std::string*, jvalue_to_bin_state& state, bool allow_extensions, const abi_type*,
                                  event_type event, bool start);

template <typename T>
ABIEOS_NODISCARD auto json_to_bin(T*, json_to_bin_state& state, bool allow_extensions, const abi_type*,
                                  event_type event, bool start) -> std::enable_if_t<std::is_arithmetic_v<T>, bool>;
ABIEOS_NODISCARD bool json_to_bin(std::string*, json_to_bin_state& state, bool allow_extensions, const abi_type*,
                                  event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_optional*, json_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_extension*, json_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_object*, json_to_bin_state& state, bool allow_extensions, const abi_type* type,
                                  event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_array*, json_to_bin_state& state, bool allow_extensions, const abi_type* type,
                                  event_type event, bool start);
ABIEOS_NODISCARD bool json_to_bin(pseudo_variant*, json_to_bin_state& state, bool allow_extensions,
                                  const abi_type* type, event_type event, bool start);

template <typename T>
ABIEOS_NODISCARD auto bin_to_json(T*, bin_to_json_state& state, bool allow_extensions, const abi_type*, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool>;
ABIEOS_NODISCARD bool bin_to_json(std::string*, bin_to_json_state& state, bool allow_extensions, const abi_type*,
                                  bool start);
ABIEOS_NODISCARD bool bin_to_json(pseudo_optional*, bin_to_json_state& state, bool allow_extensions,
                                  const abi_type* type, bool start);
ABIEOS_NODISCARD bool bin_to_json(pseudo_extension*, bin_to_json_state& state, bool allow_extensions,
                                  const abi_type* type, bool start);
ABIEOS_NODISCARD bool bin_to_json(pseudo_object*, bin_to_json_state& state, bool allow_extensions, const abi_type* type,
                                  bool start);
ABIEOS_NODISCARD bool bin_to_json(pseudo_array*, bin_to_json_state& state, bool allow_extensions, const abi_type* type,
                                  bool start);
ABIEOS_NODISCARD bool bin_to_json(pseudo_variant*, bin_to_json_state& state, bool allow_extensions,
                                  const abi_type* type, bool start);

///////////////////////////////////////////////////////////////////////////////
// serializable types
///////////////////////////////////////////////////////////////////////////////

template <typename T, typename State>
ABIEOS_NODISCARD bool json_to_number(T& dest, State& state, event_type event) {
    if (event == event_type::received_bool) {
        dest = state.get_bool();
        return true;
    }
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if constexpr (std::is_integral_v<T>) {
            return decimal_to_binary(dest, state.error, s);
        } else if constexpr (std::is_same_v<T, float>) {
            errno = 0;
            char* end;
            dest = strtof(s.c_str(), &end);
            if (errno || end == s.c_str())
                return set_error(state.error, "number is out of range or has bad format");
            return true;
        } else if constexpr (std::is_same_v<T, double>) {
            errno = 0;
            char* end;
            dest = strtod(s.c_str(), &end);
            if (errno || end == s.c_str())
                return set_error(state.error, "number is out of range or has bad format");
            return true;
        }
    }
    return set_error(state.error, "expected number or boolean");
} // namespace abieos

struct bytes {
    std::vector<char> data;
};

void push_varuint32(std::vector<char>& bin, uint32_t v);

ABIEOS_NODISCARD inline bool bin_to_native(bytes& obj, bin_to_native_state& state, bool) {
    uint32_t size;
    if (!read_varuint32(state.bin, state.error, size))
        return false;
    if (size > state.bin.end - state.bin.pos)
        return set_error(state, "invalid bytes size");
    obj.data.resize(size);
    return read_raw(state.bin, state.error, obj.data.data(), size);
}

ABIEOS_NODISCARD inline bool bin_to_native(input_buffer& obj, bin_to_native_state& state, bool) {
    uint32_t size;
    if (!read_varuint32(state.bin, state.error, size))
        return false;
    if (size > state.bin.end - state.bin.pos)
        return set_error(state, "invalid bytes size");
    obj = {state.bin.pos, state.bin.pos + size};
    state.bin.pos += size;
    return true;
}

inline void native_to_bin(std::vector<char>& bin, const bytes& obj) {
    push_varuint32(bin, obj.data.size());
    bin.insert(bin.end(), obj.data.begin(), obj.data.end());
}

ABIEOS_NODISCARD inline bool json_to_native(bytes& obj, json_to_native_state& state, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_native)
            printf("%*sbytes (%d hex digits)\n", int(state.stack.size() * 4), "", int(s.size()));
        if (s.size() & 1)
            return set_error(state, "odd number of hex digits");
        obj.data.clear();
        return unhex(state.error, s.begin(), s.end(), std::back_inserter(obj.data));
    } else
        return set_error(state, "expected string containing hex digits");
}

ABIEOS_NODISCARD inline bool json_to_native(input_buffer& obj, json_to_native_state& state, event_type event,
                                            bool start) {
    return set_error(state, "can not convert json to input_buffer");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(bytes*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*sbytes (%d hex digits)\n", int(state.stack.size() * 4), "", int(s.size()));
        if (s.size() & 1)
            return set_error(state, "odd number of hex digits");
        push_varuint32(state.bin, s.size() / 2);
        return unhex(state.error, s.begin(), s.end(), std::back_inserter(state.bin));
    } else
        return set_error(state, "expected string containing hex digits");
}

ABIEOS_NODISCARD inline bool bin_to_json(bytes*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    uint32_t size;
    if (!read_varuint32(state.bin, state.error, size))
        return false;
    if (size > state.bin.end - state.bin.pos)
        return set_error(state, "invalid bytes size");
    std::vector<char> raw(size);
    if (!read_raw(state.bin, state.error, raw.data(), size))
        return false;
    std::string result;
    hex(raw.begin(), raw.end(), std::back_inserter(result));
    return state.writer.String(result.c_str(), result.size());
}

template <unsigned size>
struct fixed_binary {
    std::array<uint8_t, size> value{{0}};

    explicit operator std::string() const {
        std::string result;
        hex(value.begin(), value.end(), std::back_inserter(result));
        return result;
    }
};

template <unsigned size>
bool operator==(const fixed_binary<size>& a, const fixed_binary<size>& b) {
    return a.value == b.value;
}

template <unsigned size>
bool operator!=(const fixed_binary<size>& a, const fixed_binary<size>& b) {
    return a.value != b.value;
}

using float128 = fixed_binary<16>;
using checksum160 = fixed_binary<20>;
using checksum256 = fixed_binary<32>;
using checksum512 = fixed_binary<64>;

template <unsigned size>
ABIEOS_NODISCARD bool bin_to_native(fixed_binary<size>& obj, bin_to_native_state& state, bool start) {
    return read_raw(state.bin, state.error, obj);
}

template <unsigned size>
inline void native_to_bin(std::vector<char>& bin, const fixed_binary<size>& obj) {
    bin.insert(bin.end(), obj.value.begin(), obj.value.end());
}

template <unsigned size>
ABIEOS_NODISCARD bool json_to_native(fixed_binary<size>& obj, json_to_native_state& state, event_type event,
                                     bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_native)
            printf("%*schecksum\n", int(state.stack.size() * 4), "");
        std::vector<uint8_t> v;
        if (!unhex(state.error, s.begin(), s.end(), std::back_inserter(v)))
            return false;
        if (v.size() != size)
            return set_error(state, "hex string has incorrect length");
        memcpy(obj.value.data(), v.data(), size);
        return true;
    } else
        return set_error(state, "expected string containing hex");
}

template <typename State, unsigned size>
ABIEOS_NODISCARD bool json_to_bin(fixed_binary<size>*, State& state, bool, const abi_type*, event_type event,
                                  bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*schecksum\n", int(state.stack.size() * 4), "");
        std::vector<uint8_t> v;
        if (!unhex(state.error, s.begin(), s.end(), std::back_inserter(v)))
            return false;
        if (v.size() != size)
            return set_error(state, "hex string has incorrect length");
        state.bin.insert(state.bin.end(), v.begin(), v.end());
        return true;
    } else
        return set_error(state, "expected string containing hex");
}

template <unsigned size>
ABIEOS_NODISCARD inline bool bin_to_json(fixed_binary<size>*, bin_to_json_state& state, bool, const abi_type*,
                                         bool start) {
    fixed_binary<size> v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    std::string result;
    hex(v.value.begin(), v.value.end(), std::back_inserter(result));
    return state.writer.String(result.c_str(), result.size());
}

struct uint128 {
    std::array<uint8_t, 16> value{{0}};

    explicit operator std::string() const { return binary_to_decimal(value); }
};

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(uint128*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*suint128\n", int(state.stack.size() * 4), "");
        std::array<uint8_t, 16> value;
        if (!decimal_to_binary<16>(value, state.error, s))
            return false;
        push_raw(state.bin, value);
        return true;
    } else
        return set_error(state, "expected string containing uint128");
}

ABIEOS_NODISCARD inline bool bin_to_json(uint128*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    uint128 v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    auto result = binary_to_decimal(v.value);
    return state.writer.String(result.c_str(), result.size());
}

struct int128 {
    std::array<uint8_t, 16> value{{0}};

    explicit operator std::string() const {
        auto v = value;
        bool negative = is_negative(v);
        if (negative)
            negate(v);
        auto result = binary_to_decimal(v);
        if (negative)
            result = "-" + result;
        return result;
    }
};

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(int128*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        std::string_view s = state.get_string();
        if (trace_json_to_bin)
            printf("%*sint128\n", int(state.stack.size() * 4), "");
        bool negative = false;
        if (!s.empty() && s[0] == '-') {
            negative = true;
            s = s.substr(1);
        }
        std::array<uint8_t, 16> value;
        if (!decimal_to_binary<16>(value, state.error, s))
            return false;
        if (negative)
            negate(value);
        if (is_negative(value) != negative)
            return set_error(state, "number is out of range");
        push_raw(state.bin, value);
        return true;
    } else
        return set_error(state, "expected string containing int128");
}

ABIEOS_NODISCARD inline bool bin_to_json(int128*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    uint128 v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    bool negative = is_negative(v.value);
    if (negative)
        negate(v.value);
    auto result = binary_to_decimal(v.value);
    if (negative)
        result = "-" + result;
    return state.writer.String(result.c_str(), result.size());
}

ABIEOS_NODISCARD inline bool bin_to_native(public_key& obj, bin_to_native_state& state, bool) {
    return read_raw(state.bin, state.error, obj);
}

inline void native_to_bin(std::vector<char>& bin, const public_key& obj) { return push_raw(bin, obj); }

ABIEOS_NODISCARD inline bool json_to_native(public_key& obj, json_to_native_state& state, event_type event,
                                            bool start) {
    return set_error(state, "can't convert public_key");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(public_key*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*spublic_key\n", int(state.stack.size() * 4), "");
        public_key key;
        if (!string_to_public_key(key, state.error, s))
            return false;
        push_raw(state.bin, key);
        return true;
    } else
        return set_error(state, "expected string containing public_key");
}

ABIEOS_NODISCARD inline bool bin_to_json(public_key*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    public_key v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    std::string result;
    if (!public_key_to_string(result, state.error, v))
        return false;
    return state.writer.String(result.c_str(), result.size());
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(private_key*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*sprivate_key\n", int(state.stack.size() * 4), "");
        private_key key;
        if (!string_to_private_key(key, state.error, s))
            return false;
        push_raw(state.bin, key);
        return true;
    } else
        return set_error(state, "expected string containing private_key");
}

ABIEOS_NODISCARD inline bool bin_to_json(private_key*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    private_key v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    std::string result;
    if (!private_key_to_string(result, state.error, v))
        return false;
    return state.writer.String(result.c_str(), result.size());
}

ABIEOS_NODISCARD inline bool bin_to_native(signature& obj, bin_to_native_state& state, bool) {
    return read_raw(state.bin, state.error, obj);
}

ABIEOS_NODISCARD inline bool json_to_native(signature& obj, json_to_native_state& state, event_type event, bool start) {
    if (event == event_type::received_string)
        return string_to_signature(obj, state.error, state.get_string());
    else
        return set_error(state, "expected string containing signature");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(signature*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*ssignature\n", int(state.stack.size() * 4), "");
        signature key;
        if (!string_to_signature(key, state.error, s))
            return false;
        push_raw(state.bin, key);
        return true;
    } else
        return set_error(state, "expected string containing signature");
}

ABIEOS_NODISCARD inline bool bin_to_json(signature*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    signature v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    std::string result;
    if (!signature_to_string(result, state.error, v))
        return false;
    return state.writer.String(result.c_str(), result.size());
}

inline constexpr uint64_t char_to_name_digit(char c) {
    if (c >= 'a' && c <= 'z')
        return (c - 'a') + 6;
    if (c >= '1' && c <= '5')
        return (c - '1') + 1;
    return 0;
}

inline constexpr uint64_t string_to_name(const char* str) {
    uint64_t name = 0;
    int i = 0;
    for (; str[i] && i < 12; ++i)
        name |= (char_to_name_digit(str[i]) & 0x1f) << (64 - 5 * (i + 1));
    if (i == 12)
        name |= char_to_name_digit(str[12]) & 0x0F;
    return name;
}

inline constexpr bool char_to_name_digit_strict(char c, uint64_t& result) {
    if (c >= 'a' && c <= 'z') {
        result = (c - 'a') + 6;
        return true;
    }
    if (c >= '1' && c <= '5') {
        result = (c - '1') + 1;
        return true;
    }
    if (c == '.') {
        result = 0;
        return true;
    }
    return false;
}

inline constexpr bool string_to_name_strict(std::string_view str, uint64_t& name) {
    name = 0;
    unsigned i = 0;
    for (; i < str.size() && i < 12; ++i) {
        uint64_t x = 0;
        if (!char_to_name_digit_strict(str[i], x))
            return false;
        name |= (x & 0x1f) << (64 - 5 * (i + 1));
    }
    if (i < str.size() && i == 12) {
        uint64_t x = 0;
        if (!char_to_name_digit_strict(str[i], x) || x != (x & 0xf))
            return false;
        name |= x;
        ++i;
    }
    if (i < str.size())
        return false;
    return true;
}

inline std::string name_to_string(uint64_t name) {
    static const char* charmap = ".12345abcdefghijklmnopqrstuvwxyz";
    std::string str(13, '.');

    uint64_t tmp = name;
    for (uint32_t i = 0; i <= 12; ++i) {
        char c = charmap[tmp & (i == 0 ? 0x0f : 0x1f)];
        str[12 - i] = c;
        tmp >>= (i == 0 ? 4 : 5);
    }

    const auto last = str.find_last_not_of('.');
    return str.substr(0, last + 1);
}

struct name {
    uint64_t value = 0;

    constexpr name() = default;
    constexpr explicit name(uint64_t value) : value{value} {}
    constexpr explicit name(const char* str) : value{string_to_name(str)} {}
    constexpr name(const name&) = default;

    explicit operator std::string() const { return name_to_string(value); }
};

ABIEOS_NODISCARD inline bool operator==(name a, name b) { return a.value == b.value; }
ABIEOS_NODISCARD inline bool operator!=(name a, name b) { return a.value != b.value; }
ABIEOS_NODISCARD inline bool operator<(name a, name b) { return a.value < b.value; }

ABIEOS_NODISCARD inline bool bin_to_native(name& obj, bin_to_native_state& state, bool start) {
    return bin_to_native(obj.value, state, start);
}

inline void native_to_bin(std::vector<char>& bin, const name& obj) { native_to_bin(bin, obj.value); }

ABIEOS_NODISCARD inline bool json_to_native(name& obj, json_to_native_state& state, event_type event, bool start) {
    if (event == event_type::received_string) {
        obj.value = string_to_name(state.get_string().c_str());
        if (trace_json_to_native)
            printf("%*sname: %s (%08llx) %s\n", int(state.stack.size() * 4), "", state.get_string().c_str(),
                   (unsigned long long)obj.value, std::string{obj}.c_str());
        return true;
    } else
        return set_error(state, "expected string containing name");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(name*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        name obj{string_to_name(state.get_string().c_str())};
        if (trace_json_to_bin)
            printf("%*sname: %s (%08llx) %s\n", int(state.stack.size() * 4), "", state.get_string().c_str(),
                   (unsigned long long)obj.value, std::string{obj}.c_str());
        push_raw(state.bin, obj.value);
        return true;
    } else
        return set_error(state, "expected string containing name");
}

ABIEOS_NODISCARD inline bool bin_to_json(name*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    name v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    auto s = std::string{v};
    return state.writer.String(s.c_str(), s.size());
}

struct varuint32 {
    uint32_t value = 0;

    explicit operator std::string() const { return std::to_string(value); }
};

inline void push_varuint32(std::vector<char>& bin, uint32_t v) {
    uint64_t val = v;
    do {
        uint8_t b = val & 0x7f;
        val >>= 7;
        b |= ((val > 0) << 7);
        bin.push_back(b);
    } while (val);
}

ABIEOS_NODISCARD inline bool read_varuint32(input_buffer& bin, std::string& error, uint32_t& dest) {
    dest = 0;
    int shift = 0;
    uint8_t b = 0;
    do {
        if (shift >= 35)
            return set_error(error, "invalid varuint32 encoding");
        if (!read_raw(bin, error, b))
            return false;
        dest |= uint32_t(b & 0x7f) << shift;
        shift += 7;
    } while (b & 0x80);
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_native(varuint32& obj, bin_to_native_state& state, bool) {
    return read_varuint32(state.bin, state.error, obj.value);
}

inline void native_to_bin(std::vector<char>& bin, const varuint32& obj) { push_varuint32(bin, obj.value); }

ABIEOS_NODISCARD inline bool json_to_native(varuint32& obj, json_to_native_state& state, event_type event, bool) {
    uint32_t x;
    if (!json_to_number(x, state, event))
        return false;
    obj = varuint32{x};
    return true;
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(varuint32*, State& state, bool, const abi_type*, event_type event, bool start) {
    uint32_t x;
    if (!json_to_number(x, state, event))
        return false;
    push_varuint32(state.bin, x);
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_json(varuint32*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    uint32_t v;
    if (!read_varuint32(state.bin, state.error, v))
        return false;
    return state.writer.Uint64(v);
}

struct varint32 {
    int32_t value = 0;

    explicit operator std::string() const { return std::to_string(value); }
};

inline void push_varint32(std::vector<char>& bin, int32_t v) {
    push_varuint32(bin, (uint32_t(v) << 1) ^ uint32_t(v >> 31));
}

ABIEOS_NODISCARD inline bool read_varint32(input_buffer& bin, std::string& error, int32_t& result) {
    uint32_t v;
    if (!read_varuint32(bin, error, v))
        return false;
    if (v & 1)
        result = ((~v) >> 1) | 0x8000'0000;
    else
        result = v >> 1;
    return true;
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(varint32*, State& state, bool, const abi_type*, event_type event, bool start) {
    int32_t x;
    if (!json_to_number(x, state, event))
        return false;
    push_varint32(state.bin, x);
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_json(varint32*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    int32_t v;
    if (!read_varint32(state.bin, state.error, v))
        return false;
    return state.writer.Int64(v);
}

inline std::string microseconds_to_str(uint64_t microseconds) {
    std::string result;

    auto append_uint = [&result](uint32_t value, int digits) {
        char s[20];
        char* ch = s;
        while (digits--) {
            *ch++ = '0' + (value % 10);
            value /= 10;
        };
        std::reverse(s, ch);
        result.insert(result.end(), s, ch);
    };

    std::chrono::microseconds us{microseconds};
    date::sys_days sd(std::chrono::floor<date::days>(us));
    auto ymd = date::year_month_day{sd};
    uint32_t ms = (std::chrono::round<std::chrono::milliseconds>(us) - sd.time_since_epoch()).count();
    us -= sd.time_since_epoch();
    append_uint((int)ymd.year(), 4);
    result.push_back('-');
    append_uint((unsigned)ymd.month(), 2);
    result.push_back('-');
    append_uint((unsigned)ymd.day(), 2);
    result.push_back('T');
    append_uint(ms / 3600000 % 60, 2);
    result.push_back(':');
    append_uint(ms / 60000 % 60, 2);
    result.push_back(':');
    append_uint(ms / 1000 % 60, 2);
    result.push_back('.');
    append_uint(ms % 1000, 3);
    return result;
}

struct time_point_sec {
    uint32_t utc_seconds = 0;

    explicit operator std::string() { return microseconds_to_str(uint64_t(utc_seconds) * 1'000'000); }
};

ABIEOS_NODISCARD inline bool string_to_time_point_sec(time_point_sec& result, std::string& error, const char* s) {
    auto parse_uint = [&](uint32_t& result, int digits) {
        result = 0;
        while (digits--) {
            if (*s >= '0' && *s <= '9')
                result = result * 10 + *s++ - '0';
            else
                return set_error(error, "expected string containing time_point_sec");
        }
        return true;
    };
    uint32_t y, m, d, h, min, sec;
    if (!parse_uint(y, 4))
        return false;
    if (*s++ != '-')
        return set_error(error, "expected string containing time_point_sec");
    if (!parse_uint(m, 2))
        return false;
    if (*s++ != '-')
        return set_error(error, "expected string containing time_point_sec");
    if (!parse_uint(d, 2))
        return false;
    if (*s++ != 'T')
        return set_error(error, "expected string containing time_point_sec");
    if (!parse_uint(h, 2))
        return false;
    if (*s++ != ':')
        return set_error(error, "expected string containing time_point_sec");
    if (!parse_uint(min, 2))
        return false;
    if (*s++ != ':')
        return set_error(error, "expected string containing time_point_sec");
    if (!parse_uint(sec, 2))
        return false;
    result.utc_seconds =
        date::sys_days(date::year(y) / m / d).time_since_epoch().count() * 86400 + h * 3600 + min * 60 + sec;
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_native(time_point_sec& obj, bin_to_native_state& state, bool) {
    return read_raw(state.bin, state.error, obj.utc_seconds);
}

inline void native_to_bin(std::vector<char>& bin, const time_point_sec& obj) { native_to_bin(bin, obj.utc_seconds); }

ABIEOS_NODISCARD inline bool json_to_native(time_point_sec& obj, json_to_native_state& state, event_type event,
                                            bool start) {
    return set_error(state, "can't convert time_point_sec");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(time_point_sec*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        time_point_sec obj;
        if (!string_to_time_point_sec(obj, state.error, state.get_string().c_str()))
            return false;
        if (trace_json_to_bin)
            printf("%*stime_point_sec: %s (%u) %s\n", int(state.stack.size() * 4), "", state.get_string().c_str(),
                   (unsigned)obj.utc_seconds, std::string{obj}.c_str());
        push_raw(state.bin, obj.utc_seconds);
        return true;
    } else
        return set_error(state, "expected string containing time_point_sec");
}

ABIEOS_NODISCARD inline bool bin_to_json(time_point_sec*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    time_point_sec v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    auto s = std::string{v};
    return state.writer.String(s.c_str(), s.size());
}

struct time_point {
    uint64_t microseconds = 0;

    explicit operator std::string() const { return microseconds_to_str(microseconds); }
};

ABIEOS_NODISCARD inline bool string_to_time_point(time_point& dest, std::string& error, const std::string& s) {
    time_point_sec tps;
    if (!string_to_time_point_sec(tps, error, s.c_str()))
        return false;
    dest.microseconds = tps.utc_seconds * 1000000ull;
    auto dot = s.find('.');
    if (dot != std::string::npos) {
        auto ms = s.substr(dot);
        ms[0] = '1';
        while (ms.size() < 4)
            ms.push_back('0');
        uint32_t u;
        if (!decimal_to_binary(u, error, ms))
            return false;
        dest.microseconds += (u - 1000) * 1000;
    }
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_native(time_point& obj, bin_to_native_state& state, bool) {
    return read_raw(state.bin, state.error, obj.microseconds);
}

inline void native_to_bin(std::vector<char>& bin, const time_point& obj) { native_to_bin(bin, obj.microseconds); }

ABIEOS_NODISCARD inline bool json_to_native(time_point& obj, json_to_native_state& state, event_type event,
                                            bool start) {
    return set_error(state, "can't convert time_point");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(time_point*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        time_point obj;
        if (!string_to_time_point(obj, state.error, state.get_string()))
            return false;
        if (trace_json_to_bin)
            printf("%*stime_point: %s (%llu) %s\n", int(state.stack.size() * 4), "", state.get_string().c_str(),
                   (unsigned long long)obj.microseconds, std::string{obj}.c_str());
        push_raw(state.bin, obj.microseconds);
        return true;
    } else
        return set_error(state, "expected string containing time_point");
}

ABIEOS_NODISCARD inline bool bin_to_json(time_point*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    time_point v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    auto s = std::string{v};
    return state.writer.String(s.c_str(), s.size());
}

struct block_timestamp {
    static constexpr uint16_t interval_ms = 500;
    static constexpr uint64_t epoch_ms = 946684800000ll; // Year 2000
    uint32_t slot;

    block_timestamp() = default;
    explicit block_timestamp(uint32_t slot) : slot(slot) {}
    explicit block_timestamp(time_point t) { slot = (t.microseconds / 1000 - epoch_ms) / interval_ms; }

    explicit operator time_point() const { return time_point{(slot * (uint64_t)interval_ms + epoch_ms) * 1000}; }
    explicit operator std::string() const { return std::string{(time_point)(*this)}; }
}; // block_timestamp

ABIEOS_NODISCARD inline bool bin_to_native(block_timestamp& obj, bin_to_native_state& state, bool) {
    return read_raw(state.bin, state.error, obj.slot);
}

inline void native_to_bin(std::vector<char>& bin, const block_timestamp& obj) { native_to_bin(bin, obj.slot); }

ABIEOS_NODISCARD inline bool json_to_native(block_timestamp& obj, json_to_native_state& state, event_type event,
                                            bool start) {
    return set_error(state, "can't convert block_timestamp");
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(block_timestamp*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        time_point tp;
        if (!string_to_time_point(tp, state.error, state.get_string()))
            return false;
        block_timestamp obj{tp};
        if (trace_json_to_bin)
            printf("%*sblock_timestamp: %s (%u) %s\n", int(state.stack.size() * 4), "", state.get_string().c_str(),
                   (unsigned)obj.slot, std::string{obj}.c_str());
        push_raw(state.bin, obj.slot);
        return true;
    } else
        return set_error(state, "expected string containing block_timestamp_type");
}

ABIEOS_NODISCARD inline bool bin_to_json(block_timestamp*, bin_to_json_state& state, bool, const abi_type*,
                                         bool start) {
    uint32_t v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    auto s = std::string{block_timestamp{v}};
    return state.writer.String(s.c_str(), s.size());
}

struct symbol_code {
    uint64_t value = 0;
};

ABIEOS_NODISCARD inline bool string_to_symbol_code(uint64_t& result, std::string& error, std::string_view str) {
    while (!str.empty() && str.front() == ' ')
        str.remove_prefix(1);
    result = 0;
    uint32_t i = 0;
    while (!str.empty() && str.front() >= 'A' && str.front() <= 'Z') {
        if (i >= 7)
            return set_error(error, "expected string containing symbol_code");
        result |= uint64_t(str.front()) << (8 * i++);
        str.remove_prefix(1);
    }
    return true;
}

inline std::string symbol_code_to_string(uint64_t v) {
    std::string result;
    while (v > 0) {
        result += char(v & 0xFF);
        v >>= 8;
    }
    return result;
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(symbol_code*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*ssymbol_code: %s\n", int(state.stack.size() * 4), "", s.c_str());
        uint64_t v;
        if (!string_to_symbol_code(v, state.error, s.c_str()))
            return false;
        push_raw(state.bin, v);
        return true;
    } else
        return set_error(state, "expected string containing symbol_code");
}

ABIEOS_NODISCARD inline bool bin_to_json(symbol_code*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    symbol_code v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    auto result = symbol_code_to_string(v.value);
    return state.writer.String(result.c_str(), result.size());
}

struct symbol {
    uint64_t value = 0;
};

ABIEOS_NODISCARD inline bool string_to_symbol(uint64_t& result, std::string& error, uint8_t precision,
                                              std::string_view str) {
    if (!string_to_symbol_code(result, error, str))
        return false;
    result = (result << 8) | precision;
    return true;
}

ABIEOS_NODISCARD inline bool string_to_symbol(uint64_t& result, std::string& error, std::string_view str) {
    uint8_t precision = 0;
    while (!str.empty() && str.front() >= '0' && str.front() <= '9') {
        precision = precision * 10 + (str.front() - '0');
        str.remove_prefix(1);
    }
    if (!str.empty() && str.front() == ',')
        str.remove_prefix(1);
    return string_to_symbol(result, error, precision, str);
}

inline std::string symbol_to_string(uint64_t v) {
    return std::to_string(v & 0xff) + "," + symbol_code_to_string(v >> 8);
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(symbol*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*ssymbol: %s\n", int(state.stack.size() * 4), "", s.c_str());
        uint64_t v;
        if (!string_to_symbol(v, state.error, s.c_str()))
            return false;
        push_raw(state.bin, v);
        return true;
    } else
        return set_error(state, "expected string containing symbol");
}

ABIEOS_NODISCARD inline bool bin_to_json(symbol*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    uint64_t v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    std::string result{symbol_to_string(v)};
    return state.writer.String(result.c_str(), result.size());
}

struct asset {
    int64_t amount = 0;
    symbol sym{};
};

ABIEOS_NODISCARD inline bool string_to_asset(asset& result, std::string& error, const char* s) {
    // todo: check overflow
    while (*s == ' ')
        ++s;
    uint64_t amount = 0;
    uint8_t precision = 0;
    bool negative = false;
    if (*s == '-') {
        ++s;
        negative = true;
    }
    while (*s >= '0' && *s <= '9')
        amount = amount * 10 + (*s++ - '0');
    if (*s == '.') {
        ++s;
        while (*s >= '0' && *s <= '9') {
            amount = amount * 10 + (*s++ - '0');
            ++precision;
        }
    }
    if (negative)
        amount = -amount;
    uint64_t code;
    if (!string_to_symbol_code(code, error, s))
        return false;
    result = asset{(int64_t)amount, symbol{(code << 8) | precision}};
    return true;
}

inline std::string asset_to_string(const asset& v) {
    std::string result;
    uint64_t amount;
    if (v.amount < 0)
        amount = -v.amount;
    else
        amount = v.amount;
    uint8_t precision = v.sym.value;
    if (precision) {
        while (precision--) {
            result += '0' + amount % 10;
            amount /= 10;
        }
        result += '.';
    }
    do {
        result += '0' + amount % 10;
        amount /= 10;
    } while (amount);
    if (v.amount < 0)
        result += '-';
    std::reverse(result.begin(), result.end());
    return result + ' ' + symbol_code_to_string(v.sym.value >> 8);
}

template <typename State>
ABIEOS_NODISCARD bool json_to_bin(asset*, State& state, bool, const abi_type*, event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*sasset: %s\n", int(state.stack.size() * 4), "", s.c_str());
        asset v;
        if (!string_to_asset(v, state.error, s.c_str()))
            return false;
        push_raw(state.bin, v.amount);
        push_raw(state.bin, v.sym.value);
        return true;
    } else
        return set_error(state, "expected string containing asset");
}

ABIEOS_NODISCARD inline bool bin_to_json(asset*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    asset v{};
    if (!read_raw(state.bin, state.error, v.amount))
        return false;
    if (!read_raw(state.bin, state.error, v.sym.value))
        return false;
    auto s = asset_to_string(v);
    return state.writer.String(s.c_str(), s.size());
}

///////////////////////////////////////////////////////////////////////////////
// abi types
///////////////////////////////////////////////////////////////////////////////

using extensions_type = std::vector<std::pair<uint16_t, bytes>>;

struct type_def {
    std::string new_type_name{};
    std::string type{};
};

template <typename F>
constexpr void for_each_field(type_def*, F f) {
    f("new_type_name", member_ptr<&type_def::new_type_name>{});
    f("type", member_ptr<&type_def::type>{});
}

struct field_def {
    std::string name{};
    std::string type{};
};

template <typename F>
constexpr void for_each_field(field_def*, F f) {
    f("name", member_ptr<&field_def::name>{});
    f("type", member_ptr<&field_def::type>{});
}

struct struct_def {
    std::string name{};
    std::string base{};
    std::vector<field_def> fields{};
};

template <typename F>
constexpr void for_each_field(struct_def*, F f) {
    f("name", member_ptr<&struct_def::name>{});
    f("base", member_ptr<&struct_def::base>{});
    f("fields", member_ptr<&struct_def::fields>{});
}

struct action_def {
    ::abieos::name name{};
    std::string type{};
    std::string ricardian_contract{};
};

template <typename F>
constexpr void for_each_field(action_def*, F f) {
    f("name", member_ptr<&action_def::name>{});
    f("type", member_ptr<&action_def::type>{});
    f("ricardian_contract", member_ptr<&action_def::ricardian_contract>{});
}

struct table_def {
    ::abieos::name name{};
    std::string index_type{};
    std::vector<std::string> key_names{};
    std::vector<std::string> key_types{};
    std::string type{};
};

template <typename F>
constexpr void for_each_field(table_def*, F f) {
    f("name", member_ptr<&table_def::name>{});
    f("index_type", member_ptr<&table_def::index_type>{});
    f("key_names", member_ptr<&table_def::key_names>{});
    f("key_types", member_ptr<&table_def::key_types>{});
    f("type", member_ptr<&table_def::type>{});
}

struct clause_pair {
    std::string id{};
    std::string body{};
};

template <typename F>
constexpr void for_each_field(clause_pair*, F f) {
    f("id", member_ptr<&clause_pair::id>{});
    f("body", member_ptr<&clause_pair::body>{});
}

struct error_message {
    uint64_t error_code{};
    std::string error_msg{};
};

template <typename F>
constexpr void for_each_field(error_message*, F f) {
    f("error_code", member_ptr<&error_message::error_code>{});
    f("error_msg", member_ptr<&error_message::error_msg>{});
}

struct variant_def {
    std::string name{};
    std::vector<std::string> types{};
};

template <typename F>
constexpr void for_each_field(variant_def*, F f) {
    f("name", member_ptr<&variant_def::name>{});
    f("types", member_ptr<&variant_def::types>{});
}

struct abi_def {
    std::string version{};
    std::vector<type_def> types{};
    std::vector<struct_def> structs{};
    std::vector<action_def> actions{};
    std::vector<table_def> tables{};
    std::vector<clause_pair> ricardian_clauses{};
    std::vector<error_message> error_messages{};
    extensions_type abi_extensions{};
    might_not_exist<std::vector<variant_def>> variants{};
};

template <typename F>
constexpr void for_each_field(abi_def*, F f) {
    f("version", member_ptr<&abi_def::version>{});
    f("types", member_ptr<&abi_def::types>{});
    f("structs", member_ptr<&abi_def::structs>{});
    f("actions", member_ptr<&abi_def::actions>{});
    f("tables", member_ptr<&abi_def::tables>{});
    f("ricardian_clauses", member_ptr<&abi_def::ricardian_clauses>{});
    f("error_messages", member_ptr<&abi_def::error_messages>{});
    f("abi_extensions", member_ptr<&abi_def::abi_extensions>{});
    f("variants", member_ptr<&abi_def::variants>{});
}

ABIEOS_NODISCARD inline bool check_abi_version(const std::string& s, std::string& error) {
    if (s.substr(0, 13) != "eosio::abi/1.")
        return set_error(error, "unsupported abi version");
    return true;
}

ABIEOS_NODISCARD inline bool check_abi_version(input_buffer bin, std::string& error) {
    std::string version;
    if (!read_string(bin, error, version))
        return false;
    return check_abi_version(version, error);
}

///////////////////////////////////////////////////////////////////////////////
// json_to_jvalue
///////////////////////////////////////////////////////////////////////////////

ABIEOS_NODISCARD bool json_to_jobject(jvalue& value, json_to_jvalue_state& state, event_type event, bool start);
ABIEOS_NODISCARD bool json_to_jarray(jvalue& value, json_to_jvalue_state& state, event_type event, bool start);

ABIEOS_NODISCARD inline bool receive_event(struct json_to_jvalue_state& state, event_type event, bool start) {
    if (state.stack.empty())
        return set_error(state, "extra data");
    if (state.stack.size() > max_stack_size)
        return set_error(state, "recursion limit reached");
    if (trace_json_to_jvalue_event)
        printf("(event %d)\n", (int)event);
    auto& v = *state.stack.back().value;
    if (start) {
        state.stack.pop_back();
        if (event == event_type::received_null) {
            v.value = nullptr;
        } else if (event == event_type::received_bool) {
            v.value = state.get_bool();
        } else if (event == event_type::received_string) {
            v.value = std::move(state.get_string());
        } else if (event == event_type::received_start_object) {
            v.value = jobject{};
            return json_to_jobject(v, state, event, start);
        } else if (event == event_type::received_start_array) {
            v.value = jarray{};
            return json_to_jarray(v, state, event, start);
        } else {
            return false;
        }
    } else {
        if (std::holds_alternative<jobject>(v.value))
            return json_to_jobject(v, state, event, start);
        else if (std::holds_alternative<jarray>(v.value))
            return json_to_jarray(v, state, event, start);
        else
            return set_error(state, "extra data");
    }
    return true;
}

ABIEOS_NODISCARD inline bool json_to_jvalue(jvalue& value, std::string& error, std::string_view json) {
    std::string mutable_json{json};
    mutable_json.push_back(0);
    json_to_jvalue_state state{.error = error};
    state.stack.push_back({&value});
    rapidjson::Reader reader;
    rapidjson::InsituStringStream ss(mutable_json.data());
    return reader.Parse<rapidjson::kParseValidateEncodingFlag | rapidjson::kParseIterativeFlag |
                        rapidjson::kParseNumbersAsStringsFlag>(ss, state);
}

ABIEOS_NODISCARD inline bool json_to_jobject(jvalue& value, json_to_jvalue_state& state, event_type event, bool start) {
    if (start) {
        if (event != event_type::received_start_object)
            return set_error(state, "expected object");
        if (trace_json_to_jvalue)
            printf("%*s{\n", int(state.stack.size() * 4), "");
        state.stack.push_back({&value});
        return true;
    } else if (event == event_type::received_end_object) {
        if (trace_json_to_jvalue)
            printf("%*s}\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (event == event_type::received_key) {
        stack_entry.key = std::move(state.received_data.key);
        return true;
    } else {
        if (trace_json_to_jvalue)
            printf("%*sfield %s (event %d)\n", int(state.stack.size() * 4), "", stack_entry.key.c_str(), (int)event);
        auto& x = std::get<jobject>(value.value)[stack_entry.key] = {};
        state.stack.push_back({&x});
        return receive_event(state, event, true);
    }
}

ABIEOS_NODISCARD inline bool json_to_jarray(jvalue& value, json_to_jvalue_state& state, event_type event, bool start) {
    if (start) {
        if (event != event_type::received_start_array)
            return set_error(state, "expected array");
        if (trace_json_to_jvalue)
            printf("%*s[\n", int(state.stack.size() * 4), "");
        state.stack.push_back({&value});
        return true;
    } else if (event == event_type::received_end_array) {
        if (trace_json_to_jvalue)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    auto& v = std::get<jarray>(value.value);
    if (trace_json_to_jvalue)
        printf("%*sitem %d (event %d)\n", int(state.stack.size() * 4), "", int(v.size()), (int)event);
    v.emplace_back();
    state.stack.push_back({&v.back()});
    return receive_event(state, event, true);
}

///////////////////////////////////////////////////////////////////////////////
// native serializer implementations
///////////////////////////////////////////////////////////////////////////////

template <typename T>
struct native_serializer_impl : native_serializer {
    ABIEOS_NODISCARD bool bin_to_native(void* v, bin_to_native_state& state, bool start) const override {
        using ::abieos::bin_to_native;
        return bin_to_native(*reinterpret_cast<T*>(v), state, start);
    }
    ABIEOS_NODISCARD bool json_to_native(void* v, json_to_native_state& state, event_type event,
                                         bool start) const override {
        using ::abieos::json_to_native;
        return json_to_native(*reinterpret_cast<T*>(v), state, event, start);
    }
};

template <typename T>
inline constexpr auto native_serializer_for = native_serializer_impl<T>{};

template <typename member_ptr>
constexpr auto create_native_field_serializer_methods_impl() {
    struct impl : native_field_serializer_methods {
        ABIEOS_NODISCARD bool bin_to_native(void* v, bin_to_native_state& state, bool start) const override {
            using ::abieos::bin_to_native;
            return bin_to_native(member_from_void(member_ptr{}, v), state, start);
        }
        ABIEOS_NODISCARD bool json_to_native(void* v, json_to_native_state& state, event_type event,
                                             bool start) const override {
            using ::abieos::json_to_native;
            return json_to_native(member_from_void(member_ptr{}, v), state, event, start);
        }
    };
    return impl{};
}

template <typename member_ptr>
inline constexpr auto field_serializer_methods_for = create_native_field_serializer_methods_impl<member_ptr>();

template <typename T>
constexpr auto create_native_field_serializers() {
    constexpr auto num_fields = ([&]() constexpr {
        int num_fields = 0;
        for_each_field((T*)nullptr, [&](auto, auto) { ++num_fields; });
        return num_fields;
    }());
    std::array<native_field_serializer, num_fields> fields;
    int i = 0;
    for_each_field((T*)nullptr, [&](auto* name, auto member_ptr) {
        fields[i++] = {name, &field_serializer_methods_for<decltype(member_ptr)>};
    });
    return fields;
}

template <typename T>
inline constexpr auto native_field_serializers_for = create_native_field_serializers<T>();

///////////////////////////////////////////////////////////////////////////////
// bin_to_native
///////////////////////////////////////////////////////////////////////////////

template <typename T>
ABIEOS_NODISCARD bool bin_to_native(T& obj, std::string& error, input_buffer& bin) {
    bin_to_native_state state{error, bin};
    if (!native_serializer_for<T>.bin_to_native(&obj, state, true))
        return false;
    while (!state.stack.empty()) {
        if (!state.stack.back().ser->bin_to_native(state.stack.back().obj, state, false))
            return false;
        if (state.stack.size() > max_stack_size)
            return set_error(state, "recursion limit reached");
    }
    bin = state.bin;
    return true;
}

template <typename T>
ABIEOS_NODISCARD auto bin_to_native(T& obj, bin_to_native_state& state, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool> {
    return read_raw(state.bin, state.error, obj);
}

template <typename T>
ABIEOS_NODISCARD auto bin_to_native(T& obj, bin_to_native_state& state, bool start)
    -> std::enable_if_t<std::is_class_v<T>, bool> {
    if (start) {
        if (trace_bin_to_native)
            printf("%*s{ %d fields\n", int(state.stack.size() * 4), "", int(native_field_serializers_for<T>.size()));
        state.stack.push_back({&obj, &native_serializer_for<T>});
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (stack_entry.position < (ptrdiff_t)native_field_serializers_for<T>.size()) {
        auto& field_ser = native_field_serializers_for<T>[stack_entry.position];
        if (trace_bin_to_native)
            printf("%*sfield %d/%d: %s %p\n", int(state.stack.size() * 4), "", int(stack_entry.position),
                   int(native_field_serializers_for<T>.size()), std::string{field_ser.name}.c_str(), field_ser.methods);
        ++stack_entry.position;
        return field_ser.methods->bin_to_native(&obj, state, true);
    } else {
        if (trace_bin_to_native)
            printf("%*s}\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
}

template <typename T>
ABIEOS_NODISCARD bool bin_to_native(std::vector<T>& v, bin_to_native_state& state, bool start) {
    if (start) {
        v.clear();
        uint32_t size;
        if (!read_varuint32(state.bin, state.error, size))
            return false;
        if (trace_bin_to_native)
            printf("%*s[ %u items\n", int(state.stack.size() * 4), "", int(size));
        state.stack.push_back({&v, &native_serializer_for<std::vector<T>>});
        state.stack.back().array_size = size;
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (stack_entry.position < stack_entry.array_size) {
        if (trace_bin_to_native)
            printf("%*sitem %d/%d\n", int(state.stack.size() * 4), "", int(stack_entry.position),
                   int(stack_entry.array_size));
        v.emplace_back();
        stack_entry.position = v.size();
        return native_serializer_for<T>.bin_to_native(&v.back(), state, true);
    } else {
        if (trace_bin_to_native)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
}

template <typename First, typename Second>
ABIEOS_NODISCARD bool bin_to_native(std::pair<First, Second>& obj, bin_to_native_state& state, bool start) {
    if (start) {
        if (trace_bin_to_native)
            printf("%*s[ pair\n", int(state.stack.size() * 4), "");
        state.stack.push_back({&obj, &native_serializer_for<std::pair<First, Second>>});
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (stack_entry.position == 0) {
        if (trace_bin_to_native)
            printf("%*sitem 0/1\n", int(state.stack.size() * 4), "");
        ++stack_entry.position;
        return native_serializer_for<First>.bin_to_native(&obj.first, state, true);
    } else if (stack_entry.position == 1) {
        if (trace_bin_to_native)
            printf("%*sitem 1/1\n", int(state.stack.size() * 4), "");
        ++stack_entry.position;
        return native_serializer_for<Second>.bin_to_native(&obj.second, state, true);
    } else {
        if (trace_bin_to_native)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
}

ABIEOS_NODISCARD inline bool bin_to_native(std::string& obj, bin_to_native_state& state, bool) {
    uint32_t size;
    if (!read_varuint32(state.bin, state.error, size))
        return false;
    if (size > state.bin.end - state.bin.pos)
        return set_error(state, "invalid string size");
    obj.resize(size);
    return read_raw(state.bin, state.error, obj.data(), size);
}

template <typename T>
ABIEOS_NODISCARD bool bin_to_native(std::optional<T>& obj, bin_to_native_state& state, bool) {
    bool present;
    if (!read_raw(state.bin, state.error, present))
        return false;
    if (!present) {
        obj.reset();
        return true;
    }
    obj.emplace();
    return bin_to_native(*obj, state, true);
}

template <uint32_t I, typename... Ts>
ABIEOS_NODISCARD bool bin_to_native_variant(std::variant<Ts...>& v, bin_to_native_state& state, uint32_t i) {
    if constexpr (I < std::variant_size_v<std::variant<Ts...>>) {
        if (i == I) {
            auto& x = v.template emplace<std::variant_alternative_t<I, std::variant<Ts...>>>();
            return bin_to_native(x, state, true);
        } else {
            return bin_to_native_variant<I + 1>(v, state, i);
        }
    } else {
        return set_error(state, "bad variant index");
    }
}

template <typename... Ts>
ABIEOS_NODISCARD bool bin_to_native(std::variant<Ts...>& obj, bin_to_native_state& state, bool) {
    uint32_t u;
    if (!read_varuint32(state.bin, state.error, u))
        return false;
    return bin_to_native_variant<0>(obj, state, u);
}

template <typename T>
ABIEOS_NODISCARD bool bin_to_native(might_not_exist<T>& obj, bin_to_native_state& state, bool start) {
    if (state.bin.pos != state.bin.end)
        return bin_to_native(obj.value, state, start);
    return true;
}

///////////////////////////////////////////////////////////////////////////////
// native_to_bin
///////////////////////////////////////////////////////////////////////////////

template <typename T>
void native_to_bin(std::vector<char>& bin, const T& obj) {
    if constexpr (std::is_class_v<T>) {
        for_each_field((T*)nullptr, [&](auto* name, auto member_ptr) { //
            native_to_bin(bin, member_from_void(member_ptr, &obj));
        });
    } else {
        static_assert(std::is_arithmetic_v<T>);
        push_raw(bin, obj);
    }
}

template <typename T>
std::vector<char> native_to_bin(const T& obj) {
    std::vector<char> bin;
    native_to_bin(bin, obj);
    return bin;
}

inline void native_to_bin(std::vector<char>& bin, const std::string& obj) {
    push_varuint32(bin, obj.size());
    bin.insert(bin.end(), obj.begin(), obj.end());
}

template <typename T>
void native_to_bin(std::vector<char>& bin, const std::vector<T>& obj) {
    push_varuint32(bin, obj.size());
    for (auto& v : obj)
        native_to_bin(bin, v);
}

///////////////////////////////////////////////////////////////////////////////
// json_to_native
///////////////////////////////////////////////////////////////////////////////

ABIEOS_NODISCARD inline bool receive_event(struct json_to_native_state& state, event_type event, bool start) {
    if (state.stack.empty())
        return set_error(state, "extra data");
    if (state.stack.size() > max_stack_size)
        return set_error(state, "recursion limit reached");
    if (trace_json_to_native_event)
        printf("(event %d)\n", (int)event);
    auto x = state.stack.back();
    if (start)
        state.stack.clear();
    return x.ser && x.ser->json_to_native(x.obj, state, event, start);
}

template <typename T>
ABIEOS_NODISCARD bool json_to_native(T& obj, std::string& error, std::string_view json) {
    std::string mutable_json{json};
    mutable_json.push_back(0);
    json_to_native_state state{.error = error};
    state.stack.push_back(native_stack_entry{&obj, &native_serializer_for<T>, 0});
    rapidjson::Reader reader;
    rapidjson::InsituStringStream ss(mutable_json.data());
    return reader.Parse<rapidjson::kParseValidateEncodingFlag | rapidjson::kParseIterativeFlag |
                        rapidjson::kParseNumbersAsStringsFlag>(ss, state);
}

template <typename T>
ABIEOS_NODISCARD auto json_to_native(T& obj, json_to_native_state& state, event_type event, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool> {

    return json_to_number(obj, state, event);
}

template <typename T>
ABIEOS_NODISCARD auto json_to_native(T& obj, json_to_native_state& state, event_type event, bool start)
    -> std::enable_if_t<std::is_class_v<T>, bool> {

    if (start) {
        if (event != event_type::received_start_object)
            return set_error(state, "expected object");
        if (trace_json_to_native)
            printf("%*s{ %d fields\n", int(state.stack.size() * 4), "", int(native_field_serializers_for<T>.size()));
        state.stack.push_back({&obj, &native_serializer_for<T>});
        return true;
    } else if (event == event_type::received_end_object) {
        if (trace_json_to_native)
            printf("%*s}\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (event == event_type::received_key) {
        stack_entry.position = 0;
        while (stack_entry.position < (ptrdiff_t)native_field_serializers_for<T>.size() &&
               native_field_serializers_for<T>[stack_entry.position].name != state.received_data.key)
            ++stack_entry.position;
        if (stack_entry.position >= (ptrdiff_t)native_field_serializers_for<T>.size())
            return set_error(state, "unknown field " + state.received_data.key); // TODO: eat unknown subtree
        return true;
    } else if (stack_entry.position < (ptrdiff_t)native_field_serializers_for<T>.size()) {
        auto& field_ser = native_field_serializers_for<T>[stack_entry.position];
        if (trace_json_to_native)
            printf("%*sfield %d/%d: %s (event %d)\n", int(state.stack.size() * 4), "", int(stack_entry.position),
                   int(native_field_serializers_for<T>.size()), std::string{field_ser.name}.c_str(), (int)event);
        return field_ser.methods->json_to_native(&obj, state, event, true);
    } else {
        return true;
    }
    return true;
}

template <typename T>
ABIEOS_NODISCARD bool json_to_native(std::vector<T>& v, json_to_native_state& state, event_type event, bool start) {
    if (start) {
        if (event != event_type::received_start_array)
            return set_error(state, "expected array");
        if (trace_json_to_native)
            printf("%*s[\n", int(state.stack.size() * 4), "");
        state.stack.push_back({&v, &native_serializer_for<std::vector<T>>});
        return true;
    } else if (event == event_type::received_end_array) {
        if (trace_json_to_native)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    if (trace_json_to_native)
        printf("%*sitem %d (event %d)\n", int(state.stack.size() * 4), "", int(v.size()), (int)event);
    v.emplace_back();
    return json_to_native(v.back(), state, event, true);
}

template <typename First, typename Second>
ABIEOS_NODISCARD bool json_to_native(std::pair<First, Second>& obj, json_to_native_state& state, event_type event,
                                     bool start) {
    return set_error(state, "pair not implemented"); // TODO
}

ABIEOS_NODISCARD inline bool json_to_native(std::string& obj, json_to_native_state& state, event_type event,
                                            bool start) {
    if (event == event_type::received_string) {
        obj = state.get_string();
        if (trace_json_to_native)
            printf("%*sstring: %s\n", int(state.stack.size() * 4), "", obj.c_str());
        return true;
    } else
        return set_error(state, "expected string");
}

template <typename T>
ABIEOS_NODISCARD bool json_to_native(std::optional<T>& obj, json_to_native_state& state, event_type event, bool) {
    if (event == event_type::received_null)
        return true;
    obj.emplace();
    return json_to_native(*obj, state, event, true);
}

template <typename... Ts>
ABIEOS_NODISCARD bool json_to_native(std::variant<Ts...>& obj, json_to_native_state& state, event_type event, bool) {
    return set_error(state, "can not convert json to variant");
}

template <typename T>
ABIEOS_NODISCARD bool json_to_native(might_not_exist<T>& obj, json_to_native_state& state, event_type event,
                                     bool start) {
    return json_to_native(obj.value, state, event, start);
}

///////////////////////////////////////////////////////////////////////////////
// abi serializer implementations
///////////////////////////////////////////////////////////////////////////////

template <typename F>
constexpr void for_each_abi_type(F f) {
    static_assert(sizeof(float) == 4);
    static_assert(sizeof(double) == 8);

    f("bool", (bool*)nullptr);
    f("int8", (int8_t*)nullptr);
    f("uint8", (uint8_t*)nullptr);
    f("int16", (int16_t*)nullptr);
    f("uint16", (uint16_t*)nullptr);
    f("int32", (int32_t*)nullptr);
    f("uint32", (uint32_t*)nullptr);
    f("int64", (int64_t*)nullptr);
    f("uint64", (uint64_t*)nullptr);
    f("int128", (int128*)nullptr);
    f("uint128", (uint128*)nullptr);
    f("varuint32", (varuint32*)nullptr);
    f("varint32", (varint32*)nullptr);
    f("float32", (float*)nullptr);
    f("float64", (double*)nullptr);
    f("float128", (float128*)nullptr);
    f("time_point", (time_point*)nullptr);
    f("time_point_sec", (time_point_sec*)nullptr);
    f("block_timestamp_type", (block_timestamp*)nullptr);
    f("name", (name*)nullptr);
    f("bytes", (bytes*)nullptr);
    f("string", (std::string*)nullptr);
    f("checksum160", (checksum160*)nullptr);
    f("checksum256", (checksum256*)nullptr);
    f("checksum512", (checksum512*)nullptr);
    f("public_key", (public_key*)nullptr);
    f("private_key", (private_key*)nullptr);
    f("signature", (signature*)nullptr);
    f("symbol", (symbol*)nullptr);
    f("symbol_code", (symbol_code*)nullptr);
    f("asset", (asset*)nullptr);
}

template <typename T>
struct abi_serializer_impl : abi_serializer {
    ABIEOS_NODISCARD bool json_to_bin(jvalue_to_bin_state& state, bool allow_extensions, const abi_type* type,
                                      event_type event, bool start) const override {
        return ::abieos::json_to_bin((T*)nullptr, state, allow_extensions, type, event, start);
    }
    ABIEOS_NODISCARD bool json_to_bin(json_to_bin_state& state, bool allow_extensions, const abi_type* type,
                                      event_type event, bool start) const override {
        return ::abieos::json_to_bin((T*)nullptr, state, allow_extensions, type, event, start);
    }
    ABIEOS_NODISCARD bool bin_to_json(bin_to_json_state& state, bool allow_extensions, const abi_type* type,
                                      bool start) const override {
        return ::abieos::bin_to_json((T*)nullptr, state, allow_extensions, type, start);
    }
};

template <typename T>
inline constexpr auto abi_serializer_for = abi_serializer_impl<T>{};

///////////////////////////////////////////////////////////////////////////////
// abi handling
///////////////////////////////////////////////////////////////////////////////

struct abi_field {
    std::string name{};
    struct abi_type* type{};
};

struct abi_type {
    std::string name{};
    std::string alias_of_name{};
    const ::abieos::struct_def* struct_def{};
    const ::abieos::variant_def* variant_def{};
    abi_type* alias_of{};
    abi_type* optional_of{};
    abi_type* extension_of{};
    abi_type* array_of{};
    abi_type* base{};
    std::vector<abi_field> fields{};
    bool filled_struct{};
    bool filled_variant{};
    const abi_serializer* ser{};
};

struct contract {
    std::map<name, std::string> action_types;
    std::map<std::string, abi_type> abi_types;
};

template <int i>
bool ends_with(const std::string& s, const char (&suffix)[i]) {
    return s.size() >= i - 1 && !strcmp(s.c_str() + s.size() - (i - 1), suffix);
}

ABIEOS_NODISCARD inline bool get_type(abi_type*& result, std::string& error, std::map<std::string, abi_type>& abi_types,
                                      const std::string& name, int depth) {
    if (depth >= 32)
        return set_error(error, "abi recursion limit reached");
    auto it = abi_types.find(name);
    if (it == abi_types.end()) {
        if (ends_with(name, "?")) {
            abi_type type{name};
            if (!get_type(type.optional_of, error, abi_types, name.substr(0, name.size() - 1), depth + 1))
                return false;
            if (type.optional_of->optional_of || type.optional_of->array_of)
                return set_error(error, "optional (?) and array ([]) don't support nesting");
            if (type.optional_of->extension_of)
                return set_error(error, "optional (?) may not contain binary extensions ($)");
            type.ser = &abi_serializer_for<pseudo_optional>;
            result = &(abi_types[name] = std::move(type));
            return true;
        } else if (ends_with(name, "[]")) {
            abi_type type{name};
            if (!get_type(type.array_of, error, abi_types, name.substr(0, name.size() - 2), depth + 1))
                return false;
            if (type.array_of->array_of || type.array_of->optional_of)
                return set_error(error, "optional (?) and array ([]) don't support nesting");
            if (type.array_of->extension_of)
                return set_error(error, "array ([]) may not contain binary extensions ($)");
            type.ser = &abi_serializer_for<pseudo_array>;
            result = &(abi_types[name] = std::move(type));
            return true;
        } else if (ends_with(name, "$")) {
            abi_type type{name};
            if (!get_type(type.extension_of, error, abi_types, name.substr(0, name.size() - 1), depth + 1))
                return false;
            if (type.extension_of->extension_of)
                return set_error(error, "binary extensions ($) may not contain binary extensions ($)");
            type.ser = &abi_serializer_for<pseudo_extension>;
            result = &(abi_types[name] = std::move(type));
            return true;
        } else
            return set_error(error, "unknown type \"" + name + "\"");
    }
    if (it->second.alias_of) {
        result = it->second.alias_of;
        return true;
    }
    if (it->second.alias_of_name.empty()) {
        result = &it->second;
        return true;
    }
    if (!get_type(result, error, abi_types, it->second.alias_of_name, depth + 1))
        return false;
    it->second.alias_of = result;
    return true;
}

ABIEOS_NODISCARD inline bool fill_struct(std::map<std::string, abi_type>& abi_types, std::string& error, abi_type& type,
                                         int depth) {
    if (depth >= 32)
        return set_error(error, "abi recursion limit reached");
    if (type.filled_struct)
        return true;
    if (!type.struct_def)
        return set_error(error, "abi type \"" + type.name + "\" is not a struct");
    if (!type.struct_def->base.empty()) {
        abi_type* t;
        if (!get_type(t, error, abi_types, type.struct_def->base, depth + 1))
            return false;
        if (!fill_struct(abi_types, error, *t, depth + 1))
            return false;
        type.fields = t->fields;
    }
    for (auto& field : type.struct_def->fields) {
        abi_type* t;
        if (!get_type(t, error, abi_types, field.type, depth + 1))
            return false;
        type.fields.push_back(abi_field{field.name, t});
    }
    type.filled_struct = true;
    return true;
}

ABIEOS_NODISCARD inline bool fill_variant(std::map<std::string, abi_type>& abi_types, std::string& error,
                                          abi_type& type, int depth) {
    if (depth >= 32)
        return set_error(error, "abi recursion limit reached");
    if (type.filled_variant)
        return true;
    if (!type.variant_def)
        return set_error(error, "abi type \"" + type.name + "\" is not a variant");
    for (auto& types : type.variant_def->types) {
        abi_type* t;
        if (!get_type(t, error, abi_types, types, depth + 1))
            return false;
        type.fields.push_back(abi_field{types, t});
    }
    type.filled_variant = true;
    return true;
}

ABIEOS_NODISCARD inline bool fill_contract(contract& c, std::string& error, const abi_def& abi) {
    for (auto& a : abi.actions)
        c.action_types[a.name] = a.type;
    for_each_abi_type([&](const char* name, auto* p) {
        abi_type type{name};
        type.ser = &abi_serializer_for<std::decay_t<decltype(*p)>>;
        c.abi_types.insert({name, std::move(type)});
    });
    {
        abi_type type{"extended_asset"};
        abi_type *asset_type, *name_type;
        if (!get_type(asset_type, error, c.abi_types, "asset", 0) ||
            !get_type(name_type, error, c.abi_types, "name", 0))
            return false;
        type.fields.push_back(abi_field{"quantity", asset_type});
        type.fields.push_back(abi_field{"contract", name_type});
        type.filled_struct = true;
        type.ser = &abi_serializer_for<pseudo_object>;
        c.abi_types.insert({"extended_asset", std::move(type)});
    }

    for (auto& t : abi.types) {
        if (t.new_type_name.empty())
            return set_error(error, "abi has a type with a missing name");
        auto [_, inserted] = c.abi_types.insert({t.new_type_name, abi_type{t.new_type_name, t.type}});
        if (!inserted)
            return set_error(error, "abi redefines type \"" + t.new_type_name + "\"");
    }
    for (auto& s : abi.structs) {
        if (s.name.empty())
            return set_error(error, "abi has a struct with a missing name");
        abi_type type{s.name};
        type.struct_def = &s;
        type.ser = &abi_serializer_for<pseudo_object>;
        auto [_, inserted] = c.abi_types.insert({s.name, std::move(type)});
        if (!inserted)
            return set_error(error, "abi redefines type \"" + s.name + "\"");
    }
    for (auto& v : abi.variants.value) {
        if (v.name.empty())
            return set_error(error, "abi has a variant with a missing name");
        abi_type type{v.name};
        type.variant_def = &v;
        type.ser = &abi_serializer_for<pseudo_variant>;
        auto [_, inserted] = c.abi_types.insert({v.name, std::move(type)});
        if (!inserted)
            return set_error(error, "abi redefines type \"" + v.name + "\"");
    }
    for (auto& [_, t] : c.abi_types)
        if (!t.alias_of_name.empty())
            if (!get_type(t.alias_of, error, c.abi_types, t.alias_of_name, 0))
                return false;
    for (auto& [_, t] : c.abi_types) {
        if (t.struct_def) {
            if (!fill_struct(c.abi_types, error, t, 0))
                return false;
        } else if (t.variant_def) {
            if (!fill_variant(c.abi_types, error, t, 0))
                return false;
        }
    }
    for (auto& [_, t] : c.abi_types) {
        t.struct_def = nullptr;
        t.variant_def = nullptr;
        if (t.alias_of && t.alias_of->extension_of)
            return set_error(error, "can't use extensions ($) within typedefs");
    }
    return true;
}

///////////////////////////////////////////////////////////////////////////////
// json_to_bin (jvalue)
///////////////////////////////////////////////////////////////////////////////

ABIEOS_NODISCARD inline bool json_to_bin(std::vector<char>& bin, std::string& error, const abi_type* type,
                                         const jvalue& value) {
    jvalue_to_bin_state state{error, bin, &value};
    bool result = [&] {
        if (!type->ser->json_to_bin(state, true, type, get_event_type(value), true))
            return false;
        while (!state.stack.empty()) {
            auto& entry = state.stack.back();
            if (!entry.type->ser->json_to_bin(state, entry.allow_extensions, entry.type, get_event_type(*entry.value),
                                              false))
                return false;
        }
        return true;
    }();
    if (result)
        return true;
    std::string s;
    if (!state.stack.empty() && state.stack[0].type->filled_struct)
        s += state.stack[0].type->name;
    for (auto& entry : state.stack) {
        if (entry.type->array_of)
            s += "[" + std::to_string(entry.position) + "]";
        else if (entry.type->filled_struct) {
            if (entry.position >= 0 && entry.position < (int)entry.type->fields.size())
                s += "." + entry.type->fields[entry.position].name;
        } else if (entry.type->optional_of) {
            s += "<optional>";
        } else if (entry.type->filled_variant) {
            s += "<variant>";
        } else {
            s += "<?>";
        }
    }
    if (!s.empty())
        s += ": ";
    error = s + error;
    return false;
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_optional*, jvalue_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool) {
    if (event == event_type::received_null) {
        state.bin.push_back(0);
        return true;
    }
    state.bin.push_back(1);
    return type->optional_of->ser &&
           type->optional_of->ser->json_to_bin(state, allow_extensions, type->optional_of, event, true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_extension*, jvalue_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool) {
    return type->extension_of->ser &&
           type->extension_of->ser->json_to_bin(state, allow_extensions, type->extension_of, event, true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_object*, jvalue_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool start) {
    if (start) {
        if (!state.received_value || !std::holds_alternative<jobject>(state.received_value->value))
            return set_error(state.error, "expected object");
        if (trace_jvalue_to_bin)
            printf("%*s{ %d fields, allow_ex=%d\n", int(state.stack.size() * 4), "", int(type->fields.size()),
                   allow_extensions);
        state.stack.push_back({type, allow_extensions, state.received_value, -1});
        return true;
    }
    auto& stack_entry = state.stack.back();
    ++stack_entry.position;
    if (stack_entry.position == (int)type->fields.size()) {
        if (trace_jvalue_to_bin)
            printf("%*s}\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    auto& field = stack_entry.type->fields[stack_entry.position];
    auto& obj = std::get<jobject>(stack_entry.value->value);
    auto it = obj.find(field.name);
    if (trace_jvalue_to_bin)
        printf("%*sfield %d/%d: %s (event %d)\n", int(state.stack.size() * 4), "", int(stack_entry.position),
               int(type->fields.size()), std::string{field.name}.c_str(), (int)event);
    if (it == obj.end()) {
        if (field.type->extension_of && allow_extensions) {
            state.skipped_extension = true;
            return true;
        }
        stack_entry.position = -1;
        return set_error(state.error, "expected field \"" + field.name + "\"");
    }
    if (state.skipped_extension)
        return set_error(state.error, "unexpected field \"" + field.name + "\"");
    state.received_value = &it->second;
    return field.type->ser && field.type->ser->json_to_bin(state, allow_extensions && &field == &type->fields.back(),
                                                           field.type, get_event_type(it->second), true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_array*, jvalue_to_bin_state& state, bool, const abi_type* type,
                                         event_type event, bool start) {
    if (start) {
        if (!state.received_value || !std::holds_alternative<jarray>(state.received_value->value))
            return set_error(state.error, "expected array");
        if (trace_jvalue_to_bin)
            printf("%*s[ %d elements\n", int(state.stack.size() * 4), "",
                   int(std::get<jarray>(state.received_value->value).size()));
        push_varuint32(state.bin, std::get<jarray>(state.received_value->value).size());
        state.stack.push_back({type, false, state.received_value, -1});
        return true;
    }
    auto& stack_entry = state.stack.back();
    auto& arr = std::get<jarray>(stack_entry.value->value);
    ++stack_entry.position;
    if (stack_entry.position == (int)arr.size()) {
        if (trace_jvalue_to_bin)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    state.received_value = &arr[stack_entry.position];
    if (trace_jvalue_to_bin)
        printf("%*sitem (event %d)\n", int(state.stack.size() * 4), "", (int)event);
    return type->array_of->ser &&
           type->array_of->ser->json_to_bin(state, false, type->array_of, get_event_type(*state.received_value), true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_variant*, jvalue_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool start) {
    if (start) {
        if (!state.received_value || !std::holds_alternative<jarray>(state.received_value->value))
            return set_error(state.error, R"(expected variant: ["type", value])");
        auto& arr = std::get<jarray>(state.received_value->value);
        if (arr.size() != 2)
            return set_error(state.error, R"(expected variant: ["type", value])");
        if (!std::holds_alternative<std::string>(arr[0].value))
            return set_error(state.error, R"(expected variant: ["type", value])");
        auto& typeName = std::get<std::string>(arr[0].value);
        if (trace_jvalue_to_bin)
            printf("%*s[ variant %s\n", int(state.stack.size() * 4), "", typeName.c_str());
        state.stack.push_back({type, allow_extensions, state.received_value, 0});
        return true;
    }
    auto& stack_entry = state.stack.back();
    auto& arr = std::get<jarray>(stack_entry.value->value);
    if (stack_entry.position == 0) {
        auto& typeName = std::get<std::string>(arr[0].value);
        auto it = std::find_if(stack_entry.type->fields.begin(), stack_entry.type->fields.end(),
                               [&](auto& field) { return field.name == typeName; });
        if (it == stack_entry.type->fields.end())
            return set_error(state.error, "type is not valid for this variant");
        push_varuint32(state.bin, it - stack_entry.type->fields.begin());
        state.received_value = &arr[++stack_entry.position];
        return it->type->ser && it->type->ser->json_to_bin(state, allow_extensions, it->type,
                                                           get_event_type(*state.received_value), true);
    } else {
        if (trace_jvalue_to_bin)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
}

template <typename T>
ABIEOS_NODISCARD auto json_to_bin(T*, jvalue_to_bin_state& state, bool, const abi_type*, event_type event, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool> {

    T x;
    if (!json_to_number(x, state, event))
        return false;
    push_raw(state.bin, x);
    return true;
}

ABIEOS_NODISCARD inline bool json_to_bin(std::string*, jvalue_to_bin_state& state, bool, const abi_type*,
                                         event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_jvalue_to_bin)
            printf("%*sstring: %s\n", int(state.stack.size() * 4), "", s.c_str());
        push_varuint32(state.bin, s.size());
        state.bin.insert(state.bin.end(), s.begin(), s.end());
        return true;
    } else
        return set_error(state.error, "expected string");
}

///////////////////////////////////////////////////////////////////////////////
// json_to_bin
///////////////////////////////////////////////////////////////////////////////

ABIEOS_NODISCARD inline bool receive_event(struct json_to_bin_state& state, event_type event, bool start) {
    if (state.stack.empty())
        return false;
    if (trace_json_to_bin_event)
        printf("(event %d %d)\n", (int)event, start);
    auto entry = state.stack.back();
    auto* type = entry.type;
    if (start)
        state.stack.clear();
    if (state.stack.size() > max_stack_size)
        return set_error(state, "recursion limit reached");
    return type->ser && type->ser->json_to_bin(state, entry.allow_extensions, type, event, start);
}

ABIEOS_NODISCARD inline bool json_to_bin(std::vector<char>& bin, std::string& error, const abi_type* type,
                                         std::string_view json) {
    std::string mutable_json{json};
    mutable_json.push_back(0);
    json_to_bin_state state{.error = error};
    state.stack.push_back({type, true});
    rapidjson::Reader reader;
    rapidjson::InsituStringStream ss(mutable_json.data());

    if (!reader.Parse<rapidjson::kParseValidateEncodingFlag | rapidjson::kParseIterativeFlag |
                      rapidjson::kParseNumbersAsStringsFlag>(ss, state)) {
        if (error.empty())
            error = "failed to parse";
        std::string s;
        if (!state.stack.empty() && state.stack[0].type->filled_struct)
            s += state.stack[0].type->name;
        for (auto& entry : state.stack) {
            if (entry.type->array_of)
                s += "[" + std::to_string(entry.position) + "]";
            else if (entry.type->filled_struct) {
                if (entry.position >= 0 && entry.position < (int)entry.type->fields.size())
                    s += "." + entry.type->fields[entry.position].name;
            } else if (entry.type->optional_of) {
                s += "<optional>";
            } else if (entry.type->filled_variant) {
                s += "<variant>";
            } else {
                s += "<?>";
            }
        }
        if (!s.empty())
            s += ": ";
        error = s + error;
        return false;
    }

    size_t pos = 0;
    for (auto& insertion : state.size_insertions) {
        bin.insert(bin.end(), state.bin.begin() + pos, state.bin.begin() + insertion.position);
        push_varuint32(bin, insertion.size);
        pos = insertion.position;
    }
    bin.insert(bin.end(), state.bin.begin() + pos, state.bin.end());
    return true;
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_optional*, json_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool) {
    if (event == event_type::received_null) {
        state.bin.push_back(0);
        return true;
    }
    state.bin.push_back(1);
    return type->optional_of->ser &&
           type->optional_of->ser->json_to_bin(state, allow_extensions, type->optional_of, event, true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_extension*, json_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool) {
    return type->extension_of->ser &&
           type->extension_of->ser->json_to_bin(state, allow_extensions, type->extension_of, event, true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_object*, json_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool start) {
    if (start) {
        if (event != event_type::received_start_object)
            return set_error(state, "expected object");
        if (trace_json_to_bin)
            printf("%*s{ %d fields, allow_ex=%d\n", int(state.stack.size() * 4), "", int(type->fields.size()),
                   allow_extensions);
        state.stack.push_back({type, allow_extensions});
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (event == event_type::received_end_object) {
        if (stack_entry.position + 1 != (ptrdiff_t)type->fields.size()) {
            auto& field = type->fields[stack_entry.position + 1];
            if (!field.type->extension_of || !allow_extensions) {
                stack_entry.position = -1;
                return set_error(state, "expected field \"" + field.name + "\"");
            }
            ++stack_entry.position;
            state.skipped_extension = true;
            return true;
        }
        if (trace_json_to_bin)
            printf("%*s}\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    if (event == event_type::received_key) {
        if (++stack_entry.position >= (ptrdiff_t)type->fields.size() || state.skipped_extension)
            return set_error(state, "unexpected field \"" + state.received_data.key + "\"");
        auto& field = type->fields[stack_entry.position];
        if (state.received_data.key != field.name) {
            stack_entry.position = -1;
            return set_error(state, "expected field \"" + field.name + "\"");
        }
        return true;
    } else {
        auto& field = type->fields[stack_entry.position];
        if (trace_json_to_bin)
            printf("%*sfield %d/%d: %s (event %d)\n", int(state.stack.size() * 4), "", int(stack_entry.position),
                   int(type->fields.size()), std::string{field.name}.c_str(), (int)event);
        return field.type->ser &&
               field.type->ser->json_to_bin(state, allow_extensions && &field == &type->fields.back(), field.type,
                                            event, true);
    }
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_array*, json_to_bin_state& state, bool, const abi_type* type,
                                         event_type event, bool start) {
    if (start) {
        if (event != event_type::received_start_array)
            return set_error(state, "expected array");
        if (trace_json_to_bin)
            printf("%*s[\n", int(state.stack.size() * 4), "");
        state.stack.push_back({type, false});
        state.stack.back().size_insertion_index = state.size_insertions.size();
        state.size_insertions.push_back({state.bin.size()});
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (event == event_type::received_end_array) {
        if (trace_json_to_bin)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.size_insertions[stack_entry.size_insertion_index].size = stack_entry.position + 1;
        state.stack.pop_back();
        return true;
    }
    ++stack_entry.position;
    if (trace_json_to_bin)
        printf("%*sitem (event %d)\n", int(state.stack.size() * 4), "", (int)event);
    return type->array_of->ser && type->array_of->ser->json_to_bin(state, false, type->array_of, event, true);
}

ABIEOS_NODISCARD inline bool json_to_bin(pseudo_variant*, json_to_bin_state& state, bool allow_extensions,
                                         const abi_type* type, event_type event, bool start) {
    if (start) {
        if (event != event_type::received_start_array)
            return set_error(state, R"(expected variant: ["type", value])");
        if (trace_json_to_bin)
            printf("%*s[ variant\n", int(state.stack.size() * 4), "");
        state.stack.push_back({type, allow_extensions});
        return true;
    }
    auto& stack_entry = state.stack.back();
    ++stack_entry.position;
    if (event == event_type::received_end_array) {
        if (stack_entry.position != 2)
            return set_error(state, R"(expected variant: ["type", value])");
        if (trace_json_to_bin)
            printf("%*s]\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        return true;
    }
    if (stack_entry.position == 0) {
        if (event == event_type::received_string) {
            auto& typeName = state.get_string();
            if (trace_json_to_bin)
                printf("%*stype: %s\n", int(state.stack.size() * 4), "", typeName.c_str());
            auto it = std::find_if(stack_entry.type->fields.begin(), stack_entry.type->fields.end(),
                                   [&](auto& field) { return field.name == typeName; });
            if (it == stack_entry.type->fields.end())
                return set_error(state, "type is not valid for this variant");
            stack_entry.variant_type_index = it - stack_entry.type->fields.begin();
            push_varuint32(state.bin, stack_entry.variant_type_index);
            return true;
        } else
            return set_error(state, R"(expected variant: ["type", value])");
    } else if (stack_entry.position == 1) {
        auto& field = stack_entry.type->fields[stack_entry.variant_type_index];
        return field.type->ser && field.type->ser->json_to_bin(state, allow_extensions, field.type, event, true);
    } else {
        return set_error(state, R"(expected variant: ["type", value])");
    }
}

template <typename T>
ABIEOS_NODISCARD auto json_to_bin(T*, json_to_bin_state& state, bool, const abi_type*, event_type event, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool> {

    T x;
    if (!json_to_number(x, state, event))
        return false;
    push_raw(state.bin, x);
    return true;
}

ABIEOS_NODISCARD inline bool json_to_bin(std::string*, json_to_bin_state& state, bool, const abi_type*,
                                         event_type event, bool start) {
    if (event == event_type::received_string) {
        auto& s = state.get_string();
        if (trace_json_to_bin)
            printf("%*sstring: %s\n", int(state.stack.size() * 4), "", s.c_str());
        push_varuint32(state.bin, s.size());
        state.bin.insert(state.bin.end(), s.begin(), s.end());
        return true;
    } else
        return set_error(state, "expected string");
}

///////////////////////////////////////////////////////////////////////////////
// bin_to_json
///////////////////////////////////////////////////////////////////////////////

ABIEOS_NODISCARD inline bool bin_to_json(input_buffer& bin, std::string& error, const abi_type* type,
                                         std::string& dest) {
    if (!type->ser)
        return false;
    rapidjson::StringBuffer buffer{};
    rapidjson::Writer<rapidjson::StringBuffer> writer{buffer};
    bin_to_json_state state{bin, error, writer};
    if (!type->ser || !type->ser->bin_to_json(state, true, type, true))
        return false;
    while (!state.stack.empty()) {
        auto& entry = state.stack.back();
        if (!entry.type->ser || !entry.type->ser->bin_to_json(state, entry.allow_extensions, entry.type, false))
            return false;
        if (state.stack.size() > max_stack_size)
            return set_error(state, "recursion limit reached");
    }
    dest = buffer.GetString();
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_json(pseudo_optional*, bin_to_json_state& state, bool allow_extensions,
                                         const abi_type* type, bool) {
    bool present;
    if (!read_raw(state.bin, state.error, present))
        return false;
    if (present)
        return type->optional_of->ser &&
               type->optional_of->ser->bin_to_json(state, allow_extensions, type->optional_of, true);
    state.writer.Null();
    return true;
}

ABIEOS_NODISCARD inline bool bin_to_json(pseudo_extension*, bin_to_json_state& state, bool allow_extensions,
                                         const abi_type* type, bool) {
    return type->extension_of->ser &&
           type->extension_of->ser->bin_to_json(state, allow_extensions, type->extension_of, true);
}

ABIEOS_NODISCARD inline bool bin_to_json(pseudo_object*, bin_to_json_state& state, bool allow_extensions,
                                         const abi_type* type, bool start) {
    if (start) {
        if (trace_bin_to_json)
            printf("%*s{ %d fields\n", int(state.stack.size() * 4), "", int(type->fields.size()));
        state.stack.push_back({type, allow_extensions});
        state.writer.StartObject();
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (++stack_entry.position < (ptrdiff_t)type->fields.size()) {
        auto& field = type->fields[stack_entry.position];
        if (trace_bin_to_json)
            printf("%*sfield %d/%d: %s\n", int(state.stack.size() * 4), "", int(stack_entry.position),
                   int(type->fields.size()), std::string{field.name}.c_str());
        if (state.bin.pos == state.bin.end && field.type->extension_of && allow_extensions) {
            state.skipped_extension = true;
            return true;
        }
        state.writer.Key(field.name.c_str(), field.name.length());
        return field.type->ser && field.type->ser->bin_to_json(
                                      state, allow_extensions && &field == &type->fields.back(), field.type, true);
    } else {
        if (trace_bin_to_json)
            printf("%*s}\n", int((state.stack.size() - 1) * 4), "");
        state.stack.pop_back();
        state.writer.EndObject();
        return true;
    }
}

ABIEOS_NODISCARD inline bool bin_to_json(pseudo_array*, bin_to_json_state& state, bool, const abi_type* type,
                                         bool start) {
    if (start) {
        state.stack.push_back({type, false});
        if (!read_varuint32(state.bin, state.error, state.stack.back().array_size))
            return false;
        if (trace_bin_to_json)
            printf("%*s[ %d items\n", int(state.stack.size() * 4), "", int(state.stack.back().array_size));
        state.writer.StartArray();
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (++stack_entry.position < (ptrdiff_t)stack_entry.array_size) {
        if (trace_bin_to_json)
            printf("%*sitem %d/%d %p %s\n", int(state.stack.size() * 4), "", int(stack_entry.position),
                   int(stack_entry.array_size), type->array_of->ser, type->array_of->name.c_str());
        return type->array_of->ser && type->array_of->ser->bin_to_json(state, false, type->array_of, true);
    } else {
        if (trace_bin_to_json)
            printf("%*s]\n", int((state.stack.size()) * 4), "");
        state.stack.pop_back();
        state.writer.EndArray();
        return true;
    }
}

ABIEOS_NODISCARD inline bool bin_to_json(pseudo_variant*, bin_to_json_state& state, bool allow_extensions,
                                         const abi_type* type, bool start) {
    if (start) {
        state.stack.push_back({type, allow_extensions});
        if (trace_bin_to_json)
            printf("%*s[ variant\n", int(state.stack.size() * 4), "");
        state.writer.StartArray();
        return true;
    }
    auto& stack_entry = state.stack.back();
    if (++stack_entry.position == 0) {
        uint32_t index;
        if (!read_varuint32(state.bin, state.error, index))
            return false;
        if (index >= stack_entry.type->fields.size())
            return set_error(state, "invalid variant type index");
        auto& f = stack_entry.type->fields[index];
        state.writer.String(f.name.c_str());
        return f.type->ser &&
               f.type->ser->bin_to_json(state, allow_extensions && stack_entry.allow_extensions, f.type, true);
    } else {
        if (trace_bin_to_json)
            printf("%*s]\n", int((state.stack.size()) * 4), "");
        state.stack.pop_back();
        state.writer.EndArray();
        return true;
    }
}

template <typename T>
ABIEOS_NODISCARD auto bin_to_json(T*, bin_to_json_state& state, bool, const abi_type*, bool start)
    -> std::enable_if_t<std::is_arithmetic_v<T>, bool> {

    T v;
    if (!read_raw(state.bin, state.error, v))
        return false;
    if constexpr (std::is_same_v<T, bool>) {
        return state.writer.Bool(v);
    } else if constexpr (std::is_floating_point_v<T>) {
        return state.writer.Double(v);
    } else if constexpr (sizeof(T) == 8) {
        auto s = std::to_string(v);
        return state.writer.String(s.c_str(), s.size());
    } else if constexpr (std::is_signed_v<T>) {
        return state.writer.Int64(v);
    } else {
        return state.writer.Uint64(v);
    }
}

ABIEOS_NODISCARD inline bool bin_to_json(std::string*, bin_to_json_state& state, bool, const abi_type*, bool start) {
    std::string s;
    if (!read_string(state.bin, state.error, s))
        return false;
    return state.writer.String(s.c_str(), s.size());
}

inline namespace literals {
inline constexpr name operator""_n(const char* s, size_t) { return name{s}; }
} // namespace literals

} // namespace abieos
