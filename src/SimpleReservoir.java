import java.util.ArrayList;
import java.util.Random;

public class SimpleReservoir<T> {
    int K; // amount of sample
    long total;// total elements in the stream
    ArrayList<T> reservoir;
    static Random random = new Random();

    boolean flipCoin(double winningRate) {
        return random.nextDouble() < winningRate;
    }

    public <T> SimpleReservoir(int amountOfSample) {
        K = amountOfSample;
        reservoir = new ArrayList<>(amountOfSample);
    }

    public boolean trySample(T element) {
        if (total < K) {
            reservoir.add(element);
            total++;
            return true;
        }

        total++;
        // else total >= K
        // number of elements in stream is now larger than amount of sample
        if (flipCoin(K / (double) (total))) {
            //keep the new element

            int idx = (int)(random.nextDouble() * K);
            // idx is in [0, K)

            reservoir.set(idx, element);

            return true;
        }
        return false;
    } // boolean trySample()

    ArrayList<T> getReservoir(){
        return reservoir;
    }

}
