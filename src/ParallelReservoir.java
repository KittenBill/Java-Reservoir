/*
 * 1. main() calls startSampling, starting sequential sampling threads
 * 2. Sampling threads start sampling until dataFeeder returns null
 *      or being stopped by lock.
 * 3. Whenever main() needs a sample result, it calls getSampleResult(),
 *      locking SampleThread when SampleThread is calling SimpleReservoir.getSampleResult().
 * */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;


public class ParallelReservoir<T> {

    private final int SAMPLE_COUNT;
    private ArrayList<SamplerHandle<T>> samplers;
    public ParallelReservoir(int sampleCount) {
        samplers = new ArrayList<>();
        this.SAMPLE_COUNT = sampleCount;
    }

    public SamplerHandle<T> getSamplerHandle(){
        SamplerHandle<T> sampler = new SamplerHandle<>(SAMPLE_COUNT);
        samplers.add(sampler);
        return sampler;
    }

    /**
     * this function should do things as follows:
     * 1. get SampleResult(s) from SampleThreads
     * 2. merge them together in parallel
     *
     * @return the SampleResult summed up from SampleThreads
     */
    public SampleResult<T> getSampleResult() {
        int threadCount = samplers.size();

        ArrayBlockingQueue<SampleResult<T>> queue = new ArrayBlockingQueue<>(threadCount);

        for (SamplerHandle<T> sampler : samplers) {
            SampleResult<T> result = sampler.getSampleResult();
            if (result == null)
                return null;
            queue.add(sampler.getSampleResult());
        }

        Runnable merger = () -> {
            try {
                SampleResult<T> r1 = queue.take(),
                        r2 = queue.take();
                queue.put(merge(r1, r2));
            } catch (InterruptedException e) {
                e.printStackTrace();
                /*
                todo: do something here
                 (although it shouldn't be interrupted)
                 */
                //Thread.currentThread().interrupt();
            }
        };

        /*
         * There are THREADS_COUNT SampleResult(s) at the beginning and
         * fn merge() merges 2 SampleResult(s) into 1 at a time,
         * therefore merge() should be called THREAD_COUNT - 1 times in total.
         * So there are THREAD_COUNT - 1 mergerThreads started in total.
         * */
        ArrayList<Thread> mergerThreads = new ArrayList<>(threadCount - 1);
        for (int i = 0; i < threadCount - 1; i++) {
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
    private SampleResult<T> merge(SampleResult<T> x, SampleResult<T> y) {
        RandomUtility rand = new RandomUtility();

        // possibility of keeping a randomly picked element from SampleResult x
        double possibility = x.total / (double) (x.total + y.total);

        ArrayList<T> ret;

        int from_x = 0, from_y = 0;

        // solved: ERROR: 同一个元素可能被取一次以上
        // todo: need for an algorithm with lower complexity
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

class SamplerHandle<T> extends SimpleReservoir<T> {
    ReentrantLock lock = new ReentrantLock();

    /**
     * there's gonna to be data racing on SimpleReservoir
     * 1. Thread itself calls trySample()
     * 2. ParallelReservoir calls getSampleResult
     * therefore we need a lock locking SimpleReservoir
    */
    SamplerHandle(int sampleCount) {
        super(sampleCount);
    }

    @Override
    public SampleResult<T> getSampleResult() {
        try{
            lock.lock();
            return super.getSampleResult();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public boolean trySample(T element){
        try{
            lock.lock();
            return super.trySample(element);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public int getTotal() {
        try{
            lock.lock();
            return super.getTotal();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void sampleFrom(Iterator<T> it) {
        try{
            lock.lock();
            super.sampleFrom(it);
        }finally {
            lock.unlock();
        }
    }
}
