package com.mycelium.wallet.bitid;

import android.net.Uri;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.squareup.otto.Subscribe;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BitIdAsyncTaskTest {
   private CountDownLatch signal;
   private BitIdResponse response;
   private Bus bus;

   @Before
   public void setUp() {
      signal = new CountDownLatch(1);
      response = null;
      bus = new Bus(ThreadEnforcer.ANY);
      bus.register(this);
   }

   @Test
   public void testBitcoinblueExpiredNonce() throws Exception {
      InMemoryPrivateKey privateKey = new InMemoryPrivateKey("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF", NetworkParameters.productionNetwork);
      Address address = Address.fromString("1CC3X2gu58d6wXUWMffpuzN9JAfTUWu4Kj");
      BitIDSignRequest bitid = BitIDSignRequest.parse(Uri.parse("bitid://bitid.bitcoin.blue/callback?x=e7befd6d54c306ef&u=1")).get();
      new BitIdAsyncTask(new BitIdAuthenticator(bitid, true, privateKey, address), bus).execute();

      assertTrue(signal.await(20, TimeUnit.SECONDS));
      assertNotNull(response);
      assertTrue(response.status == BitIdResponse.ResponseStatus.ERROR);
      assertTrue(response.message.contains("NONCE is illegal"));
   }

   @Test
   public void testBitcoinblueInvalidSignature() throws Exception {
      InMemoryPrivateKey privateKey = new InMemoryPrivateKey("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF", NetworkParameters.productionNetwork);
      Address address = Address.fromString("17Dwg2Xx4RVaQgg8crbkvrT8WxNSQYuqLz"); //does not match the priv key
      BitIDSignRequest bitid = BitIDSignRequest.parse(Uri.parse("bitid://bitid.bitcoin.blue/callback?x=e7befd6d54c306ef&u=1")).get();
      new BitIdAsyncTask(new BitIdAuthenticator(bitid, true, privateKey, address), bus).execute();

      assertTrue(signal.await(20, TimeUnit.SECONDS));
      assertNotNull(response);
      assertTrue(response.status == BitIdResponse.ResponseStatus.ERROR);
      assertTrue(response.message.contains("Signature is incorrect"));
   }

   @Subscribe
   public void getCallback(BitIdResponse response) {
      this.response = response;
      bus.unregister(this);
      signal.countDown();
   }
}
