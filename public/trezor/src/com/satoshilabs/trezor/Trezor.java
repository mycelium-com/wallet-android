package com.satoshilabs.trezor;

// based on https://github.com/trezor/trezor-android
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage.*;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class Trezor {
   private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";
   public static final int TREZOR_USB_VENDOR_ID = 0x534c;
   public static final int TREZOR_USB_PROD_ID = 0x0001;
   private UsbDeviceConnection conn;
   private String serial;
   private UsbEndpoint epr, epw;
   private UsbManager usbManager;
   private UsbInterface iface;

   private final LinkedBlockingQueue<Boolean> gotRights = new LinkedBlockingQueue<Boolean>(1);

   /**
    * Receives broadcast when a supported USB device is attached, detached or
    * when a permission to communicate to the device has been granted.
    */
   private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         String action = intent.getAction();
         UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
         String deviceName = usbDevice.getDeviceName();

         if (ACTION_USB_PERMISSION.equals(action)) {
            boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                  false);
            Log.d("usb", "ACTION_USB_PERMISSION: " + permission + " Device: " + deviceName);

            // sync with connect
            gotRights.add(permission);
         }
      }
   };

   public Trezor(Context context) {
      usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
   }

   private UsbDevice getTrezorDevice(Context context){
      HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
      Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

      while (deviceIterator.hasNext()) {
         UsbDevice device = deviceIterator.next();
         // check if the device is TREZOR
         if (device.getVendorId() != TREZOR_USB_VENDOR_ID || device.getProductId() != TREZOR_USB_PROD_ID) {
            continue;
         }
         Log.i("Trezor.getDevice()", "TREZOR device found");
         if (device.getInterfaceCount() < 1) {
            Log.e("Trezor.getDevice()", "Wrong interface count");
            continue;
         }
         // use first interface
         iface = device.getInterface(0);

         // try to find read/write endpoints
         for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint ep = iface.getEndpoint(i);
            if (epr == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x81) {
               // number = 1 ; dir = USB_DIR_IN
               epr = ep;
               continue;
            }
            if (epw == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x01) {
               // number = 1 ; dir = USB_DIR_OUT
               epw = ep;
               continue;
            }
         }
         if (epr == null) {
            Log.e("Trezor.getDevice()", "Could not find read endpoint");
            continue;
         }
         if (epw == null) {
            Log.e("Trezor.getDevice()", "Could not find write endpoint");
            continue;
         }
         if (epr.getMaxPacketSize() != 64) {
            Log.e("Trezor.getDevice()", "Wrong packet size for read endpoint");
            continue;
         }
         if (epw.getMaxPacketSize() != 64) {
            Log.e("Trezor.getDevice()", "Wrong packet size for write endpoint");
            continue;
         }

         return device;
      }

      return null;
   }

   public boolean isTrezorPluggedIn(Context context){
      return (getTrezorDevice(context) != null);
   }

	public boolean connect(final Context context) {

      IntentFilter filter = new IntentFilter();
      filter.addAction(ACTION_USB_PERMISSION);
      context.registerReceiver(mUsbReceiver, filter);

      final UsbDevice trezorDevice = getTrezorDevice(context);
      final Intent intent = new Intent(ACTION_USB_PERMISSION);

      usbManager.requestPermission(trezorDevice, PendingIntent.getBroadcast(context, 0, intent, 0));


      // retry because of InterruptedException
      while (true) {
         try {
            // gotRights.take blocks until the UsbManager gives us the rights via callback to the BroadcastReceiver
            // this might need an user interaction
            return gotRights.take() && initConnection(trezorDevice);
         } catch (InterruptedException ignored) {
         }
      }

   }


   private boolean initConnection(UsbDevice device){
      // try to open the device
      UsbDeviceConnection conn = usbManager.openDevice(device);
      if (conn == null) {
         Log.e("Trezor.connect()", "Could not open connection");
         return false;
      }
      boolean claimed = conn.claimInterface(iface,  true);
      if (!claimed) {
         Log.e("Trezor.connect()", "Could not claim interface");
         return false;
      }

      this.conn = conn;
      this.serial = this.conn.getSerial();

      return true;
   }

	@Override
	public String toString() {
		return "TREZOR(#" + this.serial + ")";
	}

	private void messageWrite(Message msg) {
		int msg_size = msg.getSerializedSize();
		String msg_name = msg.getClass().getSimpleName();
		int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
		Log.d("Trezor.messageWrite()", String.format("Got message: %s", msg_name));
		ByteBuffer data = ByteBuffer.allocate(32768);
		data.put((byte)'#');
		data.put((byte)'#');
		data.put((byte)((msg_id >> 8) & 0xFF));
		data.put((byte)(msg_id & 0xFF));
		data.put((byte)((msg_size >> 24) & 0xFF));
		data.put((byte)((msg_size >> 16) & 0xFF));
		data.put((byte)((msg_size >> 8) & 0xFF));
		data.put((byte)(msg_size & 0xFF));
		data.put(msg.toByteArray());
		while (data.position() % 63 > 0) {
			data.put((byte)0);
		}
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epw);
		int chunks = data.position() / 63;
		Log.d("Trezor.messageWrite()", String.format("Writing %d chunks", chunks));
		data.rewind();
		for (int i = 0; i < chunks; i++) {
			byte[] buffer = new byte[64];
			buffer[0] = (byte)'?';
			data.get(buffer, 1, 63);
			/*
			String s = "chunk:";
			for (int j = 0; j < 64; j++) {
				s += String.format(" %02x", buffer[j]);
			}
			Log.i("Trezor.messageWrite()", s);
			*/
			request.queue(ByteBuffer.wrap(buffer), 64);
			conn.requestWait();
		}
      request.close();
	}

	private Message parseMessageFromBytes(MessageType type, byte[] data) {
		Message msg = null;
		Log.i("TrezorParseMessage", String.format("Parsing %s (%d bytes):", type, data.length));
		try {
			if (type.getNumber() == MessageType.MessageType_Success_VALUE) msg = Success.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Failure_VALUE) msg = Failure.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Entropy_VALUE) msg = Entropy.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PublicKey_VALUE) msg = PublicKey.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Features_VALUE) msg = Features.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PinMatrixRequest_VALUE) msg = PinMatrixRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_TxRequest_VALUE) msg = TxRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_ButtonRequest_VALUE) msg = ButtonRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Address_VALUE) msg = Address.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_EntropyRequest_VALUE) msg = EntropyRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_MessageSignature_VALUE) msg = MessageSignature.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PassphraseRequest_VALUE) msg = PassphraseRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_TxSize_VALUE) msg = TxSize.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_WordRequest_VALUE) msg = WordRequest.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			Log.e("TrezorParseMessage", e.toString());
			return null;
		}
		return msg;
	}

	private Message messageRead() {
		ByteBuffer data = ByteBuffer.allocate(32768);
		ByteBuffer buffer = ByteBuffer.allocate(64);
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epr);
		MessageType type;
		int msg_size;
      int repeats=0;
		for (;;) {
			request.queue(buffer, 64);
			conn.requestWait();
			byte[] b = buffer.array();
			Log.d("Trezor.messageRead()", String.format("Read chunk: %d bytes", b.length));

         if (repeats > 100){
            // Sometimes we get infinite empty packets - break after 100
            Log.e("Trezor", "Read aborted after 100 packets.");
            throw new TrezorConnectionException("Unable to read response from trezor");
         }
         repeats ++;

			if (b.length < 9) continue;
			if (b[0] != (byte)'?' || b[1] != (byte)'#' || b[2] != (byte)'#') continue;
			type = MessageType.valueOf((b[3] << 8) + b[4]);
         if (b[0] != (byte)'?') {
            continue;
         }

         // Cast to "unsigned Byte" and combine to int
			msg_size = (
               ((int)b[5]& 0x8F) << (8*3)) +
               (((int)b[6]& 0xFF) << (8*2)) +
               (((int)b[7]& 0xFF) << 8) +
               ((int)b[8]& 0xFF);

			data.put(b, 9, b.length - 9);
			break;
		}
		while (data.position() < msg_size) {
			request.queue(buffer, 64);
			conn.requestWait();
			byte[] b = buffer.array();
			Log.d("Trezor.messageRead()", String.format("Read chunk (cont): %d bytes", b.length));
			data.put(b, 1, b.length - 1);
		}
      request.close();
		return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
	}

	public Message send(Message msg) {
		messageWrite(msg);
		return messageRead();
	}

}
