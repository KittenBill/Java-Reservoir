import java.util.ArrayList;

public class SimpleReservoir<T> {
    int sampleCnt; // number of samples
    long total;// number of total elements in the stream
    ArrayList<T> reservoir;



    public <T> SimpleReservoir(int sampleCnt) {
        this.sampleCnt = sampleCnt;
        reservoir = new ArrayList<>(sampleCnt);
    }

    public boolean trySample(T element) {
        if (total < sampleCnt) {
            reservoir.add(element);
            total++;
            return true;
        }

        total++;
        // else total >= K
        // number of elements in stream is now larger than amount of sample
        if (MyUtil.flipCoin(sampleCnt / (double) (total))) {
            //keep the new element

            int idx = MyUtil.chooseOneRandomly(sampleCnt); // idx is in [0, K)
            reservoir.set(idx, element);

            return true;
        }
        return false;
    } // boolean trySample()

    ArrayList<T> getReservoir(){
        return reservoir;
    }

}
