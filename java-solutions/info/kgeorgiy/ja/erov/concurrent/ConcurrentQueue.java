package info.kgeorgiy.ja.erov.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A bounded thread-safe wrapper for {@link Queue}.
 * @param <T> type of elements to be queued
 */
public class ConcurrentQueue<T> {
    private final Queue<T> queue;
    private final int maxSize;

    /**
     * Creates a queue with fixed maximal size. Prevents unbound size growth.
     * @param maxSize maximal allowed size of queue
     */
    public ConcurrentQueue(int maxSize) {
        queue = new ArrayDeque<>();
        this.maxSize = maxSize;
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * Stays in passive-waiting if current size is maximum allowed size until there is free space.
     *
     * @param item the element to add
     * @throws InterruptedException if any thread interrupted the current thread
     *                              before or while the current thread was waiting
     */
    public synchronized void add(final T item) throws InterruptedException {
        while (queue.size() == maxSize) {
            wait();
        }
        queue.add(item);
        notifyAll();
    }

    /**
     * Retrieves and removes the head of this queue.
     * Stays in passive waiting if queue is empty until there is any item in queue.
     *
     * @return the head of this queue
     * @throws InterruptedException if any thread interrupted the current thread
     *                              before or while the current thread was waiting
     */
    public synchronized T poll() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        final T result = queue.poll();
        notifyAll();
        return result;
    }

    /**
     * Determine if there are no elements in this queue.
     *
     * @return {@code true} if this collection contains no elements
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
