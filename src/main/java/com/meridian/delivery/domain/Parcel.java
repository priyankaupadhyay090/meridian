package com.meridian.delivery.domain;

import java.util.stream.IntStream;

public record Parcel(int weightGrams, int lengthCm, int widthCm, int heightCm) {

    public int longestSide() {
        return IntStream.of(lengthCm, widthCm, heightCm).max().orElse(0);
    }

    public int shortestSide() {
        return IntStream.of(lengthCm, widthCm, heightCm).min().orElse(0);
    }

    public int longestPlusShortest() {
        return longestSide() + shortestSide();
    }

    public int[] sortedSides() {
        return IntStream.of(lengthCm, widthCm, heightCm).sorted().toArray();
    }
}
