package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StatBean {

    private double value;
    private long timestamp;

    public double getValue() {
        return value;
    }

    public void setValue(double v) {
        this.value = v;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}

