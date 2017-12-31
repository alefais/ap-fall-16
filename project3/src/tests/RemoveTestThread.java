/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package tests;

import concurrent_multiset.ConcurrentMultiset;
import exceptions.InvalidCountException;
import exceptions.NullValueException;

import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * <p>Thread that tests the {@link concurrent_multiset.ConcurrentMultiset#remove(Object, int)} and
 * {@link concurrent_multiset.ConcurrentMultiset#removeAll(Collection)} operations on the multiset.</p>
 *
 * @author Alessandra Fais
 */
public class RemoveTestThread extends Thread {

    private int beginIndex;
    private boolean functionalityTest;
    private ConcurrentMultiset<Integer> testMultiset;
    private CyclicBarrier barrier;
    private int occurrences;
    private Collection collection;

    /**
     * The constructor method.
     * @param occ the number of occurrences of the element to be removed (used by {@link concurrent_multiset.ConcurrentMultiset#remove(Object, int)})
     * @param c the collection containing the elements to be removed (used by {@link concurrent_multiset.ConcurrentMultiset#removeAll(Collection)})
     * @param beginIndex the first element to remove
     * @param functionalityTest a boolean flag to activate the functionality test
     * @param testMultiset the multiset
     * @param barrier the barrier used to synchronize the threads
     */
    public RemoveTestThread(int occ, Collection c, int beginIndex, boolean functionalityTest,
                            ConcurrentMultiset<Integer> testMultiset, CyclicBarrier barrier) {
        this.beginIndex = beginIndex;
        this.functionalityTest = functionalityTest;
        this.testMultiset = testMultiset;
        this.barrier = barrier;
        this.occurrences = occ;
        this.collection = c;
    }

    public void run() {
        if (collection != null && occurrences == -1) {
            testMultiset.removeAll(collection);
        } else {
            for (int i = beginIndex; i < TestConcurrent.SIZE; i += TestConcurrent.THREAD_NUM) {
                if (functionalityTest) {
                    try {
                        barrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    testMultiset.remove(i, occurrences);
                } catch (NullValueException | InvalidCountException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
