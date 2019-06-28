package beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Hashtable;

@XmlRootElement
public class StatPkgBean {

    private Hashtable<Integer, StatBean> houseStat;
    private StatBean condoStat;

    public StatPkgBean() {
    }

    public Hashtable<Integer, StatBean> getHouseStat() {
        return houseStat;
    }

    public void addHouseStat(int id, StatBean stat) {
        houseStat.put(id, stat);
    }

    public StatBean getCondoStat() {
        return condoStat;
    }

    public void setCondoStat(StatBean stat) {
        this.condoStat = stat;
    }

}
