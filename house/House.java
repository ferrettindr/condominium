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
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;

public class House {

    static final RWLock stoppedLock = new RWLock();
    static volatile boolean stopped = false;

    static final RWLock boostLock = new RWLock();
    static final Hashtable<Integer, RequestQueue> boosts = new Hashtable<>();

    static HouseBean coordinator;
    static int coordinatorCounter;
    static final RWLock coordinatorLock = new RWLock();
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

        final RequestQueue boostOne = new RequestQueue();
        final RequestQueue boostTwo = new RequestQueue();
        boosts.put(1, boostOne);
        boosts.put(2, boostTwo);

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

        Hashtable<Integer, HouseBean> housesList = new Hashtable<>();
        //server response with the other houses in the network
        try {
            housesList = response.getEntity(new GenericType<Hashtable<Integer, HouseBean>>() {});
            housesList.remove(houseServer.getHouseBean().getId());
        } catch (Exception e) {}

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
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));

        while (!stopped) {
            System.out.println("Insert \"exit\" to exit from the condo or \"boost\" to request a boost in usage:");
            String command = "";
            try {
                command = bfr.readLine();
            } catch (IOException e) {System.err.println(e.getMessage() + "Cannot read user input");}
            switch (command) {
                case "exit":
                    sendRemovalToServer(client);
                    quit();
                    break;
                case "boost":
                    boostLock.beginWrite();
                    if (boosts.get(1).isResourceOccupied() || boosts.get(2).isUsingResource())
                        System.out.println("Boost already requested.");
                    else {
                        boosts.get(1).waitResource();
                        boosts.get(2).waitResource();
                        try {requestBoost();} catch (IOException e) {e.printStackTrace();}
                        catch (InterruptedException e) {e.printStackTrace();}
                    }
                    boostLock.endWrite();
                    break;
                case "coordinator":
                    System.out.println("My coordinator ID is: " + coordinator.getId() + " with counter: " + coordinatorCounter);
                    break;
                case "condo":
                    System.out.println("My condo is: " + Condo.getInstance().getCondoTable().toString());
                    break;
                case "waiting":
                    for (int i = 1; i<=boosts.size(); i++) {
                        ArrayList<HouseBean> tmp = null;
                        try {
                            tmp = boosts.get(i).resetWaiting();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String  str = "";
                        for (HouseBean hb: tmp)
                            str += " " + hb.getId();
                        System.out.println("Waiting queue for boost: " + i + " waiting queue: " + str);
                    }
                    break;
                default:
                    System.out.println("Unknown command");
            }
        }
        System.out.println("Exiting house");
    }

    private static void quit() {
        stoppedLock.beginWrite();

        if (isCoordinator() && !Condo.getInstance().getCondoTable().isEmpty()) {
            try {electNewCoordinator();}
            catch (IOException e) {e.printStackTrace();}
            catch (JSONException e) {e.printStackTrace();}
            catch (InterruptedException e) {e.printStackTrace();}
        }
        stopped = true;
        System.out.println("sending removal to network");
        sendRemovalToNetwork(Condo.getInstance().getCondoTable());
        stoppedLock.endWrite();

        //no need to check if executing boost because stopped sending stats
        boostLock.beginWrite();
        try {sendOkBoosts();} catch (IOException e) {e.printStackTrace();}
        boostLock.endWrite();
        shutdown();
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

    //send ok to all the elements in the waiting queue for each boost and reset it
    static private void sendOkBoosts() throws IOException {
        for (int i = 1; i <= boosts.size(); i++) {
            //remove yourself so it doesn't send ok to yourself (message handler already shut down)
            boosts.get(i).removeFromWaiting(houseServer.getHouseBean().getId());
            sendOkMessageToCondo(i);
        }
    }

    //send ok message to all the element in waiting queue of boostIndex and reset it
    static void sendOkMessageToCondo(int boostIndex) throws IOException {
        Message ok = new Message();
        ok.setHeader("BOOST_OK");
        ok.setContent(houseServer.getHouseBean());
        ok.addParameter(boostIndex);
        ArrayList<HouseBean> tmp = boosts.get(boostIndex).resetWaiting();
        String  str = "";
        for (HouseBean hb: tmp)
            str += " " + hb.getId();
        System.out.println("Freeing boost num: " + boostIndex + " waiting queue: " + str);
        sendMessageToCondo(tmp, ok);
    }

    //handle http response from restful web server. If abort is true the house is shutdown in case of an error status
    static void handleResponse(ClientResponse cr, boolean abort) {
        if (cr.getStatus() != 200) {
            System.out.println("" + cr.getStatus() + " " + cr.getStatusInfo().getReasonPhrase() + ": " + cr.getEntity(String.class));
            if (abort)
                quit();
        }
    }

    //send to all the condo (including yourself) the boost request
    static void requestBoost() throws IOException, InterruptedException {
        Message msg = new Message();
        msg.setHeader("BOOST_REQUEST");

        try {
            //ask for both boost
            msg.setContent(houseServer.getHouseBean());
            msg.setTimestamp(System.currentTimeMillis());
        } catch (IOException e) {e.printStackTrace();}

        Hashtable<Integer, HouseBean> list = Condo.getInstance().getCondoTable();
        //TODO change okqueue. When receive a boostOK add the sender to your ok queue and check your okQueue vs the condo
        //TODO if okQueue set is greater than condo set get the boost
        //TODO even if condo is empty send request to yourself and if you get a request from youself and the condo is empty get the boost
        //if ok list is empty i'm the only one and i can use the boost
        if (list.isEmpty()) {
            //TODO inform server you have the boost
            //leave lock to allow yourself to handle boost requests from other nodes while you are using the boost
            boostLock.endWrite();
            sms.boost();
            boostLock.beginWrite();
            for (int i = 1; i <= boosts.size(); i++)
                boosts.get(i).freeResource();
        }
        else {
            for (int i = 1; i <= boosts.size(); i++) {
                boosts.get(i).resetOk();
                //set my self in every queue as first
                boosts.get(i).resetWaiting();
                boosts.get(i).addToWaiting(msg);
            }
        }


        //send message to yourself also so you can put yourself in the waiting queue
        //list.put(houseServer.getHouseBean().getId(), houseServer.getHouseBean());
        sendMessageToCondo(list.values(), msg);
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
        handleResponse(response,false);
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

