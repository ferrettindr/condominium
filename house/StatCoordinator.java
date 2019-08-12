package house;

import beans.HouseBean;
import beans.StatBean;
import beans.StatPkgBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import utility.Condo;

import java.util.Hashtable;
import java.util.Set;

public class StatCoordinator {

    StatPkgBean stats;

    Client client;

    public StatCoordinator(Client client) {
        stats = new StatPkgBean();
        this.client = client;
    }

    public synchronized void reset() {
        stats.resetStats();
    }

    public synchronized void addStat(HouseBean hb, StatBean sb) {
        //if the stat is more recent add it otherwise discard it
        StatBean houseStat = stats.getHousesStat().get(hb.getId());
        if (houseStat == null || sb.getTimestamp() > houseStat.getTimestamp())
            stats.addHouseStat(hb.getId(), sb);

        //check if all the house in the condo have given a stat if they did send it to server and reset stats
        Set<Integer> statsID = stats.getHousesStat().keySet();
        //put yourself in the condo because you want also you stat
        Hashtable<Integer, HouseBean> condoTemp = Condo.getInstance().getCondoTable();
        condoTemp.put(House.houseServer.getHouseBean().getId(), House.houseServer.getHouseBean());
        Set<Integer> condoID = condoTemp.keySet();

        if (statsID.containsAll(condoID)) {
            //compute updated condo cumulative stat with the max timestamp available
            long maxTime = 0;
            double sum = 0;
            for (StatBean var: stats.getHousesStat().values()) {
                if (var.getTimestamp() > maxTime)
                    maxTime = var.getTimestamp();
                sum += var.getValue();
            }
            StatBean condoStat = new StatBean();
            condoStat.setValue(sum);
            condoStat.setTimestamp(maxTime);
            stats.setCondoStat(condoStat);

            //and send to server
            WebResource res = client.resource(House.webURL+"stats/add/");
            ClientResponse response = res.type("application/json").put(ClientResponse.class, stats);
            House.handleResponse(response, false);

            //reset stats
            stats.resetStats();
        }
    }

}
