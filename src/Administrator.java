import beans.AdministratorBean;
import beans.HouseBean;
import beans.StatBean;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;

public class Administrator {

    public static void main(String args[]) {
        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);

        String webURL = "http://" + serverIp + ":" + serverPort + "/condo/";

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JacksonJsonProvider.class);
        Client client = Client.create(cc);

        //server socket for notifications
        ServerSocket ss = null;
        int listenerServerPort = Integer.parseInt(args[2]);
        try {ss = new ServerSocket(listenerServerPort);}
        catch (IOException e) {e.printStackTrace();}

        AdministratorListener listener = new AdministratorListener(ss);
        listener.start();

        boolean exit = false;
        AdministratorBean ab = new AdministratorBean();
        try {
            ab.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        ab.setPort(listenerServerPort);

        String help = "\nCommands:\n" +
                "CONDO               - to get the list of houses in the condo\n" +
                "HOUSESTATS ID N     - to get the last N statistics (or less if not available) of the house with id ID\n" +
                "CONDOSTATS N        - to get the last N statistics (or less if not available) of the condo\n" +
                "HOUSEANALYTICS ID N - to get the mean and standard deviation of the last N statistics (or less if not available) of the house with id ID\n" +
                "CONDOANALYTICS N    - to get the mean and standard deviation of the last N statistics (or less if not available) of the condo\n" +
                "SUBSCRIBE SERVICE   - to subscribe for push notifications for the service SERVICE (IN, OUT, BOOST)\n" +
                "UNSUBSCRIBE SERVICE - to unsubscribe for push notifications for the service SERVICE (IN, OUT, BOOST)\n" +
                "HELP                - to print this list of commands again\n" +
                "QUIT                - to quit the application\n";
        System.out.println("\nWelcome to the condo administrator interface.");
        System.out.println(help);

        WebResource resource;
        ClientResponse response;
        while (!exit) {

            BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
            String line = "";
            try { line = bfr.readLine(); }
            catch (IOException e) { e.printStackTrace(); }
            String command[] = line.split(" ");
            switch (command[0]) {
                case "CONDO":
                    if (isCorrectParameters(command.length-1, 0)) {
                        resource = client.resource(webURL);
                        response = resource.type("application/json").get(ClientResponse.class);
                        if (isGoodResponse(response)) {
                            Hashtable<Integer, HouseBean> condo = response.getEntity(new GenericType<Hashtable<Integer, HouseBean>>() {
                            });
                            System.out.println("Condo:");
                            for (HouseBean hb : condo.values()) {
                                System.out.println("House - id: " + hb.getId() + " - ip: " + hb.getIpAddress() + " - port: " + hb.getPort());
                            }
                        }
                    }
                    break;
                case "HOUSESTATS":
                    if (isCorrectParameters(command.length-1, 2)) {
                        resource = client.resource(webURL + "stats/house/" + command[1] + "/" + command[2] + "/");
                        response = resource.type("application/json").get(ClientResponse.class);
                        if (isGoodResponse(response)) {
                            ArrayList<StatBean> stat = response.getEntity(new GenericType<ArrayList<StatBean>>() {
                            });
                            System.out.println("The last " + stat.size() + " stats of the house with id: " + command[1]);
                            for (StatBean sb : stat) {
                                long totSeconds = sb.getTimestamp()/1000;
                                long s = totSeconds % 60;
                                long m = (totSeconds / 60) % 60;
                                long h = (totSeconds / (60*60)) % 60;
                                System.out.println("Value: " + sb.getValue() + " Timestamp: " + String.format("%02d:%02d:%02d", h, m, s));
                            }
                        }
                    }
                    break;
                case "CONDOSTATS":
                    if (isCorrectParameters(command.length-1, 1)) {
                        resource = client.resource(webURL + "stats/" + command[1] + "/");
                        response = resource.type("application/json").get(ClientResponse.class);
                        if (isGoodResponse(response)) {
                            ArrayList<StatBean> condoStat = response.getEntity(new GenericType<ArrayList<StatBean>>() {
                            });
                            System.out.println("The last " + condoStat.size() + " stats of the condo:");
                            for (StatBean sb : condoStat) {
                                long totSeconds = sb.getTimestamp()/1000;
                                long s = totSeconds % 60;
                                long m = (totSeconds / 60) % 60;
                                long h = (totSeconds / (60*60)) % 60;
                                System.out.println("Value: " + sb.getValue() + " Timestamp: " + String.format("%02d:%02d:%02d", h, m, s));
                            }
                        }
                    }
                    break;
                case "HOUSEANALYTICS":
                    if (isCorrectParameters(command.length-1, 2)) {
                        resource = client.resource(webURL + "stats/analytics/house/" + command[1] + "/" + command[2] + "/");
                        response = resource.type("application/json").get(ClientResponse.class);
                        if (isGoodResponse(response)) {
                            double[] houseAnalytics = response.getEntity(double[].class);
                            System.out.println("Mean: " + houseAnalytics[0] + " and Standard deviation: " + houseAnalytics[1]);
                            System.out.println("calculated from last " + command[2] + " stats of the house with id: " + command[1]);
                        }
                    }
                    break;
                case "CONDOANALYTICS":
                    if (isCorrectParameters(command.length-1, 1)) {
                        resource = client.resource(webURL + "stats/analytics/" + command[1] + "/");
                        response = resource.type("application/json").get(ClientResponse.class);
                        if (isGoodResponse(response)) {
                            double[] condoAnalytics = response.getEntity(double[].class);
                            System.out.println("Mean: " + condoAnalytics[0] + " and Standard deviation: " + condoAnalytics[1]);
                            System.out.println("calculated from last " + command[1] + " stats of the condo");
                        }
                    }
                    break;
                case "SUBSCRIBE":
                    if (isCorrectParameters(command.length-1, 1)) {
                        resource = client.resource(webURL + "sub/" + command[1] + "/");
                        response = resource.type("application/json").post(ClientResponse.class, ab);
                        if (isGoodResponse(response)) {
                            System.out.println("Registered correctly for " + command[1] + " push notifications");
                        }
                    }
                    break;
                case "UNSUBSCRIBE":
                    if (isCorrectParameters(command.length-1, 1)) {
                        resource = client.resource(webURL + "unsub/" + command[1] + "/");
                        response = resource.type("application/json").post(ClientResponse.class, ab);
                        if (isGoodResponse(response)) {
                            System.out.println("Unregistered correctly for " + command[1] + " push notifications");
                        }
                    }
                    break;
                case "HELP":
                    System.out.println(help);
                    break;
                case "QUIT":
                    //unsubscribe from everything
                    resource = client.resource(webURL + "unsub/IN/");
                    response = resource.type("application/json").post(ClientResponse.class, ab);
                    resource = client.resource(webURL + "unsub/OUT/");
                    response = resource.type("application/json").post(ClientResponse.class, ab);
                    resource = client.resource(webURL + "unsub/BOOST/");
                    response = resource.type("application/json").post(ClientResponse.class, ab);
                    exit = true;
                    break;
                default:
                    System.out.println("Unknown command. Try with HELP for a list of commands");
                    break;
            }
        }
        try { ss.close(); }
        catch (IOException e) {e.printStackTrace();}
    }

    private static boolean isCorrectParameters(int parameters, int expectedParameters) {
        if (parameters < expectedParameters) {
            System.out.println("Wrong number of parameters. Expected " + expectedParameters + " parameters.");
            return false;
        }
        return true;
    }

    private static boolean isGoodResponse(ClientResponse cr) {
        if (cr.getStatus() != 200) {
            System.out.println("" + cr.getStatus() + " " + cr.getStatusInfo().getReasonPhrase() + ": " + cr.getEntity(String.class));
            return false;
        }
        return true;
    }
}
