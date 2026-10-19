package com.meridian.contract.domain;

final class Quantities {

    private Quantities() {
    }

    static double number(String text) {
        return Double.parseDouble(text.replace(',', '.'));
    }

    static int grams(String kilograms) {
        return (int) Math.round(number(kilograms) * 1000);
    }
}
