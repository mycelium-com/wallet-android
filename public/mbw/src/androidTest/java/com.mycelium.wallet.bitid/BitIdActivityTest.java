package com.mycelium.wallet.bitid;


import android.content.Intent;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;

public class BitIdActivityTest extends ActivityInstrumentationTestCase2<BitIDAuthenticationActivity> {

   //private final String URISTRING = "bitid://wiki.unsystem.net/en/index.php/Special:BitIDLogin/callback?x=d94831d37eb690b0";
   private final String URISTRING = "bitid://bitid-demo.herokuapp.com/callback?x=bdb2dd0f04d858f6&u=1";


   public BitIdActivityTest() {
      super(BitIDAuthenticationActivity.class);
   }

   @Override
   protected void setUp() throws Exception {
      super.setUp();
      setActivityInitialTouchMode(false);
   }

   public void testStartingActivity() {
      BitIDSignRequest bitid = BitIDSignRequest.parse(Uri.parse(URISTRING)).get();
      Intent i = new Intent();
      i.putExtra("request", bitid);
      setActivityIntent(i);
      BitIDAuthenticationActivity activity = getActivity();
   }
}
