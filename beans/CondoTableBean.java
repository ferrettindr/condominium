package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import java.util.Hashtable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CondoTableBean {
    private Hashtable<Integer, HouseBean> houseTable;

    public void setHouseTable(Hashtable<Integer, HouseBean> ht) {
        houseTable = new Hashtable<>(ht);
    }

    public Hashtable<Integer, HouseBean> getHouseTable() {
        return houseTable;
    }

}
