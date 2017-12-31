/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package tests;

import concurrent_multiset.ConcurrentMultiset;
import exceptions.NullValueException;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * <p>Thread that tests the {@link concurrent_multiset.ConcurrentMultiset#count(Object)} and
 * {@link concurrent_multiset.ConcurrentMultiset#contains(Object)} operations on the multiset.</p>
 *
 * @author Alessandra Fais
 */
public class ContainsTestThread extends Thread {

    private int beginIndex;
    private boolean functionalityTest;
    private ConcurrentMultiset<Integer> testMultiset;
    private CyclicBarrier barrier;

    /**
     * The constructor method.
     * @param beginIndex the first element to check
     * @param functionalityTest a boolean flag to activate the functionality test
     * @param testMultiset the multiset
     * @param barrier the barrier used to synchronize the threads
     */
    public ContainsTestThread(int beginIndex, boolean functionalityTest,
                              ConcurrentMultiset<Integer> testMultiset, CyclicBarrier barrier) {
        this.beginIndex = beginIndex;
        this.functionalityTest = functionalityTest;
        this.testMultiset = testMultiset;
        this.barrier = barrier;
    }

    public void run() {
        for (int i = beginIndex; i < TestConcurrent.SIZE; i += TestConcurrent.THREAD_NUM) {
            if (functionalityTest) {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (testMultiset.count(i) != 1) throw new AssertionError("Element " + i + " count is " +
                        testMultiset.count(i) + ": contains returns " + testMultiset.contains(i));
            } catch (NullValueException | AssertionError e) {
                e.printStackTrace();
            }
        }
    }
}
