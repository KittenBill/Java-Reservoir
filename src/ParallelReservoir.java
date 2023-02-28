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


    // number of sampling threads
    // which should also be the capacity of blocking clarified queue below
    private final int THREADS_COUNT;
    private final int SAMPLE_COUNT;
    private ArrayList<SampleThread<T>> sampleThreads;

    public ParallelReservoir(ArrayList<IDataFeeder<T>> dataFeeders, int threadsCount, int sampleCount) {
        this.THREADS_COUNT = threadsCount;
        this.SAMPLE_COUNT = sampleCount;
        this.dataFeeders = dataFeeders;
    }

    /**
     * init the SampleThread(s) and call start()
     */
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
     * 1. get SampleResult(s) from SampleThreads
     * 2. merge them together in parallel
     *
     * @return the SampleResult summed up from SampleThreads
     */
    public SampleResult<T> getSampleResult() {
        ArrayBlockingQueue<SampleResult<T>> queue = new ArrayBlockingQueue<>(THREADS_COUNT);

        for (var sampleThread :
                sampleThreads) {
            queue.add(sampleThread.getSampleResult());
        }

        Runnable merger = () -> {
            try {
                SampleResult<T> r1 = queue.take(),
                        r2 = queue.take();
                queue.put(merge(r1, r2));
            } catch (InterruptedException e) {
                /*
                todo: do something here (although it shouldn't be interrupted)
                 */
                //Thread.currentThread().interrupt();
            }
        };

        /*
         * There are THREADS_COUNT SampleResult(s) at the beginning and
         * fn merge() merges 2 SampleResult(s) into 1 at a time,
         * therefore merge() should be called THREAD_COUNT - 1 times in total.
         * */
        ArrayList<Thread> mergerThreads = new ArrayList<>(THREADS_COUNT - 1);
        for (int i = 0; i < THREADS_COUNT - 1; i++){
            Thread thread = new Thread(merger);
            thread.start();
            mergerThreads.add(thread);
        }

        for (Thread thread: mergerThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // todo: needs something to do
                // actually nothing to do here
            }
        }

        try {
            assert(queue.size() == 1);
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }// SampleResult<T> getSampleResult()

    /**
     * merges 2 SampleResult(s) into 1
     *
     * @param x one SampleResult
     * @param y another SampleResult
     * @return merge result
     */
    public SampleResult<T> merge(SampleResult<T> x, SampleResult<T> y) {
        RandomUtility rand = new RandomUtility();

        // possibility of keeping a randomly picked element from SampleResult x
        double possibility = x.total / (double) (x.total + y.total);

        ArrayList<T> ret = new ArrayList<>(SAMPLE_COUNT);

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            ret.add(
                    (rand.flipCoin(possibility) ? x : y).samples
                            .get(rand.chooseOneRandomly(SAMPLE_COUNT))
            );
        }
        return new SampleResult<>(ret, x.total + y.total);
    }
}

class SampleThread<T> extends SimpleReservoir<T> implements Runnable {

    private IDataFeeder<T> dataFeeder;

    /**
     * there's gonna to be data racing on SimpleReservoir
     * 1. Thread itself calls trySample()
     * 2. ParallelReservoir calls getSampleResult
     * therefore we need a lock locking SimpleReservoir
     */
    private ReentrantLock lock = new ReentrantLock();

    SampleThread(int sampleCount, IDataFeeder<T> dataFeeder) {
        super(sampleCount);
        this.dataFeeder = dataFeeder;
    }

    /**
     * @todo: solve problems below
     * 1. don't know whether it's concurrent-safe
     * 2. shallow copy
     */
    @Override
    SampleResult getSampleResult() {

        lock.lock();
        var ret = super.getSampleResult();
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
                    // todo: needs notifying or interrupting from dataFeeder
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
