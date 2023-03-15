import java.util.ArrayList;

public class Benchmark {
    public static final int
            MIN_FOR_ONE_THREAD = 1_000,
            MAX_FOR_ONE_THREAD = 10_000,
            ONE_THREAD = 100_0000,
            SAMPLE_COUNT = 1000,
            THREADS_COUNT = 16,
            SIMPLE_RESERVOIR_THREAD = THREADS_COUNT * ONE_THREAD;

    public static void main(String[] args) {

        testSimpleReservoir();

        System.out.println("\n\n-------------------------\n\n");

        testParallelReservoir();

        return;
    }

    static void testSimpleReservoir() {

        RandomUtility rand = new RandomUtility();

        SimpleReservoir<Integer> simpleReservoir = new SimpleReservoir<>(SAMPLE_COUNT);

        System.out.println("expected total = " + SIMPLE_RESERVOIR_THREAD);

        long startTime = System.currentTimeMillis();
        System.out.println("SEQUENTIAL Sampling started at " + startTime);

        for (int i = 0; i < SIMPLE_RESERVOIR_THREAD; i++)
            simpleReservoir.trySample(rand.nextInt());

        SampleResult<Integer> sampleResult = simpleReservoir.getSampleResult();
        long endTime = System.currentTimeMillis();
        System.out.println("SEQUENTIAL Sampling completed at " + endTime + ", using " + (endTime - startTime) + "ms");

        System.out.println("sample result: \n" + sampleResult.samples + "\n" +
                "\twith actual total = " + sampleResult.total);
    }
    static void testParallelReservoir(){
        int expectedTotal = 0;

        ArrayList<IDataFeeder<Integer>> dataFeeders = new ArrayList<>(THREADS_COUNT);
        for (int i = 0; i < THREADS_COUNT; i++) {
            RandomDataFeeder dataFeeder = new RandomDataFeeder(ONE_THREAD);
            dataFeeders.add(dataFeeder);
            expectedTotal += dataFeeder.TOTAL;
        }

        System.out.println("expected total = " + expectedTotal);

        ParallelReservoir<Integer> parallelReservoir =
                new ParallelReservoir<>(dataFeeders, THREADS_COUNT, SAMPLE_COUNT);

        long startTime = System.currentTimeMillis();
        System.out.println("PARALLEL Sampling started at " + startTime);

        System.out.println("calling startSampling()");

        parallelReservoir.startSampling();

        System.out.println("calling getSampleResult()");

        SampleResult<Integer> sampleResult = parallelReservoir.getSampleResult();
        long endTime = System.currentTimeMillis();
        System.out.println("PARALLEL Sampling completed at " + endTime + ", using " + (endTime - startTime) + "ms");

        System.out.println("sample result: \n" + sampleResult.samples + "\n" +
                "\twith actual total = " + sampleResult.total);

    }
}

class RandomDataFeeder implements IDataFeeder<Integer> {
    protected final int TOTAL;
    private int fedDataCount = 0;

    private RandomUtility rand = new RandomUtility();

    RandomDataFeeder() {
        int temp = rand.nextInt();
        while (temp < Benchmark.MIN_FOR_ONE_THREAD) temp <<= 1;
        while (temp > Benchmark.MAX_FOR_ONE_THREAD) temp >>= 1;
        TOTAL = temp;
    }

    RandomDataFeeder(int total) {
        this.TOTAL = total;
    }

    @Override
    public Integer getData() {
        if (fedDataCount >= TOTAL) return null;
        else {
            fedDataCount++;
            return rand.nextInt();
        }
    }
}