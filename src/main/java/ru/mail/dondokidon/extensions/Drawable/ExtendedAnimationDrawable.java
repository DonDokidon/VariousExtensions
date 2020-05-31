package ru.mail.dondokidon.extensions.Drawable;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.animation.LinearInterpolator;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

/**
 * {@link AnimationDrawable} child class which provides methods to work with animation:
 * <ul>
 *     <li>Add listener and other interactions with animator</li>
 *     <li>Reverse animation from any current state</li>
 * </ul>
 */
public class ExtendedAnimationDrawable extends AnimationDrawable {
    private static final String TAG = "ExtendedAnimationDrawable";

    public static final int NO_FRAME = -1;

    /**
     * Directions of animation
     */
    public enum AnimationDirection{
        /**
         * 0..getNumberOfFrames()-1
         */
        FORWARD,
        /**
         * getNumberOfFrames()-1..0
         */
        BACKWARD;
    }

    private int CurrentFrameIndex = 0;
    private AnimationDirection CurrentAnimationDirection = AnimationDirection.FORWARD;

    private boolean isPlayed = false;

    private boolean isReverseOnEnd = true;
    private boolean isRuntimeReverse = true;

    private ValueAnimator animator = new ValueAnimator();
    
    private ArrayList<OnFrameChangedListener> onFrameChangedListeners = new ArrayList<>();
    
    public interface OnFrameChangedListener{
        void onFrameChanged(int OldFrameIndex, int NewFrameIndex);
    }

    public ExtendedAnimationDrawable(Resources resources, int resourceID, Resources.Theme theme) {
        Drawable drawable = resources.getDrawable(resourceID, theme);
        if (drawable instanceof  AnimationDrawable){
            init(((AnimationDrawable) drawable));
        } else
            throw new RuntimeException(TAG + ": Loaded resource is not instance of AnimationDrawable");
    }

    public ExtendedAnimationDrawable(AnimationDrawable animationDrawable){
        init(animationDrawable);
    }

