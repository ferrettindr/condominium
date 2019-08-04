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

    //TODO change in a singleton class with 3 different lists
    Notifier inNotifier = new Notifier(Notifier.PushType.IN);
    Notifier outNotifier = new Notifier(Notifier.PushType.OUT);
    Notifier spikeNotifier = new Notifier(Notifier.PushType.SPIKE);


    @GET
    @Produces({"application/json"})
    public Response getCondo(){

        return Response.ok(Condo.getInstance().getCondoTable()).build();

    }

    //insert new house in the condo if not already present
    @Path("house/add")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response addHouse(HouseBean h){
        if (Condo.getInstance().addHouse(h)) {
            inNotifier.notify(h);
            return Response.ok(Condo.getInstance().getCondoTable()).build();
        }
        else
            return Response.status(Response.Status.CONFLICT).entity("The chosen ID: " + h.getId() + " is already being used by another house.").build();
    }

    //remove a house and its stats from the condo
    @Path("house/remove/{id}")
    @DELETE
    public Response removeHouse(@PathParam("id") int id){
        Statistics stat = Statistics.getInstance();
        //lock to avoid race condition if someone is using stats
        //no-deadlock because it's the same thread
        stat.rwLock.beginWrite();

        //if trying to remove a house not present abort
        Response result = Response.ok().build();
        HouseBean h = Condo.getInstance().getHouse(id);
        if (!Condo.getInstance().removeHouse(id)) {
            result = Response.status(Response.Status.CONFLICT).entity("Could not remove the house. There is no house with ID: " +  id).build();
        }
        else {
            stat.removeHouseStats(id);
            outNotifier.notify(h);
        }

        stat.rwLock.endWrite();

        return result;
    }

    @Path("stats/add")
    @PUT
    @Consumes({"application/json"})
    public Response addStatistics(StatPkgBean pkg) {
        Statistics.getInstance().addStatistics(pkg.getHousesStat(), pkg.getCondoStat());
        return Response.ok().build();
    }

    //TODO handle if there are no stats
    @Path("stats/{n}")
    @GET
    @Produces({"application/json"})
    public Response getCondoStatistics(@PathParam("n") int n) {
        List<StatBean> stats = Statistics.getInstance().getCondoStat(n);
        return Response.ok(stats).build();
    }

    //TODO handle case if a nonexistant id is used or the house doesn't have any stat
    @Path("stats/house/{id}/{n}")
    @GET
    @Produces({"application/json"})
    public Response getHouseStatistics(@PathParam("id") int id,@PathParam("n") int n) {
        List<StatBean> stats = Statistics.getInstance().getHouseStat(id, n);
        return Response.ok(stats).build();
    }

    //TODO handle if there are no stats
    @Path("stats/analytics/{n}")
    @GET
    @Produces({"application/json"})
    public Response getCondoAnalytics(@PathParam("n") int n) {
        List<StatBean> stats = Statistics.getInstance().getCondoStat(n);
        return Response.ok(calculateAnalytics(stats, n)).build();
    }

    //TODO handle case if a nonexistant id is used or the house doesn't have any stat
    @Path("stats/analytics/house/{id}/{n}")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response getHouseAnalytics(@PathParam("id") int id,@PathParam("n") int n) {
        List<StatBean> stats = Statistics.getInstance().getHouseStat(id, n);
        return Response.ok(calculateAnalytics(stats, n)).build();
    }

    //TODO method that notify when there's a spike
    @Path("sub/{action}")
    @POST
    @Consumes({"application/json"})
    public Response subNotification(@PathParam("action") Notifier.PushType type , AdministratorBean obs) {
        switch(type) {
            case IN:
                inNotifier.addObserver(obs);
                break;
            case OUT:
                outNotifier.addObserver(obs);
                break;
            case SPIKE:
                spikeNotifier.addObserver(obs);
                break;
            default:
                return Response.status(Response.Status.BAD_REQUEST).entity("Wrong subscription type requested.").build();
        }
        return Response.ok().build();
    }

    @Path("unsub/{action}")
    @POST
    @Consumes({"application/json"})
    public Response unsubNotification(@PathParam("action") Notifier.PushType type , AdministratorBean obs) {
        switch(type) {
            case IN:
                inNotifier.removeObserver(obs);
                break;
            case OUT:
                outNotifier.removeObserver(obs);
                break;
            case SPIKE:
                spikeNotifier.removeObserver(obs);
                break;
            default:
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
            sum += stat.getConsumption();
        return sum;
    }


    private double sumSquareStat(Iterable<StatBean> stats) {
        double sum = 0;
        for (StatBean stat: stats)
            sum +=  Math.pow(stat.getConsumption(), 2);
        return sum;
    }

    /*
    //permette di prelevare con un determinato nome
    @Path("get/{name}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getByName(@PathParam("name") String name){
        User u = Users.getInstance().getByName(name);
        if(u!=null)
            return Response.ok(u).build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    */


}