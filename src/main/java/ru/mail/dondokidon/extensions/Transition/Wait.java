package ru.mail.dondokidon.extensions.Transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import androidx.transition.TransitionValues;
import androidx.transition.Visibility;

import android.view.View;
import android.view.ViewGroup;

/**
 * Transition to make view remain visible until animation end.
 */
public class Wait extends Visibility {

    public Animator createAnimation(View v) {
        ValueAnimator VA = ValueAnimator.ofFloat(0f, 1f);
        VA.addListener(new Listener(v));
        return VA;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return createAnimation(view);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return createAnimation(view);
    }

    private static class Listener extends AnimatorListenerAdapter {
        private final View mView;
        private boolean mLayerTypeChanged = false;
        public Listener(View view) {
            mView = view;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (mView.hasOverlappingRendering() && mView.getLayerType() == View.LAYER_TYPE_NONE) {
                mLayerTypeChanged = true;
                mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (mLayerTypeChanged) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }
}
