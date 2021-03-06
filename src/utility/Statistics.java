package utility;

import java.util.*;

import beans.StatBean;


public class Statistics {

    //server data structure for saving stats of all houses

    private Hashtable<Integer, ArrayList<StatBean>> statsTable;
    private ArrayList<StatBean> condoStats;

    private  static Statistics instance = null;
    public RWLock rwLock;

    //singleton
    private Statistics() {
        statsTable = new Hashtable<Integer, ArrayList<StatBean>>();
        condoStats = new ArrayList<StatBean>();
        rwLock = new RWLock();
    }

    public synchronized static Statistics getInstance(){
        if(instance==null)
            instance = new Statistics();
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
            ArrayList<StatBean> val = new ArrayList<>();
            val.add(stat);
            statsTable.put(id, val);
        }
    }

    //return a copy the last n (or less if not available) stats associated with the id of the house
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

    //return a copy of the last n (or less if not available) condo stat
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
        int start = stats.size() > n ? stats.size() - n : 0;
        for (int i = start; i < stats.size(); i++)
            nStat.add(0, stats.get(i));
        return nStat;
    }

    //remove all the stats of the house and the house from the table
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
