import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Benchmark {
    public static final int
            ONE_THREAD = 100_0000,
            SAMPLE_COUNT = 1000,
            THREADS_COUNT = 8,
            SIMPLE_RESERVOIR_THREAD = THREADS_COUNT * ONE_THREAD,
            TEST_COUNT = 100;

    public static void main(String[] args) {

        bench("SEQUENTIAL RESERVOIR", testSimpleReservoir);

        System.out.println("------------PARTITION-------------");

        bench("PARALLEL RESERVOIR", testParallelReservoir);
    }

    static void bench(String name, Testable toTest){
        System.out.println("testing " + name + ", running it for " + TEST_COUNT + " times");
        long totalTime = 0, maxTime = Long.MIN_VALUE, minTime = Long.MAX_VALUE;
        for (int i = 0; i < TEST_COUNT; i++){
            long time = toTest.test();
            maxTime = Long.max(maxTime, time);
            minTime = Long.min(minTime, time);
            totalTime += time;
        }

        System.out.println("Average time to run " + name + " is " + totalTime / (double)(TEST_COUNT)+"ms");
        System.out.println("\t with max elapsed time = " + maxTime + "ms; min elapsed time = " + minTime+"ms");
    }



    static Testable testSimpleReservoir = ()-> {

        //RandomUtility rand = new RandomUtility();

        SimpleReservoir<Integer> simpleReservoir = new SimpleReservoir<>(SAMPLE_COUNT);

        // System.out.println("expected total = " + SIMPLE_RESERVOIR_THREAD);

        long startTime = System.nanoTime();
        // System.out.println("SEQUENTIAL Sampling started");

        for (int i = 0; i < SIMPLE_RESERVOIR_THREAD; i++)
            simpleReservoir.trySample(i);

        SampleResult<Integer> sampleResult = simpleReservoir.getSampleResult();
        long endTime = System.nanoTime();
        long timeElapsed = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        /*
        System.out.println("SEQUENTIAL Sampling completed, using " + timeElapsed + "ms");

        System.out.println("sample result: \n" + sampleResult.samples + "\n" +
                "\twith actual total = " + sampleResult.total);
         */

        return timeElapsed;
    };

    static Testable testParallelReservoir = ()->{
        // System.out.println("expected total = " + ONE_THREAD * THREADS_COUNT);

        ParallelReservoir<Integer> parallelReservoir =
                new ParallelReservoir<>(SAMPLE_COUNT);

        long startTime = System.nanoTime();
        // System.out.println("PARALLEL Sampling started");

        ArrayList<Thread> handles = new ArrayList<>(THREADS_COUNT);

        for (int i = 0; i < THREADS_COUNT; i++) {
            int start = i * ONE_THREAD;
            var sampler = parallelReservoir.getSamplerHandle();
            Runnable sampleThread = () -> {
                for (int j = start; j < start + ONE_THREAD; j++)
                    sampler.trySample(j);
            };
            Thread thread = new Thread(sampleThread);
            thread.setName("sampler thread no." + i);
            handles.add(thread);
            thread.start();
        }

        for (Thread handle : handles) {
            try {
                handle.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        SampleResult<Integer> sampleResult = parallelReservoir.getSampleResult();
        long endTime = System.nanoTime();
        long timeElapsed = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        /*
        System.out.println("PARALLEL Sampling completed, using " + timeElapsed + "ms");

        System.out.println("sample result: \n" + sampleResult.samples + "\n" +
                "\twith actual total = " + sampleResult.total);

         */
        return timeElapsed;
    };
}

interface Testable{
    long test();
}
