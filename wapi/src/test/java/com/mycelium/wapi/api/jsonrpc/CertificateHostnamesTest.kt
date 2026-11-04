package com.mycelium.wapi.api.jsonrpc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateHostnamesTest {
    @Test
    fun exactName() {
        assertTrue(CertificateHostnames.matches("mycelium.com", "mycelium.com"))
        assertTrue(CertificateHostnames.matches("Electrumx-BTC-3a.mycelium.com.", "electrumx-btc-3a.mycelium.com"))
        assertFalse(CertificateHostnames.matches("electrumx-btc-3a.mycelium.com", "mycelium.com"))
    }

    @Test
    fun wildcardCoversOneLabel() {
        assertTrue(CertificateHostnames.matches("electrumx-btc-3a.mycelium.com", "*.mycelium.com"))
        assertFalse(CertificateHostnames.matches("mycelium.com", "*.mycelium.com"))
        assertFalse(CertificateHostnames.matches("a.b.mycelium.com", "*.mycelium.com"))
    }

    @Test
    fun otherDomainsDoNotMatch() {
        assertFalse(CertificateHostnames.matches("electrumx-btc-3a.mycelium.com", "*.attacker.example"))
        assertFalse(CertificateHostnames.matches("evilmycelium.com", "*.mycelium.com"))
        assertFalse(CertificateHostnames.matches("mycelium.com.attacker.example", "*.mycelium.com"))
        assertFalse(CertificateHostnames.matches("mycelium.com", "*.com"))
    }
}
