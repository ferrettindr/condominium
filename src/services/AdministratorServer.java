package services;


import beans.*;
import utility.Condo;
import utility.Notifier;
import utility.Statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("condo")
public class AdministratorServer {

    @GET
    @Produces({"application/json"})
    public Response getCondo(){

        return Response.ok(Condo.getInstance().getCondoTable()).build();

    }

    //insert new house in the condo if not already present
    //remove the stat associated with previous id chosen now
    @Path("house/add")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response addHouse(HouseBean h){

        //get lock to avoid people adding new stats
        Statistics.getInstance().rwLock.beginWrite();

        Response result;
        if (Condo.getInstance().addHouse(h)) {
            Statistics.getInstance().removeHouseStats(h.getId());
            Notifier.getIstance().notify(Notifier.PushType.IN, h);
            result = Response.ok(Condo.getInstance().getCondoTable()).build();
        }
        else
            result = Response.status(Response.Status.CONFLICT).entity("There is already a house with ID: " + h.getId()).build();

        Statistics.getInstance().rwLock.endWrite();

        return result;
    }

    //remove a house from the condo but leave the stats
    @Path("house/remove/{id}")
    @DELETE
    public Response removeHouse(@PathParam("id") int id){

        //if trying to remove a house not present abort
        Response result = Response.ok().build();
        HouseBean h = Condo.getInstance().getHouse(id);
        if (!Condo.getInstance().removeHouse(id)) {
            result = Response.status(Response.Status.CONFLICT).entity("Could not remove the house. There is no house with ID: " +  id).build();
        }
        else {
            Notifier.getIstance().notify(Notifier.PushType.OUT, h);
        }

        return result;
    }

    @Path("stats/add")
    @PUT
    @Consumes({"application/json"})
    public Response addStatistics(StatPkgBean pkg) {
        Statistics.getInstance().addStatistics(pkg.getHousesStat(), pkg.getCondoStat());
        System.out.println("Stats are being added. Condo: " + pkg.getCondoStat().getValue() + ":" + pkg.getCondoStat().getTimestamp());
        System.out.println("Houses: " + pkg.getHousesStat().toString());
        return Response.ok().build();
    }

    @Path("stats/{n}")
    @GET
    @Produces({"application/json"})
    public Response getCondoStatistics(@PathParam("n") int n) {
        List<StatBean> stats = Statistics.getInstance().getCondoStat(n);
        return Response.ok(stats).build();
    }

    @Path("stats/house/{id}/{n}")
    @GET
    @Produces({"application/json"})
    public Response getHouseStatistics(@PathParam("id") int id,@PathParam("n") int n) {
        //if the house is not present you can't send stats
        if (!Condo.getInstance().getCondoTable().containsKey(id))
            return Response.status(Response.Status.CONFLICT).entity("There is no house with ID: " +  id).build();
        List<StatBean> stats = Statistics.getInstance().getHouseStat(id, n);
        return Response.ok(stats).build();
    }

    @Path("stats/analytics/{n}")
    @GET
    @Produces({"application/json"})
    public Response getCondoAnalytics(@PathParam("n") int n) {
        List<StatBean> stats = Statistics.getInstance().getCondoStat(n);
        return Response.ok(calculateAnalytics(stats, n)).build();
    }

    @Path("stats/analytics/house/{id}/{n}")
    @GET
    @Produces({"application/json"})
    public Response getHouseAnalytics(@PathParam("id") int id,@PathParam("n") int n) {
        //if the house is not present you can't send stats
        if (!Condo.getInstance().getCondoTable().containsKey(id))
            return Response.status(Response.Status.CONFLICT).entity("There is no house with ID: " +  id).build();
        List<StatBean> stats = Statistics.getInstance().getHouseStat(id, n);
        return Response.ok(calculateAnalytics(stats, n)).build();
    }

    //notify when there's a boost
    @Path("boost")
    @PUT
    @Consumes({"application/json"})
    public Response boostAlert(HouseBean hb) {
        Notifier.getIstance().notify(Notifier.PushType.BOOST, hb);
        return Response.ok().build();
    }

    @Path("sub/{action}")
    @POST
    @Consumes({"application/json"})
    public Response subNotification(@PathParam("action") Notifier.PushType type , AdministratorBean obs) {
        try {
            Notifier.getIstance().addObserver(type, obs);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Wrong subscription type requested.").build();
        }
        return Response.ok().build();
    }

    @Path("unsub/{action}")
    @POST
    @Consumes({"application/json"})
    public Response unsubNotification(@PathParam("action") Notifier.PushType type , AdministratorBean obs) {
        try {
            Notifier.getIstance().removeObserver(type, obs);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).entity("Wrong unsubscription type requested.").build();
        }
        return Response.ok().build();
    }

    private double[] calculateAnalytics(List<StatBean> stats, int n) {
        double mean, std_dev;
        double res[] = new double[2];
        mean = sumStat(stats) / n;
        std_dev = Math.sqrt((sumSquareStat(stats)/n) - Math.pow(mean, 2));
        res[0] = mean;
        res[1] = std_dev;

        return res;
    }

    private double sumStat(Iterable<StatBean> stats) {
        double sum = 0;
        for (StatBean stat: stats)
            sum += stat.getValue();
        return sum;
    }


    private double sumSquareStat(Iterable<StatBean> stats) {
        double sum = 0;
        for (StatBean stat: stats)
            sum +=  Math.pow(stat.getValue(), 2);
        return sum;
    }

}