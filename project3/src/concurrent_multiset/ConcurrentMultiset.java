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
 * The interface that defines the methods that each implementation of a
 * concurrent multiset must provide.
 * @param <T> the type of the elements that can be inserted in the multiset
 *
 * @author Alessandra Fais
 */
public interface ConcurrentMultiset<T> {

    /**
     * Adds a single occurrence of the specified element to the multiset.
     * @param element the element to be inserted in the multiset
     * @return true if the element is inserted in the multiset
     *         (always because duplicates are allowed)
     */
    boolean add(T element);

    /**
     * Determines whether the multiset contains the specified elements or not.
     * @param element the element to be found in the multiset
     * @return true if the element is present, false otherwise
     */
    boolean contains(Object element);

    /**
     * Returns the number of occurrences of the specified element in the multiset.
     * @param element the element to be found in the multiset
     * @return the occurrences of the element in the multiset
     */
    int count(Object element);

    /**
     * Removes a single occurrence of the specified element from the multiset, if
     * it is present.
     * @param element the element to be removed from the multiset
     * @return true if the occurrence is removed, false if the element is not present
     */
    boolean remove(Object element);

    /**
     * Adds a number of occurrences of the specified element to the multiset.
     * @param element the element to be inserted in the multiset
     * @param occurrences the number of occurrences of the element
     * @return the previous number of occurrences of the element (0 if the element
     *         was not already in the multiset)
     */
    int add(T element, int occurrences);

    /**
     * Removes a number of occurrences of the specified element from the multiset.
     * @param element the element to be removed from the multiset
     * @param occurrences the number of occurrences of the element
     * @return the previous number of occurrences of the element
     */
    int remove(Object element, int occurrences);

    /**
     * Removes all the elements of the collection from the multiset.
     * @param c the collection containing the elements to be removed from the multiset
     * @return true if the multiset has been changed, false otherwise
     */
    boolean removeAll(Collection<?> c);

    /**
     * Returns the number of <code>MultisetElement</code> entries inside the multiset.
     * @return the number of entries in the multiset
     */
    int getSize();

    /**
     * Returns the number of elements (the summation of the the occurrences of all the
     * elements contained in the multiset).
     * @return the total number of elements in the multiset
     */
    int getActualSize();

}
