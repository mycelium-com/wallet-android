// copyright defined in abieos/LICENSE.txt

#include "abieos.h"
#include "abieos.hpp"
#include "fuzzer.hpp"
#include <boost/algorithm/hex.hpp>
#include <stdexcept>
#include <stdio.h>
#include <string>
#include <vector>

inline const bool generate_corpus = false;

const char tokenHexAbi[] = "0e656f73696f3a3a6162692f312e30010c6163636f756e745f6e616d65046e61"
                           "6d6505087472616e7366657200040466726f6d0c6163636f756e745f6e616d65"
                           "02746f0c6163636f756e745f6e616d65087175616e7469747905617373657404"
                           "6d656d6f06737472696e67066372656174650002066973737565720c6163636f"
                           "756e745f6e616d650e6d6178696d756d5f737570706c79056173736574056973"
                           "737565000302746f0c6163636f756e745f6e616d65087175616e746974790561"
                           "73736574046d656d6f06737472696e67076163636f756e7400010762616c616e"
                           "63650561737365740e63757272656e63795f7374617473000306737570706c79"
                           "0561737365740a6d61785f737570706c79056173736574066973737565720c61"
                           "63636f756e745f6e616d6503000000572d3ccdcd087472616e73666572000000"
                           "000000a531760569737375650000000000a86cd4450663726561746500020000"
                           "00384f4d113203693634010863757272656e6379010675696e74363407616363"
                           "6f756e740000000000904dc603693634010863757272656e6379010675696e74"
                           "36340e63757272656e63795f7374617473000000";

const char testHexAbi[] = "0E656F73696F3A3A6162692F312E310004027331000102783104696E74380273"
                          "32000202793105696E74382402793205696E7438240273330003027A3105696E"
                          "743824027A3203763124027A3303733224027334000202613106696E74383F24"
                          "02623107696E74385B5D240000000000010276310304696E7438027331027332";

const char testAbi[] = R"({
    "version": "eosio::abi/1.1",
    "structs": [
        {
            "name": "s1",
            "fields": [
                {
                    "name": "x1",
                    "type": "int8"
                }
            ]
        },
        {
            "name": "s2",
            "fields": [
                {
                    "name": "y1",
                    "type": "int8$"
                },
                {
                    "name": "y2",
                    "type": "int8$"
                }
            ]
        },
        {
            "name": "s3",
            "fields": [
                {
                    "name": "z1",
                    "type": "int8$"
                },
                {
                    "name": "z2",
                    "type": "v1$"
                },
                {
                    "name": "z3",
                    "type": "s2$"
                }
            ]
        },
        {
            "name": "s4",
            "fields": [
                {
                    "name": "a1",
                    "type": "int8?$"
                },
                {
                    "name": "b1",
                    "type": "int8[]$"
                }
            ]
        },
        {
            "name": "s5",
            "fields": [
                {
                    "name": "x1",
                    "type": "int8"
                },
                {
                    "name": "x2",
                    "type": "int8"
                },
                {
                    "name": "x3",
                    "type": "s6"
                }
            ]
        },
        {
            "name": "s6",
            "fields": [
                {
                    "name": "c1",
                    "type": "int8"
                },
                {
                    "name": "c2",
                    "type": "s5[]"
                },
                {
                    "name": "c3",
                    "type": "int8"
                }
            ]
        }
    ],
    "variants": [
        {
            "name": "v1",
            "types": ["int8","s1","s2"]
        }
    ]
})";

const char transactionAbi[] = R"({
    "version": "eosio::abi/1.0",
    "types": [
        {
            "new_type_name": "account_name",
            "type": "name"
        },
        {
            "new_type_name": "action_name",
            "type": "name"
        },
        {
            "new_type_name": "permission_name",
            "type": "name"
        }
    ],
    "structs": [
        {
            "name": "permission_level",
            "base": "",
            "fields": [
                {
                    "name": "actor",
                    "type": "account_name"
                },
                {
                    "name": "permission",
                    "type": "permission_name"
                }
            ]
        },
        {
            "name": "action",
            "base": "",
            "fields": [
                {
                    "name": "account",
                    "type": "account_name"
                },
                {
                    "name": "name",
                    "type": "action_name"
                },
                {
                    "name": "authorization",
                    "type": "permission_level[]"
                },
                {
                    "name": "data",
                    "type": "bytes"
                }
            ]
        },
        {
            "name": "extension",
            "base": "",
            "fields": [
                {
                    "name": "type",
                    "type": "uint16"
                },
                {
                    "name": "data",
                    "type": "bytes"
                }
            ]
        },
        {
            "name": "transaction_header",
            "base": "",
            "fields": [
                {
                    "name": "expiration",
                    "type": "time_point_sec"
                },
                {
                    "name": "ref_block_num",
                    "type": "uint16"
                },
                {
                    "name": "ref_block_prefix",
                    "type": "uint32"
                },
                {
                    "name": "max_net_usage_words",
                    "type": "varuint32"
                },
                {
                    "name": "max_cpu_usage_ms",
                    "type": "uint8"
                },
                {
                    "name": "delay_sec",
                    "type": "varuint32"
                }
            ]
        },
        {
            "name": "transaction",
            "base": "transaction_header",
            "fields": [
                {
                    "name": "context_free_actions",
                    "type": "action[]"
                },
                {
                    "name": "actions",
                    "type": "action[]"
                },
                {
                    "name": "transaction_extensions",
                    "type": "extension[]"
                }
            ]
        }
    ]
})";

std::string string_to_hex(const std::string& s) {
    std::string result;
    uint8_t size = s.size();
    boost::algorithm::hex(&size, &size + 1, std::back_inserter(result));
    boost::algorithm::hex(s.begin(), s.end(), std::back_inserter(result));
    return result;
}

