package com.mrd.bitlib.util;

import com.google.common.base.Joiner;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1String;
import org.spongycastle.asn1.x500.AttributeTypeAndValue;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.RFC4519Style;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.GeneralSecurityException;

public class X509Utils {
    /**
     * Returns either a string that "sums up" the certificate for humans, in a similar manner to what you might see
     * in a web browser, or null if one cannot be extracted. This will typically be the common name (CN) field, but
     * can also be the org (O) field, org+location+country if withLocation is set, or the email
     * address for S/MIME certificates.
     */
    @Nullable
    public static String getDisplayNameFromCertificate(@Nonnull X509Certificate certificate, boolean withLocation) throws CertificateParsingException {
        X500Name name = new X500Name(certificate.getSubjectX500Principal().getName());
        String commonName = null, org = null, location = null, country = null;
        for (RDN rdn : name.getRDNs()) {
            AttributeTypeAndValue pair = rdn.getFirst();
            String val = ((ASN1String) pair.getValue()).getString();
            ASN1ObjectIdentifier type = pair.getType();
            if (type.equals(RFC4519Style.cn))
                commonName = val;
            else if (type.equals(RFC4519Style.o))
                org = val;
            else if (type.equals(RFC4519Style.l))
                location = val;
            else if (type.equals(RFC4519Style.c))
                country = val;
        }
        final Collection<List<?>> subjectAlternativeNames = certificate.getSubjectAlternativeNames();
        String altName = null;
        if (subjectAlternativeNames != null)
            for (final List<?> subjectAlternativeName : subjectAlternativeNames)
                if ((Integer) subjectAlternativeName.get(0) == 1) // rfc822name
                    altName = (String) subjectAlternativeName.get(1);

        if (org != null) {
            return withLocation ? Joiner.on(", ").skipNulls().join(org, location, country) : org;
        } else if (commonName != null) {
            return commonName;
        } else {
            return altName;
        }
    }

    /** The string that prefixes all text messages signed using Bitcoin keys. */
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
    private static final byte[] BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(StandardCharsets.UTF_8);

    /**
     * <p>Given a textual message, returns a byte buffer formatted as follows:</p>
     * <p>{@code [24] "Bitcoin Signed Message:\n" [message.length as a varint] message}</p>
     */

    public static byte[] formatMessageForSigning(String message) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.length);
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES);
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            VarInt size = new VarInt(messageBytes.length);
            bos.write(size.encode());
            bos.write(messageBytes);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /** Returns a key store loaded from the given stream. Just a convenience around the Java APIs. */
    public static KeyStore loadKeyStore(String keystoreType, @Nullable String keystorePassword, InputStream is)
            throws KeyStoreException {
        try {
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            keystore.load(is, keystorePassword != null ? keystorePassword.toCharArray() : null);
            return keystore;
        } catch (IOException x) {
            throw new KeyStoreException(x);
        } catch (GeneralSecurityException x) {
            throw new KeyStoreException(x);
        } finally {
            try {
                is.close();
            } catch (IOException x) {
                // Ignored.
            }
        }
    }
}