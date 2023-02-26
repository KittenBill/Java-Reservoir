import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static void main(String[] args) {

        int n, k;
        n = 1000;
        k = 10;

        SimpleReservoir reservoir = new SimpleReservoir(10);

        for (int i = 1; i <= n; i++) {
            if (reservoir.trySample(i))
                System.out.println("take " + i);
            else System.out.println("drop " + i);
        }

        ArrayList list = reservoir.getReservoir();


        for (Object i : list) {
            System.out.println(i);
        }
        return;
    }
}