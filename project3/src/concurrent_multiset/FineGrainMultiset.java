/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package concurrent_multiset;

import exceptions.FullMultisetException;
import exceptions.InvalidCountException;
import exceptions.NullCollectionException;
import exceptions.NullValueException;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The fine grain implementation of a concurrent multiset.
 * @param <T> the type of the elements that can be inserted in the multiset
 *
 * @author Alessandra Fais
 */
public class FineGrainMultiset<T> implements ConcurrentMultiset<T> {

    /**
     * The class that defines the entry of a fine grain multiset.
     * @param <T> the type of the value contained in each entry (the
     *           element of the multiset)
     */
    private static final class MultisetElement<T> {

        private final T value;
        private volatile int occurrences;
        private volatile MultisetElement<T> next;
        private ReentrantLock lock;

        /**
         * Constructor for the special elements (the sentinels) of the multiset.
         */
        public MultisetElement() {
            this(null, -1, null);
        }

        /**
         * Constructor for a generic element of the multiset.
         * @param value the value of the element of the multiset
         * @param occurrences the number of occurrences of the element to put into the multiset
         * @param next the successor of the current element in the multiset
         */
        public MultisetElement(T value, int occurrences, MultisetElement<T> next) {
            this.value = value;
            this.occurrences = occurrences;
            this.next = next;
            this.lock = new ReentrantLock();
        }

        /**
         * Get method for the value of the element.
         * @return the value of the element
         */
        public T getValue() {
            return value;
        }

        /**
         * Get method for the occurrences of the element in the multiset.
         * @return the number of occurrences of the element
         */
        public int getOccurrences() {
            return occurrences;
        }

        /**
         * Get method for the successor of the element.
         * @return the successor element in the multiset
         */
        public MultisetElement<T> getNext() {
            return next;
        }

        /**
         * Set method for the occurrences of the element in the multiset
         * @param delta the quantity to add to the current value of the
         *              <code>occurrences</code> field
         */
        public void setOccurrences(int delta) {
            this.occurrences += delta;
        }

        /**
         * Set method for the successor of the element.
         * @param next the new successor element in the multiset
         */
        public void setNext(MultisetElement<T> next) {
            this.next = next;
        }

        /**
         * Locks the current element.
         */
        public void lock() {
            lock.lock();
        }

        /**
         * Unlocks the current element.
         */
        public void unlock() {
            lock.unlock();
        }

        /**
         * Checks if the current element is a sentinel.
         * @return true if it is a sentinel, false otherwise
         */
        public boolean isSentinel() {
            return (value == null);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Creates a string containing the representation of an element
         * of the multiset.</p>
         * @return the representation of the element
         */
        public String toString() {
            return "<" + value.toString() + ", " + occurrences + ">";
        }

    }

    private volatile AtomicInteger size;
    private volatile AtomicInteger actual_size;
    private Integer capacity;
    private MultisetElement<T> head;
    private MultisetLock read_only_multiset;

    /**
     * Constructor of the fine grain multiset.
     * @param capacity the maximum number of elements that the multiset
     *                 can contain
     */
    public FineGrainMultiset(int capacity) {
        this.size = new AtomicInteger(0);
        this.actual_size = new AtomicInteger(0);
        this.capacity = capacity;
        MultisetElement<T> left_sentinel = new MultisetElement<>();
        MultisetElement<T> right_sentinel = new MultisetElement<>();
        left_sentinel.setNext(right_sentinel);
        this.head = left_sentinel;
        read_only_multiset = new MultisetLock();
    }

    /**
     * Searches the element inside the multiset.
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are locked: each node carries its own lock and
     * the threads acquire them in a hand-over-hand fashion.</p>
     * @param value the element to be found
     * @return the predecessor of the element, or the last element
     *          of the multiset if the element has not been found
     */
    private MultisetElement<T> search(Object value) {
        head.lock();
        head.getNext().lock();
        MultisetElement<T> pre = head;
        MultisetElement<T> cur = head.getNext();

        while (!cur.isSentinel() && !value.equals(cur.getValue())) {
            pre.unlock();
            pre = cur;
            cur = cur.getNext();
            cur.lock();
        }

        return pre;
    }

