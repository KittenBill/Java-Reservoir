import java.util.ArrayList;

public class SimpleReservoir<T> {
    private final int SAMPLE_COUNT; // number of samples
    private int total;// number of elements in the stream in all
    private ArrayList<T> samples;

    public RandomUtility rand = new RandomUtility();

    public SimpleReservoir(int sampleCount) {
        this.SAMPLE_COUNT = sampleCount;
        samples = new ArrayList<>(sampleCount);
    }

    public boolean trySample(T element) {
        if (total < SAMPLE_COUNT) {
            samples.add(element);
            total++;
            return true;
        }

        total++;
        // else total >= K
        // number of elements in stream is now larger than amount of sample
        if (rand.flipCoin(SAMPLE_COUNT / (double) (total))) {
            //keep the new element

            int idx = rand.chooseOneRandomly(SAMPLE_COUNT); // idx is in [0, K)
            samples.set(idx, element);

            return true;
        }
        return false;
    } // boolean trySample()

    SampleResult<T> getSampleResult(){
        return new SampleResult<>((ArrayList<T>) samples.clone(), total);
    }

    public int getTotal() {
        return total;
    }
}

class SampleResult<T>{
    public ArrayList<T> samples;
    public int total; // number of elements in the stream in all

    public SampleResult(ArrayList<T> elements, int total){
        this.samples = elements;
        this.total = total;
    }
}
