import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Benchmark {
    public static final int
            ONE_THREAD = 1_0000_0000,
            SAMPLE_COUNT = 1000000,
            THREADS_COUNT = 4,
            SIMPLE_RESERVOIR_THREAD = THREADS_COUNT * ONE_THREAD;

    public static void main(String[] args) {

        //testSimpleReservoir();

        System.out.println("------------PARTITION-------------");

        testParallelReservoir();
    }

    static void testSimpleReservoir() {

        //RandomUtility rand = new RandomUtility();

        SimpleReservoir<Integer> simpleReservoir = new SimpleReservoir<>(SAMPLE_COUNT);

        System.out.println("expected total = " + SIMPLE_RESERVOIR_THREAD);

        long startTime = System.nanoTime();
        System.out.println("SEQUENTIAL Sampling started");

        for (int i = 0; i < SIMPLE_RESERVOIR_THREAD; i++)
            simpleReservoir.trySample(i);

        SampleResult<Integer> sampleResult = simpleReservoir.getSampleResult();
        long endTime = System.nanoTime();
        System.out.println("SEQUENTIAL Sampling completed, using " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + "ms");

        System.out.println("sample result: \n" + sampleResult.samples + "\n" +
                "\twith actual total = " + sampleResult.total);
    }

    static void testParallelReservoir() {
        System.out.println("expected total = " + ONE_THREAD * THREADS_COUNT);

        ParallelReservoir<Integer> parallelReservoir =
                new ParallelReservoir<>(SAMPLE_COUNT);

        long startTime = System.nanoTime();
        System.out.println("PARALLEL Sampling started");

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
        System.out.println("PARALLEL Sampling completed, using " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + "ms");

        System.out.println("sample result: \n" + sampleResult.samples + "\n" +
                "\twith actual total = " + sampleResult.total);

    }
}
