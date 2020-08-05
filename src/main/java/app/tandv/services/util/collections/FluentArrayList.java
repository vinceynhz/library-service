package app.tandv.services.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.*;

/**
 * @author vic on 2020-08-03
 */
public class FluentArrayList<T> extends ArrayList<T> {
    public static <U> FluentArrayList<U> empty() {
        return new FluentArrayList<>();
    }

    private FluentArrayList() {
        super();
    }

    /**
     * We inherit the use of {@link SafeVarargs} on this method since {@link Arrays#asList(Object[])} also does
     *
     * @param t objects to pull into the array list
     */
    @SafeVarargs
    public FluentArrayList(T... t) {
        super(Arrays.asList(t));
    }

    public FluentArrayList(Collection<? extends T> collection) {
        super(collection);
    }

    /**
     * This method is intended to remove elements before a given index in the array (form 0 to n-1)
     * <p>
     * Let's assume the following elements in an object (underneath are their indexes):
     * <p>
     * ['c', 'r', 'd', 'h', 'j', 's']
     * 0    1    2    3    4    5
     * <p>
     * If we call removeBefore(3), the resulting array will not contain elements that were in indexes 0 through 2
     * <p>
     * ['h', 'j', 's']
     * 0    1    2
     * <p>
     * If we call removeBefore(n) and n is less or equal than zero, the resulting array will be the original one and no
     * changes will occur
     * <p>
     * If we call removeBefore(n) and n is greater or equal than the size of the array, the resulting array will be empty
     * (identical to the case of using {@link ArrayList#clear()}
     *
     * @param index to cut from (exclusive), if out of index we return with no changes
     * @return reference to this instance for fluent API
     */
    public FluentArrayList<T> removeBefore(int index) {
        if (index >= this.size()) {
            this.clear();
        } else if (index > 0) {
            this.removeRange(0, index);
        }
        return this;
    }

    /**
     * This method is intended to remove elements after a given index in the array (form n to end of the array)
     * <p>
     * Let's assume the following elements in an object (underneath are their indexes):
     * <p>
     * ['c', 'r', 'd', 'h', 'j', 's']
     * 0    1    2    3    4    5
     * <p>
     * If we call removeAfter(3), the resulting array will not contain elements that were in indexes 3 through 5
     * <p>
     * ['c', 'r', 'd']
     * 0    1    2
     * <p>
     * If we call removeAfter(n) and n is less or equal than zero, the resulting array will be empty (identical to the
     * case of using {@link ArrayList#clear()}
     * <p>
     * If we call removeAfter(n) and n is greater or equal than the size of the array, the resulting array will be the
     * original one and no changes will occur
     *
     * @param index to cut up to
     * @return reference to this instance for fluent API
     */
    public FluentArrayList<T> removeAfter(int index) {
        if (index <= 0) {
            this.clear();
        } else if (index < this.size()) {
            this.removeRange(index, this.size());
        }
        return this;
    }

    /**
     * @param from index to move. If this value is less than 0 it will be taken from the end of the array
     * @param to   index to inject the moved object. If this value is less than 0 it will be taken to the end of the array
     * @return reference to this instance for fluent API
     */
    public FluentArrayList<T> swap(int from, int to) {
        // we add to the size since the value is already negative
        int realFrom = from < 0 ? this.size() + from : from;
        int realTo = to < 0 ? this.size() + to : to;
        return this.thenAdd(realTo, super.remove(realFrom));

    }

    /**
     * @param t to add
     * @return reference to this instance for fluent API
     */
    public FluentArrayList<T> thenAdd(T t) {
        super.add(t);
        return this;
    }

    /**
     * @param i index to add into
     * @param t to add
     * @return reference to this instance for fluent API
     */
    public FluentArrayList<T> thenAdd(int i, T t) {
        super.add(i, t);
        return this;
    }

    /**
     * To apply an operator to all the elements in the array
     *
     * @param operator to apply
     * @return a reference to this instance for fluent API
     */
    public FluentArrayList<T> thenReplaceAll(UnaryOperator<T> operator) {
        this.replaceAll(operator);
        return this;
    }

    /**
     * To apply an operator to certain elements in the array from given indexes
     *
     * @param operator to apply
     * @param indexes  to apply to
     * @return a reference to this instance for fluent API
     */
    public FluentArrayList<T> thenReplace(UnaryOperator<T> operator, int... indexes) {
        for (int index : indexes) {
            this.add(index, operator.apply(this.remove(index)));
        }
        return this;
    }

    /**
     * Removes all of the elements of this collection that satisfy the given predicate
     *
     * @param condition to satisfy
     * @return reference to this instance for fluent API
     */
    public FluentArrayList<T> thenRemoveIf(Predicate<T> condition) {
        this.removeIf(condition);
        return this;
    }

    /**
     * The {@link ArrayList} class throws an {@link IndexOutOfBoundsException} when requesting an invalid index;
     * on this class we are providing a safety check first and returning null to be able to use other handling options
     * rather than the exception.
     *
     * @return first element of the array list or null if empty.
     */
    public T getFirst() {
        if (!this.isEmpty()) {
            return this.get(0);
        }
        return null;
    }

    /**
     * The {@link ArrayList} class throws an {@link IndexOutOfBoundsException} when requesting an invalid index;
     * on this class we are providing a safety check first and returning null to be able to use other handling options
     * rather than the exception.
     *
     * @return last element of the array list or null if empty.
     */
    public T getLast() {
        if (!this.isEmpty()) {
            return this.get(this.size() - 1);
        }
        return null;
    }

    /**
     * @param options to check for
     * @return the index of the first element not in the given options, or -1 if not found
     */
    public int firstNotIn(Collection<T> options) {
        for (int i = 0; i < this.size(); i++) {
            if (!options.contains(this.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
