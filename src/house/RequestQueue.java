package house;

import beans.HouseBean;
import utility.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

public class RequestQueue {

    //queue of nodes that want the resource ordered by timestamp
    private ArrayList<Message> waitingQueue;
    //queue of nodes that has sent back OK
    private Hashtable<Integer,HouseBean> okQueue;
    private Message localRequest;
    private volatile boolean usingResource = false;
    private volatile boolean waitingForResource = false;

    public RequestQueue() {
        waitingQueue = new ArrayList<>();
        okQueue = new Hashtable<>();
        usingResource = false;
        waitingForResource = false;
    }

    public synchronized void setLocalRequest(Message msg) {
        localRequest = msg;
    }

    public synchronized Message getLocalRequest() {
        return localRequest;
    }

    public synchronized boolean isUsingResource() {
        return usingResource;
    }

    public synchronized boolean isWaitingForResource() {
        return waitingForResource;
    }

    public synchronized void waitResource() {
        usingResource = false;
        waitingForResource = true;
    }

    public synchronized void useResource() {
        waitingForResource = false;
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
        waitingQueue.clear();
        return res;
    }

    //add a message in the correct order to waitingQueue. Lowest timestamp first
    public synchronized void addToWaiting(Message msg) throws IOException {
        int i = 0;
        for (Message m: waitingQueue) {
            if (m.getTimestamp(long.class) > msg.getTimestamp(long.class)) {
                waitingQueue.add(i, msg);
                return;
            }
            //if timestamps are equal use houseID to establish total order (smaller id has priority)
            else if (m.getTimestamp(long.class) == msg.getTimestamp(long.class))
                if (m.getContent(HouseBean.class).getId() > msg.getContent(HouseBean.class).getId()) {
                    waitingQueue.add(i, msg);
                    return;
                }
            i++;
        }
        //either the list is empty or its timestamp is greater than anyone else
        waitingQueue.add(i, msg);
    }

    //remove the first message from the waitingQueue
    public synchronized void removeFirstWaiting() {
        waitingQueue.remove(0);
    }

    //return the first message in the waiting queue with id = houseId
    public synchronized Message getFromWaiting(int houseId) throws IOException {
        for (Message m: waitingQueue) {
            if (m.getContent(HouseBean.class).getId() == houseId)
                return m;
        }
        return null;
    }

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

    //return the maximum timestamp in the waitingQueue
    public synchronized double maxTimestampWaiting() throws IOException {
        Message msg = waitingQueue.get(waitingQueue.size()-1);
        return msg.getTimestamp(long.class);
    }

    public synchronized Integer firstElementWaiting() throws IOException {
        if (waitingQueue.isEmpty())
            return null;
        else {
            return waitingQueue.get(0).getContent(HouseBean.class).getId();
        }
    }

    //adds and element to the okQueue
    public synchronized void addToOk(HouseBean hb) {
        okQueue.put(hb.getId(), hb);
    }

    //removes all elements form the okQueue
    public synchronized void resetOk() {
        okQueue.clear();
    }

    //returns a set containing the keys of the okQueue (houseID)
    public synchronized Set<Integer> getOkSet() {
        return okQueue.keySet();
    }

}
