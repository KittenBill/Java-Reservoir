/*
 * 1. main() calls startSampling, starting sequential sampling threads
 * 2. Sampling threads start sampling until dataFeeder returns null
 *      or being stopped by lock.
 * 3. Whenever main() needs a sample result, it calls getSampleResult(),
 *      locking SampleThread when SampleThread is calling SimpleReservoir.getSampleResult().
 * */

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

public class ParallelReservoir<T> {

    private ArrayList<IDataFeeder<T>> dataFeeders;

    private ArrayBlockingQueue<ArrayList<T>> queue;

    // number of sampling threads
    // which should also be the capacity of blocking clarified queue below
    private final int THREADS_COUNT;
    private final int SAMPLE_COUNT;

    public ParallelReservoir(ArrayList<IDataFeeder<T>> dataSources, int threadsCount, int sampleCount) {
        this.THREADS_COUNT = threadsCount;
        this.SAMPLE_COUNT = sampleCount;
        this.dataFeeders = dataSources;
        queue = new ArrayBlockingQueue<>(THREADS_COUNT);
    }

    private ArrayList<SampleThread<T>> sampleThreads;

    public void startSampling() {
        sampleThreads = new ArrayList<>(THREADS_COUNT);
        for (int i = 0; i < THREADS_COUNT; i++) {
            SampleThread<T> sampleThread = new SampleThread<>(SAMPLE_COUNT, dataFeeders.get(i));
            sampleThreads.add(sampleThread);
            new Thread(sampleThread).start();
        }

        return;
    }

    /**
     * this function should do things as follows:
     * 1. get sample result from SampleThreads
     * 2. merge them together
     * 3. return the result
     */
    public ArrayList<T> getSampleResult() {
        return null;
    }
}

class SampleThread<T> extends SimpleReservoir<T> implements Runnable {

    private IDataFeeder<T> dataFeeder;
    private ReentrantLock lock = new ReentrantLock(); // locks SimpleReservoir

    SampleThread(int sampleCount, IDataFeeder<T> dataFeeder) {
        super(sampleCount);
        this.dataFeeder = dataFeeder;
    }

    @Override
    ArrayList<T> getSampleResult() {
        // @todo: solve problems below
        // 1. don't know whether it's concurrent-safe
        // 2. shallow copy
        lock.lock();
        var ret = (ArrayList<T>)super.getSampleResult().clone();
        lock.unlock();
        return ret;
    }

    /**
     * expected behavior of run():
     * 1. when meeting null from getData() -> stop until further notice
     * 2. no data racing with getSampleResult()
     */
    @Override
    public void run() {
        T data;
        while (true) {
            // todo: needs an exit
            data = dataFeeder.getData();
            if (data == null) {
                try {
                    // todo: needs notifying or interrupting
                    wait();
                } catch (InterruptedException e) {
                    continue;
                }
            } else {
                lock.lock();
                trySample(data);
            }
            lock.unlock();
        }
    } // void run()
}
