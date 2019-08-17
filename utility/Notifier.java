package utility;

import beans.HouseBean;
import beans.AdministratorBean;
import org.codehaus.jettison.json.JSONException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Notifier {

    public enum PushType {
        IN,
        OUT,
        BOOST;
    }

    private RWLock inLock;
    private List<AdministratorBean> inObservers;
    private RWLock outLock;
    private List<AdministratorBean> outObservers;
    private RWLock boostLock;
    private List<AdministratorBean> boostObservers;

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
                inLock.beginWrite();
                if (!inObservers.contains(obs))
                    inObservers.add(obs);
                inLock.endWrite();
                break;
            case OUT:
                outLock.beginWrite();
                if (!outObservers.contains(obs))
                    outObservers.add(obs);
                outLock.endWrite();
                break;
            case BOOST:
                boostLock.beginWrite();
                if (!boostObservers.contains(obs))
                    boostObservers.add(obs);
                boostLock.endWrite();
                break;
            default:
                break;
        }
    }

    public void removeObserver(PushType type, AdministratorBean obs) {
        switch (type) {
            case IN:
                inLock.beginWrite();
                if (!inObservers.contains(obs))
                    inObservers.remove(obs);
                inLock.endWrite();
                break;
            case OUT:
                outLock.beginWrite();
                if (!outObservers.contains(obs))
                    outObservers.remove(obs);
                outLock.endWrite();
                break;
            case BOOST:
                boostLock.beginWrite();
                if (!boostObservers.contains(obs))
                    boostObservers.remove(obs);
                boostLock.endWrite();
                break;
            default:
                break;
        }
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
