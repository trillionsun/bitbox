package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class UDPProcessing implements  Runnable {
    String message;
    String task;
    Boolean HandShakeFlag = false;
    byte[] buffer;

    // for receiving
    DatagramPacket inputPacket;
    DatagramPacket outputPacket = null;
    DatagramSocket hostingSocket;

    // for operation
    DatagramPacket  incomingPacket;
    InetAddress address;
    int port;

    JSONObject incomingPeer;

    public static byte[] trim(byte[] bytes)
    {
        int i = 0;
        while (i <= bytes.length-1 && bytes[i] != 0)
        {
            ++i;
        }

        return Arrays.copyOf(bytes, i);
    }


    public void UDPsendJson(JSONObject json) throws IOException {
        byte[] data = json.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, this.address, this.port);
        this.hostingSocket.send(packet);
    }

    // send connection related command
    public void UDPsend(String command) throws IOException {
        JSONObject jObj = new JSONObject();
        JSONObject HostingPeer = new JSONObject();
        HostingPeer.put("host", "127.0.0.1");
        HostingPeer.put("port", hostingSocket.getLocalPort());
        jObj.put("command", command);
        if (command.equals("HANDSHAKE_REQUEST") || command.equals("HANDSHAKE_RESPONSE")) {
            jObj.put("hostPort", HostingPeer);
        } else if (command.equals("INVALID_PROTOCOL")) {
            jObj.put("message", "message must contain a command field as string");

        } else if (command.equals("CONNECTION_REFUSED")) {
            JSONArray peers = new JSONArray();
            for (JSONObject peer : ConnectionHost.getConnectedPeers()) {
                JSONObject peerN = new JSONObject();
                peerN = peer;
                peers.add(peerN);
            }
            jObj.put("message", "connection limit reached");
            jObj.put("peers", peers);
        }
        byte[] data = jObj.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, this.address, this.port);
        this.hostingSocket.send(packet);
    }


    public void UDPclose() {
        new Thread() {
            public void run() {
                synchronized (hostingSocket) {
                    hostingSocket.close();
                }
            }
        }.start();
    }


    public UDPProcessing(  InetAddress IncomingAddress, int incomingPort, byte[] data, DatagramSocket host) {
        buffer = new byte[Integer.parseInt(Configuration.getConfigurationValue("blockSize"))];
        hostingSocket = host;
        buffer = data;
        message = new String(trim(buffer));
        this.address = IncomingAddress;
        this.port = incomingPort;
        for (UDPConnection c: UDPConnectionHost.getConnectionMap().values())
        {
            if(c.getPort()==this.port)
            {
                HandShakeFlag = true;
                break;
            }
        }
    }

    public void run() {
        // parse the request and get the command
        JSONObject json = new JSONObject();
        JSONObject inComingPeer;

        try {
            json = (JSONObject) new JSONParser().parse(this.message);
        } catch (ParseException e) {
            e.printStackTrace();
            try {
                this.UDPsend("INVALID_PROTOCOL");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        String command = "";
        try {
            command = json.get("command").toString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            try {
                this.UDPsend("INVALID_PROTOCOL");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


        // protocol operation
        if (command.equals("HANDSHAKE_REQUEST") && !HandShakeFlag) {
            inComingPeer = (JSONObject) json.get("hostPort");
            System.out.println("handshake request received from " + inComingPeer);
            // if it is a replicated handshake request.
            if (UDPConnectionHost.getConnectedPeers().contains(inComingPeer)) {
                try {
                    this.UDPsend("INVALID_PROTOCOL");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("replicated request!");
            } else {
                if (UDPConnectionHost.getConnectionNum() < UDPConnectionHost.getMaximumConnections()) {
                    try {
                        this.UDPsend("HANDSHAKE_RESPONSE");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    this.incomingPeer = inComingPeer;
                    System.out.println("Handshake response sent!");
                    UDPConnection c = new UDPConnection(this.address, this.port, this.hostingSocket);
                    UDPConnectionHost.AddServerConnectionList(c);
                    UDPConnectionHost.AddConnectedPeers(inComingPeer, c);

                    // !!!!sync methods , needed to be updated
//                        UDPConnectionHost.fileOperator.getSync();

                } else {
                    try {
                        UDPsend("CONNECTION_REFUSED");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Handshake refused message sent");
                }
            }
        } else if (command.equals("HANDSHAKE_RESPONSE")) {
            inComingPeer = (JSONObject) json.get("hostPort");
            UDPConnection c = new UDPConnection(this.address, this.port, this.hostingSocket);
            if (!UDPConnectionHost.getConnectedPeers().contains(inComingPeer)) {
                this.incomingPeer = inComingPeer;
                UDPConnectionHost.AddConnectedPeers(inComingPeer, c);
                UDPConnectionHost.AddClientConnectionList(c);
                System.out.println("connection from the " +inComingPeer+ "is established.");
                //              UDPConnectionHost.fileOperator.getSync();
            }
        } else if (command.equals("INVALID_PROTOCOL")) {
            System.out.println("connection been refused by protocol problems.");
        } else if (command.equals("CONNECTION_REFUSED")) {
            System.out.println("connection been refused by incoming limit.");
        } else {
            if (HandShakeFlag)
            {
                if (command.equals("FILE_CREATE_REQUEST")) {
                    System.out.println("FILE_CREATE_REQUEST received from " + this.incomingPeer);
                    //   1. the wait and retransmist.  2. replicated message

                } else if (command.equals("FILE_CREATE_RESPONSE")) {


                } else if (command.equals("FILE_BYTES_REQUEST")) {


                } else if (command.equals("FILE_BYTES_RESPONSE")) {


                } else if (command.equals("FILE_DELETE_REQUEST")) {


                } else if (command.equals("FILE_DELETE_RESPONSE")) {


                } else if (command.equals("FILE_MODIFY_REQUEST")) {


                } else if (command.equals("FILE_MODIFY_RESPONSE")) {


                } else if (command.equals("DIRECTORY_CREATE_REQUEST")) {


                } else if (command.equals("DIRECTORY_CREATE_RESPONSE")) {


                } else if (command.equals("DIRECTORY_DELETE_REQUEST")) {


                } else if (command.equals("DIRECTORY_DELETE_RESPONSE")) {


                } else {
                    try {
                        this.UDPsend("INVALID_PROTOCOL");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                try {
                    this.UDPsend("INVALID_PROTOCOL");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(UDPConnectionHost.getServerConnectionList().size()+" incoming connections "+UDPConnectionHost.getClientConnectionList().size()+" outgoing connections");
    }

}
