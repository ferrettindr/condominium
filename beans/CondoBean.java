package beans;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class CondoBean {

    @XmlElement(name="condo")
    private Hashtable<Integer, HouseBean> condoTable;
    private static CondoBean instance;

    //TODO use here rwlock as well isntead of synchronized
    public CondoBean() {
        condoTable = new Hashtable<Integer, HouseBean>();
    }

    //singleton
    public synchronized static CondoBean getInstance(){
        if(instance==null)
            instance = new CondoBean();
        return instance;
    }

    public synchronized List<HouseBean> getCondoTable() {
        return new ArrayList<HouseBean>(condoTable.values());
    }

    //TODO synchronized or removed?
    public void setCondoTable(Hashtable<Integer, HouseBean> condoTable) {
        this.condoTable = condoTable;
    }

    public synchronized boolean addHouse(HouseBean h){
        if (condoTable.containsKey(h.getId()))
           return false;
        condoTable.put(h.getId(), h);
        return true;
    }

    public synchronized boolean removeHouse(int id) {
        if (null == condoTable.remove(id))
            return false;
        else
            return true;
    }


}