package ru.mail.dondokidon.extensions.View.ActionMode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Similar to {@link android.view.ActionMode} but with a bit more control on ActionMode state
 */
public class ActionMode {
    public static final String TAG = "ActionMode";

    private boolean isFinished = false;
    private boolean isStarted = false;

    private Toolbar target;
    private Toolbar ActionModeToolbar;
    private View.OnLayoutChangeListener onLayoutChangeListener;

    private Animator AppearAnimator;

    private Callback mCallback;

    private ArrayList<Listener> Listeners = new ArrayList<>();

    public interface Listener{
        void onStart();

        void onFinish();
    }


    /**
     * Interface to create action mode toolbar and animate its appearing and disappearing
     */
    public interface Callback {
        @NonNull
        Toolbar createToolbar(Context context);
        @Nullable
        Animator createAppearAnimator(Toolbar ActionModeToolbar);
        @Nullable
        Animator createDisappearAnimator(Toolbar ActionModeToolbar);
    }

    /**
     * Create action mode instance
     *
     * @param target Toolbar which should be replaced when ActionMode starts
     * @param callback {@link Callback}
     *
     * @return {@link ActionMode} instance
     */
    public static ActionMode createActionMode(@NotNull Toolbar target, @NotNull Callback callback){
        return new ActionMode(target, callback);
    }

    protected ActionMode(@NotNull Toolbar target, @NotNull Callback callback){
        Context context = target.getContext();
        this.target = target;
        mCallback = callback;


        ActionModeToolbar = mCallback.createToolbar(context);

        onLayoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                ViewParent parent = target.getParent();
                if (parent instanceof LinearLayout) {
                    LinearLayout.LayoutParams LP = ((LinearLayout.LayoutParams) ActionModeToolbar.getLayoutParams());
                    LP.width = target.getMeasuredWidth();
                    LP.height = target.getMeasuredHeight();
                    if (((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL) {
                        LP.topMargin = -LP.height;
                    } else {
                        LP.leftMargin = -LP.width;
                    }
                    ActionModeToolbar.setLayoutParams(LP);
                }
            }
        };
        target.addOnLayoutChangeListener(onLayoutChangeListener);
    }

    /**
     * Start action mode. If {@link #isStarted()} is <t>true</t> nothing will happen.
     */
    public void start(){
        if (!isStarted) {
            isStarted = true;
            layoutActionModeToolbar();
            AppearAnimator = mCallback.createAppearAnimator(ActionModeToolbar);
            if (AppearAnimator != null) {
                AppearAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        finish();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        finish();
                    }

                    private void finish() {
                        AppearAnimator = null;
                    }
                });
                AppearAnimator.start();
            }
            triggerOnStart();
        }
    }

    private void layoutActionModeToolbar(){
        ViewParent parent = target.getParent();

        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams LP = new LinearLayout.LayoutParams(target.getMeasuredWidth(), target.getMeasuredHeight());
            if (((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL) {
                LP.topMargin = -LP.height;
            } else {
                LP.leftMargin = -LP.width;
            }
            ActionModeToolbar.setLayoutParams(LP);
            int targetPosition = ((LinearLayout) parent).indexOfChild(target);
            ((LinearLayout) parent).addView(ActionModeToolbar, targetPosition + 1);
        }
    }

    /**
     * Get {@link Toolbar} which was returned by {@link Callback#createToolbar(Context)}
     *
     * @return Action mode toolbar
     */
    public Toolbar getActionModeToolbar(){
        return ActionModeToolbar;
    }

    /**
     * Is this action mode instance done its work. You can't use this {@link ActionMode} instance
     * again when it is finished.
     *
     * @return <t>True</t> if finished.
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Is this action mode instance started its work.
     *
     * @return <t>True</t> if {@link #start()} was called for this {@link ActionMode} instance
     */
    public boolean isStarted(){return isStarted;}

    /**
     * Finish action mode work. You can't use this {@link ActionMode} instance
     * again when it is finished.
     */
    public void finish(){
        if (!isFinished) {
            isFinished = true;

            triggerOnFinish();

            if (AppearAnimator != null)
                AppearAnimator.cancel();
            Animator DisappearAnimator = mCallback.createDisappearAnimator(ActionModeToolbar);
            if (DisappearAnimator != null) {
                DisappearAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        finish();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        finish();
                    }

                    private void finish() {
                        target.removeOnLayoutChangeListener(onLayoutChangeListener);
                        ((ViewGroup) ActionModeToolbar.getParent()).removeView(ActionModeToolbar);
                        ActionModeToolbar = null;
                    }
                });
                DisappearAnimator.start();
            } else {
                ActionModeToolbar = null;
                target.removeOnLayoutChangeListener(onLayoutChangeListener);
                ((ViewGroup) ActionModeToolbar.getParent()).removeView(ActionModeToolbar);
            }
        }
    }

    public void addListener(Listener listener){
        Listeners.add(0, listener);
    }

    public void removeListener(Listener listener){
        Listeners.remove(listener);
    }

    private void triggerOnStart(){
        for (int i = Listeners.size() - 1; i >= 0; i--){
            Listeners.get(i).onStart();
        }
    }

    private void triggerOnFinish(){
        for (int i = Listeners.size() - 1; i >= 0; i--){
            Listeners.get(i).onFinish();
        }
    }
}