    /**
     * {@inheritDoc}
     *
     * <p>See the more general method {@link #add(Object, int)}.</p>
     * @param element the element to be inserted in the multiset
     * @return true if the insertion is successful
     * @throws NullValueException if the element is null
     * @throws FullMultisetException if the multiset has already reached its capacity
     */
    @Override
    public boolean add(T element) throws NullValueException, FullMultisetException {
        return (add(element, 1) >= 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are locked: the locks are guaranteed to be
     * released thanks to the finally clause.</p>
     * @param element the element to be found in the multiset
     * @return true if the element is present, false otherwise
     * @throws NullValueException if the element is null
     */
    @Override
    public boolean contains(Object element) throws NullValueException {
        return (count(element) > 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are locked: the locks are guaranteed to be
     * released thanks to the finally clause.</p>
     * @param element the element to be found in the multiset
     * @return the occurrences of the element in the multiset
     * @throws NullValueException if the element is null
     */
    @Override
    public int count(Object element) throws NullValueException {
        if (element == null)
            throw new NullValueException("Cannot search a null element inside the multiset.");

        MultisetElement<T> pre = null;
        MultisetElement<T> cur = null;
        int count = 0;

        try {
            pre = search(element);
            cur = pre.getNext();
            if (!cur.isSentinel() && element.equals(cur.getValue()))
                count = cur.getOccurrences();
        } finally {
            if (pre != null) pre.unlock();
            if (cur != null) cur.unlock();
        }

        return count;
    }

    /**
     * {@inheritDoc}
     *
     * <p>See the more general method {@link #remove(Object, int)}.</p>
     * @param element the element to be removed from the multiset
     * @return true if the occurrence is removed, false if the element is not present
     * @throws NullValueException if the element is null
     */
    @Override
    public boolean remove(Object element) throws NullValueException {
        return (remove(element, 1) > 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the entry for the specified element already exists in the multiset then increments its
     * <code>occurrences</code> field and the number <code>actual_size</code> of elements of the multiset.
     * If the entry doesn't exist then it is created and inserted (if the multiset is not full).</p>
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are locked: each node carries its own lock and
     * the threads acquire them in a hand-over-hand fashion. While a node is being added it is guaranteed
     * that its predecessor and successor can't be removed and no other nodes can be added between them.</p>
     *
     * <p>At the beginning it is checked if the multiset is in <b>read only mode</b>: this happens when the
     * {@link #removeAll(Collection)} is executed on the multiset. The strategy is to wait until the end of the
     * execution of the {@link #removeAll(Collection)} in order to work on the updated multiset and to avoid losing
     * any changes performed during the execution of the method.</p>
     *
     * <p><b>Linearization point:</b> in the successful case when the predecessor is redirected to the added
     * entry (or when the occurrences are updated), in the unsuccessful case when an exception is raised.</p>
     * @param element the element to be inserted in the multiset
     * @return the occurrences of the element before the insertion operation
     * @throws NullValueException if the element is null
     * @throws InvalidCountException if the number of occurrences is less than 1
     * @throws FullMultisetException if the multiset has already reached its capacity
     */
    @Override
    public int add(T element, int occurrences) throws NullValueException, InvalidCountException, FullMultisetException {
        read_only_multiset.testAndWait();

        if (element == null)
            throw new NullValueException("Cannot insert null value elements into the multiset.");
        if (occurrences <= 0)
            throw new InvalidCountException("Cannot insert an element with a number of occurrences less than 1 into the multiset");

        MultisetElement<T> pre = null;
        MultisetElement<T> cur = null;
        int old_occurrences = 0;

        try {
            pre = search(element);
            cur = pre.getNext();
            if (!cur.isSentinel() && element.equals(cur.getValue())) {
                old_occurrences = cur.getOccurrences();
                cur.setOccurrences(occurrences);
                actual_size.getAndAdd(occurrences);
            }
            else {
                if (actual_size.addAndGet(occurrences) > capacity) {
                    actual_size.addAndGet(-occurrences);
                    throw new FullMultisetException("Cannot insert new entries because the multiset is full.");
                }
                MultisetElement<T> new_element = new MultisetElement<>(element, occurrences, cur);
                pre.setNext(new_element);
                size.incrementAndGet();
            }
        } finally {
            if (pre != null) pre.unlock();
            if (cur != null) cur.unlock();
        }

        return old_occurrences;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the entry for the specified element exists in the multiset then distinguishes between
     * two cases: if the current counter for that element is greater than the number of occurrences
     * to be removed then its <code>occurrences</code> field is decremented by a quantity equals to
     * the parameter <code>occurrences</code>, otherwise the entry is removed by redirecting the link
     * of the <code>pre</code> to the successor of this node and the number of entries <code>size</code>
     * and the number of elements <code>actual_size</code> inside the multiset are updated.
     * If the element is not present in the multiset no changes are performed.</p>
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are locked: each node carries its own lock and
     * the threads acquire them in a hand-over-hand fashion. While a node is being removed it is guaranteed
     * that its predecessor and successor can't be removed and no nodes can be added between them.</p>
     *
     * <p>At the beginning it is checked if the multiset is in <b>read only mode</b>: this happens when the
     * {@link #removeAll(Collection)} is executed on the multiset. The strategy is to wait until the end of the
     * execution of the {@link #removeAll(Collection)} in order to work on the updated multiset and to avoid losing
     * any changes performed during the execution of the method.</p>
     *
     * <p><b>Linearization point:</b> in the successful case when the predecessor is redirected to the successor
     * of the removed entry (or when the occurrences are updated), in the unsuccessful case when an exception
     * is raised or the element has not been found.</p>
     * @param element the element to be removed from the multiset
     * @param occurrences the number of occurrences of the element
     * @return the previous number of occurrences of the element
     * @throws NullValueException if the element is null
     * @throws InvalidCountException if the number of occurrences is less than 1
     */
    @Override
    public int remove(Object element, int occurrences) throws NullValueException, InvalidCountException {
        read_only_multiset.testAndWait();

        if (element == null)
            throw new NullValueException("Cannot remove null value elements from the multiset.");
        if (occurrences <= 0)
            throw new InvalidCountException("Cannot remove less than 1 occurrence of an element from the multiset");

        MultisetElement<T> pre = null;
        MultisetElement<T> cur = null;
        int old_occurrences = 0;

        try {
            pre = search(element);
            cur = pre.getNext();
            if (!cur.isSentinel() && element.equals(cur.getValue())) {
                old_occurrences = cur.getOccurrences();
                if (old_occurrences <= occurrences) {
                    pre.setNext(cur.getNext());
                    size.decrementAndGet();
                    actual_size.addAndGet(-old_occurrences);
                } else {
                    cur.setOccurrences(-occurrences);
                    actual_size.addAndGet(-occurrences);
                }
            }
        } finally {
            if (pre != null) pre.unlock();
            if (cur != null) cur.unlock();
        }

        return old_occurrences;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In order to guarantee a single linearization point a new private multiset is created and
     * modified, preserving the state of the original one. If an element is contained in the collection
     * then the method has to remove a number of occurrences of that element equals
     * to its frequency in the collection. If the old occurrences of the element were higher than the
     * frequency in the collection then the element is inserted in the temporary multiset
     * with the remaining number of occurrences, otherwise it is not inserted. If an element
     * is not contained inside the collection then it is copied in the temporary multiset.
     * The original multiset is modified only at the end of the method, where all the
     * changes become effective.</p>
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are locked: each node carries its own lock and
     * the threads acquire them in a hand-over-hand fashion.</p>
     *
     * <p>The main issue of this implementation is that any operation performed on the
     * multiset during the execution of the method {@link #removeAll(Collection)} is lost:
     * at the end of the method the old multiset is replaced with a new one and the result
     * of any concurrent operation performed in the meantime is erased. To avoid this issue the
     * choice is to lock the multiset in a <b>read-only state</b>. The methods {@link #add(Object, int)}
     * and {@link #remove(Object, int)} wait until the {@link #removeAll(Collection)} termination and
     * then start their execution.</p>
     *
     * <p><b>Linearization point:</b> in the successful case when the original multiset is substituted with
     * the new one and the sizes are updated, in the unsuccessful case when an exception is raised.</p>
     * @param c the collection containing the elements to be removed from the multiset
     * @return true if the multiset has been changed, false otherwise
     * @throws NullCollectionException if the collection is null
     */
    @Override
    public boolean removeAll(Collection c) throws NullCollectionException {
        read_only_multiset.markAndLock();

        if (c == null)
            throw new NullCollectionException("Cannot utilize a null collection.");

        MultisetElement<T> new_left_sentinel = new MultisetElement<>();
        MultisetElement<T> new_right_sentinel = new MultisetElement<>();
        new_left_sentinel.setNext(new_right_sentinel);
        MultisetElement<T> pointer = new_left_sentinel;

        MultisetElement<T> pre = null;
        MultisetElement<T> cur = null;
        boolean something_removed = false;
        int new_size = 0;
        int new_actual_size = 0;

        try {
            head.lock();
            head.getNext().lock();
            pre = head;
            cur = head.getNext();

            while (!cur.isSentinel()) {
                T current_value = cur.getValue();
                if (c.contains(current_value)) {
                    int occ = Collections.frequency(c, current_value);
                    if (cur.getOccurrences() > occ) {
                        int delta = cur.getOccurrences() - occ;
                        MultisetElement<T> temp = new MultisetElement<>(current_value, delta, new_right_sentinel);
                        pointer.setNext(temp);
                        pointer = pointer.getNext();
                        new_size++;
                        new_actual_size += delta;
                    }
                    something_removed = true;
                } else {
                    MultisetElement<T> temp = new MultisetElement<>(current_value, cur.getOccurrences(), new_right_sentinel);
                    pointer.setNext(temp);
                    pointer = pointer.getNext();
                    new_size++;
                    new_actual_size += cur.getOccurrences();
                }
                pre.unlock();
                pre = cur;
                cur = cur.getNext();
                cur.lock();
            }
            head.setNext(new_left_sentinel.getNext());
            size.getAndSet(new_size);
            actual_size.getAndSet(new_actual_size);
        } finally {
            read_only_multiset.release();
            if (pre != null) pre.unlock();
            if (cur != null) cur.unlock();
        }

        return something_removed;
    }

    /**
     * {@inheritDoc}
     *
     * @return the number of entries in the multiset
     */
    @Override
    public int getSize() {
        return size.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return the total number of elements in the multiset
     */
    @Override
    public int getActualSize() {
        return actual_size.get();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a string containing a representation of the elements
     * of the multiset.</p>
     * @return the representation of the multiset
     */
    @Override
    public synchronized String toString() {
        StringBuilder result = new StringBuilder("[");
        MultisetElement<T> cur = head.getNext();

        int count = 0;
        while(!cur.isSentinel()) {
            if (count < getSize()-1)
                result.append(cur.toString()).append(", ");
            else
                result.append(cur.toString());
            cur = cur.getNext();
            count++;
        }

        return result.append("]").toString();
    }
}
