package house;

import java.net.ServerSocket;
import java.net.Socket;

public class HouseServer implements Runnable{

    int serverPort;
    int id;
    ServerSocket serverSocket;

    public HouseServer(int serverPort, int id) {
        this.serverPort = serverPort;
        this.id = id;
        try {
            serverSocket = new ServerSocket(id);
        } catch (Exception e) {System.out.println(e.getMessage());}
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket s = serverSocket.accept();
            Thread handler = new Thread(new MessageHandler(s));
            handler.start();
            } catch (Exception e) {System.out.println(e.getMessage());}
        }
    }
}
