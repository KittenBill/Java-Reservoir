import java.util.Random;

public class RandomUtility extends Random {
    boolean flipCoin(double winningRate) {
        return nextDouble() < winningRate;
    }

    int pickOne(int length){
        return (int)(nextDouble() * length);
        //returned value is an integer in [0, length)
    }
}