template <typename T>
T check(T value, const char* msg = "") {
    if (!value)
        throw std::runtime_error(std::string{msg} + " failed");
    return value;
}

template <typename T>
T check_context(abieos_context* context, T value) {
    if (!value)
        throw std::runtime_error(abieos_get_error(context));
    return value;
}

void run_check_type(abieos_context* context, uint64_t contract, const char* type, const char* data,
                    const char* expected = nullptr, bool check_ordered = true) {
    if (!expected)
        expected = data;
    // printf("%s %s\n", type, data);
    check_context(context, abieos_json_to_bin_reorderable(context, contract, type, data));
    std::string reorderable_hex = check_context(context, abieos_get_bin_hex(context));
    if (check_ordered) {
        check_context(context, abieos_json_to_bin(context, contract, type, data));
        std::string ordered_hex = check_context(context, abieos_get_bin_hex(context));
        if (reorderable_hex != ordered_hex)
            throw std::runtime_error("mismatch between reorderable_hex, ordered_hex");
    }
    // printf("%s\n", reorderable_hex.c_str());
    std::string result = check_context(context, abieos_hex_to_json(context, contract, type, reorderable_hex.c_str()));
    // printf("%s\n", result.c_str());
    printf("%s %s %s %s\n", type, data, reorderable_hex.c_str(), result.c_str());
    if (result != expected)
        throw std::runtime_error("mismatch");
}

template <typename F>
void check_except(const std::string& s, F f) {
    bool ok = false;
    try {
        f();
    } catch (std::exception& e) {
        if (e.what() == s)
            ok = true;
        else
            throw std::runtime_error("expected exception: " + s + " got: " + e.what());
    }
    if (!ok)
        throw std::runtime_error("expected exception: " + s);
}

template <typename F>
void check_error(abieos_context* context, const std::string& s, F f) {
    check_except(s, [&] { check_context(context, f()); });
}

