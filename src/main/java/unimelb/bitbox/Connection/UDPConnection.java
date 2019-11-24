package unimelb.bitbox.Connection;

import org.json.simple.JSONObject;

import java.net.DatagramSocket;
import java.net.InetAddress;


// Storing  the  address and port of the client socket, not a persistent connection

public class UDPConnection {
    private int port;

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return address;
    }


    private InetAddress address;

    // hosting socket
    private DatagramSocket receiver;

    public DatagramSocket getDatagramSocket() {
        return receiver;
    }

    // sending part;    listening part;
    public UDPConnection(InetAddress address, int port, DatagramSocket socket) {
        this.port = port;
        this.address = address;
        this.receiver = socket;
    }

}





