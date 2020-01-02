package com.mycelium.wapi.api.exception;

/*
    Any RPC response-related exception could happen in JsonRpcTcpClient.write() method by any reason.
    Indicates the lack of data.
*/
public class RpcResponseException extends RuntimeException {
    public RpcResponseException(String detailMessage) {
        super(detailMessage);
    }
}
