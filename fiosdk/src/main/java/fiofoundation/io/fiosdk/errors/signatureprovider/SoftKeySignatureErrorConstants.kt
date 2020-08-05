package fiofoundation.io.fiosdk.errors.signatureprovider

object SoftKeySignatureErrorConstants
{
    const val IMPORT_KEY_CONVERT_TO_PEM_ERROR = "Can't convert %s to Pem format."

    const val IMPORT_KEY_INPUT_EMPTY_ERROR = "Input can't be empty!"

    const val CONVERT_TO_PEM_EMPTY_ERROR = "Converting to pem was success but pem result is empty."

    const val SIGN_TRANS_PREPARE_SIGNABLE_TRANS_ERROR =
        "Error when trying to prepare signable transaction from serialized transaction %s"

    const val SIGN_TRANS_EMPTY_KEY_LIST = "List of public keys to sign can't be empty!"

    const val SIGN_TRANS_EMPTY_CHAIN_ID = "Chain id can't be empty!"

    const val SIGN_TRANS_EMPTY_TRANSACTION = "Serialized Transaction can't be empty."

    const val SIGN_TRANS_NO_KEY_AVAILABLE = "No key available in signature provider! Make sure to call import key."

    const val SIGN_TRANS_SEARCH_KEY_ERROR =
        "Error when trying to search for corresponding private key from input public key %s"

    const val SIGN_TRANS_KEY_NOT_FOUND = "Found no corresponding private key with input public key %s"

    const val SIGN_TRANS_GET_CURVE_DOMAIN_ERROR = "Error when trying to get EC Curve domain of %s"

    const val SIGN_TRANS_FORMAT_SIGNATURE_ERROR = "Error when trying to format signature."

    const val GET_KEYS_KEY_FORMAT_NOT_SUPPORTED =
        "Error on trying to transform key in getAvailableKey(): Algorithm is not supported!"
}