import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static final int
            MIN_FOR_ONE_THREAD = 100_000,
            MAX_FOR_ONE_THREAD = 100_000_000,
            SAMPLE_COUNT = 100,
            THREADS_COUNT = 10;

    public static void main(String[] args) {

        ArrayList<IDataFeeder<Integer>> dataFeeders = new ArrayList<>(THREADS_COUNT);
        for (int i = 0; i < THREADS_COUNT; i++)
            dataFeeders.add(new RandomDataFeeder());

        ParallelReservoir<Integer> parallelReservoir =
                new ParallelReservoir<>(dataFeeders, THREADS_COUNT, SAMPLE_COUNT);

        parallelReservoir.startSampling();

        System.out.println(parallelReservoir.getSampleResult());

        return;
    }
}

class RandomDataFeeder implements IDataFeeder<Integer> {
    private final int TOTAL;
    private int fedDataCount = 0;

    private RandomUtility rand = new RandomUtility();

    RandomDataFeeder() {
        int temp = rand.nextInt();
        while (temp < Main.MIN_FOR_ONE_THREAD) temp <<= 1;
        while (temp > Main.MAX_FOR_ONE_THREAD) temp >>= 1;
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