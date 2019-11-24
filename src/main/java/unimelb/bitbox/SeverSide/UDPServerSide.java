package unimelb.bitbox.SeverSide;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import unimelb.bitbox.Connection.UDPConnection;
import unimelb.bitbox.Peer;
import unimelb.bitbox.Connection.UDPProcessing;
import unimelb.bitbox.util.Configuration;

public class UDPServerSide extends serverTask implements Runnable {
    private int hostingPort;
    private int blockSize;
    private ArrayList<UDPConnection> UDPServerConnectionList = new ArrayList<>();
    private int maximumConnections;

    private boolean flag = true;
    private DatagramSocket serverSocket;

    private byte[] buffer;
    private int packetSize;
    private DatagramPacket input;
    private int timeout = 10000;

    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);



    public UDPServerSide(Peer peer) throws SocketException {
        hostingPort = peer.getPortNo();
        maximumConnections = peer.getMaximumConnections();
        serverSocket = new DatagramSocket(hostingPort);
        this.blockSize = peer.getBlockSize();
        buffer = new byte[peer.getBlockSize()];
        input = new DatagramPacket(buffer, buffer.length);
        this.serverSocket.setSoTimeout(timeout);
    }

    public void run() {

        // synchronized(this){
        // this.runningThread = Thread.currentThread();
        // }
        System.out.println("Server listening for a connection on: " + hostingPort);

        while (flag) {


            try {
                serverSocket.receive(input);
                UDPProcessing handler = new UDPProcessing(input.getAddress(), input.getPort(), input.getData(),
                        this.serverSocket);
                threadPool.submit(handler);
                byte[] zero = new byte[Integer.parseInt(Configuration.getConfigurationValue("blockSize"))];
                input.setData(zero);

            } catch (IOException e) {
            }

        }

    }
}
