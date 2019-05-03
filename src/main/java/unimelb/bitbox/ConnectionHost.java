package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// need to unify the value of ConnectionList and ConnectedPeers

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
    public synchronized  static void AddServerConnectionList(Connection c)
    {
        ServerConnectionList.add(c);
    }

    private static int maximumConnections;
    private static ServerSide serverSide;
    private static ClientSide clientSide;

    public static HashMap<JSONObject, Connection> getConnectionMap() {
        return ConnectionMap;
    }

    // the map of connection and client name on the server side
    private static HashMap<JSONObject, Connection> ConnectionMap;

    public synchronized static boolean RemoveMapByConnection(Connection c) {
        if (ConnectionMap.containsValue(c)) {
            ConnectionMap.values().remove(c);
            return true;
        } else
            return false;
    }

    public synchronized static int getConnectionNum() {
        return ServerConnectionList.size();
    }

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

    public synchronized  static void AddClientConnectionList(Connection c)
    {
        ClientConnectionList.add(c);
    }

    public synchronized static void RemoveConnectedPeers(String peer, Connection c) {
        if (!ConnectedPeers.contains(peer)) {
            ConnectedPeers.remove(peer);
            ConnectionMap.remove(peer, c);
        } else {
            System.out.println("connection doesn't exists !");
        }
    }

    public ConnectionHost(Peer peer) throws NumberFormatException, NoSuchAlgorithmException, IOException
    {
        fileOperator = new ServerMain();
        ServerConnectionList= new ArrayList<>();
        ClientConnectionList = new ArrayList<>();
        ConnectionMap = new HashMap<>();
        ConnectedPeers = new ArrayList<>();
        maximumConnections=peer.getMaximumConnections();
        try{

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

    public synchronized static Connection ServerConnection(Socket s) throws IOException
    {
        Connection c = new Connection(s);
        return c;
    }

    public synchronized static Connection ClientConnection(Socket s) throws IOException {
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

        }
    }
}
