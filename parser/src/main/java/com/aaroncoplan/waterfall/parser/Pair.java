package com.aaroncoplan.waterfall.parser;

public class Pair<K, V> {

    public final K firstVal;
    public final V secondVal;

    public Pair(K firstVal, V secondVal) {
        this.firstVal = firstVal;
        this.secondVal = secondVal;
    }

    @Override
    public String toString() {
        return "(" + firstVal + ", " + secondVal + ")";
    }
}
