package com.meridian.routing.domain;

public record WeightBand(int minGrams, int maxGrams) {

    public boolean contains(int grams) {
        return grams >= minGrams && grams < maxGrams;
    }

    public boolean overlaps(WeightBand other) {
        return minGrams < other.maxGrams && other.minGrams < maxGrams;
    }

    public boolean isValid() {
        return minGrams >= 0 && minGrams < maxGrams;
    }

    @Override
    public String toString() {
        return "%d-%d g".formatted(minGrams, maxGrams);
    }
}
