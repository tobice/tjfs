package edu.uno.cs.tjfs.common;

import java.net.*;
import java.util.Enumeration;

/** Simple detection tool to obtain local IP */
public class IpDetect {

    /**
     * Detect and return local IP address.
     *
     * As one computer can have multiple interfaces and each interface can have assigned multiple
     * IP addresses, it is difficult to get that "one" IP address we need. To get rid of the
     * ambiguity, the IP address that exists in the sam subnet as Zookeeper's IP address is
     * returned. If Zookeeper and this machine are not in the same subnet, we reduce the prefix
     * length (mask) and try again. That way we return the IP that is "closest" to Zookeeper's
     * address.
     *
     * Currently only Ipv4 is supported and tested.
     *
     * @param zookeeper IP address of Zookeeper instance that this computer is connected to.
     * @return local IP address that belongs to the same subnet as Zookeeper's IP address.
     * @throws TjfsException if the given Zookeeper's address is malformed, or if we fail to obtain
     *  the list of current network interfaces and their IP addresses, or if there is not suitable
     *  IP address found.
     */
    public static String getLocalIp(String zookeeper) throws TjfsException {
        try {
            byte[] zookeeperIp = InetAddress.getByName(zookeeper).getAddress();
            for (int prefixTolerance = 0; prefixTolerance <= 24; prefixTolerance += 8) {
                for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                    NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                    for (InterfaceAddress address : iface.getInterfaceAddresses()) {
                        byte[] ip = address.getAddress().getAddress();
                        int prefix = Math.min(8, address.getNetworkPrefixLength() - prefixTolerance);
                        if (sameSubnet(ip, zookeeperIp, prefix)) {
                            return address.getAddress().getHostAddress();
                        }
                    }
                }
            }
            throw new TjfsException("No address in the same subnet as Zookeeper was found!");
        } catch (SocketException e) {
            throw new TjfsException("Could not get the list of network interfaces", e);
        } catch (UnknownHostException e) {
            throw new TjfsException("Zookeeper IP address does not look like a valid IP address", e);
        }
    }

    /**
     * Test if two addresses belong to the same subnet based on given prefix length.
     * Only Ipv4 is supported at this time.
     * @param ip1 first IP
     * @param ip2 second IP
     * @param prefixLength prefix length that should be the same in bits (8, 16, 24...)
     * @return if both addresses belong to the same subnet
     */
    protected static boolean sameSubnet(byte[] ip1, byte[] ip2, int prefixLength) {
        for (int i = 0; i < prefixLength / 8; i++) {
            if (ip1[i] != ip2[i]) {
                return false;
            }
        }

        return true;
    }
}
