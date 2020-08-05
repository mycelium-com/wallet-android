package fiofoundation.io.fiosdk.errors

class ErrorConstants {

    companion object Static {
        const val INVALID_EOS_PRIVATE_KEY = "The EOS private key provided is invalid!"

        const val INVALID_EOS_PUBLIC_KEY = "The EOS public key provided is invalid!"

        const val BASE58_DECODING_ERROR = "An error occurred while Base58 decoding the EOS key!"

        const val BASE58_EMPTY_KEY = "Input key to decode can't be empty!"

        const val BASE58_EMPTY_CHECKSUM_OR_KEY = "Input key, checksum and key type to validate can't be empty!"

        const val BASE58_EMPTY_CHECKSUM_OR_KEY_OR_KEY_TYPE = "Input key, checksum and key type to validate can't be empty!"

        const val BASE58_INVALID_CHECKSUM = "Input key has invalid checksum!"

        const val DER_TO_PEM_CONVERSION = "Error converting DER encoded key to PEM format!"

        const val UNSUPPORTED_ALGORITHM = "Unsupported algorithm!"

        const val INVALID_PEM_PRIVATE_KEY = "This is not a PEM formatted private key!"

        const val INVALID_DER_PRIVATE_KEY = "DER format of private key is incorrect!"

        val CHECKSUM_GENERATION_ERROR = "Could not generate checksum!"

        const val BASE58_ENCODING_ERROR = "Unable to Base58 encode object!"

        const val PUBLIC_KEY_DECOMPRESSION_ERROR = "Problem decompressing public key!"

        const val PUBLIC_KEY_COMPRESSION_ERROR = "Problem compressing public key!"

        const val PUBLIC_KEY_IS_EMPTY = "Input key to decode can't be empty!"

        const val EMPTY_INPUT_PREPARE_SERIALIZIED_TRANS_FOR_SIGNING = "Chain id and serialized transaction can't be empty!"

        const val EMPTY_INPUT_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE = "Signable transaction can't be empty!"

        const val INVALID_INPUT_SIGNABLE_TRANS_LENGTH_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE =
            "Length of the signable transaction must be larger than %s"

        const val INVALID_INPUT_SIGNABLE_TRANS_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE =
            "Signable transaction has to have this structure: chainId (64 characters) + serialized transaction + 32 bytes of 0!"

        const val EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE_ERROR =
            "Something went wrong when trying to extract serialized transaction from signable transaction."

        const val SIGNATURE_FORMATTING_ERROR = "An error occured formating the signature!"

        const val COULD_NOT_RECOVER_PUBLIC_KEY_FROM_SIG = "Could not recover public key from Signature."

        const val NON_CANONICAL_SIGNATURE = "Input signature is not canonical."

        const val PUBLIC_KEY_COULD_NOT_BE_EXTRACTED_FROM_PRIVATE_KEY = "This is not a private key!"

        // ABIProviderImpl Errors
        const val NO_RESPONSE_RETRIEVING_ABI = "No response retrieving ABI."
        const val MISSING_ABI_FROM_RESPONSE = "Missing ABI from GetRawAbiResponse."
        const val CALCULATED_HASH_NOT_EQUAL_RETURNED = "Calculated ABI hash does not match returned hash."
        const val REQUESTED_ACCCOUNT_NOT_EQUAL_RETURNED = "Requested account name does not match returned account name."
        const val NO_ABI_FOUND = "No ABI found for requested account name."
        const val ERROR_RETRIEVING_ABI = "Error retrieving ABI from the chain."

        //PEMProcessor Errors

        const val ERROR_READING_PEM_OBJECT = "Error reading PEM object!"

        const val ERROR_PARSING_PEM_OBJECT = "Error parsing PEM object!"

        const val KEY_DATA_NOT_FOUND = "Key data not found in PEM object!"

        const val INVALID_PEM_OBJECT = "Cannot read PEM object!"

        //TransactionProcessor Errors

        const val TRANSACTION_PROCESSOR_ACTIONS_EMPTY_ERROR_MSG = "Action list can't be empty!"


        const val TRANSACTION_PROCESSOR_RPC_GET_INFO = "Error happened on calling GetInfo RPC."

        const val TRANSACTION_PROCESSOR_PREPARE_RPC_GET_BLOCK = "Error happened on calling GetBlock RPC."

        const val TRANSACTION_PROCESSOR_PREPARE_CHAINID_NOT_MATCH = "Provided chain id %s does not match chain id %s"

        const val TRANSACTION_PROCESSOR_PREPARE_CHAINID_RPC_EMPTY = "Chain id from back end is empty!"

        val TRANSACTION_PROCESSOR_HEAD_BLOCK_TIME_PARSE_ERROR = "Failed to parse head block time"

        const val TRANSACTION_PROCESSOR_PREPARE_CLONE_ERROR = "Error happened on cloning transaction."

        const val TRANSACTION_PROCESSOR_PREPARE_CLONE_CLASS_NOT_FOUND = "Transaction class was not found"

        const val TRANSACTION_PROCESSOR_TRANSACTION_HAS_TO_BE_INITIALIZED =
            "Transaction must be initialized before this method could be called! call prepare for initialize Transaction"

        const val TRANSACTION_PROCESSOR_GET_ABI_ERROR = "Error happened on getting abi for contract [%s]"

        const val TRANSACTION_PROCESSOR_SERIALIZE_ACTION_WORKED_BUT_EMPTY_RESULT =
            "Serialization of action worked fine but got back empty result!"

        const val TRANSACTION_PROCESSOR_SERIALIZE_TRANSACTION_WORKED_BUT_EMPTY_RESULT =
            "Serialization of transaction worked fine but got back empty result!"

        const val TRANSACTION_PROCESSOR_SERIALIZE_ACTION_ERROR = "Error happened on serializing action [%s]"

        const val TRANSACTION_PROCESSOR_SERIALIZE_TRANSACTION_ERROR = "Error happened on serializing transaction"

        const val TRANSACTION_PROCESSOR_GET_AVAILABLE_KEY_ERROR = "Error happened on getAvailableKeys from SignatureProvider!"

        const val TRANSACTION_PROCESSOR_GET_AVAILABLE_KEY_EMPTY = "Signature provider return no available key"

        const val TRANSACTION_PROCESSOR_RPC_GET_REQUIRED_KEYS = "Error happened on calling getRequiredKeys RPC call."

        const val GET_REQUIRED_KEY_RPC_EMPTY_RESULT = "GetRequiredKeys RPC returned no required keys"

        const val TRANSACTION_PROCESSOR_SIGN_TRANSACTION_ERROR =
            "Error happened on calling sign transaction of Signature provider"

        const val TRANSACTION_PROCESSOR_SIGN_TRANSACTION_TRANS_EMPTY_ERROR =
            "Serialized transaction come back empty from Signature Provider"

        const val TRANSACTION_PROCESSOR_SIGN_TRANSACTION_SIGN_EMPTY_ERROR = "Signatures come back empty from Signature Provider"

        const val TRANSACTION_IS_NOT_ALLOWED_TOBE_MODIFIED =
            "The transaction is not allowed to be modified but was modified by signature provider!"

        const val TRANSACTION_PROCESSOR_GET_SIGN_DESERIALIZE_TRANS_ERROR =
            "Error happened on calling deserializeTransaction to refresh transaction object with new values"

        const val TRANSACTION_PROCESSOR_RPC_PUSH_TRANSACTION = "Error happened on calling pushTransaction RPC call"

        const val TRANSACTION_PROCESSOR_SERIALIZE_ERROR = "Error happened on calling serializeTransaction"

        const val TRANSACTION_PROCESSOR_SIGN_CREATE_SIGN_REQUEST_ERROR =
            "Error happened on creating signature request for Signature Provider to sign!"

        const val TRANSACTION_PROCESSOR_BROADCAST_TRANS_ERROR = "Error happened on pushing transaction to chain!"

        const val TRANSACTION_PROCESSOR_REQUIRED_KEY_NOT_SUBSET =
            "Required keys from back end are not available in available keys from Signature Provider."

        const val TRANSACTION_PROCESSOR_BROADCAST_SERIALIZED_TRANSACTION_EMPTY =
            "Serialized Transaction is empty or has not been populated. Make sure to call prepare then sign before calling broadcast"

        const val TRANSACTION_PROCESSOR_SIGN_BROADCAST_SERIALIZED_TRANSACTION_EMPTY =
            "Serialized Transaction is empty or has not been populated. Make sure to call prepare then sign before calling sign and broadcast"

        const val TRANSACTION_PROCESSOR_SIGN_SIGNATURE_RESPONSE_ERROR = "Error happened on the response of getSignature."

        const val TRANSACTION_PROCESSOR_GET_SIGN_DESERIALIZE_TRANS_EMPTY_ERROR = "Deserialized transaction is null or empty"

        const val TRANSACTION_PROCESSOR_BROADCAST_SIGN_EMPTY =
            "Can't call broadcast because Signature is empty. Make sure of calling sign before calling broadcast."

        const val TRANSACTION_PROCESSOR_SIGN_BROADCAST_SIGN_EMPTY =
            "Can't call sign and broadcast because Signature is empty. Make sure of calling sign before calling sign and broadcast."
    }
}