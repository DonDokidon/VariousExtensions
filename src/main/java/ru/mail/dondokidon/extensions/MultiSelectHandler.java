package ru.mail.dondokidon.extensions;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Class to store selections
 *
 * @param <mClass> Class which instances will be selected
 */
public class MultiSelectHandler <mClass>{
    private static final String TAG = "MultiSelectHandler";

    private ArrayList<mClass> SelectionsList = new ArrayList<>();
    private ArrayList<MultiSelectListener<mClass>> Listeners = new ArrayList<>();

    public interface MultiSelectListener <mClass>{
        void onMultiSelectStart(MultiSelectHandler<mClass> handler);
        void onMultiSelectFinish(MultiSelectHandler<mClass> handler);
        void onMultiSelectItemSelected(MultiSelectHandler<mClass> handler, mClass o);
        void onMultiSelectItemDeselected(MultiSelectHandler<mClass> handler, mClass o);
    }

    public void addListener(MultiSelectListener<mClass> listener){
        Listeners.add(0, listener);
    }

    public void removeListener(MultiSelectListener<mClass> listener){
        Listeners.remove(listener);
    }

    public void removeAllListeners(){
        Listeners.clear();
    }

    private void triggerListenersStart(){
        for(int i = Listeners.size() - 1; i >= 0; i--) {
            MultiSelectListener<mClass> Listener = Listeners.get(i);
            Listener.onMultiSelectStart(this);
        }
    }

    private void triggerListenersFinish(){
        for(int i = Listeners.size() - 1; i >= 0; i--) {
            MultiSelectListener<mClass> Listener = Listeners.get(i);
            Listener.onMultiSelectFinish(this);
        }
    }

    private void triggerListenersItemSelected(mClass o){
        for(int i = Listeners.size() - 1; i >= 0; i--) {
            MultiSelectListener<mClass> Listener = Listeners.get(i);
            Listener.onMultiSelectItemSelected(this,o);
        }
    }

    private void triggerListenersItemDeselected(mClass o){
        for(int i = Listeners.size() - 1; i >= 0; i--) {
            MultiSelectListener<mClass> Listener = Listeners.get(i);
            Listener.onMultiSelectItemDeselected(this, o);
        }
    }

    /**
     * Put object in list of selected objects.
     *
     * @param o Object to put
     */
    public void selectItem(@NotNull mClass o){
        if (SelectionsList.isEmpty())
            triggerListenersStart();
        else if (SelectionsList.contains(o))
            return;
        SelectionsList.add(o);
        triggerListenersItemSelected(o);
    }

    /**
     * Remove object from list of selected objects.
     *
     * @param o Object to remove
     */
    public void unselectItem(@NotNull mClass o){
        SelectionsList.remove(o);
        triggerListenersItemDeselected(o);
        if (SelectionsList.isEmpty())
            triggerListenersFinish();
    }

    /**
     * Is object contains in list of selected objects.
     *
     * @param o Object to check
     */
    public boolean isSelected(mClass o){
        return SelectionsList.contains(o);
    }

    /**
     * Clear list of selected objects
     */
    public void unselectAll(){
        if (!SelectionsList.isEmpty()) {
            ArrayList<mClass> SelectionsListCopy = new ArrayList<>(SelectionsList);
            SelectionsList.clear();
            for (int i = SelectionsListCopy.size() - 1; i >= 0; i--)
                triggerListenersItemDeselected(SelectionsListCopy.get(i));
            triggerListenersFinish();
        }
    }

    /**
     * Is in selection mode (Is list of selected items is not empty).
     *
     * @return True if in selection mode
     */
    public boolean isInSelectionMode(){
        return !SelectionsList.isEmpty();
    }

    /**
     * Get list of selected objects.
     *
     * @return List of selected objects
     */
    public ArrayList<mClass> getSelectionsList(){
        return new ArrayList<>(SelectionsList);
    }

    /**
     * Get count of selected objects.
     *
     * @return Count
     */
    public int getSelectionsCount(){
        return SelectionsList.size();
    }
}
