package info.kgeorgiy.ja.erov.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A bounded thread-safe {@link List} wrapper allows get instance when all indices will be first time set or skipped.
 * @param <T> value type.
 */
public class ConcurrentCollectingList<T> {
    private final List<T> list;
    private int processedIndices;

    /**
     * Create {@link List} of given size for collecting values.
     * @param size number of values in list
     */
    public ConcurrentCollectingList(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size of list must be a non-negative integer");
        }
        this.list = new ArrayList<>(Collections.nCopies(size, null));
        processedIndices = 0;
    }

    /**
     * Provides all-indices values set.
     *
     * @return collected list
     * @throws InterruptedException if any thread interrupted the current thread
     *                              before or while the current thread was waiting
     */
    public synchronized List<T> getList() throws InterruptedException {
        // :NOTE: synchronized
        // fixed
        while (processedIndices != list.size()) {
            wait();
        }
        return list;
    }

    /**
     * Places the element at the specified position in this list with the specified element for the first time
     *
     * @param index  index of the element to replace
     * @param value element to be stored at the specified position
     * @throws IllegalArgumentException if placing is not first time one
     */
    public synchronized void set(int index, T value) {
        // :NOTE: synchronized
        // fixed
        if (list.get(index) != null) {
            throw new IllegalArgumentException("Index re-initialization is not allowed");
        }
        list.set(index, value);
        ++processedIndices;
        notify();
    }

    /**
     * Signals that this list will be filled by one less element.
     */
    public synchronized void skip() {
        ++processedIndices;
        notify();
    }
}
