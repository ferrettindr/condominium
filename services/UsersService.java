package services;


import beans.CondoBean;
import beans.HouseBean;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("condo")
public class AdministratorServer {

    //restituisce la lista di utenti
    @GET
    @Produces({"application/json"})
    public Response getCondo(){

        return Response.ok(CondoBean.getInstance()).build();

    }

    //permette di inserire un utente (nome e cognome)
    @Path("house/add")
    @POST
    @Consumes({"application/json", "application/xml"})
    public Response addUser(User u){
        Users.getInstance().add(u);
        return Response.ok().build();
    }

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


}