package house;

import beans.HouseBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import utility.Condo;
import utility.Message;
import utility.RWLock;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Collection;
import java.util.Hashtable;

public class House {

    protected static final RWLock stoppedLock = new RWLock();
    protected static volatile boolean stopped = false;
    protected static String webURL;
    private static HouseServer houseServer;

    public static void main(String args[]) {
        int id = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        int remoteServerPort = Integer.parseInt(args[2]);
        String remoteServerIp = args[3];

        houseServer = new HouseServer(id, port, remoteServerPort, remoteServerIp);
        //start the house server
        Thread houseServerThread = new Thread(houseServer);
        houseServerThread.start();

        //Insert the house into the condo and get the list of houses from the server
        webURL = "http://" + remoteServerIp + ":" + remoteServerPort + "/condo/";

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JacksonJsonProvider.class);
        //cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        Client client = Client.create(cc);

        WebResource resource = client.resource(webURL+"house/add/");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, houseServer.getHouseBean());
        handleResponse(response, true);

        //server response with the other houses in the network
        Hashtable<Integer, HouseBean> housesList = response.getEntity(new GenericType<Hashtable<Integer, HouseBean>>() {});
        housesList.remove(houseServer.getHouseBean().getId());

        //send a hello msg to all the other houses in the network
        sendHello(housesList);

        //prompt user for remove or boost
        System.out.println("Insert \"exit\" to exit from the condo or \"boost\" to request a boost in usage:");
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));

        while (!stopped) {
            String command = "";
            try {
                command = bfr.readLine();
            } catch (IOException e) {System.err.println(e.getMessage() + "Cannot read user input");}
            switch (command) {
                case "exit":
                    sendRemovalToServer(client);
                    sendRemovalToNetwork(Condo.getInstance().getCondoTable());
                    shutdown();
                    break;
                case "boost":
                    break;
                default:
                    System.out.println("Unknown command");
            }
        }
    }

    private static void sendRemovalToServer(Client client) {
        WebResource resource = client.resource(webURL+"house/remove/"+ houseServer.getHouseBean().getId());
        ClientResponse response = resource.type("application/json").delete(ClientResponse.class);
        handleResponse(response, false);
    }

    //shuts down the house and the server
    private static void shutdown() {
        try {
            stopped = true;
            houseServer.getSocket().close();
        } catch (IOException e) {
            //should never happen
            e.printStackTrace();
        }
    }

    //handle http response from restful web server. If abort is true the house is shotdown in case of an error status
    private static void handleResponse(ClientResponse cr, boolean abort) {
        if (cr.getStatus() != 200) {
            System.out.println("" + cr.getStatus() + " " + cr.getStatusInfo().getReasonPhrase() + ": " + cr.getEntity(String.class));
            if (abort)
                shutdown();
        }
    }

    //send a message to all the houses in the list
    private static void sendMessageToCondo(Collection<HouseBean> housesList, Message msg) {
        for (HouseBean hb: housesList) {
            try {
                Socket s = new Socket(hb.getIpAddress(), hb.getPort());
                DataOutputStream Out = new DataOutputStream(s.getOutputStream());
                Out.writeUTF(msg.toJSONString());
                Out.flush();
                Out.close();
                s.close();
            } catch(Exception e) {System.err.println(e.getMessage() + ". Unable to send " + msg.getHeader() + " msg to house with ID: " + hb.getId());}
        }
    }

    //inform the other houses of the condo of its exit
    private static void sendRemovalToNetwork(Hashtable<Integer, HouseBean> housesTable) {
        //mutual exclusion with other messages
        stopped = true;
        stoppedLock.beginWrite();

        Message msg = new Message();
        msg.setHeader("REMOVE");
        try {
            msg.setContent(houseServer.getHouseBean());
        } catch (IOException e) {e.printStackTrace();}

        sendMessageToCondo(housesTable.values(), msg);

        stoppedLock.endWrite();
    }


    //inform the other houses of the condo of its entrance
    private static void sendHello(Hashtable<Integer, HouseBean> housesTable) {
        Message msg = new Message();
        msg.setHeader("HELLO");
        try {
            msg.setContent(houseServer.getHouseBean());
        } catch (IOException e) {e.printStackTrace();}

        sendMessageToCondo(housesTable.values(), msg);

    }

}

