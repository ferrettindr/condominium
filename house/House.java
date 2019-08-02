package house;

import beans.CondoTableBean;
import beans.HouseBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class House {

    int localId;
    int localPort;
    int serverPort;
    String serverIp;
    HouseBean houseBean = new HouseBean();

    public House(int id, int port, int serverPort, String serverIp) {
        this.localId = id;
        this.localPort = port;
        this.serverPort = serverPort;
        this.serverIp = serverIp;
        houseBean.setId(localId);
        houseBean.setPort(localPort);
        try {
            houseBean.setIpAddress(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {throw new RuntimeException("Unable to get local IP");}
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

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JacksonJsonProvider.class);
        Client client = Client.create(cc);

        WebResource resource = client.resource(webURL+"house/add/");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, house.getHouseBean());
        if (response.getStatus() == 409) {
            throw new RuntimeException("House ID: " + house.getHouseBean().getId() + " already in use pick another one and try again");
        }
        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed to register to the condo server, aborting. HTTP error: " + response.getStatus());
        }

        CondoTableBean housesList = response.getEntity(CondoTableBean.class);



    }
}

