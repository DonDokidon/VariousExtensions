package ru.mail.dondokidon.extensions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * Class that allows you to sort by multiple attributes.
 *
 * <h3>How it works:</h3>
 * 1. Perform sorting by sortingDegree.
 * 2. Equal items (by result of this degree comparison) are separated into subgroups.
 * 3. If (sortingDegree < getSortingDegreesCount() - 1) for each subgroup goto (1)
 * with sortingDegree++.
 */
public class MultiCompare {

    /**
     * Class for block header, which will be returned among sorted data by
     * {@link #sortNamed} methods
     */
    public static class SortedBlockName{
        public int Degree;
        public String BlockName;

        SortedBlockName(int Degree, String BlockName){
            this.Degree = Degree;
            this.BlockName = BlockName;
        }

        @NotNull
        @Override
        public String toString() {
            return "Header (Degree: " + Degree + " Name: " + BlockName + ")";
        }
    }

    /**
     * Interface that class must implement to be able to compare its objects.
     *
     * @param <SortingType> The type of objects that this object may be compared to
     * @see Comparable
     */
    public interface MultiComparable<SortingType>{
        /**
         * Compare method.
         *
         * @param obj Object to which this object should be compared
         * @param sortingDegree Compare degree
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @see Comparable#compareTo(Object)
         */
        int compareTo(@NotNull SortingType obj, int sortingDegree);

        /**
         * Get count degrees
         *
         * @return Count of sorting degrees
         */
        int getSortingDegreesCount();

        /**
         * Get name for this object subgroup for passed sorting degree.
         *
         * @param sortingDegree Sorting degree
         * @return Name
         * @see #sortNamed
         */
        String getBlockName(int sortingDegree);
    }

    /**
     * Interface that able to compare two class objects.
     *
     * @param <SortingType>
     */
    public interface MultiComparator<SortingType>{
        /**
         * Compare method.
         *
         * @param obj1 Object 1 to compare
         * @param obj2 Object 2 to compare
         * @param sortingDegree Compare degree
         * @return A negative integer, zero, or a positive integer as the
         * first argument is less than, equal to, or greater than the
         * second.
         * @see Comparator#compare(Object, Object)
         */
        int compare(@NotNull SortingType obj1, @NotNull SortingType obj2, int sortingDegree);

        /**
         * Get count of degrees
         *
         * @return Count of sorting degrees
         */
        int getSortingDegreesCount();
        /**
         * Get name for passed object subgroup for passed sorting degree.
         *
         * @param obj Object which subgroup name needed
         * @param sortingDegree Sorting degree
         * @return Name
         * @see #sortNamed
         */
        String getBlockName(SortingType obj, int sortingDegree);
    }

    private static class SortedGroupNamePosition {
        int Position;
        int Degree;

        SortedGroupNamePosition(int Pos, int Deg){
            Position = Pos;
            Degree = Deg;
        }
    }

    /**
     * Sort array and name each subgroup of sorted data
     *
     * @param mas Array to sort (will be sorted in result as well)
     * @param <SortingType> The type of objects that will be compared
     * @return Sorted array with named subgroups
     */
    public static <SortingType extends MultiComparable<? super SortingType>> ArrayList<Object> sortNamed(
            @NotNull List<SortingType> mas) {
        Object[] pep = mas.toArray();

        ArrayList<Object> Return = rsortAndReturnArrayWithNamedBlocks(pep, null);

        ListIterator<SortingType> iter = mas.listIterator();
        for (Object item : pep) {
            iter.next();
            //All items in Temp are SortingType class.
            @SuppressWarnings("unchecked")
            SortingType Cast = (SortingType) item;
            iter.set(Cast);
        }

        return Return;
    }


