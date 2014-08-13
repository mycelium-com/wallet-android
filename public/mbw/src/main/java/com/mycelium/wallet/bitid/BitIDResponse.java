package com.mycelium.wallet.bitid;


public class BitIDResponse {
   public enum ResponseStatus {NONE, SSLPROBLEM, TIMEOUT, NOCONNECTION, SUCCESS, ERROR}
   public ResponseStatus status = ResponseStatus.NONE;
   public int code = 0;
   public String message = "";
}
