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

import java.util.*;
import java.util.concurrent.CyclicBarrier;

/**
 * <p>The class that defines a concurrent test for the three implementations
 * of the multiset.</p>
 *
 * @author Alessandra Fais
 */
public class TestConcurrent {
    public static int THREAD_NUM = 50;
    public static int CAPACITY = 30000;
    public static int SIZE = 10000;

    /**
     * <p>Utilizes an array of threads in order to test the operations of {@link concurrent_multiset.ConcurrentMultiset#add(Object, int)},
     * {@link concurrent_multiset.ConcurrentMultiset#remove(Object, int)}, {@link concurrent_multiset.ConcurrentMultiset#count(Object)},
     * {@link concurrent_multiset.ConcurrentMultiset#contains(Object)} and {@link concurrent_multiset.ConcurrentMultiset#removeAll(Collection)}
     * on the multiset. Some <code>assert</code> statements are used in the code to test the results of the operations on the multiset.</p>
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        ConcurrentMultiset<Integer> testMultiset = null;
        CyclicBarrier barrier = new CyclicBarrier(THREAD_NUM);
        List<Thread> threads;
        long completionTime;

        System.out.println("+--------------------------------------------------------------+");
        System.out.println("|                 CONCURRENT TEST COMPLETION TIME              |");
        System.out.println("+--------------------------------------------------------------+");
        System.out.println("  Version\t\tAdd\t\tRemove   RemoveAll   Contains");

        for (int i = 0; i < 3; i++) {

            switch (i) {
                case 0:	System.out.print("FINE GRAINED\t");
                    testMultiset = new FineGrainMultiset<>(CAPACITY);
                    break;
                case 1: System.out.print("LAZY\t\t\t");
                    testMultiset = new LazyMultiset<>(CAPACITY);
                    break;
                case 2: System.out.print("LOCK FREE\t\t");
                    testMultiset = new LockFreeMultiset<>(CAPACITY);
                    break;
            }

            /* FUNCTIONALITY add(T element) */

            //System.out.println("Functionality add(T element): successful if no exception is raised.");

            threads = new LinkedList<>();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new AddTestThread(1, j, true, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                //System.out.println("In assert after ADD: \n" + testMultiset.getActualSize() + " " + testMultiset.getSize());
                assert(testMultiset.getActualSize() == SIZE) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == SIZE) : testMultiset.getSize();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            /* FUNCTIONALITY remove(Object element) */

            //System.out.println("Functionality remove(Object element): successful if no exception is raised.");

            threads = new LinkedList<>();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new RemoveTestThread(1, null, j, true, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                //System.out.println("In assert after REMOVE: \n" + testMultiset.getActualSize() + " " + testMultiset.getSize());
                assert(testMultiset.getActualSize() == 0) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == 0) : testMultiset.getSize();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            /* FUNCTIONALITY add(T element, int occurrences) */

            //System.out.println("Functionality add(T element, int occurrences): successful if no exception is raised.");

            threads = new LinkedList<>();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new AddTestThread(3, j, true, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                //System.out.println("In assert after ADD_OCC: \n" + testMultiset.getActualSize() + " " + testMultiset.getSize());
                assert(testMultiset.getActualSize() == CAPACITY) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == SIZE) : testMultiset.getSize();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            /* FUNCTIONALITY remove(Object element, int occurrences) */

            //System.out.println("Functionality remove(Object element, int occurrences): successful if no exception is raised.");

            threads = new LinkedList<>();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new RemoveTestThread(2, null, j, true, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                //System.out.println("In assert after REMOVE_OCC: \n" + testMultiset.getActualSize() + " " + testMultiset.getSize());
                assert(testMultiset.getActualSize() == SIZE) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == SIZE) : testMultiset.getSize();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            /* FUNCTIONALITY removeAll(Collection c) */

            ArrayList<Integer>[] collections = (ArrayList<Integer>[]) new ArrayList[THREAD_NUM];
            for (int j = 0; j < THREAD_NUM; j++) {
                collections[j] = new ArrayList<>();
            }

            for (int j = 0; j < SIZE; j++) {
                collections[j % THREAD_NUM].add(j);
            }

            threads = new LinkedList<>();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new RemoveTestThread(-1, collections[j], j, true, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                //System.out.println("In assert after REMOVE_OCC: \n" + testMultiset.getActualSize() + " " + testMultiset.getSize());
                assert(testMultiset.getActualSize() == 0) : testMultiset.getActualSize();
                assert(testMultiset.getSize() == 0) : testMultiset.getSize();
            } catch (AssertionError e) {
                e.printStackTrace();
            }


            // EVALUATION OF THE COMPLETION TIME

            /* PERFORMANCE add(T element, int occurrences) */

            threads = new LinkedList<>();

            completionTime = System.currentTimeMillis();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new AddTestThread(2, j, false, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            completionTime = System.currentTimeMillis() - completionTime;
            System.out.print(completionTime);

            /* PERFORMANCE remove(Object element) */

            threads = new LinkedList<>();

            completionTime = System.currentTimeMillis();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new RemoveTestThread(1, null, j, false, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            completionTime = System.currentTimeMillis() - completionTime;
            System.out.print("\t\t" + completionTime);

            /* PERFORMANCE contains(T element) */

            threads = new LinkedList<>();

            long containsComplTime = System.currentTimeMillis();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new ContainsTestThread(j, false, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            containsComplTime = System.currentTimeMillis() - containsComplTime;

            /* PERFORMANCE removeAll(Collection c) */

            threads = new LinkedList<>();

            completionTime = System.currentTimeMillis();

            for (int j = 0; j < THREAD_NUM; j++)
                threads.add(new RemoveTestThread(-1, collections[j], j, false, testMultiset, barrier));

            for (Thread t : threads)
                t.start();

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            completionTime = System.currentTimeMillis() - completionTime;
            System.out.print("\t\t  " + completionTime);
            System.out.println("\t\t\t" + containsComplTime);
        }

    }
}
