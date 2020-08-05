package fiofoundation.io.fiosdk.models.fionetworkprovider

object FIOApiEndPoints {
    const val get_public_address = "get_pub_address"
    const val get_fio_names = "get_fio_names"
    const val availability_check = "avail_check"
    const val get_fio_balance = "get_fio_balance"
    const val get_fee = "get_fee"
    const val get_info = "get_info"
    const val get_block = "get_block"
    const val get_raw_abi = "get_raw_abi"
    const val register_fio_domain = "register_fio_domain"
    const val register_fio_address = "register_fio_address"
    const val transfer_tokens_pub_key = "transfer_tokens_pub_key"
    const val renew_fio_domain = "renew_fio_domain"
    const val renew_fio_address = "renew_fio_address"
    const val push_transaction = "push_transaction"
    const val get_required_keys = "get_required_keys"
    const val get_pending_fio_requests = "get_pending_fio_requests"
    const val get_sent_fio_requests = "get_sent_fio_requests"
    const val get_obt_data = "get_obt_data"
    const val new_funds_request = "new_funds_request"
    const val reject_funds_request = "reject_funds_request"
    const val record_obt_data = "record_obt_data"
    const val register_fio_name_behalf_of_user = "register_fio_name"
    const val add_public_address = "add_pub_address"
    const val set_domain_visibility = "set_fio_domain_public"

    enum class FeeEndPoint (val endpoint: String) {
        RegisterFioDomain(register_fio_domain),
        RegisterFioAddress(register_fio_address),
        RenewFioDomain(renew_fio_domain),
        RenewFioAddress(renew_fio_address),
        TransferTokens(transfer_tokens_pub_key),
        SetDomainVisibility(set_domain_visibility)
    }
}

