package utility;

import beans.HouseBean;
import beans.AdministratorBean;
import org.codehaus.jettison.json.JSONException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Notifier {

    public enum PushType {
        IN,
        OUT,
        BOOST;
    }

    private RWLock inLock;
    private ArrayList<AdministratorBean> inObservers;
    private RWLock outLock;
    private ArrayList<AdministratorBean> outObservers;
    private RWLock boostLock;
    private ArrayList<AdministratorBean> boostObservers;

    private static Notifier instance = null;

    //singleton
    public static synchronized Notifier getIstance() {
        if (instance == null)
            instance = new Notifier();
        return instance;
    }

    private Notifier() {
        inLock = new RWLock();
        inObservers = new ArrayList<>();
        outLock = new RWLock();
        outObservers = new ArrayList<>();
        boostLock = new RWLock();
        boostObservers = new ArrayList<>();
    }

    public void addObserver(PushType type, AdministratorBean obs) {
        switch (type) {
            case IN:
                addObserverToList(inObservers, inLock, obs);
                break;
            case OUT:
                addObserverToList(outObservers, outLock, obs);
                break;
            case BOOST:
                addObserverToList(boostObservers, boostLock, obs);
                break;
            default:
                break;
        }
    }

    public void removeObserver(PushType type, AdministratorBean obs) {
        switch (type) {
            case IN:
                removeObserverFromList(inObservers, inLock, obs);
                break;
            case OUT:
                removeObserverFromList(outObservers, outLock, obs);
                break;
            case BOOST:
                removeObserverFromList(boostObservers, boostLock, obs);
                break;
            default:
                break;
        }
    }

    //add observer to the list if not already present
    private void addObserverToList(ArrayList<AdministratorBean> abl, RWLock lock, AdministratorBean obs) {
        lock.beginWrite();
        if (!containsObserver(abl, obs))
            abl.add(obs);
        lock.endWrite();
    }
    private void removeObserverFromList(ArrayList<AdministratorBean> abl, RWLock lock, AdministratorBean obs) {
        lock.beginWrite();
        if (abl.contains(obs))
            abl.remove(obs);
        lock.endWrite();
    }

    private static boolean containsObserver(ArrayList<AdministratorBean> abl, AdministratorBean obs) {
        for (AdministratorBean ab: abl) {
            if (ab.getIp().equals(obs.getIp()) && ab.getPort() == obs.getPort())
                return true;
        }
        return false;
    }

    public void notify(PushType type, HouseBean hb) {
        ArrayList<AdministratorBean> copy = new ArrayList<>();
        Message msg = new Message();
        try { msg.setContent(hb); }
        catch (IOException e) { e.printStackTrace(); }
        switch (type) {
            case IN:
                inLock.beginRead();
                msg.setHeader("IN");
                copy.addAll(inObservers);
                inLock.endRead();
                break;
            case OUT:
                outLock.beginRead();
                msg.setHeader("OUT");
                copy.addAll(outObservers);
                outLock.endRead();
                break;
            case BOOST:
                boostLock.beginRead();
                msg.setHeader("BOOST");
                copy.addAll(boostObservers);
                boostLock.endRead();
                break;
            default:
                break;
        }

        for (AdministratorBean obs: copy) {
            try {
                Socket s = new Socket(obs.getIp(), obs.getPort());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                dos.writeUTF(msg.toJSONString());
                dos.flush();
                dos.close();
                s.close();
            } catch (IOException | JSONException e) {e.printStackTrace();}
        }

    }
}