    private void init (AnimationDrawable animationDrawable){
        setOneShot(animationDrawable.isOneShot());

        int NumberOfFrames = animationDrawable.getNumberOfFrames();
        int SumDuration = 0;
        for (int i = 0; i < NumberOfFrames; i++) {
            int Duration = animationDrawable.getDuration(i);
            addFrame(animationDrawable.getFrame(i), Duration);
            SumDuration+= Duration;
        }
        animator.setDuration(SumDuration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int index = ((Integer) animation.getAnimatedValue());
                if (index == getCurrentFrameIndex())
                    return;
                selectDrawable(index);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isReverseOnEnd) {
                    CurrentAnimationDirection = CurrentAnimationDirection == AnimationDirection.FORWARD ?
                            AnimationDirection.BACKWARD : AnimationDirection.FORWARD;
                }
                isPlayed = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                switch (CurrentAnimationDirection){
                    case FORWARD:
                        selectDrawable(0);
                        break;

                    case BACKWARD:
                        selectDrawable(getNumberOfFrames() - 1);
                        break;
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * Start animation.
     *
     * If {@link #isStarted()} is <t>true</t> then if {@link #isOneShot()} is <t>false</t> and
     * {@link #isRuntimeReverse()} is <t>true</t> then current animation
     * will be reversed, else {@link #cancel()} will be called and animation will be started
     * from begin.
     */
    @Override
    public void start() {
        if (isVisible() && !(isOneShot() && isPlayed)) {
            if (animator.isStarted()){
                if (isRuntimeReverse && !isOneShot()){
                    CurrentAnimationDirection = CurrentAnimationDirection.equals(AnimationDirection.FORWARD) ?
                            AnimationDirection.BACKWARD : AnimationDirection.FORWARD;
                    animator.reverse();
                } else {
                    cancel();
                    animator.start();

                }
            } else {
                switch (CurrentAnimationDirection) {
                    case FORWARD:
                        animator.setIntValues(0, getNumberOfFrames() - 1);
                        break;

                    case BACKWARD:
                        animator.setIntValues(getNumberOfFrames() - 1, 0);
                        break;
                }
                animator.start();
            }
        }
    }

    /**
     * End animation. Immediately set animation to end value
     *
     * @see ValueAnimator#end()
     */
    @Override
    public void stop() {
        if (animator.isStarted())
            animator.end();
    }

    /**
     * Cancel animation
     *
     * @see ValueAnimator#cancel()
     */
    public void cancel(){
        if (animator.isStarted())
            animator.cancel();
    }

    /**
     * @see ValueAnimator#isRunning()
     */
    @Override
    public boolean isRunning() {
        return animator.isRunning();
    }

    /**
     * Set duration of animation
     *
     * @param duration Animation duration
     */
    public void setDuration(long duration){
        animator.setDuration(duration);
    }

    /**
     * Get duration of animation
     *
     * @return Animation duration
     */
    public long getDuration(){
        return animator.getDuration();
    }

    /**
     * Add listener to animator
     *
     * @param listener Listener to add
     * @see ValueAnimator#addListener(Animator.AnimatorListener)
     */
    public void addListener(Animator.AnimatorListener listener){
        animator.addListener(listener);
    }

    /**
     * Remove listener from animator
     *
     * @param listener Listener to remove
     * @see ValueAnimator#removeListener(Animator.AnimatorListener)
     */
    public void removeListener(Animator.AnimatorListener listener){
        animator.removeListener(listener);
    }

    /**
     * Add listener to frame change
     *
     * @param listener Listener to add
     */
    public void addOnFrameChangedListener(OnFrameChangedListener listener){
        onFrameChangedListeners.add(listener);
    }

    /**
     * Remove listener from frame change
     *
     * @param listener Listener to remove
     */
    public void removeOnFrameChangedListener(OnFrameChangedListener listener){
        onFrameChangedListeners.remove(listener);
    }

    private void triggerOnFrameChangedListeners(int OldFrameIndex, int NewFrameIndex){
        for (int i = onFrameChangedListeners.size() - 1; i >= 0; i--){
            onFrameChangedListeners.get(i).onFrameChanged(OldFrameIndex, NewFrameIndex);
        }
    }

    /**
     * Set current frame. If there is no frame with passed index set it to {@link #NO_FRAME}
     *
     * @param index Index of frame
     * @return True if successfully set
     * @see AnimationDrawable#selectDrawable(int)
     */
    @Override
    public boolean selectDrawable(int index) {
        boolean ret = super.selectDrawable(index);
        int OldFrameIndex = CurrentFrameIndex;
        if (ret)
            CurrentFrameIndex = index;
        else
            CurrentFrameIndex = NO_FRAME;
        triggerOnFrameChangedListeners(OldFrameIndex, CurrentFrameIndex);
        return ret;
    }

    /**
     * Get currently set frame index
     *
     * @return Currently sef frame index
     */
    public int getCurrentFrameIndex(){
        return CurrentFrameIndex;
    }

    /**
     * Is animation will switch its direction on end.
     *
     * @return True if animation will switch its direction on end
     */
    public boolean isReverseOnEnd() {
        return isReverseOnEnd;
    }

    /**
     * Set if animation should switch its direction on end.
     *
     * @param isReverseOnEnd True if animation should switch its direction on end
     */
    public void setReverseOnEnd(boolean isReverseOnEnd){
        this.isReverseOnEnd = isReverseOnEnd;
    }

    /**
     * Set {@link AnimationDirection} in which animation should play.
     *
     * If passed {@link AnimationDirection} isn't equals current animation direction and
     * {@link #isStarted()}, {@link #isRuntimeReverse} are <t>true</t>, {@link #isOneShot()}
     * is <t>false</t>, current animation play will reverse.
     *
     * @param animationDirection {@link AnimationDirection} in which animation should play.
     */
    public void setAnimationDirection(AnimationDirection animationDirection){
        if (CurrentAnimationDirection.equals(animationDirection))
            return;
        CurrentAnimationDirection = animationDirection;
        if (animator.isStarted() && isRuntimeReverse && !isOneShot()){
            animator.reverse();
        }
    }

    /**
     * Get current {@link AnimationDirection} in which animation will play/playing.
     *
     * @return Current {@link AnimationDirection} in which animation will play/playing.
     */
    public AnimationDirection getAnimationDirection(){
        return CurrentAnimationDirection;
    }

    /**
     * @see ValueAnimator#setRepeatCount(int) 
     */
    public void setRepeatCount(int count){
        animator.setRepeatCount(count);
    }

    /**
     * @see ValueAnimator#getRepeatCount()
     */
    public int getRepeatCount(){
        return animator.getRepeatCount();
    }

    /**
     * @see ValueAnimator#setRepeatMode(int)
     */
    public void setRepeatMode(int repeatMode){
        animator.setRepeatMode(repeatMode);
    }

    /**
     * @see ValueAnimator#getRepeatMode()
     */
    public int getRepeatMode(){
        return animator.getRepeatCount();
    }

    /**
     * @see ValueAnimator#setStartDelay(long)
     */
    public void setStartDelay(long delay){
        animator.setStartDelay(delay);
    }

    /**
     * @see ValueAnimator#getStartDelay()
     */
    public long getStartDelay(){
        return animator.getStartDelay();
    }

    /**
     * @see ValueAnimator#isStarted()
     */
    public boolean isStarted(){
        return animator.isStarted();
    }

    /**
     * Is animation able to reverse its direction from current frame during runtime.
     *
     * @return True if able
     */
    public boolean isRuntimeReverse() {
        return isRuntimeReverse;
    }

    /**
     * Set animation to be able to reverse its direction from current frame during runtime.
     *
     * @param isRuntimeReverse True if able
     */
    public void setRuntimeReverse(boolean isRuntimeReverse){
        this.isRuntimeReverse = isRuntimeReverse;
    }

    /**
     * Move to last frame of animation
     */
    public void moveToEnd(){
        selectDrawable(getNumberOfFrames() - 1);
    }

    /**
     * Move to first frame of animation
     */
    public void moveToStart(){
        selectDrawable(0);
    }
}
