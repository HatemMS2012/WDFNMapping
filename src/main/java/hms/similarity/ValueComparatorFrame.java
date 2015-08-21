package hms.similarity;

import hms.alignment.data.Frame;

import java.util.Comparator;
import java.util.Map;

public class ValueComparatorFrame implements Comparator<Frame> {

    Map<Frame, Double> base;
    public ValueComparatorFrame(Map<Frame, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(Frame a, Frame b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}