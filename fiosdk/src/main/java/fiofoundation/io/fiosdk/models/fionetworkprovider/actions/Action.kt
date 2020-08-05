package fiofoundation.io.fiosdk.models.fionetworkprovider.actions

import com.google.gson.GsonBuilder
import fiofoundation.io.fiosdk.models.fionetworkprovider.Authorization


open class Action(account: String, name: String,authorization:Authorization,data:String): IAction
{
    override var account = account
    override var name = name
    override var authorization = ArrayList<Authorization>()
    override var data = data


    init {

        this.authorization.add(authorization)
    }

    fun toJson(): String {
        val gson = GsonBuilder().create()
        return gson.toJson(this,this.javaClass)
    }
}