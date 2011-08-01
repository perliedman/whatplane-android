package net.liedman.whatplane.filter;

public interface Filter {
    void feed(float value);
    float getValue();
}
