package house;

import beans.HouseBean;
import utility.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class RequestQueue {

    //queue of nodes that want the resource ordered by timestamp
    private ArrayList<Message> waitingQueue;
    //queue of nodes that need to send back OK
    private Hashtable<Integer, HouseBean> okQueue;
    private volatile boolean usingResource = false;
    private volatile boolean waitingForResource = false;

    public RequestQueue() {
        waitingQueue = new ArrayList<>();
        okQueue = new Hashtable<>();
        usingResource = false;
        waitingForResource = false;
    }

    public void setOkQueue(Hashtable<Integer, HouseBean> okQueue) {
        this.okQueue = okQueue;
    }

    public synchronized boolean isUsingResource() {
        return usingResource;
    }

    public synchronized boolean isWaitingForResource() {
        return waitingForResource;
    }

    public synchronized void waitResource() {
        waitingForResource = true;
    }

    public synchronized void useResource() {
        usingResource = true;
    }

    public synchronized void freeResource() {
        waitingForResource = false;
        usingResource = false;
    }

    public synchronized boolean isResourceOccupied() {
        return waitingForResource || usingResource;
    }

    //resets the waiting queue and return list of houses in it
    public synchronized ArrayList<HouseBean> resetWaiting() throws IOException {
        ArrayList<HouseBean> res = new ArrayList<>();
        for (Message msg: waitingQueue) {
            res.add(msg.getContent(HouseBean.class));
        }
        return res;
    }

    //TODO
    //add a message in the correct order to waitingQueue. Lowest timestamp first
    public synchronized void addToWaiting(Message msg) throws IOException {
        int i = 0;
        for (Message m: waitingQueue) {
            if (m.getTimestamp(long.class) > msg.getTimestamp(long.class)) {
                waitingQueue.add(i, msg);
                return;
            }
            i++;
        }
        //either the list is empty or its timestamp is greater than anyone else
        waitingQueue.add(i, msg);
    }

    //TODO
    //remove the first message from the waitingQueue
    public synchronized void removeFirstWaiting() {
        waitingQueue.remove(0);
    }

    //TODO
    //return the first message in the waiting queue with id = houseId
    public synchronized Message getFromWaiting(int houseId) throws IOException {
        for (Message m: waitingQueue) {
            if (m.getContent(HouseBean.class).getId() == houseId)
                return m;
        }
        return null;
    }

    //TODO
    //remove the first occurrence of a message with id = houseId from waitingQueue
    public synchronized void removeFromWaiting(int houseId) throws IOException {
        int i = 0;
        for (Message m: waitingQueue) {
            if (m.getContent(HouseBean.class).getId() == houseId) {
                waitingQueue.remove(i);
                return;
            }
            i++;
        }
    }

    //remove an element from the okQueue
    public synchronized void removeFromOk(int houseId) {
        okQueue.remove(houseId);
    }

    public synchronized Integer firstElementWaiting() throws IOException {
        if (waitingQueue.isEmpty())
            return null;
        else {
            return waitingQueue.get(0).getContent(HouseBean.class).getId();
        }
    }

    public synchronized boolean isOkEmpty() {
        return okQueue.isEmpty();
    }

}
