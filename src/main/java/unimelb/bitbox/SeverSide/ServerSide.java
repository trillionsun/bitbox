package unimelb.bitbox.SeverSide;

// basic functions: listening and sending, need to be included as a new thread;
// implement thread pool to deal with each incoming connection


import unimelb.bitbox.Connection.Connection;
import unimelb.bitbox.Connection.ConnectionHost;
import unimelb.bitbox.Peer;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSide extends serverTask implements Runnable{
    private int hostingPort;
    private int maximumConnections;
    private boolean flag = true;
    private ServerSocket serverSocket;
    protected Thread runningThread = null;
    protected ExecutorService threadPool =
            Executors.newFixedThreadPool(maximumConnections);


    public ServerSide(Peer peer) throws IOException {
        hostingPort = peer.getPortNo();
        maximumConnections = peer.getMaximumConnections();
        serverSocket = new ServerSocket(hostingPort);
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        System.out.println("Server listening for a connection on: " + hostingPort);
        while (flag) {
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
