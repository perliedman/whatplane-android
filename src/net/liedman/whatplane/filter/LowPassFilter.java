package net.liedman.whatplane.filter;

public class LowPassFilter implements Filter {
    private float[] values;
    private int historyLength = 0;
    private int currentIndex = 0;
    private float sum = 0f;
    
    public LowPassFilter(int filterLength) {
        values = new float[filterLength];
    }
    
    public void feed(float value) {
        if (historyLength >= values.length) {
            sum -= values[currentIndex];
        } else {
            historyLength++;
        }
        
        sum += value;       
        values[currentIndex] = value;
        currentIndex = (currentIndex + 1) % values.length;
    }

    public float getValue() {
        if (historyLength > 0) {
            return sum / historyLength;
        } else {
            throw new RuntimeException("No values fed.");
        }
    }

}
