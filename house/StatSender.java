package house;

import beans.HouseBean;
import beans.StatBean;
import utility.Condo;
import utility.Message;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;

public class StatSender extends Thread {

    private StatBean stat;
    private SlidingBuffer bf;

    public StatSender(SlidingBuffer bf) {
        this.bf = bf;
    }

    public void run() {
        while (!House.stopped) {
            try {
                getStatFromBuffer();
                sendStat();
            } catch (InterruptedException e) {};
        }
    }

    private void getStatFromBuffer() throws InterruptedException {
        stat = bf.getSlidingWindow();
    }

    public StatBean getStat() {
        return stat;
    }

    //send the stat to the condo with the house sender
    public void sendStat() {
        Message msg = new Message();
        msg.setHeader("STATS");
        try {
            msg.setContent(House.houseServer.getHouseBean());
            msg.addParameter(stat);
            msg.setTimestamp(stat.getTimestamp());
        }
        catch (IOException e) { e.printStackTrace(); }
        //send stat also you yourself
        Hashtable<Integer, HouseBean> list = Condo.getInstance().getCondoTable();
        list.put(House.houseServer.getHouseBean().getId(), House.houseServer.getHouseBean());
        House.sendMessageToCondo(list.values(), msg);
    }

}
