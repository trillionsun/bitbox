package unimelb.bitbox.Connection;

import org.json.simple.JSONObject;
import unimelb.bitbox.Peer;
import unimelb.bitbox.FileHandler.ServerMain;
import unimelb.bitbox.ClientSide.UDPClientSide;
import unimelb.bitbox.SeverSide.UDPServerSide;

import java.io.IOException;
import java.net.DatagramPacket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class UDPConnectionHost implements Runnable {
    Boolean flag = true;
    private static ArrayList<JSONObject> ConnectedPeers;

    public synchronized static ArrayList<UDPConnection> getServerConnectionList() {
        return ServerConnectionList;
    }

    public synchronized static ArrayList<UDPConnection> getClientConnectionList() {
        return ClientConnectionList;
    }

    private static ArrayList<UDPConnection> ServerConnectionList;
    private static ArrayList<UDPConnection> ClientConnectionList;
    public static ServerMain fileOperator;
    private static HashMap<JSONObject, UDPConnection> ConnectionMap;

    public synchronized static ArrayList<JSONObject> getConnectedPeers() {
        return ConnectedPeers;
    }

    public static void UDPsendAll(JSONObject json) throws IOException {

        for (UDPConnection connection : ConnectionMap.values()) {
            byte[] data = json.toJSONString().getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, connection.getAddress(), connection.getPort());
            connection.getDatagramSocket().send(packet);
        }
    }

    public synchronized static void AddServerConnectionList(UDPConnection c) {
        ServerConnectionList.add(c);
    }

    private static int maximumConnections;
    private static UDPServerSide serverSide;
    private static UDPClientSide clientSide;

    public synchronized static HashMap<JSONObject, UDPConnection> getConnectionMap() {
        return ConnectionMap;
    }


    public synchronized static int getConnectionNum() {
        return ServerConnectionList.size();
    }

    public synchronized static int getMaximumConnections() {
        return maximumConnections;
    }

    public synchronized static void AddConnectedPeers(JSONObject peer, UDPConnection c) {
        if (!ConnectedPeers.contains(peer)) {
            ConnectedPeers.add(peer);
            ConnectionMap.put(peer, c);
        } else {
            System.out.println("connection already exists !");
        }

    }

    public synchronized static void AddClientConnectionList(UDPConnection c) {
        ClientConnectionList.add(c);
    }

    public synchronized static void RemoveConnectedPeers(String peer, UDPConnection c) {
        if (!ConnectedPeers.contains(peer)) {
            ConnectedPeers.remove(peer);
            ConnectionMap.remove(peer, c);
        } else {
            System.out.println("connection doesn't exists !");
        }
    }


    public UDPConnectionHost(Peer peer) throws IOException, NoSuchAlgorithmException {
        fileOperator = new ServerMain("udp");
        ServerConnectionList = new ArrayList<>();
        ClientConnectionList = new ArrayList<>();
        ConnectionMap = new HashMap<>();
        ConnectedPeers = new ArrayList<>();
        maximumConnections = peer.getMaximumConnections();

        serverSide = new UDPServerSide(peer);
        clientSide = new UDPClientSide(peer);
        Thread serverThread = new Thread(serverSide);
        Thread clientThread = new Thread(clientSide);
        serverThread.start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        clientThread.start();

    }

    public void run() {
        while (flag) {
        }


    }
}
