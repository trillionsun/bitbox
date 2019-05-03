package unimelb.bitbox;
import unimelb.bitbox.util.Configuration;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;


public class Peer
{
    private String address;
    private int portNo;

    public String[] getPeers() {
        return peers;
    }

    private String[] peers;



    private static Logger log = Logger.getLogger(Peer.class.getName());
    private static final int maximumConnections= Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
    private static final int synxInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));



// getter for all necessary attributes of the peer.


    public int getPortNo() {
        return portNo;
    }

    public int getMaximumConnections() {
        return maximumConnections;
    }

    public int getSynxInterval() {
        return synxInterval;
    }

    // setter for NumConnection and incomingPeers








    // Construction function
    public Peer() {
        address=  Configuration.getConfigurationValue("advertisedName");
        portNo = Integer.parseInt(Configuration.getConfigurationValue("port"));
        peers = Configuration.getConfigurationValue("peers").split(",");
    }

    public static void main(String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();



       Peer peer1= new Peer();
       ConnectionHost host = new ConnectionHost(peer1);
       Thread HostThread = new Thread(host);
       HostThread.start();











        /*  listening testing    */
       // peer1.listen();

       /* sending testing */
       //peer1.sendCommand(8112, "localhost", "saved for different command");


    }
}


