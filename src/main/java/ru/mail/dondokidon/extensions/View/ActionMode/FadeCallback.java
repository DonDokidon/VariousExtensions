package ru.mail.dondokidon.extensions.View.ActionMode;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

/**
 * {@link ru.mail.dondokidon.extensions.View.ActionMode.ActionMode.Callback}
 * with fade appearing and disappearing animations.
 */
public abstract class FadeCallback implements ActionMode.Callback {

    private int AnimationDuration = 300;
    private long AppearPlaytime = 0;

    public FadeCallback(){}

    public FadeCallback(int AnimationDuration){
        this.AnimationDuration = AnimationDuration;
    }

    @NonNull
    @Override
    public abstract Toolbar createToolbar(Context context);

    @Nullable
    @Override
    public Animator createAppearAnimator(Toolbar ActionModeToolbar) {
        ValueAnimator AppearAnimator = ValueAnimator.ofFloat(0, 1);
        AppearAnimator.setDuration(AnimationDuration);
        AppearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            @SuppressLint("NewApi")
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ActionModeToolbar.setTransitionAlpha(((Float) valueAnimator.getAnimatedValue()));
                AppearPlaytime = valueAnimator.getCurrentPlayTime();
            }
        });
        return AppearAnimator;
    }

    @Nullable
    @Override
    public Animator createDisappearAnimator(Toolbar ActionModeToolbar) {
        ValueAnimator VA = ValueAnimator.ofFloat(1, 0);
        VA.setDuration(AnimationDuration);
        VA.setCurrentPlayTime(VA.getDuration() - AppearPlaytime);
        VA.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            @SuppressLint("NewApi")
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ActionModeToolbar.setTransitionAlpha(((Float) valueAnimator.getAnimatedValue()));
            }
        });
        return VA;
    }

    public int getAnimationDuration() {
        return AnimationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        AnimationDuration = animationDuration;
    }
}
