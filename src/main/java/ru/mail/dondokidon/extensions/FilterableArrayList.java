package ru.mail.dondokidon.extensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Extended ArrayList class which provide methods to filter content without changes in real
 * data set.
 *
 * @param <mClass> Class of stored data
 */
public class FilterableArrayList <mClass>extends ArrayList<mClass> {
    private static final String TAG ="FilterableArrayList";

    private ArrayList<DataHolder> FilterResult = new ArrayList<>();
    private FilterInterface<? super mClass> Filter = null;
    private String FilterString = "";

    /**
     * @see ArrayList#ArrayList(int)
     */
    public FilterableArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * @see ArrayList#ArrayList()
     */
    public FilterableArrayList() {
        super();
    }

    /**
     * @see ArrayList#ArrayList(Collection)
     */
    public FilterableArrayList(@NonNull Collection<? extends mClass> c) {
        super(c);
        filterContent();
    }

    /**
     * Get size of filter result list.
     *
     * @return Size of filter result list
     */
    public int filterResultSize(){
        return FilterResult.size();
    }

    /**
     * @see ArrayList#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if filter result list contains no elements.
     *
     * @return <tt>true</tt> if filter result list contains no elements.
     */
    public boolean isFilterResultEmpty(){
        return FilterResult.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if filter result list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if filter result list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in filter result list is to be tested
     * @return <tt>true</tt> if filter result list contains the specified element
     */
    public boolean filterResultContains(@Nullable Object o) {
        return FilterResult.contains(o);
    }

    @NonNull
    @Override
    public Object clone() {
        FilterableArrayList<mClass> clone = new FilterableArrayList<>(this);
        clone.FilterResult = new ArrayList<>(FilterResult);
        clone.Filter = Filter;
        clone.FilterString = FilterString;
        return clone;
    }

    /**
     * Returns the element at the specified position in filter result list.
     *
     * @param  index index of the element to return
     * @return the element at the specified position in filter result list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public mClass filterResultGet(int index){
        return FilterResult.get(index).Data;
    }

    /**
     * @see ArrayList#set(int, Object)
     */
    @Override
    public mClass set(int index, mClass element) {
        mClass Item = super.set(index, element);
        if (Filter != null){
            if (Filter.filter(element, FilterString)) {
                int FilterResultChildPosition = findFilterResultChildPositionByRealPosition(index);
                if (FilterResultChildPosition != -1) {
                    FilterResult.set(FilterResultChildPosition, new DataHolder(index, element));
                } else {
                    addFilterResultItem(new DataHolder(index, element));
                }
            } else {
                if (Filter.filter(Item, FilterString))
                    removeFilterResultItem(index);
            }
        } else {
            int FilterResultChildPosition = findFilterResultChildPositionByRealPosition(index);
            FilterResult.set(FilterResultChildPosition, new DataHolder(index, element));
        }
        return Item;
    }

    /**
     * @see ArrayList#add(Object)
     */
    @Override
    public boolean add(mClass o) {
        boolean ret = super.add(o);
        if (Filter == null || Filter.filter(o, FilterString)){
            FilterResult.add(new DataHolder(size() - 1, o));
        }
        return ret;
    }

    /**
     * @see ArrayList#add(int, Object)
     */
    @Override
    public void add(int index, mClass element) {
        super.add(index, element);
        if (Filter == null || Filter.filter(element, FilterString)){
            for (DataHolder Item : FilterResult)
                if (Item.position >= index)
                    Item.position++;
            addFilterResultItem(new DataHolder(index, element));
        }
    }

    /**
     * @see ArrayList#remove(int)
     */
    @Override
    public mClass remove(int index) {
        mClass Item = super.remove(index);
        removeFilterResultItem(index);
        for (DataHolder mData : FilterResult)
            if (mData.position > index)
                mData.position--;
        return Item;
    }

    /**
     * @see ArrayList#remove(Object)
     */
    @Override
    public boolean remove(@Nullable Object o) {
        int indexOf = indexOf(o);
        boolean ret = super.remove(o);
        if (ret) {
            removeFilterResultItem(indexOf);
            for (DataHolder mData : FilterResult)
                if (mData.position > indexOf)
                    mData.position--;
        }
        return ret;
    }

    /**
     * @see ArrayList#clear()
     */
    @Override
    public void clear() {
        super.clear();
        FilterResult.clear();
    }

    /**
     * @see ArrayList#addAll(Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(@NonNull Collection<? extends mClass> c) {
        int Size = size();
        boolean ret = super.addAll(c);
        int i = 0;
        if (Filter == null) {
            for (mClass o : (mClass[]) c.toArray()) {
                addFilterResultItem(new DataHolder(Size + i, o));
                i++;
            }
        } else {
            for (mClass o : (mClass[]) c.toArray()) {
                if (Filter.filter(o, FilterString)) {
                    addFilterResultItem(new DataHolder(Size + i, o));
                }
                i++;
            }
        }
        return ret;
    }

    /**
     * @see ArrayList#addAll(int, Collection)
     */
    @Override
    public boolean addAll(int index, @NonNull Collection<? extends mClass> c) {
        boolean ret = super.addAll(index, c);
        for (DataHolder Item : FilterResult)
            if (Item.position >= index)
                Item.position+= c.size();
        int i = 0;
        if (Filter == null){
            for (mClass o : c) {
                addFilterResultItem(new DataHolder(index + i, o));
                i++;
            }
        } else {
            for (mClass o : c) {
                if (Filter.filter(o, FilterString)) {
                    addFilterResultItem(new DataHolder(index + i, o));
                }
                i++;
            }
        }
        return ret;
    }

    /**
     * @see ArrayList#removeRange(int, int)
     */
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        int i = 0;
        ListIterator<mClass> mIterator = listIterator(fromIndex);
        if (mIterator.hasNext()) {
            for (mIterator.next(); mIterator.nextIndex() < toIndex; mIterator.next()) {
                int CurrentItemPos = mIterator.previousIndex() - i;
                removeFilterResultItem(CurrentItemPos);
                for (DataHolder mData : FilterResult) {
                    if (mData.position > CurrentItemPos)
                        mData.position--;
                }
                i++;
            }
        }
        super.removeRange(fromIndex, toIndex);
    }

