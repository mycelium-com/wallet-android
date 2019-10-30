package com.mycelium.wapi.api.response;

/**
 * This enum is used by the backend to signal a pending alert message (via FeatureWarning) to the client.
 * The app is supposed to show these messages, when the user wants to activate or use certain aspects of the wallet.
 * This allows us to timely notify users of possible problems or bugs and mitigate them.
 */
public enum Feature {
   GENERAL,          // matches for every warning check, will override all other warnings after it
   APP_START,        // handle warning on app start
   MAIN_SCREEN,      // handle warning on app start, or in main screen if later discovered
   SEND_BITCOIN,     // handle warning in the send dialog
   RECEIVE_BITCOIN,  // handle warning in the receive dialog or main screen if we show an QR code

   // Colu
   COLU_NEW_ACCOUNT,
   COLU_PREPARE_OUTGOING_TX
}
