package fiofoundation.io.fiosdk.models.serializationprovider

class AbiFIOSerializationObject (val contract: String?, val name: String, val type: String?, val abi: String)
{
    var hex: String = ""
    var json: String = ""
}