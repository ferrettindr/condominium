package beans;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import utility.RWLock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class CondoBean {

    @XmlElement(name="condo")
    private Hashtable<Integer, HouseBean> condoTable;
    private RWLock rwLock;

    public CondoBean() {
        condoTable = new Hashtable<Integer, HouseBean>();
        rwLock = new RWLock();
    }

    //singleton
    /*
    public synchronized static CondoBean getInstance(){
        if(instance==null)
            instance = new CondoBean();
        return instance;
    }
    */

    public List<HouseBean> getCondoList() {
        rwLock.beginRead();

        ArrayList<HouseBean> result = (ArrayList<HouseBean>) condoTable.values();

        rwLock.endRead();

        return result;
    }

    public HouseBean getHouse(int id) {
        rwLock.beginRead();

        HouseBean result = null;
        result = condoTable.get(id);

        rwLock.endRead();

        return result;
    }

    //TODO synchronized or removed?
    //public void setCondoTable(Hashtable<Integer, HouseBean> condoTable) {
    //    this.condoTable = condoTable;
    //}

    //add a house to the condo if not already present
    public boolean addHouse(HouseBean h){
        rwLock.beginWrite();

        boolean result = false;
        if (condoTable.containsKey(h.getId()))
           result = false;
        else {
            condoTable.put(h.getId(), h);
            result = true;
        }

        rwLock.endWrite();

        return result;
    }

    //remove a house from the condo if present
    public boolean removeHouse(int id) {
        rwLock.beginWrite();

        boolean result = false;
        if (null == condoTable.remove(id))
            result = false;
        else
            result = true;

        rwLock.endWrite();

        return result;
    }


}