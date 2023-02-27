import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static void main(String[] args) {

        int n, k;
        n = 10000000;
        k = 100;

        SimpleReservoir<Integer> reservoir = new SimpleReservoir<>(10);

        for (int i = 1; i <= n; i++) {
            if (reservoir.trySample(i))
                System.out.println("take " + i);
            else System.out.println("drop " + i);
        }

        ArrayList<Integer> list = reservoir.getReservoir();


        for (Object i : list) {
            System.out.println(i);
        }
        return;
    }
}