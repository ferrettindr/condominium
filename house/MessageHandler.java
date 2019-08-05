package house;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import beans.HouseBean;
import org.codehaus.jettison.json.JSONException;
import utility.Condo;
import utility.Message;

public class MessageHandler implements Runnable {

    private Socket socket;
    private DataInputStream input;
    private HouseBean houseBean;

    public MessageHandler(Socket s, HouseBean houseBean) {
        socket = s;
        this.houseBean = houseBean;
        try {
            input = new DataInputStream(s.getInputStream());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    //TODO handle all message that can be possibly received
    @Override
    public void run() {
        //message reception in mutual exclusion to removal
        House.stoppedLock.beginRead();

        if (House.stopped) {
            House.stoppedLock.endRead();
            return;
        }

        Message msg = new Message();
        try {
            String stringMsg = input.readUTF();
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
                } catch (IOException e) {System.err.println("Cannot respond to hello ack message: " + e.getMessage()); e.printStackTrace();}
                break;
            case "HELLO":
                //removing the house and responding to hello are mutual exclusive
                try {
                    helloResponse(msg);
                } catch (Exception e) {
                    System.err.println("Cannot respond to hello message: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case "REMOVE":
                try {
                    HouseBean hb = msg.getContent(HouseBean.class);
                    Condo.getInstance().removeHouse(hb.getId());
                } catch (IOException e) {System.err.println("Cannot respond to remove msg: " + e.getMessage());}
                System.out.println(Condo.getInstance().getCondoTable().toString());
                break;
            default:
                System.err.println("Unknown message format: " + msg.getHeader());
        }

        System.out.println("Finished handling msg");
        House.stoppedLock.endRead();
    }


    //add the house that sent hello to local condo and send back ack
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

    //add house that sent the ack to the local condo
    private void helloAckResponse(Message msg) throws IOException {
        HouseBean hb = msg.getContent(HouseBean.class);
        Condo.getInstance().addHouse(hb);
        System.out.println("HELLO ACK RESPONSE by id: " + houseBean.getId());
        System.out.println("Condo table: " + Condo.getInstance().getCondoTable().toString());
    }
}