    /**
     * Sort array and name each subgroup of sorted data
     *
     * @param mas Array to sort (will be sorted in result as well)
     * @param Comparator {@link MultiComparator} which will compare objects in array
     * @param <SortingType> The type of objects that will be compared
     * @return Sorted array with named subgroups
     */
    public static <SortingType> ArrayList<Object> sortNamed(
            @NotNull List<SortingType> mas, @NotNull MultiComparator<? super SortingType> Comparator){
        //All items in Temp are SortingType class.
        @SuppressWarnings("unchecked")
        SortingType[] HeapPollutionTemp = (SortingType[]) mas.toArray();

        ArrayList<Object> Return = rsortAndReturnArrayWithNamedBlocks(HeapPollutionTemp, Comparator);

        ListIterator<SortingType> iter = mas.listIterator();
        for (SortingType item : HeapPollutionTemp) {
            iter.next();
            iter.set(item);
        }

        return Return;
    }

    /**
     * Sort array and name each subgroup of sorted data
     *
     * @param mas Array to sort (will be sorted in result as well)
     * @param <SortingType> The type of objects that will be compared
     * @return Sorted array with named subgroups
     */
    public static <SortingType extends MultiComparable<? super SortingType>> ArrayList<Object> sortNamed(
            @NotNull SortingType[] mas) {
        return rsortAndReturnArrayWithNamedBlocks(mas, null);
    }

    /**
     * Sort array and name each subgroup of sorted data
     *
     * @param mas Array to sort (will be sorted in result as well)
     * @param Comparator {@link MultiComparator} which will compare objects in array
     * @param <SortingType> The type of objects that will be compared
     * @return Sorted array with named subgroups
     */
    public static <SortingType> ArrayList<Object> sortNamed(
            @NotNull SortingType[] mas, @NotNull MultiComparator<? super SortingType> Comparator){
        return rsortAndReturnArrayWithNamedBlocks(mas, Comparator);
    }

    @SuppressWarnings("unchecked") //Everything here has been checked before.
    private static <SortingType> ArrayList<Object> rsortAndReturnArrayWithNamedBlocks (
            @NotNull SortingType[] mas, @Nullable MultiComparator<? super SortingType> Comparator){
        if (mas.length == 0)
            return new ArrayList<>();
        ArrayList<SortedGroupNamePosition> Borders = rsort(mas, Comparator);

        int SortingDegreesCount;
        if (Comparator != null)
            SortingDegreesCount = Comparator.getSortingDegreesCount();
        else
            SortingDegreesCount = ((MultiComparable)mas[0]).getSortingDegreesCount();
        ArrayList<Object> Return = new ArrayList<>();
        Collections.addAll(Return, mas);
        for (int i = 0; i < Borders.size() - 1; i++){
            SortedGroupNamePosition Pos = Borders.get(i);
            for (int q = Pos.Degree, AddsCount = 0; q < SortingDegreesCount; q++, AddsCount++){
                String BlockName;
                if (Comparator != null)
                    BlockName = Comparator.getBlockName((SortingType) Return.get(Pos.Position + 1 + AddsCount), q);
                else
                    BlockName = ((MultiComparable)Return.get(Pos.Position + 1 + AddsCount)).getBlockName(q);
                Return.add(Pos.Position + 1 + AddsCount, new SortedBlockName(q, BlockName));
            }
            for (int q = i + 1; q < Borders.size(); q++)
                Borders.get(q).Position += SortingDegreesCount - Pos.Degree;
        }
        return Return;
    }

    /**
     * Sort array
     *
     * @param mas Array to sort
     * @param Comparator {@link MultiComparator} which will compare objects in array
     * @param <SortingType> The type of objects that will be compared
     */
    public static <SortingType> void sort(@NotNull SortingType[] mas, @NotNull MultiComparator<? super SortingType> Comparator){
        rsort(mas, Comparator);
    }

    /**
     * Sort array
     *
     * @param mas Array to sort
     * @param <SortingType> The type of objects that will be compared
     */
    public static <SortingType extends MultiComparable<? super SortingType>> void sort(@NotNull List<SortingType> mas){
        Object[] Temp = mas.toArray();
        rsort(Temp, null);

        ListIterator<SortingType> iter = mas.listIterator();
        for (Object item : Temp) {
            iter.next();
            //All items in Temp are SortingType class.
            @SuppressWarnings("unchecked")
            SortingType Cast = (SortingType) item;
            iter.set(Cast);
        }
    }

