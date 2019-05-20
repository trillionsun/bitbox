package unimelb.bitbox;

// basic functions: listening and sending, need to be included as a new thread;
// implement thread pool to deal with each incoming connection


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 1, only 10 threads, when 11th come, handshake, how to refuse;
class ServerSide implements Runnable
{
    private  int hostingPort;
    private  ArrayList<Connection> ServerConnectionList = new ArrayList<>();
    private  int maximumConnections;
    ArrayList<String> ConnectedPeers;
    private  boolean flag = true;
    private ServerSocket serverSocket;
    protected Thread    runningThread= null;
    protected ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    

    public int getHostingPort() {
        return hostingPort;
    }

    public  ArrayList<Connection> getServerConnectionList() {
        return ServerConnectionList;
    }

    public int getMaximumConnections() {
        return maximumConnections;
    }


    public ServerSide (Peer peer) throws IOException {
        hostingPort= peer.getPortNo();
        maximumConnections = peer.getMaximumConnections();
        serverSocket = new ServerSocket(hostingPort);
    }

    public int getConnectionNum() {
        return getServerConnectionList().size();
    }



    public void run () {
       synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        System.out.println("Server listening for a connection on: " + hostingPort);
        while (flag)
        {
            try {
                Socket clientSocket = serverSocket.accept();
                Connection c = ConnectionHost.ServerConnection(clientSocket);
                threadPool.execute(c);
            } catch (IOException e) {
                System.out.println("Listen socket:" + e.getMessage());
            }
    }

    }
}
