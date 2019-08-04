package house;

import beans.HouseBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import utility.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;

public class House {

    public static void main(String args[]) {
        int id = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        int serverPort = Integer.parseInt(args[2]);
        String serverIp = args[3];

        //TODO insert the possibility to insert the parameters after launching the program
        HouseServer houseServer = new HouseServer(id, port, serverPort, serverIp);
        //start the house server
        Thread houseServerThread = new Thread(houseServer);
        houseServerThread.start();

        //Insert the house into the condo and get the list of houses from the server
        String webURL = "http://" + serverIp + ":" + serverPort + "/condo/";

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JacksonJsonProvider.class);
        //cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        Client client = Client.create(cc);

        WebResource resource = client.resource(webURL+"house/add/");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, houseServer.getHouseBean());
        if (response.getStatus() == 409) {
            throw new RuntimeException("House ID: " + houseServer.getHouseBean().getId() + " already in use pick another one and try again");
        }
        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed to register to the condo server, aborting. HTTP error: " + response.getStatus());
        }


        //server response with the other houses in the network
        Hashtable<Integer, HouseBean> housesList = response.getEntity(new GenericType<Hashtable<Integer, HouseBean>>() {});
        housesList.remove(houseServer.getHouseBean().getId());


        /* how to serialize and deserialize with ow
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = "ciao";
        try {
            json = ow.writeValueAsString(houseServer.getHouseBean());
        } catch (IOException e) {
            throw new RuntimeException("Cannot transform HouseBean to JSON");
        }
        ObjectMapper om = new ObjectMapper();
        HouseBean hb = null;
        try {
            hb = om.readValue(json, HouseBean.class);
        } catch (IOException e) {
            throw new RuntimeException(("Cannot transform JSON to HouseBean"));
        }
        */

        //send hello to all the other houses when entering the network
        /*
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String houseJSON;
        try {
            houseJSON = ow.writeValueAsString(houseServer.getHouseBean());
        } catch (IOException e) {
            throw new RuntimeException("Cannot transform HouseBean to JSON");
        }
        */
        Message msg = new Message();
        try {
            msg.setHeader("HELLO");
            msg.setContent(houseServer.getHouseBean());
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (HouseBean hb: housesList.values()) {
            try {
                Socket helloSocket = new Socket(hb.getIpAddress(), hb.getPort());
                DataOutputStream helloOut = new DataOutputStream(helloSocket.getOutputStream());
                helloOut.writeUTF(msg.toJSONString());
                helloOut.flush();
                helloOut.close();
                helloSocket.close();
            } catch (Exception e) {System.err.println(e.getMessage() + ". Unable to send HELLO msg to the house with ID: " + hb.getId());}
        }


    }
}

