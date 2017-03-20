trezor-android
==============

TREZOR Library for Android

Example
-------

.. code:: java

  import com.google.protobuf.Message;
  import com.satoshilabs.trezor.Trezor;
  import com.satoshilabs.trezor.protobuf.TrezorMessage;

  Trezor t = Trezor.getDevice(this);
  if (t != null) {
    TrezorMessage.Initialize req = TrezorMessage.Initialize.newBuilder().build();
    Message resp = t.send(req);
    if (resp != null) {
      // "got: " + resp.getClass().getSimpleName()
    }
  }
