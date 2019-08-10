package utility;

import java.io.DataOutputStream;
import java.net.Socket;

public class MessageSender implements Runnable {

    private String receiverIp;
    private int receiverPort;
    private int id;
    private Message msg;

    public MessageSender(String ip, int port, Message msg, int id) {
        receiverIp = ip;
        receiverPort = port;
        this.msg = msg;
        this.id = id;
    }

    public void run() {
        try {
            Socket s = new Socket(receiverIp, receiverPort);
            DataOutputStream Out = new DataOutputStream(s.getOutputStream());
            Out.writeUTF(msg.toJSONString());
            Out.flush();
            Out.close();
            s.close();
        } catch(Exception e) {System.err.println(e.getMessage() + ". Unable to send " + msg.getHeader() + " msg to house with ID: " + id);}
    }
}

