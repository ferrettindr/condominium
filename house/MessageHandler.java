package house;

import java.io.DataInputStream;
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
            input = new DataInputStream(socket.getInputStream());
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
            msg.fromJSONString(stringMsg);
        }
        catch (IOException e) {System.err.println(e.getMessage());}
        catch (JSONException e) {System.err.println("Wrong message format: " + e.getMessage()); e.printStackTrace();}
        switch (msg.getHeader()) {
            case "EMPTY":
                break;
                //send to everyone ack_hello_coordinator and to coordinator ack_elected
            case "ELECTED":
                getStoppedLock();
                System.out.println("I'm being elected");
                try {handleElected(msg);}
                catch (IOException e) {e.printStackTrace();}
                catch (JSONException e) {e.printStackTrace();}
                releaseStoppedLock();
                break;
            case "ACK_ELECTED":
                // not in mutual exclusion to stopped because
                //TODO understand why it thorws illegalMonitorStateException
                synchronized ((House.newElected)) {
                    House.newElected = true;
                    //House.newElected.notify();
                }
                break;
            case "ACK_HELLO_COORDINATOR":
                getStoppedLock();
                try {
                    handleAckHello(msg, true);
                } catch (IOException e) {System.err.println("Cannot respond to hello ack message: " + e.getMessage()); e.printStackTrace();}
                releaseStoppedLock();
                break;
            case "ACK_HELLO":
                getStoppedLock();
                try {
                    handleAckHello(msg, false);
                } catch (IOException e) {System.err.println("Cannot respond to hello ack message: " + e.getMessage()); e.printStackTrace();}
                releaseStoppedLock();
                break;
            case "HELLO":
                getStoppedLock();
                try {
                    handleHello(msg);
                } catch (Exception e) {
                    System.err.println("Cannot respond to hello message: " + e.getMessage());
                    e.printStackTrace();
                }
                releaseStoppedLock();
                break;
            case "REMOVE":
                getStoppedLock();
                try {
                    HouseBean hb = msg.getContent(HouseBean.class);
                    Condo.getInstance().removeHouse(hb.getId());
                } catch (IOException e) {System.err.println("Cannot respond to remove msg: " + e.getMessage());}
                System.out.println(Condo.getInstance().getCondoTable().toString());
                releaseStoppedLock();
                break;
            default:
                System.err.println("Unknown message format: " + msg.getHeader());
        }

        System.out.println("Finished handling msg: " + msg.getHeader());
        House.stoppedLock.endRead();
    }

    //set itself as coordinator, informs everyone in the condo, acks the old coordinator
    private void handleElected(Message msg) throws IOException, JSONException {
        HouseBean sender = msg.getContent(HouseBean.class);
        int counter = msg.getParameters(Integer.class).get(0);
        House.updateCoordinator(houseBean, counter);

        Message ackHello = new Message();
        ackHello.setHeader("ACK_HELLO_COORDINATOR");
        ackHello.setContent(houseBean);
        ackHello.addParameter(counter);
        House.sendMessageToCondo(Condo.getInstance().getCondoTable().values(), ackHello);

        Message ackElected = new Message();
        ackElected.setHeader("ACK_ELECTED");
        House.sendMessageToHouse(sender, ackElected);
    }

    //add the house that sent hello to local condo and send back ack (informing if you are coordinator)
    private void handleHello(Message msg) throws JSONException, IOException {
        HouseBean newHouse = msg.getContent(HouseBean.class);
        Condo.getInstance().addHouse(newHouse);
        //check if new house should be coordinator
        House.updateCoordinator(newHouse, msg.getParameters(Integer.class).get(0));

        Message helloAck = new Message();
        //if the node responding to hello is the coordinator alert in the ack
        House.coordinatorLock.beginRead();
        if (House.coordinator.getId() == houseBean.getId()) {
            helloAck.setHeader("ACK_HELLO_COORDINATOR");
            helloAck.addParameter(House.coordinatorCounter);
        }
        else
            helloAck.setHeader("ACK_HELLO");
        House.coordinatorLock.endRead();

        helloAck.setContent(houseBean);
        House.sendMessageToHouse(newHouse, helloAck);
        System.out.println("HELLO RESPONSE to id: " + newHouse.getId());
    }

    //add house that sent the ack to the local condo
    private void handleAckHello(Message msg, boolean ackedByCord) throws IOException {
        HouseBean hb = msg.getContent(HouseBean.class);
        Condo.getInstance().addHouse(hb);
        //if the sender of the ack is the coordinator
        if (ackedByCord) {
            House.updateCoordinator(hb, msg.getParameters(Integer.class).get(0));
            System.out.println("Acked by coordinator with ID: " + House.coordinator.getId() + " with counter: " + House.coordinatorCounter);
        }
        System.out.println("HELLO ACK RESPONSE by id: " + hb.getId());
        System.out.println("Condo table: " + Condo.getInstance().getCondoTable().toString());
    }

    private void getStoppedLock() {
        //message reception in mutual exclusion to removal
        House.stoppedLock.beginRead();
        if (House.stopped) {
            House.stoppedLock.endRead();
            return;
        }
    }

    private void releaseStoppedLock() {
        House.stoppedLock.endRead();
    }
}
