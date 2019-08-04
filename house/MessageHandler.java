package house;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import beans.HouseBean;
//import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import utility.Condo;
import utility.Message;

public class MessageHandler implements Runnable {

    protected enum headerTYPE{
        HELLO,
        ACK_HELLO,
        EMPTY;
    }

    Socket socket;
    DataOutputStream output;
    DataInputStream input;
    HouseBean houseBean;

    public MessageHandler(Socket s, HouseBean houseBean) {
        socket = s;
        this.houseBean = houseBean;
        try {
            input = new DataInputStream(s.getInputStream());
            output = new DataOutputStream(s.getOutputStream());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    //TODO handle all message that can be possibly received
    @Override
    public void run() {
        Message msg = new Message();
        try {
            String stringMsg = input.readUTF();
            System.out.println(stringMsg);
            msg.fromJSONString(stringMsg);
        }
        catch (IOException e) {System.err.println(e.getMessage());}
        catch (JSONException e) {System.err.println("Wrong message format: " + e.getMessage()); e.printStackTrace();}
        switch (msg.getHeader()) {
            case "EMPTY":
                break;
            case "ACK_HELLO":
                try {
                    helloAckResponse(msg);
                } catch (IOException e) {System.err.println("Cannot respond to hello ack message" + e.getMessage()); e.printStackTrace();}
                break;
            case "HELLO":
                try {
                    helloResponse(msg);
                } catch (Exception e) {System.err.println("Cannot respond to hello message: " + e.getMessage()); e.printStackTrace();}
                break;
            default:
                System.err.println("Unknown message format: " + msg.getHeader());
        }
        //switch (msg.getHeader())
    }

    private void helloResponse(Message msg) throws JSONException, IOException {
        HouseBean newHouse = msg.getContent(HouseBean.class);
        Condo.getInstance().addHouse(newHouse);
        System.out.println("Condo table: " + Condo.getInstance().getCondoTable().toString());

        Message helloAck = new Message();
        helloAck.setHeader("ACK_HELLO");
        helloAck.setContent(houseBean);
        System.out.println("about to write response ack");
        Socket rSocket = new Socket(newHouse.getIpAddress(), newHouse.getPort());
        DataOutputStream dos = new DataOutputStream(rSocket.getOutputStream());
        dos.writeUTF(helloAck.toJSONString());
        dos.flush();
        dos.close();
        rSocket.close();
        System.out.println("HELLO RESPONOSE by id: " + houseBean.getId());
    }

    private void helloAckResponse(Message msg) throws IOException {
        HouseBean hb = msg.getContent(HouseBean.class);
        Condo.getInstance().addHouse(hb);
        System.out.println("HELLO ACK RESPONSE by id: " + houseBean.getId());
        System.out.println("Condo table: " + Condo.getInstance().getCondoTable().toString());
    }
}