void check_types() {
    auto context = check(abieos_create());
    auto token = check_context(context, abieos_string_to_name(context, "eosio.token"));
    auto testAbiName = check_context(context, abieos_string_to_name(context, "test.abi"));
    auto testHexAbiName = check_context(context, abieos_string_to_name(context, "test.hex"));
    check_context(context, abieos_set_abi(context, 0, transactionAbi));
    check_context(context, abieos_set_abi_hex(context, token, tokenHexAbi));
    check_context(context, abieos_set_abi(context, testAbiName, testAbi));
    check_context(context, abieos_set_abi_hex(context, testHexAbiName, testHexAbi));

    int next_id = 0;
    auto write_corpus = [&](bool abi_is_bin, uint8_t operation, uint64_t contract, abieos::input_buffer abi,
                            abieos::input_buffer type, abieos::input_buffer data) {
        fuzzer_header header;
        header.abi_is_bin = abi_is_bin;
        header.operation = operation;
        header.contract = contract;
        header.abi_size = abi.end - abi.pos;
        header.type_size = type.end - type.pos;

        std::vector<char> v;
        v.insert(v.end(), (const char*)(&header), (const char*)(&header + 1));
        v.insert(v.end(), abi.pos, abi.end);
        v.insert(v.end(), type.pos, type.end);
        v.insert(v.end(), data.pos, data.end);

        auto f = fopen(("corpus/" + std::to_string(next_id++)).c_str(), "wb");
        if (!f)
            return;
        fwrite(v.data(), v.size(), 1, f);
        fclose(f);
    };

    auto check_type = [&](abieos_context* context, uint64_t contract, const char* type, const char* data,
                          const char* expected = nullptr, bool check_ordered = true) {
        if (!generate_corpus)
            return run_check_type(context, contract, type, data, expected, check_ordered);

        bool abi_is_bin;
        std::vector<char> abi;
        if (contract == 0) {
            abi_is_bin = false;
            abi = {transactionAbi, transactionAbi + strlen(transactionAbi)};
        } else if (contract == token) {
            abi_is_bin = true;
            boost::algorithm::unhex(tokenHexAbi, tokenHexAbi + strlen(tokenHexAbi), std::back_inserter(abi));
        } else if (contract == testAbiName) {
            abi_is_bin = false;
            abi = {testAbi, testAbi + strlen(testAbi)};
        } else if (contract == testHexAbiName) {
            abi_is_bin = true;
            boost::algorithm::unhex(testHexAbi, testHexAbi + strlen(testHexAbi), std::back_inserter(abi));
        } else {
            throw std::runtime_error("missing case in check_type");
        }
        abi.push_back(0);

        write_corpus(abi_is_bin, fuzzer_json_to_bin, contract, {abi.data(), abi.data() + abi.size()},
                     {type, type + strlen(type) + 1}, {data, data + strlen(data) + 1});

        check_context(context, abieos_json_to_bin_reorderable(context, contract, type, data));
        std::string hex = check_context(context, abieos_get_bin_hex(context));

        write_corpus(abi_is_bin, fuzzer_hex_to_json, contract, {abi.data(), abi.data() + abi.size()},
                     {type, type + strlen(type) + 1}, {hex.c_str(), hex.c_str() + hex.size() + 1});
    };

    check_error(context, "no data", [&] { return abieos_set_abi_hex(context, 8, ""); });
    check_error(context, "unsupported abi version", [&] { return abieos_set_abi_hex(context, 8, "00"); });
    check_error(context, "unsupported abi version",
                [&] { return abieos_set_abi_hex(context, 8, string_to_hex("eosio::abi/9.0").c_str()); });
    check_error(context, "read past end",
                [&] { return abieos_set_abi_hex(context, 8, string_to_hex("eosio::abi/1.0").c_str()); });
    check_error(context, "read past end",
                [&] { return abieos_set_abi_hex(context, 8, string_to_hex("eosio::abi/1.1").c_str()); });

    check_error(context, "unsupported abi version",
                [&] { return abieos_set_abi(context, 8, R"({"version":"eosio::abi/9.0"})"); });
    abieos_set_abi(context, 8, R"({"version":"eosio::abi/1.0"})");
    abieos_set_abi(context, 8, R"({"version":"eosio::abi/1.1"})");

    check_type(context, 0, "bool", R"(true)");
    check_type(context, 0, "bool", R"(false)");
    check_error(context, "read past end", [&] { return abieos_hex_to_json(context, 0, "bool", ""); });
    check_error(context, "failed to parse", [&] { return abieos_json_to_bin(context, 0, "bool", R"(trues)"); });
    check_error(context, "expected number or boolean",
                [&] { return abieos_json_to_bin(context, 0, "bool", R"(null)"); });
    check_error(context, "invalid number", [&] { return abieos_json_to_bin(context, 0, "bool", R"("foo")"); });
    check_type(context, 0, "int8", R"(0)");
    check_type(context, 0, "int8", R"(127)");
    check_type(context, 0, "int8", R"(-128)");
    check_type(context, 0, "uint8", R"(0)");
    check_type(context, 0, "uint8", R"(1)");
    check_type(context, 0, "uint8", R"(254)");
    check_type(context, 0, "uint8", R"(255)");
    check_error(context, "number is out of range", [&] { return abieos_json_to_bin(context, 0, "int8", "128"); });
    check_error(context, "number is out of range", [&] { return abieos_json_to_bin(context, 0, "int8", "-129"); });
    check_error(context, "expected non-negative number", [&] { return abieos_json_to_bin(context, 0, "uint8", "-1"); });
    check_error(context, "number is out of range", [&] { return abieos_json_to_bin(context, 0, "uint8", "256"); });
    check_type(context, 0, "uint8[]", R"([])");
    check_type(context, 0, "uint8[]", R"([10])");
    check_type(context, 0, "uint8[]", R"([10,9])");
    check_type(context, 0, "uint8[]", R"([10,9,8])");
    check_type(context, 0, "int16", R"(0)");
    check_type(context, 0, "int16", R"(32767)");
    check_type(context, 0, "int16", R"(-32768)");
    check_error(context, "read past end", [&] { return abieos_hex_to_json(context, 0, "int16", "01"); });
    check_type(context, 0, "uint16", R"(0)");
    check_type(context, 0, "uint16", R"(65535)");
    check_error(context, "number is out of range", [&] { return abieos_json_to_bin(context, 0, "int16", "32768"); });
    check_error(context, "number is out of range", [&] { return abieos_json_to_bin(context, 0, "int16", "-32769"); });
    check_error(context, "expected non-negative number",
                [&] { return abieos_json_to_bin(context, 0, "uint16", "-1"); });
    check_error(context, "number is out of range", [&] { return abieos_json_to_bin(context, 0, "uint16", "655356"); });
    check_type(context, 0, "int32", R"(0)");
    check_type(context, 0, "int32", R"(2147483647)");
    check_type(context, 0, "int32", R"(-2147483648)");
    check_type(context, 0, "uint32", R"(0)");
    check_type(context, 0, "uint32", R"(4294967295)");
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "int32", "2147483648"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "int32", "-2147483649"); });
    check_error(context, "expected non-negative number",
                [&] { return abieos_json_to_bin(context, 0, "uint32", "-1"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "uint32", "4294967296"); });
    check_type(context, 0, "int64", R"(0)", R"("0")");
    check_type(context, 0, "int64", R"(1)", R"("1")");
    check_type(context, 0, "int64", R"(-1)", R"("-1")");
    check_type(context, 0, "int64", R"("0")");
    check_type(context, 0, "int64", R"("9223372036854775807")");
    check_type(context, 0, "int64", R"("-9223372036854775808")");
    check_type(context, 0, "uint64", R"("0")");
    check_type(context, 0, "uint64", R"("18446744073709551615")");
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "int64", "9223372036854775808"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "int64", "-9223372036854775809"); });
    check_error(context, "expected non-negative number",
                [&] { return abieos_json_to_bin(context, 0, "uint64", "-1"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "uint64", "18446744073709551616"); });
    check_type(context, 0, "int128", R"("0")");
    check_type(context, 0, "int128", R"("1")");
    check_type(context, 0, "int128", R"("-1")");
    check_type(context, 0, "int128", R"("18446744073709551615")");
    check_type(context, 0, "int128", R"("-18446744073709551615")");
    check_type(context, 0, "int128", R"("170141183460469231731687303715884105727")");
    check_type(context, 0, "int128", R"("-170141183460469231731687303715884105727")");
    check_type(context, 0, "int128", R"("-170141183460469231731687303715884105728")");
    check_type(context, 0, "uint128", R"("0")");
    check_type(context, 0, "uint128", R"("1")");
    check_type(context, 0, "uint128", R"("18446744073709551615")");
    check_type(context, 0, "uint128", R"("340282366920938463463374607431768211454")");
    check_type(context, 0, "uint128", R"("340282366920938463463374607431768211455")");
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "int128", "170141183460469231731687303715884105728"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "int128", "-170141183460469231731687303715884105729"); });
    check_error(context, "expected string containing int128",
                [&] { return abieos_json_to_bin(context, 0, "int128", "true"); });
    check_error(context, "invalid number", [&] { return abieos_json_to_bin(context, 0, "uint128", "-1"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "uint128", "340282366920938463463374607431768211456"); });
    check_error(context, "expected string containing uint128",
                [&] { return abieos_json_to_bin(context, 0, "uint128", "true"); });
    check_type(context, 0, "varuint32", R"(0)");
    check_type(context, 0, "varuint32", R"(127)");
    check_type(context, 0, "varuint32", R"(128)");
    check_type(context, 0, "varuint32", R"(129)");
    check_type(context, 0, "varuint32", R"(16383)");
    check_type(context, 0, "varuint32", R"(16384)");
    check_type(context, 0, "varuint32", R"(16385)");
    check_type(context, 0, "varuint32", R"(2097151)");
    check_type(context, 0, "varuint32", R"(2097152)");
    check_type(context, 0, "varuint32", R"(2097153)");
    check_type(context, 0, "varuint32", R"(268435455)");
    check_type(context, 0, "varuint32", R"(268435456)");
    check_type(context, 0, "varuint32", R"(268435457)");
    check_type(context, 0, "varuint32", R"(4294967294)");
    check_type(context, 0, "varuint32", R"(4294967295)");
    check_type(context, 0, "varint32", R"(0)");
    check_type(context, 0, "varint32", R"(-1)");
    check_type(context, 0, "varint32", R"(1)");
    check_type(context, 0, "varint32", R"(-2)");
    check_type(context, 0, "varint32", R"(2)");
    check_type(context, 0, "varint32", R"(-2147483647)");
    check_type(context, 0, "varint32", R"(2147483647)");
    check_type(context, 0, "varint32", R"(-2147483648)");
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "varint32", "2147483648"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "varint32", "-2147483649"); });
    check_error(context, "expected non-negative number",
                [&] { return abieos_json_to_bin(context, 0, "varuint32", "-1"); });
    check_error(context, "number is out of range",
                [&] { return abieos_json_to_bin(context, 0, "varuint32", "4294967296"); });
    check_type(context, 0, "float32", R"(0.0)");
    check_type(context, 0, "float32", R"(0.125)");
    check_type(context, 0, "float32", R"(-0.125)");
    check_type(context, 0, "float64", R"(0.0)");
    check_type(context, 0, "float64", R"(0.125)");
    check_type(context, 0, "float64", R"(-0.125)");
    check_type(context, 0, "float128", R"("00000000000000000000000000000000")");
    check_type(context, 0, "float128", R"("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")");
    check_type(context, 0, "float128", R"("12345678ABCDEF12345678ABCDEF1234")");
    check_type(context, 0, "time_point_sec", R"("1970-01-01T00:00:00.000")");
    check_type(context, 0, "time_point_sec", R"("2018-06-15T19:17:47.000")");
    check_type(context, 0, "time_point_sec", R"("2030-06-15T19:17:47.000")");
    check_error(context, "expected string containing time_point_sec",
                [&] { return abieos_json_to_bin(context, 0, "time_point_sec", "true"); });
    check_type(context, 0, "time_point", R"("1970-01-01T00:00:00.000")");
    check_type(context, 0, "time_point", R"("1970-01-01T00:00:00.001")");
    check_type(context, 0, "time_point", R"("1970-01-01T00:00:00.002")");
    check_type(context, 0, "time_point", R"("1970-01-01T00:00:00.010")");
    check_type(context, 0, "time_point", R"("1970-01-01T00:00:00.100")");
    check_type(context, 0, "time_point", R"("2018-06-15T19:17:47.000")");
    check_type(context, 0, "time_point", R"("2018-06-15T19:17:47.999")");
    check_type(context, 0, "time_point", R"("2030-06-15T19:17:47.999")");
    check_error(context, "expected string containing time_point",
                [&] { return abieos_json_to_bin(context, 0, "time_point", "true"); });
    check_type(context, 0, "block_timestamp_type", R"("2000-01-01T00:00:00.000")");
    check_type(context, 0, "block_timestamp_type", R"("2000-01-01T00:00:00.500")");
    check_type(context, 0, "block_timestamp_type", R"("2000-01-01T00:00:01.000")");
    check_type(context, 0, "block_timestamp_type", R"("2018-06-15T19:17:47.500")");
    check_type(context, 0, "block_timestamp_type", R"("2018-06-15T19:17:48.000")");
    check_error(context, "expected string containing block_timestamp_type",
                [&] { return abieos_json_to_bin(context, 0, "block_timestamp_type", "true"); });
    check_type(context, 0, "name", R"("")");
    check_type(context, 0, "name", R"("1")");
    check_type(context, 0, "name", R"("abcd")");
    check_type(context, 0, "name", R"("ab.cd.ef")");
    check_type(context, 0, "name", R"("ab.cd.ef.1234")");
    check_type(context, 0, "name", R"("..ab.cd.ef..")", R"("..ab.cd.ef")");
    check_type(context, 0, "name", R"("zzzzzzzzzzzz")");
    check_type(context, 0, "name", R"("zzzzzzzzzzzzz")", R"("zzzzzzzzzzzzj")");
    check_error(context, "expected string containing name",
                [&] { return abieos_json_to_bin(context, 0, "name", "true"); });
    check_type(context, 0, "bytes", R"("")");
    check_type(context, 0, "bytes", R"("00")");
    check_type(context, 0, "bytes", R"("AABBCCDDEEFF00010203040506070809")");
    check_error(context, "odd number of hex digits", [&] { return abieos_json_to_bin(context, 0, "bytes", R"("0")"); });
    check_error(context, "expected hex string", [&] { return abieos_json_to_bin(context, 0, "bytes", R"("yz")"); });
    check_error(context, "expected string containing hex digits",
                [&] { return abieos_json_to_bin(context, 0, "bytes", R"(true)"); });
    check_error(context, "invalid bytes size", [&] { return abieos_hex_to_json(context, 0, "bytes", "01"); });
    check_type(context, 0, "string", R"("")");
    check_type(context, 0, "string", R"("z")");
    check_type(context, 0, "string", R"("This is a string.")");
    check_type(context, 0, "string", R"("' + '*'.repeat(128) + '")");
    check_type(context, 0, "string", R"("\u0000  这是一个测试  Это тест  هذا اختبار 👍")");
    check_error(context, "invalid string size", [&] { return abieos_hex_to_json(context, 0, "string", "01"); });
    check_type(context, 0, "checksum160", R"("0000000000000000000000000000000000000000")");
    check_type(context, 0, "checksum160", R"("123456789ABCDEF01234567890ABCDEF70123456")");
    check_type(context, 0, "checksum256", R"("0000000000000000000000000000000000000000000000000000000000000000")");
    check_type(context, 0, "checksum256", R"("0987654321ABCDEF0987654321FFFF1234567890ABCDEF001234567890ABCDEF")");
    check_type(
        context, 0, "checksum512",
        R"("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")");
    check_type(
        context, 0, "checksum512",
        R"("0987654321ABCDEF0987654321FFFF1234567890ABCDEF001234567890ABCDEF0987654321ABCDEF0987654321FFFF1234567890ABCDEF001234567890ABCDEF")");
    check_error(context, "expected hex string",
                [&] { return abieos_json_to_bin(context, 0, "checksum256", R"("yz")"); });
    check_error(context, "expected string containing hex",
                [&] { return abieos_json_to_bin(context, 0, "checksum256", R"(true)"); });
    check_error(context, "hex string has incorrect length",
                [&] { return abieos_json_to_bin(context, 0, "checksum256", R"("a0")"); });
    check_type(context, 0, "public_key", R"("EOS1111111111111111111111111111111114T1Anm")",
               R"("PUB_K1_11111111111111111111111111111111149Mr2R")");
    check_type(context, 0, "public_key", R"("EOS11111111111111111111111115qCHTcgbQwptSz99m")",
               R"("PUB_K1_11111111111111111111111115qCHTcgbQwpvP72Uq")");
    check_type(context, 0, "public_key", R"("EOS111111111111111114ZrjxJnU1LA5xSyrWMNuXTrYSJ57")",
               R"("PUB_K1_111111111111111114ZrjxJnU1LA5xSyrWMNuXTrVub2r")");
    check_type(context, 0, "public_key", R"("EOS1111111113diW7pnisfdBvHTXP7wvW5k5Ky1e5DVuF23dosU")",
               R"("PUB_K1_1111111113diW7pnisfdBvHTXP7wvW5k5Ky1e5DVuF4PizpM")");
    check_type(context, 0, "public_key", R"("EOS11DsZ6Lyr1aXpm9aBqqgV4iFJpNbSw5eE9LLTwNAxqjJgmjgbT")",
               R"("PUB_K1_11DsZ6Lyr1aXpm9aBqqgV4iFJpNbSw5eE9LLTwNAxqjJgXSdB8")");
    check_type(context, 0, "public_key", R"("EOS12wkBET2rRgE8pahuaczxKbmv7ciehqsne57F9gtzf1PVYNMRa2")",
               R"("PUB_K1_12wkBET2rRgE8pahuaczxKbmv7ciehqsne57F9gtzf1PVb7Rf7o")");
    check_type(context, 0, "public_key", R"("EOS1yp8ebBuKZ13orqUrZsGsP49e6K3ThVK1nLutxSyU5j9SaXz9a")",
               R"("PUB_K1_1yp8ebBuKZ13orqUrZsGsP49e6K3ThVK1nLutxSyU5j9Tx1r96")");
    check_type(context, 0, "public_key", R"("EOS9adaAMuB9v8yX1mZ5PtoB6VFSCeqRGjASd8ZTM6VUkiHL7mue4K")",
               R"("PUB_K1_9adaAMuB9v8yX1mZ5PtoB6VFSCeqRGjASd8ZTM6VUkiHLB5XEdw")");
    check_type(context, 0, "public_key", R"("EOS69X3383RzBZj41k73CSjUNXM5MYGpnDxyPnWUKPEtYQmTBWz4D")",
               R"("PUB_K1_69X3383RzBZj41k73CSjUNXM5MYGpnDxyPnWUKPEtYQmVzqTY7")");
    check_type(context, 0, "public_key", R"("EOS7yBtksm8Kkg85r4in4uCbfN77uRwe82apM8jjbhFVDgEgz3w8S")",
               R"("PUB_K1_7yBtksm8Kkg85r4in4uCbfN77uRwe82apM8jjbhFVDgEcarGb8")");
    check_type(context, 0, "public_key", R"("EOS7WnhaKwHpbSidYuh2DF1qAExTRUtPEdZCaZqt75cKcixuQUtdA")",
               R"("PUB_K1_7WnhaKwHpbSidYuh2DF1qAExTRUtPEdZCaZqt75cKcixtU7gEn")");
    check_type(context, 0, "public_key", R"("EOS7Bn1YDeZ18w2N9DU4KAJxZDt6hk3L7eUwFRAc1hb5bp6xJwxNV")",
               R"("PUB_K1_7Bn1YDeZ18w2N9DU4KAJxZDt6hk3L7eUwFRAc1hb5bp6uEBZA8")");
    check_type(context, 0, "public_key", R"("PUB_K1_11111111111111111111111111111111149Mr2R")");
    check_type(context, 0, "public_key", R"("PUB_K1_11111111111111111111111115qCHTcgbQwpvP72Uq")");
    check_type(context, 0, "public_key", R"("PUB_K1_111111111111111114ZrjxJnU1LA5xSyrWMNuXTrVub2r")");
    check_type(context, 0, "public_key", R"("PUB_K1_1111111113diW7pnisfdBvHTXP7wvW5k5Ky1e5DVuF4PizpM")");
    check_type(context, 0, "public_key", R"("PUB_K1_11DsZ6Lyr1aXpm9aBqqgV4iFJpNbSw5eE9LLTwNAxqjJgXSdB8")");
    check_type(context, 0, "public_key", R"("PUB_K1_12wkBET2rRgE8pahuaczxKbmv7ciehqsne57F9gtzf1PVb7Rf7o")");
    check_type(context, 0, "public_key", R"("PUB_K1_1yp8ebBuKZ13orqUrZsGsP49e6K3ThVK1nLutxSyU5j9Tx1r96")");
    check_type(context, 0, "public_key", R"("PUB_K1_9adaAMuB9v8yX1mZ5PtoB6VFSCeqRGjASd8ZTM6VUkiHLB5XEdw")");
    check_type(context, 0, "public_key", R"("PUB_K1_69X3383RzBZj41k73CSjUNXM5MYGpnDxyPnWUKPEtYQmVzqTY7")");
    check_type(context, 0, "public_key", R"("PUB_K1_7yBtksm8Kkg85r4in4uCbfN77uRwe82apM8jjbhFVDgEcarGb8")");
    check_type(context, 0, "public_key", R"("PUB_K1_7WnhaKwHpbSidYuh2DF1qAExTRUtPEdZCaZqt75cKcixtU7gEn")");
    check_type(context, 0, "public_key", R"("PUB_K1_7Bn1YDeZ18w2N9DU4KAJxZDt6hk3L7eUwFRAc1hb5bp6uEBZA8")");
    check_type(context, 0, "public_key", R"("PUB_R1_1111111111111111111111111111111116amPNj")");
    check_type(context, 0, "public_key", R"("PUB_R1_67vQGPDMCR4gbqYV3hkfNz3BfzRmmSj27kFDKrwDbaZKtaX36u")");
    check_type(context, 0, "public_key", R"("PUB_R1_6FPFZqw5ahYrR9jD96yDbbDNTdKtNqRbze6oTDLntrsANgQKZu")");
    check_type(context, 0, "public_key", R"("PUB_R1_7zetsBPJwGQqgmhVjviZUfoBMktHinmTqtLczbQqrBjhaBgi6x")");
    check_error(context, "expected string containing public_key",
                [&] { return abieos_json_to_bin(context, 0, "public_key", "true"); });
    check_error(context, "unrecognized public key format",
                [&] { return abieos_json_to_bin(context, 0, "public_key", R"("foo")"); });
    check_type(context, 0, "private_key", R"("PVT_R1_PtoxLPzJZURZmPS4e26pjBiAn41mkkLPrET5qHnwDvbvqFEL6")");
    check_type(context, 0, "private_key", R"("PVT_R1_vbRKUuE34hjMVQiePj2FEjM8FvuG7yemzQsmzx89kPS9J8Coz")");
    check_error(context, "expected string containing private_key",
                [&] { return abieos_json_to_bin(context, 0, "private_key", "true"); });
    check_error(context, "unrecognized private key format",
                [&] { return abieos_json_to_bin(context, 0, "private_key", R"("foo")"); });
    check_type(
        context, 0, "signature",
        R"("SIG_K1_Kg2UKjXTX48gw2wWH4zmsZmWu3yarcfC21Bd9JPj7QoDURqiAacCHmtExPk3syPb2tFLsp1R4ttXLXgr7FYgDvKPC5RCkx")");
    check_type(
        context, 0, "signature",
        R"("SIG_R1_Kfh19CfEcQ6pxkMBz6xe9mtqKuPooaoyatPYWtwXbtwHUHU8YLzxPGvZhkqgnp82J41e9R6r5mcpnxy1wAf1w9Vyo9wybZ")");
    check_error(context, "expected string containing signature",
                [&] { return abieos_json_to_bin(context, 0, "signature", "true"); });
    check_error(context, "unrecognized signature format",
                [&] { return abieos_json_to_bin(context, 0, "signature", R"("foo")"); });
    check_type(context, 0, "symbol_code", R"("A")");
    check_type(context, 0, "symbol_code", R"("B")");
    check_type(context, 0, "symbol_code", R"("SYS")");
    check_error(context, "expected string containing symbol_code",
                [&] { return abieos_json_to_bin(context, 0, "symbol_code", "true"); });
    check_type(context, 0, "symbol", R"("0,A")");
    check_type(context, 0, "symbol", R"("1,Z")");
    check_type(context, 0, "symbol", R"("4,SYS")");
    check_error(context, "expected string containing symbol",
                [&] { return abieos_json_to_bin(context, 0, "symbol", "null"); });
    check_type(context, 0, "asset", R"("0 FOO")");
    check_type(context, 0, "asset", R"("0.0 FOO")");
    check_type(context, 0, "asset", R"("0.00 FOO")");
    check_type(context, 0, "asset", R"("0.000 FOO")");
    check_type(context, 0, "asset", R"("1.2345 SYS")");
    check_type(context, 0, "asset", R"("-1.2345 SYS")");
    check_error(context, "expected string containing asset",
                [&] { return abieos_json_to_bin(context, 0, "asset", "null"); });
    check_type(context, 0, "asset[]", R"([])");
    check_type(context, 0, "asset[]", R"(["0 FOO"])");
    check_type(context, 0, "asset[]", R"(["0 FOO","0.000 FOO"])");
    check_type(context, 0, "asset?", R"(null)");
    check_type(context, 0, "asset?", R"("0.123456 SIX")");
    check_type(context, 0, "extended_asset", R"({"quantity":"0 FOO","contract":"bar"})");
    check_type(context, 0, "extended_asset", R"({"quantity":"0.123456 SIX","contract":"seven"})");

    check_type(context, token, "transfer",
               R"({"from":"useraaaaaaaa","to":"useraaaaaaab","quantity":"0.0001 SYS","memo":"test memo"})");
    check_type(
        context, 0, "transaction",
        R"({"expiration":"2009-02-13T23:31:31.000","ref_block_num":1234,"ref_block_prefix":5678,"max_net_usage_words":0,"max_cpu_usage_ms":0,"delay_sec":0,"context_free_actions":[],"actions":[{"account":"eosio.token","name":"transfer","authorization":[{"actor":"useraaaaaaaa","permission":"active"}],"data":"608C31C6187315D6708C31C6187315D60100000000000000045359530000000000"}],"transaction_extensions":[]})");

    check_type( //
        context, token, "transfer",
        R"({"to":"useraaaaaaab","memo":"test memo","from":"useraaaaaaaa","quantity":"0.0001 SYS"})",
        R"({"from":"useraaaaaaaa","to":"useraaaaaaab","quantity":"0.0001 SYS","memo":"test memo"})", false);
    check_type(
        context, 0, "transaction",
        R"({"ref_block_num":1234,"ref_block_prefix":5678,"expiration":"2009-02-13T23:31:31.000","max_net_usage_words":0,"max_cpu_usage_ms":0,"delay_sec":0,"context_free_actions":[],"actions":[{"account":"eosio.token","name":"transfer","authorization":[{"actor":"useraaaaaaaa","permission":"active"}],"data":"608C31C6187315D6708C31C6187315D60100000000000000045359530000000000"}],"transaction_extensions":[]})",
        R"({"expiration":"2009-02-13T23:31:31.000","ref_block_num":1234,"ref_block_prefix":5678,"max_net_usage_words":0,"max_cpu_usage_ms":0,"delay_sec":0,"context_free_actions":[],"actions":[{"account":"eosio.token","name":"transfer","authorization":[{"actor":"useraaaaaaaa","permission":"active"}],"data":"608C31C6187315D6708C31C6187315D60100000000000000045359530000000000"}],"transaction_extensions":[]})",
        false);

    check_error(context, "recursion limit reached", [&] {
        return abieos_json_to_bin_reorderable(
            context, 0, "int8",
            "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[["
            "[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]"
            "]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
    });
    check_error(context, "json parse error", [&] { return abieos_json_to_bin_reorderable(context, 0, "int8", "1,2"); });

    check_error(context, "optional (?) and array ([]) don't support nesting",
                [&] { return abieos_json_to_bin(context, 0, "int8?[]", ""); });
    check_error(context, "optional (?) and array ([]) don't support nesting",
                [&] { return abieos_json_to_bin(context, 0, "int8[]?", ""); });
    check_error(context, "optional (?) may not contain binary extensions ($)",
                [&] { return abieos_json_to_bin(context, 0, "int8$?", ""); });
    check_error(context, "array ([]) may not contain binary extensions ($)",
                [&] { return abieos_json_to_bin(context, 0, "int8$[]", ""); });
    check_error(context, "binary extensions ($) may not contain binary extensions ($)",
                [&] { return abieos_json_to_bin(context, 0, "int8$$", ""); });
    check_error(context, "unknown type \"fee\"", [&] { return abieos_json_to_bin(context, 0, "fee", ""); });

    check_error(context, "abi has a type with a missing name", [&] {
        return abieos_set_abi( //
            context, 0,
            R"({"version":"eosio::abi/1.1","types":[{"new_type_name":"","type":"int8"}]})");
    });
    check_error(context, "can't use extensions ($) within typedefs", [&] {
        return abieos_set_abi( //
            context, 0,
            R"({"version":"eosio::abi/1.1","types":[{"new_type_name":"a","type":"int8$"}]})");
    });
    check_error(context, "abi redefines type \"a\"", [&] {
        return abieos_set_abi(
            context, 0,
            R"({"version":"eosio::abi/1.1","types":[{"new_type_name":"a","type":"int8"},{"new_type_name":"a","type":"int8"}]})");
    });

    check_error(context, "expected object", [&] { return abieos_json_to_bin(context, testAbiName, "s4", "null"); });
    check_error(context, "expected object", [&] { return abieos_json_to_bin(context, testAbiName, "s4", "[]"); });
    check_error(context, R"(s4: expected field "a1")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s4", R"({"foo":7})"); });
    check_error(context, R"(s4.a1: expected number or boolean)",
                [&] { return abieos_json_to_bin(context, testAbiName, "s4", R"({"a1":[]})"); });
    check_error(context, R"(expected variant: ["type", value])",
                [&] { return abieos_json_to_bin(context, testAbiName, "v1", "null"); });
    check_error(context, R"(<variant>: expected variant: ["type", value])",
                [&] { return abieos_json_to_bin(context, testAbiName, "v1", "[]"); });
    check_error(context, R"(<variant>: type is not valid for this variant)",
                [&] { return abieos_json_to_bin(context, testAbiName, "v1", R"(["x"])"); });
    check_error(context, R"(<variant>: expected variant: ["type", value])",
                [&] { return abieos_json_to_bin(context, testAbiName, "v1", R"(["int8",7,5])"); });
    check_error(context, R"(s5: expected field "x1")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({})"); });
    check_error(context, R"(s5: expected field "x2")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":5})"); });
    check_error(context, R"(s5: expected field "x3")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":5,"x2":7})"); });
    check_error(context, R"(s5.x1: expected number or boolean)",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":null})"); });
    check_error(context, R"(s5.x2: expected number or boolean)",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":null})"); });
    check_error(context, R"(s5: expected field "x3")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10})"); });
    check_error(context, R"(s5.x3: expected object)",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":null)"); });
    check_error(context, R"(s5.x3: expected field "c1")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{})"); });
    check_error(context, R"(s5.x3: expected field "c2")",
                [&] { return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4})"); });
    check_error(context, R"(s5.x3.c2: expected array)", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":{}})");
    });
    check_error(context, R"(s5.x3.c2[0]: expected object)", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[7])");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x1")", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{}])");
    });
    check_error(context, R"(s5.x3.c2[0].x1: expected number or boolean)", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x2")", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x2")", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5",
                                  R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x3":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0].x2: expected number or boolean)", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5",
                                  R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x3")", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5",
                                  R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0].x3: expected object)", [&] {
        return abieos_json_to_bin(context, testAbiName, "s5",
                                  R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[1]: expected object)", [&] {
        return abieos_json_to_bin(
            context, testAbiName, "s5",
            R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":{"c1":0,"c2":[],"c3":7}},null]}} )");
    });
    check_error(context, R"(s5.x3.c2[1]: expected field "x1")", [&] {
        return abieos_json_to_bin(
            context, testAbiName, "s5",
            R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":{"c1":0,"c2":[],"c3":7}},{} ]}} )");
    });
    check_error(context, R"(s5.x3.c2[1].x1: expected number or boolean)", [&] {
        return abieos_json_to_bin(
            context, testAbiName, "s5",
            R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":{"c1":0,"c2":[],"c3":7}},{"x1":null} ]}} )");
    });

    check_error(context, "expected object",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s4", "null"); });
    check_error(context, "expected object",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s4", "[]"); });
    check_error(context, R"(s4.a1: expected number or boolean)",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s4", R"({"a1":[]})"); });
    check_error(context, R"(expected variant: ["type", value])",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "v1", "null"); });
    check_error(context, R"(expected variant: ["type", value])",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "v1", "[]"); });
    check_error(context, R"(expected variant: ["type", value])",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "v1", R"(["x",7,5])"); });
    check_error(context, R"(<variant>: type is not valid for this variant)",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "v1", R"(["x",7])"); });
    check_error(context, R"(expected variant: ["type", value])",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "v1", R"(["int8",7,5])"); });
    check_error(context, R"(s5: expected field "x1")",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({})"); });
    check_error(context, R"(s5: expected field "x2")",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":5})"); });
    check_error(context, R"(s5: expected field "x3")",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":5,"x2":7})"); });
    check_error(context, R"(s5.x1: expected number or boolean)",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":null})"); });
    check_error(context, R"(s5.x2: expected number or boolean)",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":null})"); });
    check_error(context, R"(s5: expected field "x3")",
                [&] { return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":10})"); });
    check_error(context, R"(s5.x3: expected object)", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":null})");
    });
    check_error(context, R"(s5.x3: expected field "c1")", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{}})");
    });
    check_error(context, R"(s5.x3: expected field "c2")", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4}})");
    });
    check_error(context, R"(s5.x3.c2: expected array)", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":{}}})");
    });
    check_error(context, R"(s5.x3.c2[0]: expected object)", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5", R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[7]}})");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x1")", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{}]}})");
    });
    check_error(context, R"(s5.x3.c2[0].x1: expected number or boolean)", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x2")", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x2")", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x3":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0].x2: expected number or boolean)", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0]: expected field "x3")", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true}]}} )");
    });
    check_error(context, R"(s5.x3.c2[0].x3: expected object)", [&] {
        return abieos_json_to_bin_reorderable(context, testAbiName, "s5",
                                              R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":null}]}} )");
    });
    check_error(context, R"(s5.x3.c2[1]: expected object)", [&] {
        return abieos_json_to_bin_reorderable(
            context, testAbiName, "s5",
            R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":{"c1":0,"c2":[],"c3":7}},null]}} )");
    });
    check_error(context, R"(s5.x3.c2[1]: expected field "x1")", [&] {
        return abieos_json_to_bin_reorderable(
            context, testAbiName, "s5",
            R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":{"c1":0,"c2":[],"c3":7}},{} ]}} )");
    });
    check_error(context, R"(s5.x3.c2[1].x1: expected number or boolean)", [&] {
        return abieos_json_to_bin_reorderable(
            context, testAbiName, "s5",
            R"({"x1":9,"x2":10,"x3":{"c1":4,"c2":[{"x1":7,"x2":true,"x3":{"c1":0,"c2":[],"c3":7}},{"x1":null} ]}} )");
    });

    auto testWith = [&](auto& abiName) {
        check_type(context, abiName, "v1", R"(["int8",7])");
        check_type(context, abiName, "v1", R"(["s1",{"x1":6}])");
        check_type(context, abiName, "v1", R"(["s2",{"y1":5,"y2":4}])");

        check_type(context, abiName, "s3", R"({})");
        check_type(context, abiName, "s3", R"({"z1":7})");
        check_type(context, abiName, "s3", R"({"z1":7,"z2":["int8",6]})");
        check_type(context, abiName, "s3", R"({"z1":7,"z2":["int8",6],"z3":{}})", R"({"z1":7,"z2":["int8",6]})");
        check_type(context, abiName, "s3", R"({"z1":7,"z2":["int8",6],"z3":{"y1":9}})");
        check_type(context, abiName, "s3", R"({"z1":7,"z2":["int8",6],"z3":{"y1":9,"y2":10}})");

        check_type(context, abiName, "s4", R"({})");
        check_type(context, abiName, "s4", R"({"a1":null})");
        check_type(context, abiName, "s4", R"({"a1":7})");
        check_type(context, abiName, "s4", R"({"a1":null,"b1":[]})");
        check_type(context, abiName, "s4", R"({"a1":null,"b1":[5,6,7]})");
    };

    testWith(testAbiName);
    testWith(testHexAbiName);

    abieos_destroy(context);
}

int main() {
    try {
        check_types();
        printf("\nok\n\n");
        return 0;
    } catch (std::exception& e) {
        printf("error: %s\n", e.what());
        return 1;
    }
}
