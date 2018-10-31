package com.satoshilabs.trezor.lib;

// based on https://github.com/trezor/trezor-android
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.*;
import android.util.Log;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ExternalSignatureDevice {
    private static final String TAG = "ExtSig";
    private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";
    private UsbDeviceConnection conn;
    private String serial;
    private UsbEndpoint epr, epw;
    private UsbManager usbManager;
    private UsbInterface iface;

    private final LinkedBlockingQueue<Boolean> gotRights = new LinkedBlockingQueue<>(1);

    /**
     * Receives broadcast when a supported USB device is attached, detached or
     * when a permission to communicate to the device has been granted.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            String deviceName = usbDevice.getDeviceName();

            if (ACTION_USB_PERMISSION.equals(action)) {
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                Log.d(TAG, "ACTION_USB_PERMISSION: " + permission + " Device: " + deviceName);

                gotRights.clear();
                // sync with connect
                gotRights.add(permission);
                context.unregisterReceiver(mUsbReceiver);
            }
        }
    };

    public ExternalSignatureDevice(Context context) {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    abstract UsbDeviceId getUsbId();

    abstract public String getDefaultAccountName();

    abstract public VersionNumber getMostRecentFirmwareVersion();

    abstract public String getDeviceConfiguratorAppName();

    private UsbDevice getExtSigDevice() {
        if (usbManager == null) {
            return null;
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            // check if the device has the expected usb id
            if (!getUsbId().isSame(device.getVendorId(), device.getProductId())) {
                continue;
            }
            Log.i(TAG, "ExtSig " + getDefaultAccountName() + " device found");
            if (device.getInterfaceCount() < 1) {
                Log.e(TAG, "Wrong interface count");
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
                Log.e(TAG, "Could not find read endpoint");
                continue;
            }
            if (epw == null) {
                Log.e(TAG, "Could not find write endpoint");
                continue;
            }
            if (epr.getMaxPacketSize() != 64) {
                Log.e(TAG, "Wrong packet size for read endpoint");
                continue;
            }
            if (epw.getMaxPacketSize() != 64) {
                Log.e(TAG, "Wrong packet size for write endpoint");
                continue;
            }

            return device;
        }

        return null;
    }

    public boolean isDevicePluggedIn() {
        if (usbManager == null) {
            return false;
        }

        return (getExtSigDevice() != null);
    }

    public boolean connect(final Context context) {
        if (usbManager == null) {
            return false;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);

        final UsbDevice extSigDevice = getExtSigDevice();
        if (extSigDevice == null) {
            return false;
        }

        final Intent intent = new Intent(ACTION_USB_PERMISSION);

        // clear the token-queue - under some circumstances it might happen that the requestPermission
        // callback already returned but this functions wasn't waiting anymore
        gotRights.clear();
        usbManager.requestPermission(extSigDevice, PendingIntent.getBroadcast(context, 0, intent, 0));


        // retry because of InterruptedException
        while (true) {
            try {
                // gotRights.take blocks until the UsbManager gives us the rights via callback to the BroadcastReceiver
                // this might need an user interaction
                return gotRights.take() && initConnection(extSigDevice);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private boolean initConnection(UsbDevice device) {
        // try to open the device
        UsbDeviceConnection conn = usbManager.openDevice(device);
        if (conn == null) {
            Log.e(TAG, "Could not open connection");
            return false;
        }
        boolean claimed = conn.claimInterface(iface, true);
        if (!claimed) {
            Log.e(TAG, "Could not claim interface");
            return false;
        }

        this.conn = conn;
        this.serial = this.conn.getSerial();

        return true;
    }

    @Override
    public String toString() {
        return getDefaultAccountName().toUpperCase() + "(#" + this.serial + ")";
    }

    private void messageWrite(Message msg) {
        int msg_size = msg.getSerializedSize();
        String msg_name = msg.getClass().getSimpleName();
        int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
        Log.d(TAG, String.format("Got message: %s", msg_name));
        ByteBuffer data = ByteBuffer.allocate(32768)
                .put((byte) '#')
                .put((byte) '#')
                .put((byte) ((msg_id >> 8) & 0xFF))
                .put((byte) (msg_id & 0xFF))
                .put((byte) ((msg_size >> 24) & 0xFF))
                .put((byte) ((msg_size >> 16) & 0xFF))
                .put((byte) ((msg_size >> 8) & 0xFF))
                .put((byte) (msg_size & 0xFF))
                .put(msg.toByteArray());
        while (data.position() % 63 > 0) {
            data.put((byte) 0);
        }
        UsbRequest request = new UsbRequest();
        request.initialize(conn, epw);
        int chunks = data.position() / 63;
        Log.d(TAG, String.format("Writing %d chunks", chunks));
        data.rewind();
        for (int i = 0; i < chunks; i++) {
            byte[] buffer = new byte[64];
            buffer[0] = (byte) '?';
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
        Log.i(TAG, String.format("Parsing %s (%d bytes):", type, data.length));
        try {
            switch (type) {
                case MessageType_Success:
                    return Success.parseFrom(data);
                case MessageType_Failure:
                    return Failure.parseFrom(data);
                case MessageType_Entropy:
                    return Entropy.parseFrom(data);
                case MessageType_PublicKey:
                    return PublicKey.parseFrom(data);
                case MessageType_Features:
                    return Features.parseFrom(data);
                case MessageType_PinMatrixRequest:
                    return PinMatrixRequest.parseFrom(data);
                case MessageType_TxRequest:
                    return TxRequest.parseFrom(data);
                case MessageType_ButtonRequest:
                    return ButtonRequest.parseFrom(data);
                case MessageType_Address:
                    return Address.parseFrom(data);
                case MessageType_EntropyRequest:
                    return EntropyRequest.parseFrom(data);
                case MessageType_MessageSignature:
                    return MessageSignature.parseFrom(data);
                case MessageType_PassphraseRequest:
                    return PassphraseRequest.parseFrom(data);
                case MessageType_TxSize:
                    return TxSize.parseFrom(data);
                case MessageType_WordRequest:
                    return WordRequest.parseFrom(data);
                default:
                    return null;
            }
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    private Message messageRead() {
        ByteBuffer data = ByteBuffer.allocate(32768);
        ByteBuffer buffer = ByteBuffer.allocate(64);
        UsbRequest request = new UsbRequest();
        request.initialize(conn, epr);
        MessageType type;
        int msg_size;
        int repeats = 0;
        for (; ; ) {
            request.queue(buffer, 64);
            conn.requestWait();
            byte[] b = buffer.array();
            Log.d(TAG, String.format("Read chunk: %d bytes", b.length));

            if (repeats > 100) {
                // Sometimes we get infinite empty packets - break after 100
                Log.e(TAG, "Read aborted after 100 packets.");
                throw new ExtSigDeviceConnectionException("Unable to read response from ExtSigDevice");
            }
            repeats++;

            if (b.length < 9) {
                continue;
            }
            if (b[0] != (byte) '?' || b[1] != (byte) '#' || b[2] != (byte) '#') {
                continue;
            }
            type = MessageType.valueOf((b[3] << 8) + b[4]);
            if (b[0] != (byte) '?') {
                continue;
            }

            // Cast to "unsigned Byte" and combine to int
            msg_size = (
                    ((int) b[5] & 0x8F) << (8 * 3)) +
                    (((int) b[6] & 0xFF) << (8 * 2)) +
                    (((int) b[7] & 0xFF) << 8) +
                    ((int) b[8] & 0xFF);

            data.put(b, 9, b.length - 9);
            break;
        }
        while (data.position() < msg_size) {
            request.queue(buffer, 64);
            conn.requestWait();
            byte[] b = buffer.array();
            Log.d(TAG, String.format("Read chunk (cont): %d bytes", b.length));
            data.put(b, 1, b.length - 1);
        }
        request.close();
        return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
    }

    public Message send(Message msg) {
        messageWrite(msg);
        return messageRead();
    }

    protected interface UsbDeviceId {
        boolean isSame(int vendorId, int deviceId);
    }

    protected static class SingleUsbDeviceId implements UsbDeviceId {
        public final int vendorId;
        public final int deviceId;

        protected SingleUsbDeviceId(int vendorId, int deviceId) {
            this.vendorId = vendorId;
            this.deviceId = deviceId;
        }

        @Override
        public boolean isSame(int vendorId, int deviceId) {
            return vendorId == this.vendorId && deviceId == this.deviceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SingleUsbDeviceId that = (SingleUsbDeviceId) o;

            if (vendorId != that.vendorId) {
                return false;
            }
            return deviceId == that.deviceId;

        }

        @Override
        public int hashCode() {
            int result = vendorId;
            result = 31 * result + deviceId;
            return result;
        }
    }

    protected static class UsbDeviceIds implements UsbDeviceId {
        private final HashSet<UsbDeviceId> deviceIds;

        protected UsbDeviceIds(SingleUsbDeviceId... deviceIds) {
            this.deviceIds = new HashSet<UsbDeviceId>(Arrays.asList(deviceIds));
        }

        @Override
        public boolean isSame(int vendorId, int deviceId) {
            for (UsbDeviceId id : deviceIds) {
                if (id.isSame(vendorId, deviceId)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class VersionNumber {
        public final int major;
        public final int minor;
        public final int patch;

        public VersionNumber(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public boolean isNewerThan(int major, int minor, int patch) {
            if (this.major > major) {
                return true;
            } else if ((this.major == major) && (this.minor > minor)) {
                return true;
            } else if  ((this.major == major) && (this.minor == minor) && (this.patch > patch))  {
                return true;
            }

            return false;
        }
    }
}
