package info.kgeorgiy.ja.erov.arrayset;

import java.util.*;

public class ArraySet<E extends Comparable<? super E>> extends AbstractSet<E> implements NavigableSet<E> {
    private final Comparator<? super E> comparator;
    private final OrderedList<E> storage;

    public ArraySet() {
        this(new ArrayList<>(), false, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(new ArrayList<>(), false, comparator);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(SortedSet<E> set) {
        this(set, set.comparator());
    }

    public ArraySet(ArraySet<E> set) {
        this(set.storage, false, set.comparator);
    }

    public ArraySet(Collection<? extends E> collection,
                    Comparator<? super E> comparator) {
        this.comparator = comparatorOrDefault(comparator);
        this.storage = new OrderedList<>(asList(collection, comparator), false);
    }

    private ArraySet(final List<E> list,
                     boolean descending,
                     Comparator<? super E> comparator) {
        this.comparator = comparatorOrDefault(comparator);
        this.storage = new OrderedList<>(list, descending);
    }

    @Override
    public E lower(E e) {
        return itemBound(e, true, false);
    }

    @Override
    public E floor(E e) {
        return itemBound(e, true, true);
    }

    @Override
    public E ceiling(E e) {
        return itemBound(e, false, true);
    }

    @Override
    public E higher(E e) {
        return itemBound(e, false, false);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("pollFirst() error: ArraySet is immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("pollLast() error: ArraySet is immutable");
    }

    @Override
    public Iterator<E> iterator() {
        return storage.iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(storage.list, !storage.descending, Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement,
                                  boolean fromInclusive,
                                  E toElement,
                                  boolean toInclusive) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Taking subset error: fromElement > toElement");
        }
        return subSetImpl(
                indexBound(fromElement, false, fromInclusive),
                indexBound(toElement, false, !toInclusive)
        );
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return subSetImpl(0, indexBound(toElement, false, !inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return subSetImpl(indexBound(fromElement, false, inclusive), size());
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator == Comparator.naturalOrder() ? null : comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException("first() error: ArraySet is empty");
        }
        return storage.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException("last() error: ArraySet is empty");
        }
        return storage.get(size() - 1);
    }

    @Override
    public int size() {
        return storage.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return search((E) o) >= 0;
    }


    private int search(E element) {
        return Collections.binarySearch(storage, element, comparator);
    }

    private int indexBound(E e, boolean lower, boolean inclusive) {
        int ind = search(e);
        if (ind < 0) {
            ind = -(ind + 1) - (lower ? 1 : 0);
        } else {
            ind += (lower ? -1 : 1) * (inclusive ? 0 : 1);
        }
        return ind;
    }

    private E itemBound(E e, boolean lower, boolean inclusive) {
        int ind = indexBound(e, lower, inclusive);
        return ind < 0 || ind >= size() ? null : storage.get(ind);
    }

    private NavigableSet<E> subSetImpl(int fromIndex, int toIndex) {
        return new ArraySet<>(storage.subList(fromIndex, toIndex), storage.descending, comparator);
    }

    private List<E> asList(Collection<? extends E> collection,
                           Comparator<? super E> comparator) {
        TreeSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        return new ArrayList<>(treeSet);
    }

    private Comparator<? super E> comparatorOrDefault(Comparator<? super E> comparator) {
        return comparator == null ? Comparator.naturalOrder() : comparator;
    }
}
