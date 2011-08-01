package net.liedman.whatplane.filter;

import net.liedman.whatplane.filter.LowPassFilter;
import junit.framework.TestCase;

public class LowPassFilterTest extends TestCase {

    public void testFeed() {
        LowPassFilter lpf = new LowPassFilter(10);
        for (int i = 0; i < 100; i++) {
            lpf.feed(i);
        }
    }

    public void testGetValue() {
        LowPassFilter lpf = new LowPassFilter(3);
        
        try {
            lpf.getValue();
            fail("Was able to get value without feeding data.");
        } catch (Exception e) {
            // Ok
        }
        
        lpf.feed(1f);
        assertEquals(1f, lpf.getValue());
        
        lpf.feed(2);
        assertEquals(1.5f, lpf.getValue(), 1e-5);

        lpf.feed(2);
        assertEquals(5f/3f, lpf.getValue(), 1e-5);

        lpf.feed(2);
        assertEquals(2f, lpf.getValue(), 1e-5);
    }

}
