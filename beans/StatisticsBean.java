package beans;

import java.util.*;
import util.RWLock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class StatisticsBean {

    @XmlElement(name="houseStat")
    private Hashtable<Integer, ArrayList<StatBean>> statsTable;
    @XmlElement(name="condoStat")
    private ArrayList<StatBean> condoStats;
    private static StatisticsBean instance;
    public RWLock rwLock;

    public StatisticsBean() {
        statsTable = new Hashtable<Integer, ArrayList<StatBean>>();
        condoStats = new ArrayList<StatBean>();
        rwLock = new RWLock();
    }

    //singleton
    public synchronized static StatisticsBean getInstance(){
        if(instance==null)
            instance = new StatisticsBean();
        return instance;
    }

    //add (or append) all the stats received
    public void addStatistics(Hashtable<Integer, StatBean> houseStats, StatBean condo) {
        rwLock.beginWrite();

        Iterator<Map.Entry<Integer, StatBean>> it = houseStats.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, StatBean> entry = it.next();
            Integer key = entry.getKey();
            StatBean value = entry.getValue();
            addStat(key, value);
        }
        condoStats.add(condo);

        rwLock.endWrite();
    }

    //add (or append) a single stat to the id of the house
    private void addStat(int id, StatBean stat){
        if (statsTable.containsKey(id))
            statsTable.get(id).add(stat);
        else {
            ArrayList<StatBean> val = new ArrayList<StatBean>();
            val.add(stat);
            statsTable.put(id, val);
        }
    }

    //return the last n (or less if not available) stats associated with the id of the house
    public List<StatBean> getHouseStat(int id, int n) {
        rwLock.beginRead();

        List<StatBean> result = null;
        if (statsTable.containsKey(id)) {
            ArrayList<StatBean> houseStat = statsTable.get(id);
            result = getLastElements(n, houseStat);
        }

        rwLock.endRead();

        return result;
    }

    //return the last n (or less if not available) condo stat
    public List<StatBean> getCondoStat(int n) {
        rwLock.beginRead();

        List<StatBean> result = null;
        result = getLastElements(n, condoStats);

        rwLock.endRead();

        return result;
    }

    //return the last n elements of list stats (shallow copy)
    private List<StatBean> getLastElements(int n, ArrayList<StatBean> stats) {
        ArrayList<StatBean> nStat = new ArrayList<>();
        int start = condoStats.size() > n ? condoStats.size() - n - 1 : 0;
        for (int i = start; i < condoStats.size(); i++)
            nStat.add(condoStats.get(i));
        return nStat;
    }

    //remove all the stat and the house from the table
    public boolean removeHouseStats(int id) {
        rwLock.beginWrite();

        boolean result = false;
        if (null == statsTable.remove(id))
            result = false;
        else
            result = true;

        rwLock.endWrite();

        return result;
    }
}
