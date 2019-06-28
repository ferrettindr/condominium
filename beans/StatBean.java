package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StatBean {

    private double consumption;
    private int timestamp;

    public StatBean() {
    }

    public StatBean(double consumption, int timestamp) {
        this.consumption = consumption;
        this.timestamp = timestamp;
    }

    public double getConsumption() {
        return consumption;
    }

    public void setConsumption(double consumption) {
        this.consumption = consumption;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

}

