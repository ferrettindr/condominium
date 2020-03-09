package utility;

public class RWLock {

    //lock for concurrent reads and exclusive writes
    private boolean writeLocked;
    private int readingThreads;
    private Thread lockedBy;

    //unlocked on creation
    public RWLock() {
        writeLocked = false;
        readingThreads = 0;
        lockedBy = null;
    }

    public synchronized void beginRead() {
        readingThreads += 1;
        if (readingThreads == 1) {
            getWriteLock();
        }
    }

    public synchronized void endRead() {
        readingThreads -= 1;
        if (readingThreads == 0) {
            writeLocked = false;
            lockedBy = null;
            notifyAll();
        }
    }

    public synchronized void beginWrite() {
        getWriteLock();
    }

    private void getWriteLock() {
        while (writeLocked && lockedBy != Thread.currentThread()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        writeLocked = true;
        lockedBy = Thread.currentThread();
    }

    public synchronized void endWrite() {
        writeLocked = false;
        lockedBy = null;
        notifyAll();
    }



}
