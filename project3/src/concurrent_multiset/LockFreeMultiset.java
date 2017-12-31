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
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * The lock free implementation of a concurrent multiset.
 * @param <T> the type of the elements that can be inserted in the multiset
 *
 * @author Alessandra Fais
 */
public class LockFreeMultiset<T> implements ConcurrentMultiset<T> {

    /**
     * The class that defines the entry of a lock free multiset.
     * @param <T> the type of the value contained in each entry (the
     *           element of the multiset)
     */
    private static final class MultisetElement<T> extends AtomicMarkableReference<MultisetElement<T>> {

        private final T value;
        private volatile AtomicInteger occurrences;

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
            super(next, false);
            this.value = value;
            this.occurrences = new AtomicInteger(occurrences);
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
            return occurrences.get();
        }

        /**
         * Set method for the occurrences of the element in the multiset
         * @param delta the quantity to add to the current value of the
         *              <code>occurrences</code> field
         */
        public void setOccurrences(int delta) {
            this.occurrences.addAndGet(delta);
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
    private final Integer capacity;
    private final MultisetElement<T> head;
    private MultisetLock read_only_multiset;

    /**
     * Constructor of the lock free multiset.
     * @param capacity the maximum number of elements that the multiset
     *                 can contain
     */
    public LockFreeMultiset(int capacity) {
        size = new AtomicInteger(0);
        actual_size = new AtomicInteger(0);
        this.capacity = capacity;
        MultisetElement<T> left_sentinel = new MultisetElement<>();
        MultisetElement<T> right_sentinel = new MultisetElement<>();
        left_sentinel.set(right_sentinel, false);
        head = left_sentinel;
        read_only_multiset = new MultisetLock();
    }

    /**
     * Searches the element inside the multiset.
     *
     * <p>The entry <code>cur</code> is not locked.</p>
     * @param element the element to be found
     * @return the searched element, or the last element
     *          of the multiset if the element has not been found
     */
    private MultisetElement<T> search(Object element) {
        MultisetElement<T> cur = head.getReference();
        while (!cur.isSentinel() && !element.equals(cur.getValue())) {
            cur = cur.getReference();
        }
        return cur;
    }

    /**
     * <p>Searches an element and while traverses the multiset cleans it up
     * from the entries that have been logically removed.</p>
     *
     * <p><code>suc</code> is the successor of the current <code>cur</code> element and the array <code>mark</code>
     * contains the value of the mark bit of <code>cur</code>: if <code>cur</code> is marked then it has to be removed.
     * The {@link java.util.concurrent.atomic.AtomicMarkableReference#compareAndSet(Object, Object, boolean, boolean)}
     * atomically checks if <code>cur</code> is the successor of <code>pre</code> and if <code>pre</code> is not marked,
     * if these conditions are true then <code>cur</code> can be removed and <code>suc</code> becomes the new successor
     * of <code>pre</code>. If the {@link java.util.concurrent.atomic.AtomicMarkableReference#compareAndSet(Object, Object, boolean, boolean)}
     * fails then it is necessary to start again because this means that the method call may be traversing an unreachable
     * part of the multiset.</p>
     * @param value the element to be found
     * @return the predecessor of the element, or the last element
     *          of the multiset if the element has not been found
     */
    private MultisetElement<T> searchAndClean(Object value) {
        MultisetElement<T> pre = head;
        MultisetElement<T> cur = head.getReference();
        MultisetElement<T> suc;
        boolean[] mark = new boolean[1];

        while (!cur.isSentinel() && !value.equals(cur.getValue())) {
            suc = cur.get(mark);
            if (mark[0]) {
                if(pre.compareAndSet(cur, suc, false, false)) // remove cur (is marked)
                    cur = suc;
                else {
                    pre = head;
                    cur = head.getReference();
                }
            }
            else {
                pre = cur;
                cur = suc;
            }
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
     * <p>The entry <code>cur</code> is not locked.</p>
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
     * <p>The entry <code>cur</code> is not locked.</p>
     * @param element the element to be found in the multiset
     * @return the occurrences of the element in the multiset
     * @throws NullValueException if the element is null
     */
    @Override
    public int count(Object element) throws NullValueException {
        if (element == null)
            throw new NullValueException("Cannot search a null element inside the multiset.");

        MultisetElement<T> cur = search(element);
        int count = 0;

        if (!cur.isSentinel() && !cur.isMarked() && element.equals(cur.getValue()))
            count =  cur.getOccurrences();

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
     * <p>The two entries <code>pre</code> and <code>cur</code> are not locked after the {@link #searchAndClean(Object)}
     * phase. When a new entry has to be created, its insertion in the multiset is done by using the
     * {@link java.util.concurrent.atomic.AtomicMarkableReference#compareAndSet(Object, Object, boolean, boolean)}:
     * if it fails then it is necessary to start over.</p>
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

        boolean finished = false;
        int old_occurrences = 0;

        while (!finished) {
            MultisetElement<T> pre = searchAndClean(element);
            MultisetElement<T> cur = pre.getReference();

            if (!cur.isSentinel() && element.equals(cur.getValue())) {
                old_occurrences = cur.getOccurrences();
                cur.setOccurrences(occurrences);
                actual_size.getAndAdd(occurrences);
                finished = true;
            } else {
                if (actual_size.addAndGet(occurrences) > capacity) {
                    actual_size.addAndGet(-occurrences);
                    throw new FullMultisetException("Cannot insert new entries because the multiset is full.");
                }
                size.incrementAndGet();
                finished = pre.compareAndSet(cur, new MultisetElement<>(element, occurrences, cur), false, false);
                if (!finished) {
                    size.decrementAndGet();
                    actual_size.getAndAdd(-occurrences);
                }
            }
        }

        return old_occurrences;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the entry for the specified element exists in the multiset then distinguishes between
     * two cases: if the current counter for that element is greater than the number of occurrences
     * to be removed then its <code>occurrences</code> field is decremented by a quantity equals to
     * the parameter <code>occurrences</code>, otherwise the entry is marked as logically removed from
     * the multiset (and also pointers are rearranged) and the number of entries <code>size</code>
     * and the number of elements <code>actual_size</code> inside the multiset are updated.
     * If the element is not present in the multiset no changes are performed.</p>
     *
     * <p>The two entries <code>pre</code> and <code>cur</code> are not locked after the {@link #searchAndClean(Object)}
     * phase. When an entry has to be removed it is marked by using the
     * {@link java.util.concurrent.atomic.AtomicMarkableReference#compareAndSet(Object, Object, boolean, boolean)}:
     * if it fails then it is necessary to start over.</p>
     *
     * <p>At the beginning it is checked if the multiset is in <b>read only mode</b>: this happens when the
     * {@link #removeAll(Collection)} is executed on the multiset. The strategy is to wait until the end of the
     * execution of the {@link #removeAll(Collection)} in order to work on the updated multiset and to avoid losing
     * any changes performed during the execution of the method.</p>
     *
     * <p><b>Linearization point:</b> in the successful case when the entry to be removed is
     * marked (or when the occurrences are updated), in the unsuccessful case when an exception
     * is raised or the element has not been found.</p>
     * @param element the element to be removed from the multiset
     * @param occurrences the number of occurrences of the element
     * @return the previous number of occurrences of the element
     * @throws NullValueException if the element is null
     * @throws InvalidCountException if the number of occurrences is less than 1
     */
    @Override
    public int remove(Object element, int occurrences) {
        read_only_multiset.testAndWait();

        if (element == null)
            throw new NullValueException("Cannot remove null value elements from the multiset.");
        if (occurrences <= 0)
            throw new InvalidCountException("Cannot remove less than 1 occurrence of an element from the multiset");

        boolean finished = false;
        int old_occurrences = 0;

        while (!finished) {
            MultisetElement<T> pre = searchAndClean(element);
            MultisetElement<T> cur = pre.getReference();

            if (cur != null && !cur.isSentinel() && element.equals(cur.getValue()) && !cur.isMarked()) {
                old_occurrences = cur.getOccurrences();
                if (old_occurrences <= occurrences) {
                    if (cur.compareAndSet(cur.getReference(), cur.getReference(), false, true)) {
                        size.decrementAndGet();
                        actual_size.getAndAdd(-old_occurrences);
                        finished = true;
                    }
                } else {
                    cur.setOccurrences(-occurrences);
                    actual_size.getAndAdd(-occurrences);
                    finished = true;
                }
            }
            else {
                finished = true;
            }
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
     * <p>The two entries <code>cur</code> and <code>suc</code> are not locked.</p>
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
    public boolean removeAll(Collection c) {
        read_only_multiset.markAndLock();

        if (c == null)
            throw new NullCollectionException("Cannot utilize a null collection.");

        MultisetElement<T> new_left_sentinel = new MultisetElement<>();
        MultisetElement<T> new_right_sentinel = new MultisetElement<>();
        new_left_sentinel.set(new_right_sentinel, false);
        MultisetElement<T> pointer = new_left_sentinel;

        boolean[] mark = new boolean[1];
        MultisetElement<T> cur = head.getReference();
        MultisetElement<T> suc = cur.get(mark);

        boolean something_removed = false;
        int new_size = 0;
        int new_actual_size = 0;

        while (!cur.isSentinel()) {
            if (!mark[0]) {
                T current_value = cur.getValue();
                if (c.contains(current_value)) {
                    int occ = Collections.frequency(c, current_value);
                    if (cur.getOccurrences() > occ) {
                        int delta = cur.getOccurrences() - occ;
                        MultisetElement<T> temp = new MultisetElement<>(current_value, delta, new_right_sentinel);
                        pointer.set(temp, false);
                        pointer = pointer.getReference();
                        new_size++;
                        new_actual_size += delta;
                    }
                    something_removed = true;
                } else {
                    MultisetElement<T> temp = new MultisetElement<>(current_value, cur.getOccurrences(), new_right_sentinel);
                    pointer.set(temp, false);
                    pointer = pointer.getReference();
                    new_size++;
                    new_actual_size += cur.getOccurrences();
                }
            }
            cur = suc;
            suc = suc.get(mark);
        }
        head.set(new_left_sentinel.getReference(), false);
        size.getAndSet(new_size);
        actual_size.getAndSet(new_actual_size);
        read_only_multiset.release();

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
     * of the multiset: the elements must be not marked otherwise they
     * have to be considered as already removed from the multiset.</p>
     * @return the representation of the multiset
     */
    @Override
    public synchronized String toString() {
        StringBuilder result = new StringBuilder("[");
        boolean[] mark = new boolean[1];
        MultisetElement<T> cur = head.getReference();
        MultisetElement<T> succ = cur.get(mark);

        int count = 0;
        while (!cur.isSentinel()) {
            if (!mark[0])
                if (count < getSize() - 1)
                    result.append(cur.toString()).append(", ");
                else
                    result.append(cur.toString());
            cur = succ;
            succ = succ.get(mark);
            count++;
        }

       return result.append("]").toString();
    }
}
