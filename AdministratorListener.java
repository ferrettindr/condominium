import beans.HouseBean;
import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import org.codehaus.jettison.json.JSONException;
import utility.Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class AdministratorListener extends Thread {
    ServerSocket ss;

    public AdministratorListener(ServerSocket ss) {
        this.ss = ss;
    }

    public void run() {
        try {
            while (true) {
                Socket s = ss.accept();
                DataInputStream dis = new DataInputStream(s.getInputStream());
                Message msg = new Message();
                msg.fromJSONString(dis.readUTF());

                switch (msg.getHeader()) {
                    case "IN":
                        System.out.println("\n---INSERTION NOTIFICATION---\nHouse with ID: " + msg.getContent(HouseBean.class).getId() + " joined the condo\n");
                        break;
                    case "OUT":
                        System.out.println("\n---REMOVAL NOTIFICATION---\nHouse with ID: " + msg.getContent(HouseBean.class).getId() + " left the condo\n");
                        break;
                    case "BOOST":
                        System.out.println("\n---BOOST NOTIFICATION---\nHouse with ID: " + msg.getContent(HouseBean.class).getId() + " is using the boost\n");
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | JSONException e) {System.out.println("Quitting the notification listener");}
    }

}

