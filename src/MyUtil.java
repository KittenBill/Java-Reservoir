import java.util.Random;

public class MyUtil {
    static Random random = new Random();

    static boolean flipCoin(double winningRate) {
        return random.nextDouble() < winningRate;
    }

    static int chooseOneRandomly(int length){
        return (int)(random.nextDouble() * length);
        //return value is an integer in [0, length)
    }
}
