package services;


import beans.CondoBean;
import beans.HouseBean;
import beans.StatBean;
import beans.StatisticsBean;
import beans.StatPkgBean;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Path("condo")
public class AdministratorServer {

    //TODO: carefull if returns condo or the table
    @GET
    @Produces({"application/json"})
    public Response getCondo(){

        return Response.ok(CondoBean.getInstance()).build();

    }

    //insert new house in the condo if not already present
    @Path("house/add")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response addHouse(HouseBean h){
        if (CondoBean.getInstance().addHouse(h))
            return Response.ok(CondoBean.getInstance()).build();
        else
            return Response.status(Response.Status.CONFLICT).build();
    }

    //remove a house and its stats from the condo
    @Path("house/remove")
    @POST
    @Consumes({"application/json"})
    public Response removeHouse(HouseBean h){
        StatisticsBean stat = StatisticsBean.getInstance();
        //lock to avoid race condition if someone is reading stats
        //no-deadlock because it's the same thread
        stat.rwLock.beginWrite();

        //if trying to remove a house not present abort
        Response result = Response.ok().build();
        if (CondoBean.getInstance().removeHouse(h.getId())) {
            result = Response.status(Response.Status.CONFLICT).build();
        }
        stat.removeHouseStats(h.getId());

        stat.rwLock.endWrite();

        return result;
    }

    @Path("stats/add")
    @POST
    @Consumes({"application/json"})
    public Response addStatistics(StatPkgBean pkg) {
        StatisticsBean.getInstance().addStatistics(pkg.getHouseStat(), pkg.getCondoStat());
        return Response.ok().build();
    }

    @Path("stats/get")
    @POST
    @Produces({"application/json"})
    public Response getCondoStatistics(int n) {
        List<StatBean> stats = StatisticsBean.getInstance().getCondoStat(n);
        return Response.ok(stats).build();
    }

    @Path("stats/get/house")
    @POST
    @Produces({"application/json"})
    public Response getHouseStatistics(int id, int n) {
        List<StatBean> stats = StatisticsBean.getInstance().getHouseStat(id, n);
        return Response.ok(stats).build();
    }

    @Path("stats/get/analytics")
    @POST
    @Produces({"application/json"})
    public Response getCondoAnalytics(int n) {
        List<StatBean> stats = StatisticsBean.getInstance().getCondoStat(n);
        //TODO calculate mean and std dev
        return Response.ok(stats).build();
    }

    @Path("stats/get/house/analytics")
    @POST
    @Produces({"application/json"})
    public Response getHouseAnalytics(int id, int n) {
        List<StatBean> stats = StatisticsBean.getInstance().getHouseStat(id, n);
        //TODO calculate mean and std dev
        return Response.ok(stats).build();
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