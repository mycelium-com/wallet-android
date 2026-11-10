package com.mycelium.wapi.api.jsonrpc

import java.net.Socket
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Layers TLS over the connected [plain] socket and checks the certificate is issued for [host].
 *
 * A bare SSLSocket only checks that some trusted CA issued the certificate, for whatever domain,
 * so anyone on the network path with a free certificate for their own domain could play our
 * Electrum server. Naming the host also sends it as SNI.
 */
internal fun startVerifiedTls(ssf: SSLSocketFactory, plain: Socket, host: String, port: Int): SSLSocket {
    val tls = try {
        ssf.createSocket(plain, host, port, true) as SSLSocket
    } catch (e: Exception) {
        plain.close()
        throw e
    }
    try {
        tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
        tls.startHandshake()
        // Checked here as well: not every TLS provider honors the endpoint identification parameter
        val certificate = tls.session.peerCertificates.firstOrNull() as? X509Certificate
        if (certificate == null || !CertificateHostnames.matches(host, certificate)) {
            throw SSLPeerUnverifiedException("Certificate is not issued for $host")
        }
        return tls
    } catch (e: Exception) {
        tls.close()
        throw e
    }
}

internal object CertificateHostnames {
    private const val DNS_NAME = 2

    fun matches(host: String, certificate: X509Certificate): Boolean =
        certificate.subjectAlternativeNames.orEmpty()
            .filter { it[0] == DNS_NAME }
            .any { matches(host, it[1] as String) }

    /** [pattern] is a DNS name, its wildcard standing for exactly one leftmost label. */
    fun matches(host: String, pattern: String): Boolean {
        val h = host.lowercase(Locale.ROOT).trimEnd('.')
        val p = pattern.lowercase(Locale.ROOT).trimEnd('.')
        if (!p.startsWith("*.")) return h == p
        val suffix = p.substring(1)
        // "*.com" and the like are no certificate a CA issues
        if (suffix.count { it == '.' } < 2) return false
        return h.endsWith(suffix) && h.indexOf('.') == h.length - suffix.length && h.length > suffix.length
    }
}
