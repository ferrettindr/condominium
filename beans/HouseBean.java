package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HouseBean {

    private int id;
    private String ipAddress;
    private int port;

    //TODO check if constructors are needed
    /*
    public HouseBean(){}

    public HouseBean(int id, String ipAddress, int port) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
    }
    */

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}