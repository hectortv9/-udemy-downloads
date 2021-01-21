package com.hectortv9.tools;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Random;
import java.util.HashMap;
import java.util.TreeMap;

public final class Derangements {

    // cache calculated values to speed up recursive algorithm
    private static HashMap<Integer, BigInteger> numberOfDerangementsMap = new HashMap<Integer, BigInteger>();
    private static int greatestNCached = -1;

    // load numberOfDerangementsMap with initial values D(0)=1 and D(1)=0
    static {
        numberOfDerangementsMap.put(0, BigInteger.valueOf(1));
        numberOfDerangementsMap.put(1, BigInteger.valueOf(0));
        greatestNCached = 1;
    }

    private static Random rand = new Random();

    // private default constructor so class isn't accidentally instantiated
    private Derangements() {
    }

    public static BigInteger numberOfDerangements(int n) throws IllegalArgumentException {
        if (numberOfDerangementsMap.containsKey(n)) {
            return numberOfDerangementsMap.get(n);
        } else if (n >= 2) {
            // pre-load the cache to avoid stack overflow (occurs near n=5000)
            for (int i = greatestNCached + 1; i < n; i++) {
                numberOfDerangements(i);
            }
            greatestNCached = n - 1;
            // recursion for derangements: D(n) = (n-1)*(D(n-1) + D(n-2))
            BigInteger Dn_1 = numberOfDerangements(n - 1);
            BigInteger Dn_2 = numberOfDerangements(n - 2);
            BigInteger Dn = (Dn_1.add(Dn_2)).multiply(BigInteger.valueOf(n - 1));
            numberOfDerangementsMap.put(n, Dn);
            greatestNCached = n;
            return Dn;
        } else {
            throw new IllegalArgumentException("argument must be >= 0 but was " + n);
        }
    }

    public static int[] randomDerangement(int n) throws IllegalArgumentException {

        if (n < 2)
            throw new IllegalArgumentException("argument must be >= 2 but was " + n);

        int[] result = new int[n];
        boolean[] mark = new boolean[n];

        for (int i = 0; i < n; i++) {
            result[i] = i;
            mark[i] = false;
        }
        int unmarked = n;

        for (int lastUnswappedIndex = n - 1; lastUnswappedIndex >= 0; lastUnswappedIndex--) {
            if (unmarked < 2)
                break; // can't move anything else
            if (mark[lastUnswappedIndex])
                continue; // can't move item at 'lastUnswappedIndex' if marked

            // use the rejection method to generate
            // random unmarked index randomSwapIndex < lastUnswappedIndex;
            // this could be replaced by more straightforward technique
            int randomSwapIndex;
            do {
                randomSwapIndex = rand.nextInt(lastUnswappedIndex);
            } while (mark[randomSwapIndex]);

            // swap two elements of the array
            int temp = result[lastUnswappedIndex];
            result[lastUnswappedIndex] = result[randomSwapIndex];
            result[randomSwapIndex] = temp;

            // mark position j as end of cycle with probability (u-1)D(u-2)/D(u)
            double probability = (new BigDecimal(numberOfDerangements(unmarked - 2)))
                    .multiply(new BigDecimal(unmarked - 1))
                    .divide(new BigDecimal(numberOfDerangements(unmarked)), MathContext.DECIMAL64).doubleValue();
            double randProbability = rand.nextDouble();
            if (randProbability < probability) {
                mark[randomSwapIndex] = true;
                unmarked--;
            }

            // position i now becomes out of play so we could mark it
            // mark[i] = true;
            // but we don't need to because loop won't touch it from now on
            // however we do have to decrement unmarked
            unmarked--;
        }

        return result;
    }

    // unit tests
    public static void main(String[] args) {
        // test derangement numbers D(i)
        for (int i = 0; i < 100; i++) {
            System.out.println("D(" + i + ") = " + numberOfDerangements(i));
        }
        System.out.println();

        // test quantity (u-1)D_(u-2)/D_u for overflow, inaccuracy
        for (int u = 2; u < 100; u++) {
            double d = numberOfDerangements(u - 2).doubleValue() * (u - 1) / numberOfDerangements(u).doubleValue();
            System.out.println((u - 1) + " * D(" + (u - 2) + ") / D(" + u + ") = " + d);
        }

        System.out.println();

        // test derangements for correctness, uniform distribution
        int size = 5;
        long reps = 10000000;
        TreeMap<String, Integer> countMap = new TreeMap<String, Integer>();
        System.out.println("Derangement\tCount");
        System.out.println("-----------\t-----");
        for (long rep = 0; rep < reps; rep++) {
            int[] d = randomDerangement(size);
            String s = "";
            String sep = "";
            if (size > 10)
                sep = " ";
            for (int i = 0; i < d.length; i++) {
                s += d[i] + sep;
            }

            if (countMap.containsKey(s)) {
                countMap.put(s, countMap.get(s) + 1);
            } else {
                countMap.put(s, 1);
            }
        }

        for (String key : countMap.keySet()) {
            System.out.println(key + "\t\t" + countMap.get(key));
        }

        System.out.println();

        // large random derangement
        int size1 = 1000;
        System.out.println("Random derangement of " + size1 + " elements:");
        int[] d1 = randomDerangement(size1);
        for (int i = 0; i < d1.length; i++) {
            System.out.print(d1[i] + " ");
        }

        System.out.println();
        System.out.println();

        System.out.println("We start to run into memory issues around u=40000:");
        {
            // increase this number from 40000 to around 50000 to trigger
            // out of memory-type exceptions
            int u = 40003;
            BigDecimal d = (new BigDecimal(numberOfDerangements(u - 2))).multiply(new BigDecimal(u - 1))
                    .divide(new BigDecimal(numberOfDerangements(u)), MathContext.DECIMAL64);
            System.out.println((u - 1) + " * D(" + (u - 2) + ") / D(" + u + ") = " + d);
        }

    }

}
