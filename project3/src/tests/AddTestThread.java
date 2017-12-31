/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package tests;

import concurrent_multiset.ConcurrentMultiset;
import exceptions.FullMultisetException;
import exceptions.InvalidCountException;
import exceptions.NullValueException;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * <p>Thread that tests the {@link ConcurrentMultiset#add(Object, int)} operation
 * on the multiset.</p>
 *
 * @author Alessandra Fais
 */
public class AddTestThread extends Thread {

    private int beginIndex;
    private boolean functionalityTest;
    private ConcurrentMultiset<Integer> testMultiset;
    private CyclicBarrier barrier;
    private int occurrences;

    /**
     * The constructor method.
     * @param occ the number of occurrences of the element to be inserted
     * @param beginIndex the first element to insert
     * @param functionalityTest a boolean flag to activate the functionality test
     * @param testMultiset the multiset
     * @param barrier the barrier used to synchronize the threads
     */
    public AddTestThread(int occ, int beginIndex, boolean functionalityTest,
                         ConcurrentMultiset<Integer> testMultiset, CyclicBarrier barrier) {
        this.beginIndex = beginIndex;
        this.functionalityTest = functionalityTest;
        this.testMultiset = testMultiset;
        this.barrier = barrier;
        this.occurrences = occ;
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
                testMultiset.add(i, occurrences);
            } catch (NullValueException | InvalidCountException | FullMultisetException e) {
                e.printStackTrace();
            }
        }
    }

}
