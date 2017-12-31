/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package concurrent_multiset;

import java.util.Collection;

/**
 * <p>The definition of a lock utilized to block the {@link ConcurrentMultiset#add(Object, int)}
 * and {@link ConcurrentMultiset#remove(Object, int)} operations while someone is executing a
 * {@link ConcurrentMultiset#removeAll(Collection)} on the multiset. The implementation of the
 * lock is based on a boolean variable, declared <code>volatile</code> (all the threads are
 * guaranteed to see the updated value of the variable because the read and write operations
 * are addressed to the main memory, bypassing the cache).</p>
 *
 * <p>This choice has been made in order to avoid losing the results of other concurrent
 * operations performed on the multiset during the {@link ConcurrentMultiset#removeAll(Collection)}
 * method execution.</p>
 *
 * @author Alessandra Fais
 */
public class MultisetLock {
    private volatile boolean read_only;

    /**
     * Constructor for the multiset lock.
     */
    public MultisetLock() {
        read_only = false;
    }

    public void testAndWait() {
        while(read_only) { }
    }

    public synchronized void markAndLock() {
        testAndWait();
        read_only = true;
    }

    public void release() {
        read_only = false;
    }
}
