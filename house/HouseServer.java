package house;

import beans.HouseBean;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class HouseServer implements Runnable{

    ServerSocket serverSocket;
    int localId;
    int localPort;
    int remoteServerPort;
    String remoteServerIp;
    HouseBean houseBean;

    public HouseServer(int id, int port, int serverPort, String serverIp) {
        this.localId = id;
        this.localPort = port;
        this.remoteServerPort = serverPort;
        this.remoteServerIp = serverIp;

        houseBean = new HouseBean();
        houseBean.setId(localId);
        houseBean.setPort(localPort);
        try {
            houseBean.setIpAddress(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {throw new RuntimeException("Unable to get local IP");}
        try {
            serverSocket = new ServerSocket(localPort);
        } catch (IOException e) {throw new RuntimeException("Unable to start the house server socket");}
    }

    public HouseBean getHouseBean() {
        return houseBean;
    }

    protected ServerSocket getSocket() {
        return serverSocket;
    }

    @Override
    public void run() {
        System.out.println("Started house server on port: " + localPort);
        while(!House.stopped) {
            try {
                Socket s = serverSocket.accept();
                Thread handler = new Thread(new MessageHandler(s, houseBean));
                handler.start();
            } catch (IOException e) {System.out.println("Shutting down the house server...");}
            //remove the dead threads
        }
    }
}
