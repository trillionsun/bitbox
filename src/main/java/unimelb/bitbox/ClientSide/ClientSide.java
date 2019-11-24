package unimelb.bitbox.ClientSide;

// 2: 17 all update  change all the connected peers and peers to a <JSONObject> to avoid the format error

import org.json.simple.JSONObject;
import unimelb.bitbox.Connection.Connection;
import unimelb.bitbox.Connection.ConnectionHost;
import unimelb.bitbox.Peer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// sending thread, needs to implement all the different commands


public class ClientSide extends clientTask implements Runnable {

    private ArrayList<JSONObject> peers;
    private PrintWriter writer;
    private int hostingPort;

    public ClientSide(Peer peer) {

        String[] Peers = peer.getPeers();
        peers = new ArrayList<>();

        for (int i = 0; i < Peers.length; i++) {
            String Opeer = Peers[i];
            String[] peerHost = Opeer.split(":", 2);
            // convert the unrecognized localhost
            if (peerHost[0].equals("localhost")) {
                peerHost[0] = "127.0.0.1";
            }
            JSONObject json = new JSONObject();
            json.put("host", peerHost[0]);
            json.put("port", Integer.parseInt(peerHost[1]));
            peers.add(json);
        }
        hostingPort = peer.getPortNo();
    }

    public static boolean hostAvailabilityCheck(String address, int port) {
        try (Socket s = new Socket(address, port)) {
            s.close();
            return true;
        } catch (IOException ex) {
            /* ignore */
        }
        System.out.println("Connection failed cause " + address + " : " + port + " not alive");
        return false;
    }

    public void run() {
        ArrayList<JSONObject> ConnectedPeers = ConnectionHost.getConnectedPeers();
        for (JSONObject Opeer : peers) {
            if (!ConnectedPeers.contains(Opeer)) {

                JSONObject jsonP = Opeer;
                String OpeerHost = (String) jsonP.get("host");
                int OpeerPort = (int) (jsonP.get("port"));
                if (hostAvailabilityCheck(OpeerHost, OpeerPort)) {

                    Socket clientSocket = null;
                    try {
                        clientSocket = new Socket(OpeerHost, OpeerPort);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println("try to connect with :" + OpeerHost + " : " + OpeerPort);
                    JSONObject json = new JSONObject();
                    JSONObject hostPort = new JSONObject();
                    json.put("command", "HANDSHAKE_REQUEST");
                    String[] address = clientSocket.getLocalAddress().toString().split("/", 2);
                    hostPort.put("host", address[1]);
                    hostPort.put("port", hostingPort);
                    json.put("hostPort", hostPort);
                    writer.println(json.toJSONString());
                    writer.flush();
                    System.out.println("send handshake request to " + Opeer);
                    Connection c = ConnectionHost.ClientConnection(clientSocket);
                    Thread ClientConnection = new Thread(c);
                    ClientConnection.start();
                }

            }
        }
    }
}

