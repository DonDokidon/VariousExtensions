package ru.mail.dondokidon.extensions.Transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeImageTransform;
import androidx.transition.ChangeTransform;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionSet;
import androidx.transition.TransitionValues;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Transition set which contains main transitions for shared transition.
 *
 * <h3>Changes:</h3>
 * <ul>
 *    <li>In all contained transitions fixed bug when shared transition doesn't work if
 *    fragment change is result of {@link androidx.fragment.app.FragmentTransaction#show(Fragment)}
 *    and {@link androidx.fragment.app.FragmentTransaction#hide(Fragment)} instead of
 *    {@link androidx.fragment.app.FragmentTransaction#replace(int, Fragment)}.
 *    This bug appears when target fragment already created (called all methods
 *    which called before {@link Fragment#onStart()} (not sure which one exactly)).
 *    For some reason because of that
 *    {@link Transition#createAnimator(ViewGroup, TransitionValues, TransitionValues)}
 *    called twice, first call with null as third argument, second call with null as
 *    second argument. To fix that i catch both TransitionValues and pass them to super method.
 *    To make this fix work i had to block second call of this method, which means that you
 *    can't use {@link SharedElementTransition} to multiple targets.</li>
 *    <li>In {@link ChangeTransform} for {@link Build.VERSION_CODES#Q} fixed bug when attached overlay
 *    view doesn't detach after animation. It happens because {@link ViewGroupOverlay#remove(View)}
 *    was deprecated even though {@link ViewGroupOverlay#add(View)} is not. Fixed by
 *    {@link ViewGroupOverlay#clear()} call after animation.</li>
 *    <li>For {@link ChangeTransform} added feature to draw itself behind some views (by redrawing
 *    them above on overlay layer)</li>
 * </ul>
 */

public class SharedElementTransition extends TransitionSet {

    private FixedChangeBounds FCB;
    private FixedChangeTransform FCT;
    private FixedChangeImageTransform FCIT;
    private FixedFade FF;

    public SharedElementTransition() {
        super();
        init();
    }

    public SharedElementTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        FCB = new FixedChangeBounds();
        FCT = new FixedChangeTransform();
        FCIT = new FixedChangeImageTransform();
        FF = new FixedFade();
        setOrdering(TransitionSet.ORDERING_TOGETHER);
        addTransition(FCB);
        addTransition(FCT);
        addTransition(FCIT);
        addTransition(FF);
    }

    /**
     * @see ChangeTransform#setReparentWithOverlay(boolean)
     */
    public SharedElementTransition setReparentWithOverlay(boolean reparentWithOverlay) {
        FCT.setReparentWithOverlay(reparentWithOverlay);
        return this;
    }

    /**
     * Add view that should be drawn above.
     *
     * @param i Draw order, lowest number will be drawn first >=0
     * @param v View to draw
     * @return This
     * @throws IllegalStateException if called during animation.
     */
    public SharedElementTransition addViewAboveAnimation(int i, View v) {
        FCT.addViewAboveAnimation(i, v);
        return this;
    }

    /**
     * Remove view shouldn't be drawn above anymore.
     *
     * @param v View to remove
     * @return This
     * @throws IllegalStateException if called during animation.
     */
    public SharedElementTransition removeViewAboveAnimation(View v) {
        FCT.removeViewAboveAnimation(v);
        return this;
    }

    /**
     * Remove view shouldn't be drawn above anymore.
     *
     * @param i View draw order
     * @return This
     * @throws IllegalStateException if called during animation.
     */
    public SharedElementTransition removeViewAboveAnimation(int i) {
        FCT.removeViewAboveAnimation(i);
        return this;
    }

    static class FixedChangeTransform extends ChangeTransform {
        private boolean isRunning = false;

        private SparseArray<View> TopViews = new SparseArray<>();
        private ArrayMap<View, Integer> ViewsVisibility = new ArrayMap<>();
        private ArrayList<ShadowView> ShadowViews = new ArrayList<>();

        private TransitionValues startValues = null;
        private TransitionValues endValues = null;
        private boolean isCreated = false;

        FixedChangeTransform() {
            super();

            addListener(new TransitionListener() {
                @Override
                public void onTransitionStart(@NotNull Transition transition) {
                    isRunning = true;
                }

                @Override
                public void onTransitionEnd(@NotNull Transition transition) {
                    isRunning = false;
                }

                @Override
                public void onTransitionCancel(@NotNull Transition transition) {
                    isRunning = false;
                }

                @Override
                public void onTransitionPause(@NotNull Transition transition) {

                }

                @Override
                public void onTransitionResume(@NotNull Transition transition) {

                }
            });
        }

        @Override
        public void captureStartValues(@NotNull TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            startValues = transitionValues;
            endValues = null;
            isCreated = false;
        }

        @Override
        public void captureEndValues(@NotNull TransitionValues transitionValues) {
            super.captureEndValues(transitionValues);
            endValues = transitionValues;
        }

        @Override
        public Animator createAnimator(@NotNull final ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
            if ((startValues == null) && (this.startValues != null))
                startValues = this.startValues;
            if ((endValues == null) && (this.endValues != null))
                endValues = this.endValues;
            if (!isCreated) {
                isCreated = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    addListener(new TransitionListenerAdapter() {
                        @Override
                        public void onTransitionEnd(@NonNull Transition transition) {
                            super.onTransitionEnd(transition);
                            sceneRoot.getOverlay().clear();
                            removeListener(this);
                        }

                        @Override
                        public void onTransitionCancel(@NonNull Transition transition) {
                            super.onTransitionCancel(transition);
                            sceneRoot.getOverlay().clear();
                            removeListener(this);
                        }
                    });
                }
                Animator ret = super.createAnimator(sceneRoot, startValues, endValues);
                if (ret != null) {
                    ret.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            ViewGroupOverlay VOverlay = sceneRoot.getOverlay();
                            for (ShadowView SView : ShadowViews) {
                                VOverlay.remove(SView);
                            }
                            ShadowViews.clear();
                            for (int i = 0; i < TopViews.size(); i++) {
                                View v = TopViews.valueAt(i);
                                v.setVisibility(ViewsVisibility.get(v));

                            }
                            ViewsVisibility.clear();
                        }

                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            ViewGroupOverlay VOverlay = sceneRoot.getOverlay();
                            Context context = sceneRoot.getContext();
                            for (int i = 0; i < TopViews.size(); i++) {
                                View v = TopViews.valueAt(i);
                                ShadowView SView = new ShadowView(context, v);
                                VOverlay.add(SView);
                                ShadowViews.add(SView);
                                ViewsVisibility.put(v, v.getVisibility());
                                v.setVisibility(View.INVISIBLE);
                                SView.invalidate();
                            }
                        }
                    });
                    ((ObjectAnimator) ret).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            for (ShadowView SView : ShadowViews) {
                                SView.prepare();
                                SView.invalidate();
                            }
                        }
                    });
                }
                return ret;
            } else
                return null;
        }

        FixedChangeTransform addViewAboveAnimation(int i, View v) {
            if (i < 0)
                throw new ArrayIndexOutOfBoundsException(i);
            if (!isRunning) {
                int Index = TopViews.indexOfValue(v);
                if (Index != -1 && Index != i)
                    TopViews.removeAt(Index);
                TopViews.put(i, v);
            } else
                throw (new IllegalStateException("SharedElementTransition: addViewAboveAnimation call during animation run."));
            return this;
        }

        FixedChangeTransform removeViewAboveAnimation(View v) {
            if (!isRunning) {
                int Index = TopViews.indexOfValue(v);
                if (Index != -1)
                    TopViews.removeAt(Index);
            } else
                throw (new IllegalStateException("SharedElementTransition: removeViewAboveAnimation call during animation run."));
            return this;
        }

        FixedChangeTransform removeViewAboveAnimation(int i) {
            if (!isRunning) {
                TopViews.remove(i);
            } else
                throw (new IllegalStateException("SharedElementTransition: removeViewAboveAnimation call during animation run."));
            return this;
        }

        private static class ShadowView extends View {

            private final View RealView;

            ShadowView(Context context, View RealView) {
                super(context);
                this.RealView = RealView;
                prepare();
            }

            @SuppressLint("NewApi")
            private float getRealAlpha() {
                View v = RealView.getRootView();
                float realAlpha = RealView.getTransitionAlpha() * RealView.getAlpha();
                for (ViewParent parent = RealView.getParent(); parent != v; parent = parent.getParent()) {
                    realAlpha *= ((View) parent).getTransitionAlpha() * ((View) parent).getAlpha();
                    if (realAlpha == 0f)
                        break;
                }

                return realAlpha;
            }

            private void prepare() {
                int[] pos = new int[2];
                RealView.getLocationOnScreen(pos);
                setAlpha(getRealAlpha());
                measure(MeasureSpec.makeMeasureSpec(RealView.getWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(RealView.getHeight(), MeasureSpec.EXACTLY));
                layout(pos[0], pos[1], pos[0] + RealView.getWidth(), pos[1] + RealView.getHeight());
            }

            @Override
            public void draw(Canvas canvas) {
                super.draw(canvas);
                RealView.draw(canvas);
            }
        }
    }

    static class FixedChangeBounds extends ChangeBounds {
        private TransitionValues startValues = null;
        private TransitionValues endValues = null;
        private boolean isCreated = false;

        FixedChangeBounds() {
            super();
        }

        @Override
        public void captureStartValues(@NotNull TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            startValues = transitionValues;
            endValues = null;
            isCreated = false;
        }

        @Override
        public void captureEndValues(@NotNull TransitionValues transitionValues) {
            super.captureEndValues(transitionValues);
            endValues = transitionValues;
        }

        @Override
        public Animator createAnimator(@NotNull ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
            if ((startValues == null) && (this.startValues != null))
                startValues = this.startValues;
            if ((endValues == null) && (this.endValues != null))
                endValues = this.endValues;
            if (!isCreated) {
                isCreated = true;
                return super.createAnimator(sceneRoot, startValues, endValues);
            } else
                return null;
        }
    }

    static class FixedChangeImageTransform extends ChangeImageTransform {
        private TransitionValues startValues = null;
        private TransitionValues endValues = null;
        private boolean isCreated = false;

        FixedChangeImageTransform() {
            super();
        }

        @Override
        public void captureStartValues(@NotNull TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            startValues = transitionValues;
            endValues = null;
            isCreated = false;
        }

        @Override
        public void captureEndValues(@NotNull TransitionValues transitionValues) {
            super.captureEndValues(transitionValues);
            endValues = transitionValues;
        }

        @Override
        public Animator createAnimator(@NotNull ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
            if ((startValues == null) && (this.startValues != null))
                startValues = this.startValues;
            if ((endValues == null) && (this.endValues != null))
                endValues = this.endValues;
            if (!isCreated) {
                isCreated = true;
                return super.createAnimator(sceneRoot, startValues, endValues);
            } else
                return null;
        }
    }

    static class FixedFade extends Fade {
        private TransitionValues startValues = null;
        private TransitionValues endValues = null;
        private boolean isCreated = false;

        FixedFade() {
            super();
        }

        @Override
        public void captureStartValues(@NotNull TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            startValues = transitionValues;
            endValues = null;
            isCreated = false;
        }

        @Override
        public void captureEndValues(@NotNull TransitionValues transitionValues) {
            super.captureEndValues(transitionValues);
            endValues = transitionValues;
        }

        @Override
        public Animator createAnimator(@NotNull ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
            if ((startValues == null) && (this.startValues != null))
                startValues = this.startValues;
            if ((endValues == null) && (this.endValues != null))
                endValues = this.endValues;
            if (!isCreated) {
                isCreated = true;
                return super.createAnimator(sceneRoot, startValues, endValues);
            } else
                return null;
        }
    }
}
