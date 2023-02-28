import java.util.ArrayList;

public class SimpleReservoir<T> {
    private final int SAMPLE_COUNT; // number of samples
    private int total;// number of total elements in the stream
    private ArrayList<T> sampleResult;

    public SimpleReservoir(int sampleCnt) {
        this.SAMPLE_COUNT = sampleCnt;
        sampleResult = new ArrayList<>(sampleCnt);
    }

    public boolean trySample(T element) {
        if (total < SAMPLE_COUNT) {
            sampleResult.add(element);
            total++;
            return true;
        }

        total++;
        // else total >= K
        // number of elements in stream is now larger than amount of sample
        if (MyUtil.flipCoin(SAMPLE_COUNT / (double) (total))) {
            //keep the new element

            int idx = MyUtil.chooseOneRandomly(SAMPLE_COUNT); // idx is in [0, K)
            sampleResult.set(idx, element);

            return true;
        }
        return false;
    } // boolean trySample()

    ArrayList<T> getSampleResult(){
        ArrayList<T> ret = new ArrayList<>(sampleResult.size());
        return sampleResult;
    }

    public int getTotal() {
        return total;
    }
}
