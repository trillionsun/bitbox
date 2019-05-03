package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// store and implement the basic functions for each connection
public class Connection implements Runnable {
    private BufferedReader inreader;
    public JSONObject ConnectingPeer;
    protected ExecutorService ProcessingPool = Executors.newCachedThreadPool();
    private PrintWriter outwriter;
    Socket clientSocket;
    boolean flag = true;

    public Connection(Socket socket) {
        try {
            this.clientSocket = socket;
            this.outwriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
            this.inreader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    public void ConnectionClose() {
        try {
            flag = false;
            inreader.close();
            outwriter.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // only for Response or Request
    public void send(String command) throws IOException {
        outwriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
        JSONObject jObj = new JSONObject();
        CommandGroup commands = new CommandGroup();
        int port = clientSocket.getLocalPort();
        String address = clientSocket.getLocalAddress().toString().split("/", 2)[1];

        JSONObject HostingPeer = new JSONObject();
        HostingPeer.put("host", address);
        HostingPeer.put("port", port);
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
        outwriter.println(jObj.toJSONString());
        outwriter.flush();
    }

    // send json files related to the file activity
    public void sendJson(JSONObject json) throws IOException {
        outwriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
        JSONObject jObj = json;
        outwriter.println(jObj.toJSONString());
        outwriter.flush();
    }

    @Override
    public void run() {
        String command;
        String data = null; // read a line of data from the stream
        JSONObject inComingPeer;
        LinkedList<String> tasks = new LinkedList<String>();
        while (flag) {
            try {
                data = inreader.readLine();
                if (data != null) {
                    System.out.println(data);
                    tasks.add(data);
                    while (!tasks.isEmpty()) {
                        String task = tasks.poll();
                        Processing processing = new Processing(this, task);
                        ProcessingPool.execute(processing);
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}

class Processing implements Runnable {
    String data;
    Connection c;

    public Processing(Connection c, String task) {
        this.data = task;
        this.c = c;
    }

    public void run() {
        JSONObject json = new JSONObject();
        JSONObject inComingPeer;
        try {
            json = (JSONObject) new JSONParser().parse(this.data);
        } catch (ParseException e) {
            e.printStackTrace();
            try {
                c.send("INVALID_PROTOCOL");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


        String command= "";
        try {
            command = json.get("command").toString();
        }catch (NullPointerException e) {
            e.printStackTrace();
            try {
                c.send("INVALID_PROTOCOL");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


        switch (command) {
            case "HANDSHAKE_REQUEST": {
                inComingPeer = (JSONObject) json.get("hostPort");
                System.out.println("handshake received from " + inComingPeer);
                // unnecessary handshake
                if (ConnectionHost.getConnectedPeers().contains(inComingPeer)) {
                    try {
                        c.send("INVALID_PROTOCOL");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("replicated request!");
                    if (ConnectionHost.ClientConnectionList.contains(ConnectionHost.getConnectionMap().get(inComingPeer)))
                        c.ConnectionClose();
                } else {
                    if (ConnectionHost.getConnectionNum() <= ConnectionHost.getMaximumConnections()) {
                        try {
                            c.send("HANDSHAKE_RESPONSE");
                            c.ConnectingPeer = inComingPeer;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Handshake response sent!");
                        ConnectionHost.ServerConnectionList.add(c);
                        ConnectionHost.AddConnectedPeers(inComingPeer, c);
                        ConnectionHost.fileOperator.getSync();
                    } else {
                        try {
                            c.send("CONNECTION_REFUSED");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Handshake refused message sent");
                        c.ConnectionClose();
                    }
                }
                break;
            }
            case "HANDSHAKE_RESPONSE": {
                inComingPeer = (JSONObject) json.get("hostPort");
                c.ConnectingPeer = inComingPeer;
                ConnectionHost.AddConnectedPeers(inComingPeer, c);
                ConnectionHost.ClientConnectionList.add(c);
                System.out.println("connection established.");
                ConnectionHost.fileOperator.getSync();
                break;
            }
            case "INVALID_PROTOCOL": {
                System.out.println("connection been refused by protocol problems.");
                // c.ConnectionClose();
                break;
            }

            case "CONNECTION_REFUSED": {
                System.out.println("connection been refused by incoming limit.");
                // c.ConnectionClose();
                break;
            }

            case "FILE_CREATE_REQUEST": {
                System.out.println("FILE_CREATE_REQUEST received from " + c.ConnectingPeer);
                JSONObject response = null;
                try {
                    response = ConnectionHost.fileOperator.fileCreateResponse(json);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (response.get("status").equals("true")) {
                    try {
                        c.sendJson(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    JSONObject byteRequest = ConnectionHost.fileOperator.fileBytesRequest(response);
                    // if the file loader is ready, ask for file bytes
                    if (byteRequest.get("command") == null) {
                        System.out.println("file writing is finished.");
                    } else {
                        try {
                            c.sendJson(byteRequest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("FILE_BYTES_REQUEST sended.");
                    }
                }

                break;
            }

            case "FILE_CREATE_RESPONSE": {
                System.out.println(json.get("message").toString() + " from " + c.ConnectingPeer);
                break;
            }

            case "FILE_BYTES_REQUEST": {
                System.out.println("FILE_BYTES_REQUEST received from " + c.ConnectingPeer);
                JSONObject response = ConnectionHost.fileOperator.fileBytesResponse(json);
                try {
                    c.sendJson(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("FILE_BYTES_RESPONSE sended.");
                break;
            }

            case "FILE_BYTES_RESPONSE": {
                System.out.println("FILE_BYTES_RESPONSE received from " + c.ConnectingPeer);
                if (json.get("status").toString() == "true") {
                    JSONObject byteRequest = ConnectionHost.fileOperator.fileBytesRequest(json);
                    if (byteRequest.get("command") == null) {
                        System.out.println("file writing is finished.");
                    } else {
                        try {
                            c.sendJson(byteRequest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("FILE_BYTES_REQUEST sended.");
                    }
                }
                break;
            }

            case "FILE_DELETE_REQUEST": {
                System.out.println("FILE_DELETE_REQUEST received from " + c.ConnectingPeer);
                JSONObject response = null;
                try {
                    response = ConnectionHost.fileOperator.fileDeleteResponse(json);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    c.sendJson(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("FILE_DELETE_RESPONSE sended");
                break;
            }
            case "FILE_DELETE_RESPONSE": {
                System.out.println("FILE_DELETE_RESPONSE received from " + c.ConnectingPeer);
                break;

            }

            case "FILE_MODIFY_REQUEST": {
                System.out.println("FILE_MODIFY_REQUEST received from " + c.ConnectingPeer);
                JSONObject response = null;
                try {
                    response = ConnectionHost.fileOperator.fileModifyResponse(json);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    c.sendJson(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // if the file loader is ready, ask for file bytes
                if (response.get("message") == "file loader ready") {
                    JSONObject byteRequest = ConnectionHost.fileOperator.fileBytesRequest(response);
                    if (byteRequest.get("command") == null) {
                        System.out.println("file writing is finished.");
                    } else {
                        try {
                            c.sendJson(byteRequest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("FILE_BYTES_REQUEST sended.");
                    }
                }
                break;
            }

            case "FILE_MODIFY_RESPONSE": {
                System.out.println(json.get("message").toString() + " from " + c.ConnectingPeer);
                break;
            }

            case "DIRECTORY_CREATE_REQUEST": {
                System.out.println("DIRECTORY_CREATE_REQUEST received from " + c.ConnectingPeer);
                JSONObject response = null;
                try {
                    response = ConnectionHost.fileOperator.dirCreateResponse(json);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (response.get("status").toString() != "true") {
                    System.out.println(response.get("message"));
                } else {
                    try {
                        c.sendJson(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("DIRECTORY_CREATE_RESPONSE sended");
                }

                break;
            }

            case "DIRECTORY_CREATE_RESPONSE": {
                System.out.println(json.get("message").toString() + " from " + c.ConnectingPeer);
                break;

            }

            case "DIRECTORY_DELETE_REQUEST": {
                System.out.println("DIRECTORY_DELETE_REQUEST received from " + c.ConnectingPeer);
                JSONObject response = null;
                try {
                    response = ConnectionHost.fileOperator.dirDeleteResponse(json);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (response.get("status").toString() != "true") {
                    System.out.println(response.get("message"));
                } else {
                    try {
                        c.sendJson(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("DIRECTORY_DELETE_RESPONSE sended");
                }
                break;
            }
            case "DIRECTORY_DELETE_RESPONSE": {
                System.out.println("DIRECTORY_DELETE_RESPONSE received from " + c.ConnectingPeer);
                break;
            }

            default:{
                try {
                    c.send("INVALID_PROTOCOL");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

        }
        System.out.println("incoming connection num :" + ConnectionHost.ServerConnectionList.size());
        System.out.println("outgoing connection num :" + ConnectionHost.ClientConnectionList.size());

    }

}
