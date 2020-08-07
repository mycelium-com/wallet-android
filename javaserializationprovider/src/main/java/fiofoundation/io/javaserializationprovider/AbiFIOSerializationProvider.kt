package fiofoundation.io.javaserializationprovider


import fiofoundation.io.fiosdk.errors.serializationprovider.*
import fiofoundation.io.fiosdk.hexStringToByteArray
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.models.serializationprovider.AbiFIOSerializationObject
import fiofoundation.io.fiosdk.toHexString
import java.nio.ByteBuffer

class AbiFIOSerializationProvider: ISerializationProvider {

    private var context: ByteBuffer? = null

    init
    {
        context = create()
        if (null == context) {
            throw AbiFIOContextNullError(CANNOT_CREATE_CONTEXT_ERR_MSG)
        }
    }

    companion object {

        private val NULL_CONTEXT_ERR_MSG = "Null context!  Has destroyContext() already been called?"
        private val CANNOT_CREATE_CONTEXT_ERR_MSG = "Could not create abieos context."

        init {
                System.loadLibrary("abieos-lib")
        }
    }

    external fun create():ByteBuffer
    external fun destroy(context: ByteBuffer)
    external fun getError(context: ByteBuffer): String
    external fun getBinHex(context: ByteBuffer): String
    external fun stringToName(context: ByteBuffer, str: String?): Long
    external fun setAbi(context: ByteBuffer, contract: Long, abi: String): Boolean
    external fun jsonToBin(context: ByteBuffer, contract: Long, type: String, json: String, reorderable: Boolean): Boolean
    external fun hexToJson(context: ByteBuffer, contract: Long, type: String, hex: String): String
    external fun getTypeForAction(context: ByteBuffer, contract: Long, action: Long): String


    fun destroyContext() {
        if (context!= null) {
            destroy(context!!)
            context = null
        }
    }

    @Throws(AbiFIOContextNullError::class)
    fun stringToName64(str: String?): Long
    {
        if (context == null) throw AbiFIOContextNullError(NULL_CONTEXT_ERR_MSG)

        return stringToName(context!!, str)
    }

    @Throws(AbiFIOContextNullError::class)
    fun error(): String
    {
        if (context == null) throw AbiFIOContextNullError(NULL_CONTEXT_ERR_MSG)
        return getError(context!!)
    }

    @Throws(SerializeError::class)
    override fun serialize(serializationObject: AbiFIOSerializationObject)
    {

        try {
            refreshContext()

            if (serializationObject.json.isEmpty()) {
                throw SerializeError("No content to serialize.")
            }

            val contract64: Long = stringToName64(serializationObject.contract)

            if (serializationObject.abi.isEmpty()) {
                throw SerializeError(String.format("serialize -- No ABI provided for %s %s",
                    if (serializationObject.contract == null)  serializationObject.contract else "",
                    serializationObject.name))
            }

            val result: Boolean = setAbi(context!!, contract64, serializationObject.abi)

            if (!result)
            {
                val err: String? = error()
                val errMsg: String = String.format("Json to hex == Unable to set ABI. %s", if (err == null)  "" else err)
                throw SerializeError(errMsg)
            }

            val typeStr:String? = if(serializationObject.type == null) getType(serializationObject.name, contract64) else serializationObject.type

            if (typeStr == null)
            {
                val err:String? = error()
                val errMsg:String = String.format("Unable to find type for action %s. %s", serializationObject.name, if(err == null) "" else err)

                throw SerializeError(errMsg)
            }

            val jsonToBinResult:Boolean = jsonToBin(context!!, contract64, typeStr, serializationObject.json, true)

            if (!jsonToBinResult)
            {
                val err:String? = error()
                val errMsg:String = String.format("Unable to pack json to bin. %s", if(err == null) "" else err)

                throw SerializeError(errMsg)
            }

            val hex:String = getBinHex(context!!)

            serializationObject.hex = hex

        }
        catch (serializationProviderError:SerializationProviderError) {
            throw SerializeError(serializationProviderError)
        }

    }


    @Throws(SerializeTransactionError::class)
    override fun serializeTransaction(json:String): String
    {
        try {
            val abi:String = getAbiJsonString("transaction.abi.json")
            val serializationObject = AbiFIOSerializationObject(null, "", "transaction", abi)

            serializationObject.json = json
            serialize(serializationObject)
            return serializationObject.hex.toLowerCase()
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw SerializeTransactionError(serializationProviderError)
        }
    }

    @Throws(SerializeAbiError::class)
    override fun serializeAbi(json:String):String
    {
        try {
            val abi:String = getAbiJsonString("abi.abi.json")
            val serializationObject = AbiFIOSerializationObject(null, "", "abi_def", abi)

            serializationObject.json = json
            serialize(serializationObject)
            return serializationObject.hex.toLowerCase()
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw SerializeAbiError(serializationProviderError)
        }
    }

