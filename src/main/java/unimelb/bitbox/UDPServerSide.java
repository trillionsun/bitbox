package unimelb.bitbox;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPServerSide implements Runnable{
    private  int hostingPort;
    private int blockSize;
    private  ArrayList<UDPConnection> UDPServerConnectionList = new ArrayList<>();
    private  int maximumConnections;
    ArrayList<String> ConnectedPeers;
    private  boolean flag = true;
    private DatagramSocket serverSocket;
    protected Thread    runningThread= null;
    private byte[] buffer;
    private int packetSize;
    private DatagramPacket input;
    private int timeout = 10000;
    private int maxRetry=5;

    protected ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    public int getHostingPort() {
        return hostingPort;
    }

    public ArrayList<UDPConnection> getUDPServerConnectionList() {
        return UDPServerConnectionList;
    }

    public int getMaximumConnections() {
        return maximumConnections;
    }
    public UDPServerSide (Peer peer) throws SocketException {
        hostingPort= peer.getPortNo();
        maximumConnections = peer.getMaximumConnections();
        serverSocket = new DatagramSocket(hostingPort);
        this.blockSize= peer.getBlockSize();
        buffer = new byte[peer.getBlockSize()];
        input = new DatagramPacket(buffer, buffer.length);
        this.serverSocket.setSoTimeout(timeout);
    }

    public void run()
    {

  //      synchronized(this){
   //         this.runningThread = Thread.currentThread();
   //     }
        System.out.println("Server listening for a connection on: " + hostingPort);

        while (flag)
        {
            try {
                serverSocket.receive(input);
                UDPProcessing handler = new UDPProcessing(input.getAddress(),input.getPort(),input.getData(), this.serverSocket);
                threadPool.submit(handler);
            } catch (IOException e) {
            }

        }


    }
    }






