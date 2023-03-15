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


public class ParallelReservoir<T> {

    private ArrayList<IDataFeeder<T>> dataFeeders;


    // number of sampling threads
    // which should also be the capacity of blocking clarified queue below
    private final int THREADS_COUNT;
    private final int SAMPLE_COUNT;
    private ArrayList<SampleThread<T>> sampleThreads;
    ArrayBlockingQueue<SampleResult<T>> queue;

    public ParallelReservoir(ArrayList<IDataFeeder<T>> dataFeeders, int threadsCount, int sampleCount) {
        this.THREADS_COUNT = threadsCount;
        this.SAMPLE_COUNT = sampleCount;
        this.dataFeeders = dataFeeders;
        queue = new ArrayBlockingQueue<>(THREADS_COUNT);
    }

    /**
     * init the SampleThread(s) and call start()
     * todo: needs a flag to notify that every thread has done its work
     */
    public void startSampling() {
        sampleThreads = new ArrayList<>(THREADS_COUNT);
        for (int i = 0; i < THREADS_COUNT; i++) {
            SampleThread<T> sampleThread = new SampleThread<>(SAMPLE_COUNT, dataFeeders.get(i), this);
            sampleThreads.add(sampleThread);
            sampleThread.setName("sample thread no." + i);
            sampleThread.start();
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

    protected void pushResult(SampleResult<T> sampleResult) {
        queue.add(sampleResult);
    }

    public SampleResult<T> getSampleResult() {
        Runnable merger = () -> {
            //System.out.println(Thread.currentThread().getName() + " start");
            //long startTime = System.currentTimeMillis();
            try {
                SampleResult<T> r1 = queue.take(),
                        r2 = queue.take();
                //System.out.println(Thread.currentThread().getName() + " start merging");
                queue.put(merge(r1, r2));
            } catch (InterruptedException e) {
                ;
                /*
                todo: do something here
                 (although it shouldn't be interrupted)
                 */
                //Thread.currentThread().interrupt();
            }
            //long endTime = System.currentTimeMillis();
            //System.out.println(Thread.currentThread().getName() + " completed merge, using " + (endTime - startTime)+ "ms");
        };

        /*
         * There are THREADS_COUNT SampleResult(s) at the beginning and
         * fn merge() merges 2 SampleResult(s) into 1 at a time,
         * therefore merge() should be called THREAD_COUNT - 1 times in total.
         * So there are THREAD_COUNT - 1 mergerThreads started in total.
         * */
        ArrayList<Thread> mergerThreads = new ArrayList<>(THREADS_COUNT - 1);
        for (int i = 0; i < THREADS_COUNT - 1; i++) {
            Thread thread = new Thread(merger);
            thread.setName("merger thread no." + i);
            thread.start();
            mergerThreads.add(thread);
        }

        // wait until every mergerThread has done its job
        for (Thread thread : mergerThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                /* todo: needs something to do
                    actually nothing to do here
                 */
            }
        }
        try {
            // there should be only one SampleResult left in the queue
            assert (queue.size() == 1);
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

        int from_x = 0, from_y = 0;

        // solved: ERROR: 同一个元素可能被取一次以上
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            if (rand.flipCoin(possibility)) from_x++;
            else from_y++;
        }

        SimpleReservoir<T> sx = new SimpleReservoir<>(from_x), sy = new SimpleReservoir<>(from_y);
        sx.sampleFrom(x.samples.iterator());
        sy.sampleFrom(y.samples.iterator());

        ret = sx.getSampleResult().samples;
        ret.addAll(sy.getSampleResult().samples);

        return new SampleResult<>(ret, x.total + y.total);
    }
}

class SampleThread<T> extends Thread {
    SimpleReservoir<T> simpleReservoir;
    private IDataFeeder<T> dataFeeder;

    ParallelReservoir<T> parallelReservoir;


    /**
     * there's gonna to be data racing on SimpleReservoir
     * 1. Thread itself calls trySample()
     * 2. ParallelReservoir calls getSampleResult
     * therefore we need a lock locking SimpleReservoir
     * <p>
     * now assuming no real-time sampling
     */
    // private ReentrantLock lock = new ReentrantLock();

    SampleThread(int sampleCount, IDataFeeder<T> dataFeeder, ParallelReservoir<T> parallelReservoir) {
        simpleReservoir = new SimpleReservoir<>(sampleCount);
        this.dataFeeder = dataFeeder;
        this.parallelReservoir = parallelReservoir;
    }

    /**
     * todo: solve problems below
     *  1. don't know whether it's concurrent-safe
     *  2. shallow copy
     */
    SampleResult<T> getSampleResult() {
        return simpleReservoir.getSampleResult();
    }

    /**
     * expected behavior of run():
     * 1. when meeting null from getData() -> stop until further notice
     * 2. no data racing with getSampleResult()
     */
    @Override
    public void run() {
        //long startTime = System.currentTimeMillis();
        //System.out.println(Thread.currentThread().getName() + " start");
        T data;
        while (true) {
            // todo: needs an exit
            data = dataFeeder.getData();
            if (data == null) {
                break;
            } else {
                simpleReservoir.trySample(data);
                //System.out.println(Thread.currentThread().getName() + " takes " + data + " at time " + System.currentTimeMillis());
            }
        }
        SampleResult<T> sampleResult = getSampleResult();
        //long endTime = System.currentTimeMillis();
        //System.out.println(Thread.currentThread().getName() + " got SampleResult, with total = " + sampleResult.total + ", using " + (endTime - startTime)+ "ms");
        parallelReservoir.pushResult(sampleResult);

    } // void run()
}
