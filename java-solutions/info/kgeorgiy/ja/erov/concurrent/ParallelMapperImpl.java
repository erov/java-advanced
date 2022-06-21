package info.kgeorgiy.ja.erov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * {@link ParallelMapper} implementation using first-in-first-out queued order for parallel execution.
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final ConcurrentQueue<Runnable> tasks;
    private final List<Thread> threadPool;
    private int activeCalls;

    /**
     * Create instance with given amount of {@link Thread}.
     * @param threads maximum number of threads to use
     */
    public ParallelMapperImpl(final int threads) {
        if (threads < 0) {
            throw new IllegalArgumentException("threads amount must be a positive integer");
        }

        tasks = new ConcurrentQueue<>(1 << 16);

        final Runnable handler = () -> {
            try {
                while (!Thread.interrupted()) {
                    tasks.poll().run();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        };

        // :NOTE: IntStream
        // fixed
        threadPool = IntStream.range(0, threads)
                .mapToObj(i -> new Thread(handler))
                .toList();

        for (Thread thread : threadPool) {
            thread.start();
        }

        activeCalls = 0;
    }

    /**
     * @param <T> value type
     * @param <R> mapped value type
     * @throws RuntimeException if any exception occurs in the mapping runtime
     * @throws IllegalStateException if method calls after {@link #close()}
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (f == null) {
            throw new NullPointerException("Function for mapping must be non-null");
        }
        if (args == null) {
            throw new NullPointerException("List for mapping must be non-null");
        }

        boolean validState = true;
        synchronized (this) {
            if (activeCalls == -1) {
                validState = false;
            } else {
                ++activeCalls;
            }
        }

        if (!validState) {
            throw new IllegalStateException("Cannot apply mapping on closed instance");
        }

        final ConcurrentCollectingList<R> result = new ConcurrentCollectingList<>(args.size());
        final ConcurrentQueue<RuntimeException> runtimeExceptions = new ConcurrentQueue<>(args.size());

        // :NOTE: IntStream
        final List<Runnable> workingTasks = IntStream.range(0, args.size())
                .mapToObj(
                        i -> (Runnable) () -> {
                            try {
                                result.set(i, f.apply(args.get(i)));
                            } catch (RuntimeException e) {
                                result.skip();
                                try {
                                    runtimeExceptions.add(e);
                                } catch (InterruptedException ex) {
                                    // ignore
                                }
                            }
                        }
                )
                .toList();

        for (Runnable task : workingTasks) {
            tasks.add(task);
        }

        // :NOTE: Бесконечное ожидание
        // fixed - call ConcurrentCollectingList#skip() in indices where RuntimeException was thrown
        final List<R> mappedValues = result.getList();

        if (!runtimeExceptions.isEmpty()) {
            final RuntimeException exceptions = runtimeExceptions.poll();
            while (!runtimeExceptions.isEmpty()) {
                exceptions.addSuppressed(runtimeExceptions.poll());
            }
            throw exceptions;
        }

        synchronized (this) {
            --activeCalls;
        }

        return mappedValues;
    }

    @Override
    public void close() {
        boolean validState = true;

        synchronized (this) {
            if (activeCalls > 0) {
                validState = false;
            }
            if (activeCalls == -1) {
                return;
            }
            activeCalls = -1;
        }

        if (!validState) {
            throw new IllegalStateException("Cannot close ParallelMapperImpl while there are some active tasks");
        }

        for (Thread thread : threadPool) {
            thread.interrupt();
        }

        for (int i = 0; i != threadPool.size(); ++i) {
            try {
                threadPool.get(i).join();
            } catch (InterruptedException e) {
                --i;
            }
        }
    }
}
