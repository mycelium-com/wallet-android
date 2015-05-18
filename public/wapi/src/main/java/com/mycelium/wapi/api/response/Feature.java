package com.mycelium.wapi.api.response;

public enum Feature {
   GENERAL,       // matches for every warning check, will override all other warnings after it
   APP_START,     // handle warning on app start
   MAIN_SCREEN,   // handle warning on app start, or in main screen if later discovered
   SEND_BITCOIN,  // handle warning in the send dialog
   RECEIVE_BITCOIN,  // handle warning in the receive dialog or main screen if we show an QR code

   // External Services
   CASHILA,       // handle when the cashila activity gets initialized
   CASHILA_NEW_PAYMENT,    // handle when the usere creates a new payment (enqueue or pay-now)
   CASHILA_PAY,   // handle before the user sends bitcoins to cashila
}
