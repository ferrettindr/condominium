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

    @XmlElement(name="house")
    private Hashtable<Integer, HouseBean> houseList;
    private static CondoBean instance;

    public CondoBean() {
        houseList = new Hashtable<Integer, HouseBean>();
    }

    //singleton
    public synchronized static CondoBean getInstance(){
        if(instance==null)
            instance = new CondoBean();
        return instance;
    }

    public synchronized List<HouseBean> getHouseList() {
        return new ArrayList<HouseBean>(houseList.values());
    }

    //synchronized or removed?
    public void setHouseList(Hashtable<Integer, HouseBean> houseList) {
        this.houseList = houseList;
    }

    public synchronized boolean addHouse(HouseBean h){
        if (houseList.containsKey(h.getId()))
           return false;
        houseList.put(h.getId(), h);
        return true;
    }

    public synchronized void removeHouse(int id) {
        houseList.remove(id);
    }


}