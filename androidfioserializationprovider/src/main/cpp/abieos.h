// copyright defined in abieos/LICENSE.txt

#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct abieos_context_s abieos_context;
typedef int abieos_bool;

// Create a context. The context holds all memory allocated by functions in this header. Returns null on failure.
abieos_context* abieos_create();

// Destroy a context.
void abieos_destroy(abieos_context* context);

// Get last error. Never returns null. The context owns the returned string.
const char* abieos_get_error(abieos_context* context);

// Get generated binary. The context owns the returned memory. Functions return null on error; use abieos_get_error to
// retrieve error.
int abieos_get_bin_size(abieos_context* context);
const char* abieos_get_bin_data(abieos_context* context);

// Convert generated binary to hex. The context owns the returned string. Returns null on error; use abieos_get_error to
// retrieve error.
const char* abieos_get_bin_hex(abieos_context* context);

// Name conversion. The context owns the returned memory. Functions return null on error; use abieos_get_error to
// retrieve error.
uint64_t abieos_string_to_name(abieos_context* context, const char* str);
const char* abieos_name_to_string(abieos_context* context, uint64_t name);

// Set abi (JSON format). Returns false on error.
abieos_bool abieos_set_abi(abieos_context* context, uint64_t contract, const char* abi);

// Set abi (binary format). Returns false on error.
abieos_bool abieos_set_abi_bin(abieos_context* context, uint64_t contract, const char* data, size_t size);

// Set abi (hex format). Returns false on error.
abieos_bool abieos_set_abi_hex(abieos_context* context, uint64_t contract, const char* hex);

// Get the type name for an action. The contract owns the returned memory. Returns null on error; use abieos_get_error
// to retrieve error.
const char* abieos_get_type_for_action(abieos_context* context, uint64_t contract, uint64_t action);

// Convert json to binary. Use abieos_get_bin_* to retrieve result. Returns false on error.
abieos_bool abieos_json_to_bin(abieos_context* context, uint64_t contract, const char* type, const char* json);

// Convert json to binary. Allow json field reordering. Use abieos_get_bin_* to retrieve result. Returns false on error.
abieos_bool abieos_json_to_bin_reorderable(abieos_context* context, uint64_t contract, const char* type,
                                           const char* json);

// Convert binary to json. The context owns the returned string. Returns null on error; use abieos_get_error to retrieve
// error.
const char* abieos_bin_to_json(abieos_context* context, uint64_t contract, const char* type, const char* data,
                               size_t size);

// Convert hex to json. The context owns the returned memory. Returns null on error; use abieos_get_error to retrieve
// error.
const char* abieos_hex_to_json(abieos_context* context, uint64_t contract, const char* type, const char* hex);

#ifdef __cplusplus
}
#endif
