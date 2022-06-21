package info.kgeorgiy.ja.erov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Multithreading {@link List} handler.
 */
public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Default constructor.
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    /**
     * Create instance that uses {@link Thread} from given {@code parallelMapper} instance.
     * @param parallelMapper concurrent map-function handler
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Expected non-empty list of values for finding maximum");
        }
        return applyAndCollectViaTheSame(threads, values, stream -> streamMaximum(stream, comparator));
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return apply(
                threads,
                values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return mapReduce(threads, values, String::valueOf, new Monoid<>("", String::concat));
    }

    /**
     * @param <T> value type
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return applyAndCollectFlatten(threads, values, stream -> stream.filter(predicate));
    }

    /**
     * @param <T> value type
     * @param <U> mapped value type
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f)
            throws InterruptedException {
        return applyAndCollectFlatten(threads, values, stream -> stream.map(f));
    }

    /**
     * @param <T> value type
     */
    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return applyAndCollectViaTheSame(
                threads,
                values,
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator())
        );
    }

    /**
     * @param <T> value type
     * @param <R> mapped value type
     */
    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid)
            throws InterruptedException {
        return reduce(threads, map(threads, values, lift), monoid);
    }


    private <T> T streamMaximum(final Stream<T> stream, final Comparator<? super T> comparator) {
        final Optional<T> result;
        try {
            result = stream.max(comparator);
        } catch (final NullPointerException e) {
            return null;
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("List stream must be non-empty for finding maximum");
        }

        return result.get();
    }

    public <T, U> List<U> applyAndCollectFlatten(
            final int threads,
            final List<? extends T> values,
            final Function<Stream<? extends T>, Stream<? extends U>> streamHandler) throws InterruptedException {
        return apply(
                threads,
                values,
                stream -> streamHandler.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).toList()
        );
    }

    public <T> T applyAndCollectViaTheSame(
            final int threads,
            final List<T> values,
            final Function<Stream<T>, T> streamHandler) throws InterruptedException {
        return apply(
                threads,
                values,
                streamHandler,
                streamHandler
        );
    }

    private <T, R> R apply(int threads,
                           final List<T> values,
                           final Function<Stream<T>, R> subListHandler,
                           final Function<Stream<R>, R> resultsHandler) throws InterruptedException {

        if (threads <= 0) {
            throw new IllegalArgumentException("Threads number must be a positive integer");
        }
        if (values == null) {
            throw new NullPointerException("List for handling must be non-null");
        }

        threads = Math.min(threads, values.size());
        final List<Stream<T>> subListStreams = new ArrayList<>();

        if (threads > 0) {
            final int sizeOfBlock = values.size() / threads;
            final int extraSizedBlocks = values.size() % threads;

            for (int i = 0, from = 0, to; i != threads; from = to, ++i) {
                to = from + sizeOfBlock + (i < extraSizedBlocks ? 1 : 0);
                subListStreams.add(values.subList(from, to).stream());
            }
        }

        final List<R> results = parallelMapper != null ? parallelMapper.map(subListHandler, subListStreams)
                                                       : map(subListHandler, subListStreams);
        return resultsHandler.apply(results.stream());
    }

    private <T, R> List<R> map(
            final Function<Stream<T>, R> subListHandler,
            final List<Stream<T>> subListStreams) throws InterruptedException {

        final int threads = subListStreams.size();
        final List<R> results = new ArrayList<>(Collections.nCopies(threads, null));

        final List<Thread> threadPool = IntStream.range(0, threads)
                .mapToObj(
                        i -> new Thread(
                                () -> results.set(i, subListHandler.apply(subListStreams.get(i)))
                        )
                )
                .toList();

        for (Thread thread : threadPool) {
            thread.start();
        }

        InterruptedException exception = null;
        for (int i = 0; i != threads; ++i) {
            try {
                threadPool.get(i).join();
            } catch (final InterruptedException e) {
                if (exception == null) {
                    exception = e;
                    threadPool.get(i).interrupt();
                } else {
                    exception.addSuppressed(e);
                }
                --i;
            }
        }

        if (exception != null) {
            throw exception;
        }

        return results;
    }
}
