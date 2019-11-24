package unimelb.bitbox.Connection;

import org.json.simple.JSONObject;
import unimelb.bitbox.ClientSide.ClientSide;
import unimelb.bitbox.Peer;
import unimelb.bitbox.FileHandler.ServerMain;
import unimelb.bitbox.SeverSide.ServerSide;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;


public class ConnectionHost implements Runnable {
    Boolean flag = true;
    private static ArrayList<JSONObject> ConnectedPeers;
    public static ArrayList<Connection> ServerConnectionList;
    public static ArrayList<Connection> ClientConnectionList;
    public static ServerMain fileOperator;

    public static ArrayList<JSONObject> getConnectedPeers() {
        return ConnectedPeers;
    }

    // for transmitting all updating messages
    public static void sendAll(JSONObject json) throws IOException {

        for (Connection connection : ConnectionMap.values()) {
            connection.sendJson(json);
        }
    }

    private static int maximumConnections;
    private static ServerSide serverSide;
    private static ClientSide clientSide;

    public static HashMap<JSONObject, Connection> getConnectionMap() {
        return ConnectionMap;
    }

    // the map of connection and client name on the server side
    private static HashMap<JSONObject, Connection> ConnectionMap;



    public synchronized static int getMaximumConnections() {
        return maximumConnections;
    }

    public synchronized static void AddConnectedPeers(JSONObject peer, Connection c) {
        if (!ConnectedPeers.contains(peer)) {
            ConnectedPeers.add(peer);
            ConnectionMap.put(peer, c);
        } else {
            System.out.println("connection already exists !");
        }

    }

    public ConnectionHost(Peer peer) throws NumberFormatException, NoSuchAlgorithmException, IOException {
        fileOperator = new ServerMain("tcp");
        ServerConnectionList = new ArrayList<>();
        ClientConnectionList = new ArrayList<>();
        ConnectionMap = new HashMap<>();
        ConnectedPeers = new ArrayList<>();
        maximumConnections = peer.getMaximumConnections();
        try {
            serverSide = new ServerSide(peer);
            clientSide = new ClientSide(peer);
            Thread serverThread = new Thread(serverSide);
            Thread clientThread = new Thread(clientSide);
            serverThread.start();
            clientThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static Connection ServerConnection(Socket s)  {
        Connection c = new Connection(s);
        return c;
    }

    public synchronized static Connection ClientConnection(Socket s){
        Connection c = new Connection(s);
        return c;
    }

    // close the connection
    public synchronized void connectionClose(Connection con) {
        if (ServerConnectionList.contains(con))
            ServerConnectionList.remove(con);
        else
            ClientConnectionList.remove(con);
    }

    public void run() {
        while (flag) {
// management and monitor the running connections. No function needed so saved

        }
    }
}
