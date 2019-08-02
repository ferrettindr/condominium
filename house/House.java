package house;

import beans.CondoBean;
import beans.HouseBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.MultivaluedMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class House {

    int localId;
    int localPort;
    int serverPort;
    String serverIp;
    CondoBean localCondo;
    HouseBean houseBean = new HouseBean();

    public House(int id, int port, int serverPort, String serverIp) {
        this.localId = id;
        this.localPort = port;
        this.serverPort = serverPort;
        this. serverIp = serverIp;
        localCondo = new CondoBean();
        houseBean.setId(localId);
        houseBean.setPort(localPort);
        try {
            houseBean.setIpAddress(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {e.printStackTrace();}
    }

    public HouseBean getHouseBean() {
        return houseBean;
    }

    public static void main(String args[]) {
        int id = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        int serverPort = Integer.parseInt(args[2]);
        String serverIp = args[3];

        //TODO insert the possibility to insert the parameters after launching the program
        House house = new House(id, port, serverPort, serverIp);

        //Insert the house into the condo and get the list of houses from the server
        String webURL = "http://" + serverIp + ":" + serverPort + "/condo/";
        Client client = Client.create();
        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("h", house.getHouseBean());
        WebResource resource = client.resource(webURL+"house/add/");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, formData);
        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed to register to the condo server, aborting. HTTP error: " + response.getStatus());
        }

        ArrayList<HouseBean> housesList = response.getEntity(ArrayList.class);

        for (HouseBean hb: housesList) {
            System.out.println(hb.toString());
        }



    }
}
