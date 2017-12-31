/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package tests;

import concurrent_multiset.ConcurrentMultiset;
import concurrent_multiset.FineGrainMultiset;
import concurrent_multiset.LazyMultiset;
import concurrent_multiset.LockFreeMultiset;
import exceptions.FullMultisetException;
import exceptions.InvalidCountException;
import exceptions.NullValueException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>The class that defines a sequential test for the three implementations
 * of the multiset.</p>
 *
 * @author Alessandra Fais
 */
public class TestSequential {

    /**
     * <p>Tests the operations of {@link ConcurrentMultiset#add(Object, int)}, {@link ConcurrentMultiset#remove(Object, int)},
     * {@link ConcurrentMultiset#count(Object)}, {@link ConcurrentMultiset#contains(Object)} and
     * {@link ConcurrentMultiset#removeAll(Collection)} on the multiset. Some prints are used in the code to see the results of
     * the operations on the multiset.</p>
     * @param args
     */
    public static void main(String[] args) {

        ConcurrentMultiset<Integer> testMultiset = null;
        int capacity = 10;

        System.out.println("+--------------------------------------------------------------------------------------------------+");
        System.out.println("|                                         SEQUENTIAL TEST                                          |");
        System.out.println("+--------------------------------------------------------------------------------------------------+");
        System.out.println("  Version\t\tExceptions raised (correct if each version raises the three types of exception defined)");

        /* Sequential test */
        for (int j = 0; j < 3; j++) {

            switch (j) {
                case 0:
                    System.out.print("FINE GRAINED\t");
                    testMultiset = new FineGrainMultiset<>(capacity);
                    break;
                case 1:
                    System.out.print("LAZY\t\t\t");
                    testMultiset = new LazyMultiset<>(capacity);
                    break;
                case 2:
                    System.out.print("LOCK FREE\t\t");
                    testMultiset = new LockFreeMultiset<>(capacity);
                    break;
            }

            try {
                testMultiset.add(null);
            } catch (NullValueException e) {
                System.out.println("NullValueException: element can't be a null value");
            }

            try {
                testMultiset.add(1, -1);
            } catch (InvalidCountException e) {
                System.out.println("\t\t\t\tInvalidCountException: element cannot have a negative number of occurrences");
            }

            try {
                for (int i = 0; i < capacity; i++)
                    testMultiset.add(i, 4);
            } catch (FullMultisetException e) {
                System.out.println("\t\t\t\tFullMultisetException: you reached the capacity of the multiset.");
            }

            try {
                assert(testMultiset.getActualSize() == 8) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == 2) : testMultiset.getSize();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            testMultiset.remove(1);

            try {
                assert(testMultiset.getActualSize() == 7) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == 2) : testMultiset.getSize();
                assert(testMultiset.contains(1)) : testMultiset.toString();
                assert(testMultiset.count(1) == 3) : testMultiset.toString();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            ArrayList<Integer> my_collection = new ArrayList<>();
            for (int i = 0; i < 3; i++)
                my_collection.add(i);

            testMultiset.removeAll(my_collection);

            try {
                assert(testMultiset.getActualSize() == 5) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == 2) : testMultiset.getSize();
                assert(testMultiset.contains(1)) : testMultiset.toString();
                assert(testMultiset.count(0) == 3) : testMultiset.toString();
                assert(testMultiset.count(1) == 2) : testMultiset.toString();
            } catch (AssertionError e) {
                e.printStackTrace();
            }
        }
    }
}
