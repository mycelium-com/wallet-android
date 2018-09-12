package com.mycelium.wapi.api

import com.mycelium.WapiLogger
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ConnectionManagerTest {
    val logger = mock(WapiLogger::class.java)
    @Before
    fun setup() {
    }

    @Test
    fun changeEndpointsEmptyTest() {
        val oldEndpoints = arrayOf(TcpEndpoint("bla", 5), TcpEndpoint("foo", 6))
        val connectionManager = ConnectionManager(1, oldEndpoints, logger)
        val newEndpoints = emptyArray<TcpEndpoint>()
        connectionManager.changeEndpoints(newEndpoints)
        Assert.assertEquals(2, connectionManager.endpoints.size)
    }

    @Test
    fun changeEndpointsEqualTest() {
        val oldEndpoints = arrayOf(TcpEndpoint("bla", 5), TcpEndpoint("foo", 6))
        val connectionManager = ConnectionManager(1, oldEndpoints, logger)
        val newEndpoints = arrayOf(TcpEndpoint("bla", 5), TcpEndpoint("foo", 6))
        connectionManager.changeEndpoints(newEndpoints)
        Assert.assertTrue((oldEndpoints === connectionManager.endpoints))
    }

    @Test
    fun changeEndpointsDifferentTest() {
        val oldEndpoints = arrayOf(TcpEndpoint("bla", 5), TcpEndpoint("foo", 6))
        val connectionManager = ConnectionManager(1, oldEndpoints, logger)
        val newEndpoints = arrayOf(TcpEndpoint("bla", 5), TcpEndpoint("foo", 6), TcpEndpoint("bla", 7))
        connectionManager.changeEndpoints(newEndpoints)
        Assert.assertEquals(3, connectionManager.endpoints.size)
    }
}