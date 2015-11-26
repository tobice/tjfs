package edu.uno.cs.tjfs.common;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class IpDetectTest {

    protected byte[] ip(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip).getAddress();
    }

    @Test
    public void testGetLocalIp() throws Exception {
        String zookeeper = "127.0.0.2";
        assertThat(IpDetect.getLocalIp(zookeeper), equalTo("127.0.0.1"));
    }

    @Test
    public void testSameSubnet() throws Exception {
        assertTrue(IpDetect.sameSubnet(ip("192.168.0.1"), ip("192.168.0.2"), 0));
        assertTrue(IpDetect.sameSubnet(ip("192.168.0.1"), ip("192.168.0.2"), 8));
        assertTrue(IpDetect.sameSubnet(ip("192.168.0.1"), ip("192.168.0.2"), 16));
        assertTrue(IpDetect.sameSubnet(ip("192.168.0.1"), ip("192.168.0.2"), 24));
        assertFalse(IpDetect.sameSubnet(ip("192.168.0.1"), ip("192.168.0.2"), 32));

        assertTrue(IpDetect.sameSubnet(ip("192.168.100.1"), ip("192.168.200.2"), 0));
        assertTrue(IpDetect.sameSubnet(ip("192.168.100.1"), ip("192.168.200.2"), 8));
        assertTrue(IpDetect.sameSubnet(ip("192.168.100.1"), ip("192.168.200.2"), 16));
        assertFalse(IpDetect.sameSubnet(ip("192.168.100.1"), ip("192.168.200.2"), 24));

        assertTrue(IpDetect.sameSubnet(ip("127.0.0.1"), ip("127.0.0.1"), 32));
    }
}