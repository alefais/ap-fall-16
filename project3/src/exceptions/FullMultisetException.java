/*
 * Created by Alessandra Fais
 * Mat: 481017
 * Computer Science and Networking
 * Advanced Programming 2016/17
 * Homework 3
 */

package exceptions;

/**
 * <p>Exception defined to handle the insertion of an element in
 * a full multiset.</p>
 *
 * <p>The exception inherits from {@link RuntimeException}: the
 * choice is to use <b>unchecked exceptions</b>. It is not needed to
 * declare them in the <code>throws</code> clause of the methods that
 * can raise them but in order to gain in readability the choice is to
 * enumerate them in the <code>throws</code> clause (without changing
 * the signature of the methods defined in the interface
 * {@link concurrent_multiset.ConcurrentMultiset}).
 *
 * @author Alessandra Fais
 */
public class FullMultisetException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FullMultisetException(String s) {
        super(s);
    }
}
