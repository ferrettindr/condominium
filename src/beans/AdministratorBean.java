package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AdministratorBean {

    private String ip;
    private int port;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        AdministratorBean adm = (AdministratorBean) o;
        return ip.equals(adm.getIp()) && port == adm.getPort();
    }
}
