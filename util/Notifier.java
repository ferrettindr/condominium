package util;

import beans.HouseBean;
import util.RWLock;
import beans.AdministratorBean;

import java.util.ArrayList;
import java.util.List;

public class Notifier {

    public enum PushType{
        IN,
        OUT,
        SPIKE;
    }

    RWLock rwLock;
    List<AdministratorBean> observers;
    PushType type;

    public Notifier(PushType type) {
        rwLock = new RWLock();
        observers = new ArrayList<>();
        this.type = type;
    }

    public void addObserver(AdministratorBean obs) {
        rwLock.beginWrite();

        if (!observers.contains(obs))
            observers.add(obs);

        rwLock.endWrite();
    }

    public void removeObserver(AdministratorBean obs) {
        rwLock.beginWrite();

        if (observers.contains(obs))
            observers.remove(obs);

        rwLock.endWrite();
    }

    public void notify(HouseBean h) {
        rwLock.beginRead();

        //TODO send msg to observers
        //for (AdministratorBean obs: observers) {

        //}

        rwLock.endRead();
    }
}
