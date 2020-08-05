package fiofoundation.io.fiosdk.models.fionetworkprovider.request

class GetBlockRequest(blockIdentifier: String) {
    private var block_num_or_id: String = ""

    init{
        this.block_num_or_id = blockIdentifier
    }

    var blockIdentifier: String
        get(){return this.block_num_or_id}
        set(value){this.block_num_or_id = value}

}