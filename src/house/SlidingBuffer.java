package house;

import beans.StatBean;

import java.util.ArrayList;

public class SlidingBuffer implements Buffer {

    private int windowSize;
    private int overlap;
    private ArrayList<Measurement> inputList;
    private ArrayList<Measurement> outputList;
    private int initialized;
    private int counter;

    public SlidingBuffer(int window, int overlap) {
        windowSize = window;
        this.overlap = overlap;
        inputList = new ArrayList<>();
        outputList = new ArrayList<>();
        initialized = 0;
        counter = 0;
    }

    //add at the start. When windowSize reached copy in output list and notify
    @Override
    public synchronized void addMeasurement(Measurement m) {
        inputList.add(0, m);
        counter += 1;
        if (counter >= windowSize-(initialized*overlap)) {
            initialized = 1;
            outputList.addAll(inputList);
            inputList.subList(overlap, inputList.size()).clear();
            counter = 0;
            notify();
        }
    }

    public synchronized StatBean getSlidingWindow() throws InterruptedException {
        while (outputList.isEmpty()) {
            wait();
        }

        long maxTimesamp = 0;
        double sum = 0;
        for (Measurement me: outputList) {
            if (me.getTimestamp() > maxTimesamp)
                maxTimesamp = me.getTimestamp();
            sum += me.getValue();
        }

        StatBean result = new StatBean();
        result.setTimestamp(maxTimesamp);
        result.setValue(sum/outputList.size());

        outputList.clear();

        return result;
    }
}
