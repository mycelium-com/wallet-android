package com.mrd.bitlib.util;

import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class SslUtilsTest {

    // Guards against a malformed bundled ISRG Root X1 PEM: a base64 typo would make
    // CertificateFactory throw while building the factory, breaking every ElectrumX
    // connection on Android 7.0 (the very devices this factory exists to support).
    @Test
    public void systemPlusLetsEncryptSocketFactoryBuilds() {
        SSLSocketFactory factory = SslUtils.getSystemPlusLetsEncryptSocketFactory();
        assertNotNull(factory);
        // Result is cached, so a second call must return the same instance.
        assertSame(factory, SslUtils.getSystemPlusLetsEncryptSocketFactory());
    }
}
