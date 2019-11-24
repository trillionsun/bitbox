package unimelb.bitbox;

import unimelb.bitbox.Connection.ConnectionHost;
import unimelb.bitbox.Connection.UDPConnectionHost;
import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class Peer {
    private static Logger log = Logger.getLogger(Peer.class.getName());
    private static int maximumConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
    private static int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
    private String mode;
    private String address;
    private int portNo;
    private String[] peers;

    // Construction function
    public Peer() {
        mode = Configuration.getConfigurationValue("mode");
        address = Configuration.getConfigurationValue("advertisedName");
        peers = Configuration.getConfigurationValue("peers").split(",");
        if (mode.equals("tcp")) {
            portNo = Integer.parseInt(Configuration.getConfigurationValue("port"));
        } else {
            portNo = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        }
    }

// getter for all necessary attributes of the peer.
    public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        Peer peer1 = new Peer();
        if (peer1.mode.equals("tcp")) {

            ConnectionHost host = new ConnectionHost(peer1);
            Thread HostThread = new Thread(host);
            HostThread.start();
        } else {
            UDPConnectionHost host = new UDPConnectionHost(peer1);
            Thread HostThread = new Thread(host);
            HostThread.start();
        }
        /*  listening testing    */
        // peer1.listen();

        /* sending testing */
        //peer1.sendCommand(8112, "localhost", "saved for different command");
    }
    public String[] getPeers() {
        return peers;
    }

    public  int getPortNo() {
        return portNo;
    }

    public static int getBlockSize() {
        return blockSize;
    }

    public static int getMaximumConnections() {
        return maximumConnections;
    }

}


