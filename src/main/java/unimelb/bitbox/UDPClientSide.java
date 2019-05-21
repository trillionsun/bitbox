package unimelb.bitbox;

import org.json.simple.JSONObject;
import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class UDPClientSide implements Runnable {
    private ArrayList<JSONObject> peers;
    private boolean flag = true;
    private ArrayList<Connection> ClientConnectionList;
    private int hostingPort;
    private DatagramPacket response;
    private byte[] buffer;
    private int packetSize;
    private static int Maxcount =Integer.parseInt(Configuration.getConfigurationValue("udpRetries")) ;
    private static int timeout = Integer.parseInt(Configuration.getConfigurationValue("udpTimeout")) ;
    public UDPClientSide(Peer peer) {

        String[] Peers = peer.getPeers();
        peers = new ArrayList<>();


        for (int i = 0; i < Peers.length; i++) {
            String Opeer = Peers[i];
            String[] peerHost = Opeer.split(":", 2);
            JSONObject json = new JSONObject();
            json.put("host", peerHost[0]);
            json.put("port", Integer.parseInt(peerHost[1]));
            peers.add(json);
        }
        ClientConnectionList = new ArrayList<>();
        hostingPort = peer.getPortNo();
        buffer = new byte[peer.getBlockSize()];
        response = new DatagramPacket(buffer, buffer.length);
    }



    @Override
    public void run() {
        ArrayList<JSONObject> ConnectedPeers = UDPConnectionHost.getConnectedPeers();
        int count = 0;
        // response received or not
        Boolean flag = false;
        for (JSONObject Opeer : peers) {
            if (!ConnectedPeers.contains(Opeer)) {
                JSONObject jsonP = Opeer;
                String OpeerHost = (String) jsonP.get("host");
                InetAddress peerAddress = null;
                // Init the peerAddress with the localhost and then if appliacable, to the provided address
                try {
                    peerAddress = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                if (OpeerHost.equals("localhost")) {
                    try {
                        peerAddress = InetAddress.getLocalHost();

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        peerAddress = InetAddress.getByName(OpeerHost);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                int OpeerPort = (int) (jsonP.get("port"));


                System.out.println("try to connect with :" + OpeerHost + " : " + OpeerPort);
                JSONObject json = new JSONObject();
                JSONObject hostPort = new JSONObject();
                json.put("command", "HANDSHAKE_REQUEST");
                hostPort.put("host", "127.0.0.1");
                hostPort.put("port", hostingPort);
                json.put("hostPort", hostPort);
                byte[] byteData = json.toJSONString().getBytes();

                DatagramPacket HandshakeReq = new DatagramPacket(byteData, byteData.length, peerAddress, OpeerPort);
                System.out.println("send handshake request to " + Opeer);
                try {
                    DatagramSocket clientSocket = new DatagramSocket();
                    try {
                        clientSocket.setSoTimeout(timeout);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    try {
                        clientSocket.send(HandshakeReq);
                        ClientListener listener = new ClientListener(peerAddress, OpeerPort,clientSocket,HandshakeReq);
                        Thread ListenThread = new Thread(listener);
                        ListenThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } catch (SocketException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}

class ClientListener implements Runnable {
    DatagramSocket clientSocket;
    InetAddress address;
    int port;
    DatagramPacket response;
    DatagramPacket handshakeRequest;
    int Maxcount =Integer.parseInt(Configuration.getConfigurationValue("udpRetries")) ;
    int timeout = Integer.parseInt(Configuration.getConfigurationValue("udpTimeout")) ;
    boolean IfReceive = false;

    public ClientListener(InetAddress address, int port, DatagramSocket socket, DatagramPacket hsRequest)
    {
        clientSocket = socket;
        this.address = address;
        this.port = port;
        byte[] buffer = new byte[Integer.parseInt(Configuration.getConfigurationValue("blockSize"))];
        response = new DatagramPacket(buffer, buffer.length);
        this.handshakeRequest = hsRequest;
    }

    @Override
    public void run() {
        int count = 0;
        while (true) {
            try {
                this.clientSocket.receive(response);
                this.IfReceive = true;
                UDPProcessing handler = new UDPProcessing(response.getAddress(),response.getPort(),response.getData(), clientSocket);
                Thread handleThread = new Thread(handler);
                handleThread.start();
                byte[] zero = new byte[Integer.parseInt(Configuration.getConfigurationValue("blockSize"))];
                response.setData(zero);

            } catch (SocketTimeoutException a) {
                if(!IfReceive && count <= Maxcount) {
                    try {
                        clientSocket.send(handshakeRequest);
                        count++;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            if(count>Maxcount)
            {
                System.out.println(address.toString() + port + "cannot be reached !!!");
                this.clientSocket.close();
                break;
            }
        }

    }
}