    /**
     * @see ArrayList#removeAll(Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(@NonNull Collection c) {
        int i = 0;
        for (mClass o : ((Collection<mClass>) c)){
            int indexOf = indexOf(o) - i;
            removeFilterResultItem(indexOf);
            for (DataHolder mData : FilterResult) {
                if (mData.position > indexOf)
                    mData.position--;
            }
            i++;
        }
        return super.removeAll(c);
    }

    /**
     * @see ArrayList#retainAll(Collection)
     */
    @Override
    public boolean retainAll(@NonNull Collection c) {
        int i = 0;
        for (mClass o : this){
            if (!c.contains(o)){
                int indexOf = indexOf(o) - i;
                removeFilterResultItem(indexOf);
                for (DataHolder mData : FilterResult) {
                    if (mData.position > indexOf)
                        mData.position--;
                }
                i++;
            }
        }
        return super.retainAll(c);
    }

    /**
     * @see ArrayList#indexOf(Object)
     */
    @Override
    public int indexOf(@Nullable Object o) {
        return super.indexOf(o);
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in filter result list, or -1 if filter result list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     */
    public int filterResultIndexOf(@Nullable Object o) {
        return FilterResult.indexOf(o);
    }

    /**
     * @see ArrayList#lastIndexOf(Object)
     */
    @Override
    public int lastIndexOf(@Nullable Object o) {
        return super.lastIndexOf(o);
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in filter result list, or -1 if filter result list does not contain the element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     */
    public int filterResultLastIndexOf(@Nullable Object o) {
        return FilterResult.lastIndexOf(o);
    }

    /**
     * @see ArrayList#subList(int, int)
     */
    @NonNull
    @Override
    public List<mClass> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    /**
     * Returns a view of the portion of filter result list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
     * {@code fromIndex} and {@code toIndex} are equal, the returned list is
     * empty.)  The returned list is backed by filter result list, so non-structural
     * changes in the returned list are reflected in filter result list, and vice-versa.
     * The returned list supports all of the optional list operations.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public List<mClass> filterResultSubList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    /**
     * Get previously set {@link FilterInterface}
     *
     * @return Previously set {@link FilterInterface} or null
     */
    @Nullable
    public FilterInterface<? super mClass> getFilter() {
        return Filter;
    }

    /**
     * Set {@link FilterInterface} according which content will be filtered.
     *
     * @param filter {@link FilterInterface} or null
     * @see FilterInterface#filter(Object, String)
     */
    public void setFilter(@Nullable FilterInterface<? super mClass> filter) {
        Filter = filter;
        filterContent();
    }

    /**
     * Get previously set filter string.
     *
     * @return Previously set filter string or null.
     */
    public String getFilterString() {
        return FilterString;
    }

    /**
     * Set filter string according which content will be filtered.
     *
     * @param filterString String or null
     * @see FilterInterface#filter(Object, String)
     */
    public void setFilterString(@Nullable String filterString) {
        FilterString = filterString;
        filterContent();
    }

    private int findFilterResultChildPositionByRealPosition(int position){
        int i = 0;
        for (DataHolder o : FilterResult) {
            if (o.position == position)
                return i;
            i++;
        }
        return -1;
    }

    private void addFilterResultItem(DataHolder newItem){
        ListIterator<DataHolder> mIterator = FilterResult.listIterator();
        if (mIterator.hasNext()) {
            for (DataHolder mData = mIterator.next(); mIterator.hasNext(); mData = mIterator.next()) {
                if (newItem.position < mData.position) {
                    mIterator.previous();
                    mIterator.add(newItem);
                    return;
                }
            }
        }
        FilterResult.add(newItem);
    }

    private void removeFilterResultItem(int Position){
        for (DataHolder mData : FilterResult)
            if (mData.position == Position){
                FilterResult.remove(mData);
                return;
            }
    }

    private void filterContent(){
        FilterResult.clear();
        int i = 0;
        if (Filter == null || FilterString == null || FilterString.equals("")) {
            for (mClass Item : this) {
                FilterResult.add(new DataHolder(i, Item));
            }
        } else{
            for (mClass Item : this) {
                if (Filter.filter(Item, FilterString)) {
                    FilterResult.add(new DataHolder(i, Item));
                    i++;
                }
            }
        }
    }

    /**
     * Interface for filter.
     */
    public interface FilterInterface<mClass>{
        /**
         * Called to decide if Data pass the filter. If returns <tt>true</tt> for Data,
         * it will appear in result list, otherwise it will not.
         *
         * @param Data The object in question
         * @param FilterString The string according which Data should be filtered
         * @return Is Data should appear in result list
         */
        boolean filter(mClass Data, String FilterString);
    }

    class DataHolder{
        int position;
        mClass Data;

        DataHolder(int position, mClass Data){
            this.position = position;
            this.Data = Data;
        }
    }
}
