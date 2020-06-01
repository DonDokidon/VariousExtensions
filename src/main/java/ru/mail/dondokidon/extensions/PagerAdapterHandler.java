package ru.mail.dondokidon.extensions;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

/**
 * Class to make your Object work with large data set. Use it to provide your Object
 * functional to change according current adapter position.
 *
 * <h3>Glossary of terms:</h3>
 * <ul>
 *     <li><em>Adapter:</em> A subclass of {@link Adapter} responsible for providing data
 *     that represent items in a data set.</li>
 *     <li><em>Position:</em> The position of a data item within an <em>Adapter</em>.</li>
 *     <li><em>Determined item:</em> Object that represent item at some <em>Position</em>
 *     of <em>Adapter</em> data set.</li>
 *     <li><em>Relative positions:</em> <em>Positions</em> located in the area bounded by the
 *     {@link #RelativityBorder} value around middle value (included)</li>
 * </ul>
 *
 * @param <T> Class that implements {@link PagerAdapterHandler.Target}
 * @param <IH> Class that extends {@link PagerAdapterHandler.ItemHolder}
 */
public class PagerAdapterHandler<T extends PagerAdapterHandler.Target,
        IH extends PagerAdapterHandler.ItemHolder> {
    private static final String  TAG = "AdapterHandler";
    /**
     * Can be returned by {@link Adapter#getItemPosition(ItemHolder)} to notify that ItemHolder
     * doesn't determine any item in data set.
     */
    public static final int POSITION_NONE = -1;
    private final int DefaultRelativityBorder = 1;
    private final SparseArray<IH> InfoHolder = new SparseArray<>();
    private Adapter<T, IH> mAdapter = null;
    private mObserver Observer = null;
    private int CPosition = POSITION_NONE;
    private boolean isCycled = false;
    private int RelativityBorder = DefaultRelativityBorder;
    private T Target = null;

    private boolean Debug = false;

    protected PagerAdapterHandler(){}


    /**
     * Create {@link PagerAdapterHandler} instance and attach it to target object
     *
     * @param target Object that will use {@link PagerAdapterHandler} features.
     * @param <T> Class that implements {@link PagerAdapterHandler.Target}
     * @param <IH> Class that extends {@link PagerAdapterHandler.ItemHolder}
     * @return AdapterHandler instance, attached to target object
     */
    public static <T extends PagerAdapterHandler.Target, IH extends ItemHolder> PagerAdapterHandler<T, IH> attachTo(
            @NonNull T target){
        PagerAdapterHandler<T, IH> instance = new PagerAdapterHandler<>();
        instance.Target = target;
        return instance;
    }

    /**
     * Create {@link PagerAdapterHandler} instance and attach it to target Object
     *
     * @param target Object that will use {@link PagerAdapterHandler} features.
     * @param isCycled Is returned {@link PagerAdapterHandler} should see data as cycled or not
     * @param <T> Class that implements {@link PagerAdapterHandler.Target}
     * @param <IH> Class that extends {@link PagerAdapterHandler.ItemHolder}
     * @return {@link PagerAdapterHandler} instance, attached to target Object
     */
    public static <T extends PagerAdapterHandler.Target, IH extends ItemHolder> PagerAdapterHandler<T, IH> attachTo(
            @NonNull T target, boolean isCycled){
        PagerAdapterHandler<T, IH> ret = attachTo(target);
        ret.setCycled(isCycled);
        return ret;
    }

    /**
     * Retrieves the previously set adapter or null if no adapter is set.
     *
     * @return The previously set adapter
     */
    @Nullable
    public Adapter<T, IH> getAdapter() {
        return mAdapter;
    }

    /**
     * Set a new adapter to provide data set to work with.
     *
     * @param newAdapter The new adapter to set, or null to set no adapter.
     */
    public void setAdapter(@Nullable Adapter<T, IH> newAdapter){
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(Observer);
            mAdapter.startUpdate(Target);
            int ItemCount = InfoHolder.size();
            for (int i = 0; i < ItemCount; i++) {
                IH unit = InfoHolder.valueAt(i);
                mAdapter.destroyItem(InfoHolder.indexOfValue(unit), unit);
            }
            mAdapter.finishUpdate(Target);
            mAdapter.onDetachedFromTarget(Target);
            InfoHolder.clear();
        }
        Adapter oldAdapter = mAdapter;
        mAdapter = newAdapter;
        if (newAdapter == null){
            CPosition = POSITION_NONE;
        } else {
            CPosition = 0;
        }
        if (mAdapter != null) {
            if (Observer == null) {
                Observer = new mObserver();
            }
            mAdapter.registerDataSetObserver(Observer);
            mAdapter.onAttachedToTarget(Target);
            preloadInfo();
        }
        Target.onAdapterChanged(oldAdapter, newAdapter);
    }

    /**
     * Set relativity border. Default value is 1, it means that
     * {@link PagerAdapterHandler} will store determined items for current
     * adapter position, 1 position before and 1 position after current adapter position
     * (3 in total).
     *
     * @param relativityBorder New relativity border value (? > 0)
     */
    public void setRelativityBorder(int relativityBorder) {
        RelativityBorder = Math.max(relativityBorder, DefaultRelativityBorder);
        preloadInfo();
    }

    /**
     * Get relativity border. Default value is 1.
     *
     * @return Currently set relativity border.
     * @see #setRelativityBorder(int)
     */
    public int getRelativityBorder(){
        return RelativityBorder;
    }

    /**
     * Count relative to passed value positions (passed value included)
     *
     * @param position Middle position
     * @return ArrayList of relative positions
     */
    private ArrayList<Integer> countRelativePositions(int position) {
        ArrayList<Integer> Positions = new ArrayList<>();
        int PosCount  = mAdapter.getCount();
        if (PosCount == 0)
            return Positions;
        for (int RelativePosition = -RelativityBorder; RelativePosition <= RelativityBorder; RelativePosition++) {
            int Position = position + RelativePosition;
            if (isCycled) {
                if (Position < 0) {
                    Position = PosCount + Position;
                    if (!Positions.contains(Position))
                        Positions.add(Position);
                    continue;
                }
                if (Position >= PosCount) {
                    Position = Position % PosCount;
                    if (!Positions.contains(Position))
                        Positions.add(Position);
                    continue;
                }
            }
            if (!Positions.contains(Position))
                Positions.add(Position);
        }
        if (Debug)
            Log.i(TAG, "Relative positions are: " + Positions);
        return Positions;
    }

    /**
     * Set current position to passed value without animation
     * {@see AdapterHandlerTarget#onPositionChanged(int, int, boolean)}.
     *
     * @param position New position
     * @return Is position changed
     */
    public boolean setPosition(int position){
        if (CPosition == position && mAdapter == null)
            return false;

        int oldPos = CPosition;
        preloadInfo(position);
        CPosition = position;

        if (Debug)
            Log.i(TAG, "Position set to: " + CPosition);

        Target.onPositionChanged(oldPos, position, false);
        destroyInfo(position);
        return true;
    }

    /**
     * Set current position without
     * {@link PagerAdapterHandler.Target#onPositionChanged(int, int, boolean)} call.
     *
     * @param position New position
     * @return Is position changed
     */
    public boolean setPositionWithoutNotification(int position){
        if (CPosition == position && mAdapter == null)
            return false;

        preloadInfo(position);
        CPosition = position;

        if (Debug)
            Log.i(TAG, "Position set without notification to: " + CPosition);

        destroyInfo(position);
        return true;
    }

    /**
     * Call {@link #moveTo(int)} for {@link #getNextPosition()} position;
     *
     * @return Is position changed
     */
    public boolean moveToNext(){
        return moveTo(getNextPosition());
    }

    /**
     * Call {@link #moveTo(int)} for {@link #getPrevPosition()} position;
     *
     * @return Is position changed
     */
    public boolean moveToPrev(){
        return moveTo(getPrevPosition());
    }

    /**
     * Get position after {@link #getPosition()}
     *
     * @return Position after {@link #getPosition()}. If current position is last then if
     * {@link #isCycled()} return first position, else return current position.
     *
     */
    public int getNextPosition(){
        if (isCycled)
            return CPosition == mAdapter.getCount() - 1 ? 0 : CPosition + 1;
        else
            return CPosition == mAdapter.getCount() - 1 ? mAdapter.getCount() - 1 : CPosition + 1;
    }

    /**
     * Get position before {@link #getPosition()}
     *
     * @return Position before {@link #getPosition()}. If current position is first then if
     * {@link #isCycled()} return last position, else return current position.
     *
     */
    public int getPrevPosition(){
        if (isCycled)
            return CPosition == 0 ? mAdapter.getCount() - 1 : CPosition - 1;
        else
            return CPosition == 0 ? 0 : CPosition - 1;
    }

    /**
     * Set current position to passed value with animation
     * {@see AdapterHandlerTarget#onPositionChanged(int, int, boolean)}.
     *
     * @param position New position
     * @return Is position changed
     */
    public boolean moveTo(int position){
        if (CPosition == position && mAdapter == null)
            return false;

        int oldPos = CPosition;
        preloadInfo(position);
        CPosition = position;

        if (Debug)
            Log.i(TAG, "Position moved to: " + CPosition);

        Target.onPositionChanged(oldPos, position, true);
        destroyInfo(position);
        return true;
    }

    /**
     * Is {@link PagerAdapterHandler} working with data set as cycled or not.
     *
     * @return Is {@link PagerAdapterHandler} cycled.
     */
    public boolean isCycled(){return isCycled;}

    /**
     * Make {@link PagerAdapterHandler} work with data set as cycled or not.
     *
     * @param cycled Is cycled
     */
    public void setCycled(boolean cycled) {
        isCycled = cycled;
    }

    private void invalidateInfo(){
        if (mAdapter == null || mAdapter.getCount() == 0)
            return;
        ItemHolder[] InfoHolderContent = new ItemHolder[InfoHolder.size()];
        int[] InfoHolderContentPositions = new int[InfoHolder.size()];
        mAdapter.startUpdate(Target);
        for (int i = 0; i < InfoHolder.size(); i++){
            InfoHolderContent[i] = InfoHolder.valueAt(i);
            InfoHolderContentPositions[i] = InfoHolder.keyAt(i);
            if (InfoHolderContentPositions[i] == CPosition) {
                //All items in InfoHolderContent are IH
                @SuppressWarnings("unchecked") IH IHItem = (IH)InfoHolderContent[i];
                int newPosition = mAdapter.getItemPosition(IHItem);
                if (newPosition == POSITION_NONE)
                    CPosition = 0;            //If current item disappeared then move to first position
                else
                    CPosition = newPosition; //else move to new position of current item
            }
        }
        final ArrayList<Integer> Positions = countRelativePositions(CPosition);
        for (int i = 0; i < InfoHolderContent.length; i++){
            //All items in InfoHolderContent are IH
            @SuppressWarnings("unchecked") IH IHItem = (IH)InfoHolderContent[i];
            int RealItemPosition = mAdapter.getItemPosition(IHItem);
            if (Positions.contains(RealItemPosition)){
                if (InfoHolderContentPositions[i] != RealItemPosition){
                    InfoHolder.remove(InfoHolderContentPositions[i]);
                    InfoHolderContent[i].position = RealItemPosition;
                    InfoHolder.put(RealItemPosition, IHItem);
                }
            } else {
                mAdapter.destroyItem(InfoHolderContentPositions[i], IHItem);
                InfoHolder.remove(InfoHolderContentPositions[i]);
            }
        }
        for(Integer position : Positions){
            if (InfoHolder.get(position) == null){
                IH item = mAdapter.instantiateItem(position);
                item.position = position;
                InfoHolder.put(position, item);
            }
        }
        mAdapter.finishUpdate(Target);


        if (Debug)
            Log.i(TAG, "Preloaded positions after invalidateInfo(): " + InfoHolder);
    }

    /**
     * {@link #preloadInfo(int)} around {@link #getPosition()}
     */
    private void preloadInfo() {
        preloadInfo(CPosition);
    }

    /**
     * Instantiate and store determined info in relative positions to passed position
     * if it is not stored yet.
     *
     * @param middlePos Middle position
     */
    private void preloadInfo(int middlePos) {
        if (mAdapter == null || (middlePos < 0 || middlePos > mAdapter.getCount()))
            return;
        final ArrayList<Integer> Positions = countRelativePositions(middlePos);
        mAdapter.startUpdate(Target);

        for (int Position : Positions) {
            boolean isPreloaded = false;
            IH Item = InfoHolder.get(Position);
            if (Item != null) {
//                if (position == mAdapter.getSongDataQueuePosition(Item)) {
                    isPreloaded = true;
//                }
            }
            if (!isPreloaded) {
                IH item = mAdapter.instantiateItem(Position);
                item.position = Position;
                InfoHolder.put(Position, item);
            }
        }

        mAdapter.finishUpdate(Target);

        if (Debug)
            Log.i(TAG, "Preloaded info after preloadInfo(): " + InfoHolder);
    }

    /**
     * {@link #destroyInfo(int)} around {@link #getPosition()}
     */
    private void destroyInfo(){
        destroyInfo(CPosition);
    }

    /**
     * Destroy and remove determined info in not relative positions to passed position.
     *
     * @param middlePos Middle position
     */
    private void destroyInfo(int middlePos){
        if (mAdapter == null || (middlePos < 0 || middlePos > mAdapter.getCount()))
            return;
        final ArrayList<Integer> Positions = countRelativePositions(middlePos);
        mAdapter.startUpdate(Target);
        int ChildCount = InfoHolder.size();
        for (int i = 0; i < ChildCount; i++) {
            boolean isUseful = false;
            int Key = InfoHolder.keyAt(i);
            IH unit = InfoHolder.get(Key);
            if (Positions.contains(Key)) {
//                int currentItemPos = mAdapter.getItemPosition(unit);
//                if (Key == currentItemPos) {
                    isUseful = true;
//                }
            }
            if (!isUseful) {
                mAdapter.destroyItem(Key, unit);
                InfoHolder.remove(Key);
                i--;
                ChildCount--;
            }
        }
        mAdapter.finishUpdate(Target);

        if (Debug)
            Log.i(TAG, "Preloaded info after destroyInfo(): " + InfoHolder);
    }

    private void dataSetChanged() {
        invalidateInfo();
        Target.onDataSetChanged();
    }

    /**
     * Get {@link ItemHolder} for passed position.
     *
     * If item with passed position is not preloaded, {@link Adapter#instantiateItem(int)}
     * will be called, which may be not a good idea for optimisation.
     *
     * @param position Position
     * @return ItemHolder for passed position
     */
    public IH get(int position){
        if (mAdapter == null)
            return null;
        if (position < 0 || position >= mAdapter.getCount())
            throw new ArrayIndexOutOfBoundsException();
        IH ret = InfoHolder.get(position);
        if (ret == null) {
//            throw new NullPointerException("Info is not loaded.");
            ret = mAdapter.instantiateItem(position);
            InfoHolder.append(position, ret);
        }

        if (Debug)
            Log.i(TAG, "Get " + position + " item: " + ret);

        return ret;
    }

    /**
     * Get current adapter position.
     *
     * @return Current adapter position
     */
    public int getPosition(){return CPosition;}

    /**
     * Get adapter position of passed {@link ItemHolder}.
     *
     * @param IHItem {@link ItemHolder} which position needed.
     * @return Position of passed {@link ItemHolder}.
     */
    public int getItemPosition(IH IHItem){
        return mAdapter.getItemPosition(IHItem);
    }

    /**
     * Get count of items in {@link Adapter} data set
     *
     * @return Count of items.
     */
    public int getCount(){
        if (mAdapter == null)
            return 0;
        return mAdapter.getCount();
    }

    /**
     * Interface that target Object must implement
     */
    public interface Target {
        /**
         * Called when position changes.
         *
         *  isAnimated flag tells that {@link PagerAdapterHandler.Target} should animate this
         *  position change. This flag passes directly from {@link #setPosition(int)} (false)
         *  and {@link #moveTo(int)} (true) and doesn't do anything in {@link PagerAdapterHandler}
         *  code, so it can be ignored.
         *
         * @param oldPosition Old position
         * @param newPosition New position
         * @param isAnimated Is change should be animated.
         */
        void onPositionChanged(int oldPosition, int newPosition, boolean isAnimated);
        void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter);
        void onDataSetChanged();
    }

    /**
     * Base Class for an Adapter
     *
     * @param <T> Class that implements {@link PagerAdapterHandler.Target}
     * @param <IH> Class that extends {@link PagerAdapterHandler.ItemHolder}
     */
    public static abstract class Adapter<T extends PagerAdapterHandler.Target,
            IH extends ItemHolder> {

        private DataSetObservable mObservable = new DataSetObservable();

        /**
         *  Get count of items in data set
         *
         * @return Count of items.
         */
        public abstract int getCount();

        /**
         * Called before determined items set update.
         *
         * @param target Object that using {@link PagerAdapterHandler} features.
         */
        public void startUpdate(T target) {
        }

        /**
         * Called when {@link PagerAdapterHandler} require determined item for specific position.
         *
         * @param position Position of required data.
         * @return Object that determines specific position
         */
        public abstract IH instantiateItem(int position);

        /**
         * Called when {@link PagerAdapterHandler} destroy determined item for specific position.
         *
         * @param position Position of destroyed data. It represents position in which data was
         *                 instantiated and may be different from current Item position.
         * @param Item Object that determines specific position
         */
        public abstract void destroyItem(int position, IH Item);

        /**
         * Called when {@link PagerAdapterHandler} want to know determined Item current position.
         *
         * @param Item Object which current position must be returned
         *             or {@link PagerAdapterHandler#POSITION_NONE} if Item doesn't exist in
         *             adapter data set anymore.
         * @return Current position of Item
         */
        public int getItemPosition(IH Item){return POSITION_NONE;}

        /**
         * Called after determined items set update.
         *
         * @param target Object that using {@link PagerAdapterHandler} features.
         */
        public void finishUpdate(T target) {
        }

        /**
         * Notify {@link PagerAdapterHandler} that data set has been changed
         */
        public void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        void registerDataSetObserver(DataSetObserver observer) {
            mObservable.registerObserver(observer);
        }
        void unregisterDataSetObserver(DataSetObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        /**
         * Called when {@link Adapter} attached to target Object
         *
         * @param Target Object that using {@link PagerAdapterHandler} features.
         */
        protected void onAttachedToTarget(T Target){}

        /**
         * Called when {@link Adapter} detached from target Object
         *
         * @param Target Object that using {@link PagerAdapterHandler} features.
         */
        protected void onDetachedFromTarget(T Target){}
    }
//
//    public int getItemPosition(Object Item){
//        if (mAdapter == null || Item == null)
//            return POSITION_NONE;
//        return mAdapter.getItemPosition(Item);
//    }

    /**
     * Base class for ItemHolder
     *
     * @param <C> Stored class
     */
    public static class ItemHolder<C>{
        C determinedItem;
        int position = POSITION_NONE;

        public ItemHolder(C determinedItem){
            this.determinedItem = determinedItem;
        }

        @NonNull
        @Override
        public String toString() {
            return "Item position: " + position + " / Item data: " + determinedItem;
        }

        public C getDeterminedItem() {
            return determinedItem;
        }

        public int getPosition() {
            return position;
        }
    }

    class mObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }
}
