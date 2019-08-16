package house;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import beans.HouseBean;
import beans.StatBean;
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
            case "BOOST_OK":
                getStoppedLock();
                try {
                    System.out.println("received boost ok from " + msg.getContent(HouseBean.class).getId() + " for boost " + msg.getParameters(Integer.class).get(0));
                    handleBoostOk(msg);}
                catch (IOException e) {e.printStackTrace();}
                catch (InterruptedException e) {e.printStackTrace();}
                catch (JSONException e) {e.printStackTrace();}
                releaseStoppedLock();
                break;
            case "BOOST_REQUEST":
                getStoppedLock();
                try {handleBoostRequest(msg);}
                catch (IOException e) {e.printStackTrace();} catch (JSONException e) {
                    e.printStackTrace();
                }
                releaseStoppedLock();
                break;
            case "STATS":
                getStoppedLock();
                //if is elected House.houseServer.getHouseBean()add it to statistics otherwise ignore. Do a function that checks if you are the coordinator
                //if i'm the coordinator add it to the structure
                if (House.isCoordinator()) {
                    try {
                        House.statCoordinator.addStat(msg.getContent(HouseBean.class), msg.getParameters(StatBean.class).get(0));
                    } catch (IOException e) {e.printStackTrace();}
                }
                releaseStoppedLock();
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
                // not in mutual exclusion to stopped because needs to receive the message after it's stopped
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
                //TODO remove it from queue of resource, both ok and request
                break;
            default:
                System.err.println("Unknown message format: " + msg.getHeader());
        }

        //if (!msg.getHeader().equals("STATS"))
            //System.out.println("Finished handling msg: " + msg.getHeader());
        House.stoppedLock.endRead();
    }

    private void handleBoostOk(Message msg) throws IOException, InterruptedException, JSONException {
        HouseBean from = msg.getContent(HouseBean.class);
        int boostNumber = msg.getParameters(Integer.class).get(0);

        for (int i = 1; i<=House.boosts.size(); i++)
            if (i==boostNumber) {
                //all the other boosts except this one
                ArrayList<Integer> freeBoostNum = new ArrayList<>();
                for (int k=1; k<=House.boosts.size(); k++) {
                    if (k != i)
                        freeBoostNum.add(k);
                }

                //System.out.println("trying to get boost request for boost num: " + boostNumber + " ok sent by " + from.getId());
                tryToGetBoost(i, freeBoostNum, from);
            }
    }

    private void tryToGetBoost(int getBoostNum, ArrayList<Integer> freeBoostNum, HouseBean sender) throws IOException, InterruptedException, JSONException {
        House.boostLock.beginWrite();

        //remove from okQueue
        House.boosts.get(getBoostNum).addToOk(sender);
        //if atleast one resource is free it means that i already got the boost and freed that resource, therefore i don't need the ok message
        if (!(House.boosts.get(1).isResourceOccupied() && House.boosts.get(2).isResourceOccupied())) {}
        //if i'm first in waiting queue and got ok from all the house in the condo
        else if (House.boosts.get(getBoostNum).getOkSet().containsAll(Condo.getInstance().getCondoTable().keySet())
                && House.boosts.get(getBoostNum).firstElementWaiting() == houseBean.getId()) {
            //free all the other boosts
            System.out.println("Got the BOOST num: " + getBoostNum);
            for(int i = 0; i < freeBoostNum.size(); i++) {
                //remove yourself from waiting queue
                int freeBoost = freeBoostNum.get(i);
                House.boosts.get(freeBoost).removeFromWaiting(houseBean.getId());
                House.boosts.get(freeBoost).freeResource();
                House.sendOkMessageToCondo(freeBoost);
            }
            House.boosts.get(getBoostNum).useResource();
            //remove yourself from waiting queue
            House.boosts.get(getBoostNum).removeFirstWaiting();
            //release lock so that it's possible to add requests to waiting queue
            House.boostLock.endWrite();

            //start boost
            //TODO inform server you have the boost
            House.sms.boost();


            House.boostLock.beginWrite();

            House.boosts.get(getBoostNum).freeResource();
            //send ok to everyone in this boost waiting queue
            House.sendOkMessageToCondo(getBoostNum);
        }
        else {
            //just send ok to the requester
            House.boosts.get(getBoostNum).removeFromWaiting(sender.getId());
        }
        House.boostLock.endWrite();
    }


    private void handleBoostRequest(Message msg) throws IOException, JSONException {
        House.boostLock.beginRead();

        System.out.println("got a boost request from: " + msg.getContent(HouseBean.class).getId());
        //check every boost and send ok if available
        for (int i = 1; i<=House.boosts.size(); i++) {
            //if using boost add it to waiting queue
            if (House.boosts.get(i).isUsingResource()) {
                House.boosts.get(i).addToWaiting(msg);
            }
            //if waiting on boost put in waiting queue in the correct order.
            //if timestamp < local request and requestId < localId  timestamp send ok to it
            //if timestamps are equal use houseID to establish order. Lower id comes before
            else if (House.boosts.get(i).isWaitingForResource()) {
                House.boosts.get(i).addToWaiting(msg);
                long requestTs = msg.getTimestamp(long.class);
                long localTs = House.boosts.get(i).getFromWaiting(houseBean.getId()).getTimestamp(long.class);
                if (requestTs < localTs) {
                    System.out.println("SHOULD NOT HAPPEN-----------------");
                    sendOkMessageToHouse(msg.getContent(HouseBean.class), i);
                }
                else if (requestTs == localTs && msg.getContent(HouseBean.class).getId() < houseBean.getId()) {
                    System.out.println("SHOULDE NOT BE HERE");
                    sendOkMessageToHouse(msg.getContent(HouseBean.class), i);
                }
                //if i'm the first request i get the boost if ok is empty
                //if ()
            }
            //if don't care about boost send ok
            else {
                sendOkMessageToHouse(msg.getContent(HouseBean.class), i);
            }
        }
        House.boostLock.endRead();
    }

    private void sendOkMessageToHouse(HouseBean receiver, int boostIndex) throws IOException, JSONException {
        Message resp = new Message();
        resp.setHeader("BOOST_OK");
        resp.setContent(houseBean);
        resp.addParameter(boostIndex);
        House.sendMessageToHouse(receiver, resp);
    }

    //set itself as coordinator, informs everyone in the condo, acks the old coordinator
    private void handleElected(Message msg) throws IOException, JSONException {
        HouseBean sender = msg.getContent(HouseBean.class);
        int counter = msg.getParameters(Integer.class).get(0);
        House.updateCoordinator(houseBean, counter);

        Message ackHello = new Message();
        ackHello.setHeader("ACK_HELLO_COORDINATOR");
        ackHello.setContent(House.coordinator);
        ackHello.addParameter(House.coordinatorCounter);
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
        if (House.isCoordinator()) {
            helloAck.setHeader("ACK_HELLO_COORDINATOR");
            helloAck.addParameter(House.coordinatorCounter);
        }
        else
            helloAck.setHeader("ACK_HELLO");
        House.coordinatorLock.endRead();

        //TODO send boost request to the newly added house if waiting for boost

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
            System.out.println("Acked by coordinator with ID: " + hb.getId() + " with counter: " + msg.getParameters(Integer.class).get(0));
        }
        //TODO send boost request to the newly added house if waiting for boost
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
