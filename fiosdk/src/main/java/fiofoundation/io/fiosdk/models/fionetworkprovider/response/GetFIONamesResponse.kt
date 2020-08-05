package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import fiofoundation.io.fiosdk.models.FIOAddress
import fiofoundation.io.fiosdk.models.FIODomain

class GetFIONamesResponse: FIOResponse()
{
    private val fio_domains: List<FIODomain>? = null
    private val fio_addresses: List<FIOAddress>? = null

    val fioAddresses: List<FIOAddress>?
        get(){return this.fio_addresses}

    val fioDomains: List<FIODomain>?
        get(){return this.fio_domains}
}