    /**
     * Sort array
     *
     * @param mas Array to sort
     * @param Comparator {@link MultiComparator} which will compare objects in array
     * @param <SortingType> The type of objects that will be compared
     */
    public static <SortingType> void sort(
            @NotNull List<SortingType> mas, @NotNull MultiComparator<? super SortingType> Comparator){
        //All items in Temp are SortingType class.
        @SuppressWarnings("unchecked")
        SortingType[] HeapPollutionTemp = (SortingType[]) mas.toArray();
        rsort(HeapPollutionTemp, Comparator);

        ListIterator<SortingType> iter = mas.listIterator();
        for (SortingType item : HeapPollutionTemp) {
            iter.next();
            iter.set((SortingType)item);
        }
    }

    /**
     * Sort array
     *
     * @param mas Array to sort
     * @param <SortingType> The type of objects that will be compared
     */
    public static <SortingType extends MultiComparable<? super SortingType>> void sort(@NotNull SortingType[] mas){
        rsort(mas, null);
    }

    @SuppressWarnings("unchecked") //Everything here has been checked before.
    private static <ComparatorSortingType, SortingType extends ComparatorSortingType> ArrayList<SortedGroupNamePosition> rsort(
            @NotNull SortingType[] mas, @Nullable final MultiComparator<ComparatorSortingType> Comparator){
        if (mas.length == 0)
            return new ArrayList<>();
        ArrayList<SortedGroupNamePosition> Borders = new ArrayList<>();
        Borders.add(new SortedGroupNamePosition(-1, 0));
        Borders.add(new SortedGroupNamePosition(mas.length - 1, 0));

        OriginalComparator<ComparatorSortingType> OCompar = new OriginalComparator<ComparatorSortingType>() {
            @Override
            public int compare(ComparatorSortingType o1, ComparatorSortingType o2) {
                if (Comparator != null)
                    return Comparator.compare(o1, o2, Degree);
                else
                    return ((MultiComparable<ComparatorSortingType>) o1).compareTo(o2, Degree);
            }
        };

        int SortingDegreesCount;
        if (Comparator != null)
            SortingDegreesCount = Comparator.getSortingDegreesCount();
        else
            SortingDegreesCount = ((MultiComparable)mas[0]).getSortingDegreesCount();
        for (int degree = 0; degree < SortingDegreesCount; degree++) {
            for (int i = 0; i <= Borders.size() - 2; i++) {
                OCompar.setDegree(degree);
                Arrays.sort(mas, Borders.get(i).Position + 1, Borders.get(i + 1).Position + 1, OCompar);
            }
            ArrayList<SortedGroupNamePosition> TempBorders = new ArrayList<>(Borders);
            int subSortingsCount = 0;
            for (int i = 0; i <= TempBorders.size() - 2; i++){
                subSortingsCount++;
                int leftBorder = TempBorders.get(i).Position + 1, rightBorder = TempBorders.get(i + 1).Position;
                for (int q = leftBorder; q <= rightBorder - 1; q++){
                    if (Comparator != null) {
                        if (Comparator.compare(mas[q], mas[q + 1], degree) != 0) {
                            Borders.add(subSortingsCount, new SortedGroupNamePosition(q, degree));
                            subSortingsCount++;
                        }
                    }else
                        if ((((MultiComparable<ComparatorSortingType>)mas[q]).compareTo(mas[q + 1], degree) != 0)){
                            Borders.add(subSortingsCount, new SortedGroupNamePosition(q, degree));
                            subSortingsCount++;
                        }
                }
            }
        }
        return Borders;
    }


    private static abstract class OriginalComparator <ComparatorSortingType> implements Comparator<ComparatorSortingType>{
        int Degree;

        void setDegree(int degree) {
            Degree = degree;
        }

        abstract public int compare(ComparatorSortingType o1, ComparatorSortingType o2);
    }
}
