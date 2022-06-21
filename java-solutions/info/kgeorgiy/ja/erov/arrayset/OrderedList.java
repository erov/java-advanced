package info.kgeorgiy.ja.erov.arrayset;

import java.util.*;

class OrderedList<E> extends AbstractList<E> implements RandomAccess {
    protected final List<E> list;
    protected boolean descending;

    protected OrderedList(final List<E> list, boolean descending) {
        this.list = list;
        this.descending = descending;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(int index) {
        return list.get(descending ? size() - 1 - index : index);
    }
}