    @Throws(DeserializeError::class)
    override fun deserialize(deserilizationObject:AbiFIOSerializationObject)
    {
        try {
            refreshContext()

            if (deserilizationObject.hex.isEmpty()) {
                throw DeserializeError("No content to serialize.")
            }

            val contract64:Long = stringToName64(deserilizationObject.contract)

            if (deserilizationObject.abi.isEmpty()) {
                throw DeserializeError(String.format("deserialize -- No ABI provided for %s %s",
                    if(deserilizationObject.contract == null) deserilizationObject.contract else "", deserilizationObject.name))
            }

            val result:Boolean = setAbi(context!!, contract64, deserilizationObject.abi)

            if (!result)
            {
                val err:String? = error()
                val errMsg:String = String.format("deserialize == Unable to set ABI. %s", if(err == null) "" else err)

                throw DeserializeError(errMsg)
            }

            val typeStr:String? = if(deserilizationObject.type == null) getType(deserilizationObject.name, contract64) else deserilizationObject.type

            if (typeStr == null)
            {
                val err:String? = error()
                val errMsg:String = String.format("Unable to find type for action %s. %s", deserilizationObject.name, if(err == null) "" else err)

                throw DeserializeError(errMsg)
            }

            val jsonStr:String? = hexToJson(context!!, contract64, typeStr, deserilizationObject.hex)

            if (jsonStr == null)
            {
                val err:String? = error()
                val errMsg:String = String.format("Unable to unpack hex to json. %s", if(err == null) "" else err)

                throw DeserializeError(errMsg)
            }

            deserilizationObject.json = jsonStr
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw DeserializeError(serializationProviderError)
        }
    }

    @Throws(DeserializeTransactionError::class)
    override fun deserializeTransaction(hex:String):String  {
        try {
            val abi:String = getAbiJsonString("transaction.abi.json")
            val serializationObject = AbiFIOSerializationObject(null, "", "transaction", abi)

            serializationObject.hex = hex
            deserialize(serializationObject)

            return serializationObject.json
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw DeserializeTransactionError(serializationProviderError)
        }
    }

    @Throws(DeserializeAbiError::class)
    override fun deserializeAbi(hex:String):String
    {
        try {
            val abi:String = getAbiJsonString("abi.abi.json")
            val serializationObject = AbiFIOSerializationObject(null, "", "abi_def", abi)

            serializationObject.hex = hex
            deserialize(serializationObject)
            return serializationObject.json
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw DeserializeAbiError(serializationProviderError)
        }
    }

    /**
     * Serializes a specific FIO ABI structure (fio.abi.json). [AbiFIOJson]
     *
     * @param json JSON to serialize
     * @param contentType Type of content (abi structure) to serialize.  Ex: new_funds_content
     * @return [ByteArray]
     *
     * @throws [SerializeTransactionError]
     */
    @Throws(SerializeTransactionError::class)
    override fun serializeContent(json:String,contentType:String): ByteArray
    {
        try {
            val abi:String = getAbiJsonString("fio.abi.json")
            val serializationObject = AbiFIOSerializationObject(null, "", contentType, abi)

            serializationObject.json = json
            serialize(serializationObject)

            return serializationObject.hex.hexStringToByteArray()
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw SerializeTransactionError(serializationProviderError)
        }
    }

    /**
     * Deserialize a specific FIO ABI structure (fio.abi.json). [AbiFIOJson]
     *
     * @param content Serialized content.
     * @param contentType Type of content (abi structure) to deserialize.  Ex: new_funds_content
     * @return [ByteArray]
     *
     * @throws [DeserializeTransactionError]
     */
    @Throws(DeserializeTransactionError::class)
    override fun deserializeContent(content:ByteArray,contentType:String): String  {
        try {
            val abi:String = getAbiJsonString("fio.abi.json")
            val serializationObject = AbiFIOSerializationObject(null, "", contentType, abi)

            serializationObject.hex = content.toHexString()
            deserialize(serializationObject)

            return serializationObject.json
        }
        catch (serializationProviderError:SerializationProviderError)
        {
            throw DeserializeTransactionError(serializationProviderError)
        }
    }

    @Throws(SerializationProviderError::class)
    private fun refreshContext()
    {
        destroyContext()
        context = create()
        if (null == context) {
            throw AbiFIOContextNullError("Could not create abieos context.")
        }
    }

    @Throws(SerializationProviderError::class)
    private fun getAbiJsonString(abi:String):String {

        var abiString:String?

        var jsonMap:Map<String, String> = AbiFIOJson.abiFioJsonMap
        if (jsonMap.containsKey(abi))
        {
            abiString = jsonMap.getValue(abi)
        }
        else
        {
            abiString = ""
        }

        if (abiString.isEmpty()) {
            throw SerializationProviderError(String.format("Serialization Provider -- No ABI found for %s", abi))
        }

        return abiString

    }

    @Throws(SerializationProviderError::class)
    private fun getType(action: String, contract: Long): String
    {
        val action64: Long = stringToName64(action)
        return getTypeForAction(context!!, contract, action64)
    }

}