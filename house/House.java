package house;

import beans.HouseBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jettison.json.JSONException;
import utility.Condo;
import utility.Message;
import utility.MessageSender;
import utility.RWLock;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public class House {

    static final RWLock stoppedLock = new RWLock();
    static volatile boolean stopped = false;

    static HouseBean coordinator;
    static int coordinatorCounter;
    static RWLock coordinatorLock = new RWLock();
    static Boolean newElected;

    static HouseServer houseServer;
    static String webURL;

    static StatCoordinator statCoordinator;
    static StatSender statSender;
    static SmartMeterSimulator sms;

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

        //start potential coordinator before connecting to server
        statCoordinator = new StatCoordinator(client);
        SlidingBuffer sbuff = new SlidingBuffer(24, 12);
        statSender = new StatSender(sbuff);
        statSender.start();

        sms = new SmartMeterSimulator(sbuff);
        sms.start();



        //insert into the server
        WebResource resource = client.resource(webURL+"house/add/");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, houseServer.getHouseBean());
        handleResponse(response, true);

        //server response with the other houses in the network
        Hashtable<Integer, HouseBean> housesList = response.getEntity(new GenericType<Hashtable<Integer, HouseBean>>() {});
        housesList.remove(houseServer.getHouseBean().getId());

        //TESTING SLEEP
        /*
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

        //if house is empty become coordinator
        if (housesList.isEmpty())
            updateCoordinator(houseServer.getHouseBean(), 0);
        //otherwise send hello and set yourself as coordinator with less priority
        //this is down in case of all the other houses in the list removing themselves before the hello is received
        else {
            //send a hello msg to all the other houses in the network
            updateCoordinator(houseServer.getHouseBean(), -1);
            sendHello(housesList);
        }

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
                    stoppedLock.beginWrite();
                    sendRemovalToServer(client);

                    if (coordinator.getId() == houseServer.getHouseBean().getId() && !Condo.getInstance().getCondoTable().isEmpty()) {
                        try {electNewCoordinator();}
                        catch (IOException e) {e.printStackTrace();}
                        catch (JSONException e) {e.printStackTrace();}
                        catch (InterruptedException e) {e.printStackTrace();}
                    }
                    stopped = true;
                    System.out.println("sending removal to network");
                    sendRemovalToNetwork(Condo.getInstance().getCondoTable());
                    stoppedLock.endWrite();

                    shutdown();
                    break;
                case "boost":
                    break;
                case "coordinator":
                    System.out.println("My coordinator ID is: " + coordinator.getId() + " with counter: " + coordinatorCounter);
                    break;
                case "condo":
                    System.out.println("My condo is: " + Condo.getInstance().getCondoTable().toString());
                    break;
                default:
                    System.out.println("Unknown command");
            }
        }
    }


    //shuts down the house and the server
    private static void shutdown() {
        System.out.println("Shutting down");
        try {
            houseServer.getSocket().close();
        } catch (IOException e) {
            //should never happen
            e.printStackTrace();
        }
        sms.stopMeGently();
        statSender.interrupt();
    }

    //handle http response from restful web server. If abort is true the house is shotdown in case of an error status
    static void handleResponse(ClientResponse cr, boolean abort) {
        if (cr.getStatus() != 200) {
            System.out.println("" + cr.getStatus() + " " + cr.getStatusInfo().getReasonPhrase() + ": " + cr.getEntity(String.class));
            if (abort)
                shutdown();
        }
    }

    static void updateCoordinator(HouseBean newCoord, int counter) {
        coordinatorLock.beginWrite();
        //if the coordinator is more recent or same counter but greater id
        //after update if you become coordinator reset the statPkg
        if (coordinator == null) {
            coordinator = newCoord;
            coordinatorCounter = counter;
            if (isCoordinator()) {
                statCoordinator.reset();
            }
        }
        else if(counter > coordinatorCounter) {
            coordinator = newCoord;
            coordinatorCounter = counter;
            if (isCoordinator()) {
                statCoordinator.reset();
            }
        }
        else if (counter == coordinatorCounter && newCoord.getId() > coordinator.getId()) {
            coordinator = newCoord;
            coordinatorCounter = counter;
            if (isCoordinator()) {
                statCoordinator.reset();
            }
        }



        coordinatorLock.endWrite();
    }

    //TODO see if there's a better way for parallel sending and a way to wait for the ending of all the threads
    //send a message to all the houses in the list
    static ArrayList<Thread> sendMessageToCondo(Collection<HouseBean> housesList, Message msg) {
        ArrayList<Thread> threadList = new ArrayList<>();
        for (HouseBean hb: housesList) {
            Thread thread = new Thread(new MessageSender(hb.getIpAddress(), hb.getPort(), msg, hb.getId()));
            thread.start();
            threadList.add(thread);
        }
        return threadList;
    }

    //if the house is the current coordinator select a new one and tell it
    private static void electNewCoordinator() throws IOException, JSONException, InterruptedException {
        newElected = false;
        int counter = coordinatorCounter;
        coordinator = null;
        //if the selected one doesn't respond select another one (possibly the same) and set it with higher priority
        //reload the condo table to get a fresh one in case new nodes inserted or removed.
        while (!newElected) {
            Hashtable<Integer, HouseBean> condo =  Condo.getInstance().getCondoTable();
            if (!condo.isEmpty()) {
                int max = -1;
                for (Integer i : condo.keySet())
                    if (i >= max)
                        max = i;
                HouseBean newCoordinator = condo.get(max);
                Message msg = new Message();
                msg.setHeader("ELECTED");
                msg.setContent(houseServer.getHouseBean());
                counter += 1;
                msg.addParameter(counter);
                sendMessageToHouse(newCoordinator, msg);
            }
            synchronized (newElected) {
                newElected.wait(2000);
            }
        }
    }

    static void sendMessageToHouse(HouseBean hb, Message msg) throws IOException, JSONException {
        Socket sendSocket = new Socket(hb.getIpAddress(), hb.getPort());
        DataOutputStream dos = new DataOutputStream(sendSocket.getOutputStream());
        dos.writeUTF(msg.toJSONString());
        dos.flush();
        dos.close();
        sendSocket.close();
    }

    static boolean isCoordinator() {
        House.coordinatorLock.beginRead();
        //if i'm the coordinator add it to the structure
        boolean result = false;
        if (coordinator != null && coordinator.getId() == houseServer.getHouseBean().getId())
            result = true;

        House.coordinatorLock.endRead();
        return result;
    }

    private static void sendRemovalToServer(Client client) {
        WebResource resource = client.resource(webURL+"house/remove/"+ houseServer.getHouseBean().getId());
        ClientResponse response = resource.type("application/json").delete(ClientResponse.class);
        handleResponse(response, false);
    }

    //inform the other houses of the condo of its exit
    private static void sendRemovalToNetwork(Hashtable<Integer, HouseBean> housesTable) {
        Message msg = new Message();
        msg.setHeader("REMOVE");
        try {
            msg.setContent(houseServer.getHouseBean());
        } catch (IOException e) {e.printStackTrace();}

        sendMessageToCondo(housesTable.values(), msg);
    }


    //inform the other houses of the condo of its entrance
    private static void sendHello(Hashtable<Integer, HouseBean> housesTable) {
        Message msg = new Message();
        msg.setHeader("HELLO");
        try {
            msg.setContent(houseServer.getHouseBean());
            msg.addParameter(coordinatorCounter);
        } catch (IOException e) {e.printStackTrace();}

        sendMessageToCondo(housesTable.values(), msg);

    }

}

