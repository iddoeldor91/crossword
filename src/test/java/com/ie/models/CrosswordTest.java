package com.ie.models;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;


public class CrosswordTest {
    @Test
    public void computeCrossword() throws Exception {
        benchmarkMathRandomVsThreadLocalRandom();
    }

    private static void benchmarkMathRandomVsThreadLocalRandom() {
        long mathRandomStart = System.currentTimeMillis();
        int loops = 1_000_000;
        for (int i = 0; i < loops; i++) {
            double random = Math.random();
        }
        long mathRandomEnd = System.currentTimeMillis() - mathRandomStart;

        long threadLocalRandomStart = System.currentTimeMillis();
        for (int i = 0; i < loops; i++) {
            double random = ThreadLocalRandom.current().nextDouble();
        }
        long threadLocalRandomEnd = System.currentTimeMillis() - threadLocalRandomStart;

        assertTrue(mathRandomEnd > threadLocalRandomEnd);
    }
}