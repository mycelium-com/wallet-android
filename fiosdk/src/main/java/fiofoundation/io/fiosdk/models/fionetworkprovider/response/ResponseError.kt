package fiofoundation.io.fiosdk.models.fionetworkprovider.response

class ResponseError(val message: String,var code: Int, var type:String, var error:FieldError,var fields:List<FieldError>)