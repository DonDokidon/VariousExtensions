package ru.mail.dondokidon.extensions;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import androidx.annotation.Nullable;

/**
 * Class to handle scroll. Provides methods to:
 * <ul>
 *     <li>Store start and current position of pointer.</li>
 *     <li>Determine scroll direction</li>
 *     <li>Get offset between start and current positions</li>
 *     <li>Emulate scroll in passed {@link Direction}</li>
 *     <li>Set interpolator for emulated scroll</li>
 * </ul>
 */
public class MotionController {
    private final static String TAG = "MotionController";
    private Point StartPointerPosition = null;
    private Point CurrentPointerPosition = null;
    private Interpolator AnimationInterpolator = null;
    private boolean isVerticalSwapAllowed = false;
    private boolean isHorizontalSwapAllowed = true;
    private boolean Debug = false;
    private Scroller mScroller;

    public MotionController(Context context) {
        mScroller = new Scroller(context);
    }

    /**
     * Get pointer location within particular view
     *
     * @param v View in case
     * @param event MotionEvent within this view
     * @return Location
     */
    public static int[] getMotionEventLocationOnView(View v, MotionEvent event){
        int[] Location = new int[2];
        v.getLocationOnScreen(Location);
        Location[0] = (int)(event.getRawX()) - Location[0];
        Location[1] = (int)(event.getRawY()) - Location[1];
        return Location;
    }

    /**
     * Set start pointer position and set current pointer position to the same value.
     *
     * @param X X coordinate
     * @param Y Y coordinate
     */
    public void setStartPointerPosition(int X, int Y) {
        if (StartPointerPosition == null)
            StartPointerPosition = new Point(X, Y);
        else
            StartPointerPosition.set(X, Y);
        setCurrentPointerPosition(X, Y);
    }

    /**
     * Set current pointer position
     *
     * @param X X coordinate
     * @param Y Y coordinate
     */
    public void setCurrentPointerPosition(int X, int Y) {
        if (CurrentPointerPosition == null)
            CurrentPointerPosition = new Point(X, Y);
        else
            CurrentPointerPosition.set(X, Y);
    }

    /**
     * Set directions in which swap is allowed.
     * By default only horizontal swap is allowed.
     *
     * @param isHorizontalSwapAllowed True if horizontal swap allowed
     * @param isVerticalSwapAllowed True if vertical swap allowed
     */
    public void setSwapAllowance(boolean isHorizontalSwapAllowed, boolean isVerticalSwapAllowed) {
        this.isHorizontalSwapAllowed = isHorizontalSwapAllowed;
        this.isVerticalSwapAllowed = isVerticalSwapAllowed;
    }

    /**
     * Get {@link Direction} in which the pointer is currently located relative
     * to the original position. Returned directions restricted to allowed swap directions.
     * Returns {@link Direction#NO_SWAP} if both swap allowances are <tt>false</tt>.
     *
     * @return Move {@link Direction}
     * @see #setSwapAllowance(boolean, boolean)
     */
    public Direction getMoveDirection() {
        Point Difference = new Point();
        if (mScroller.isFinished()) {
            Difference.X = CurrentPointerPosition.X - StartPointerPosition.X;
            Difference.Y = CurrentPointerPosition.Y - StartPointerPosition.Y;
        } else {
            int [] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Difference.X = InterpolCurrPos[0] - StartPointerPosition.X;
            Difference.Y = InterpolCurrPos[1] - StartPointerPosition.Y;
        }
        if (isHorizontalSwapAllowed && isVerticalSwapAllowed) {
            if (Math.abs(Difference.X) > Math.abs(Difference.Y)) {
                if (Difference.X > 0)
                    return Direction.LEFT_TO_RIGHT;
                else if (Difference.X < 0)
                    return Direction.RIGHT_TO_LEFT;
                else return Direction.NO_SWAP;
            } else {
                if (Difference.Y > 0)
                    return Direction.UP_TO_DOWN;
                else if (Difference.Y < 0)
                    return Direction.DOWN_TO_UP;
                else return Direction.NO_SWAP;
            }
        }

        if (isVerticalSwapAllowed) {
            if (Difference.Y > 0)
                return Direction.UP_TO_DOWN;
            else if (Difference.Y < 0)
                return Direction.DOWN_TO_UP;
            else return Direction.NO_SWAP;
        }

        if (isHorizontalSwapAllowed) {
            if (Difference.X > 0)
                return Direction.LEFT_TO_RIGHT;
            else if (Difference.X < 0)
                return Direction.RIGHT_TO_LEFT;
            else return Direction.NO_SWAP;
        }

        return Direction.NO_SWAP;
    }

    /**
     * Reset start and current pointer positions.
     */
    public void reset(){
        StartPointerPosition = null;
        CurrentPointerPosition = null;
    }

    /**
     * Get X and Y offsets between start and current pointer positions.
     * Returned Y is 0 if vertical swap in not allowed.
     * Returned X is 0 if horizontal swap in not allowed.
     * If {@link #isAnimationFinished()} is <tt>true</tt> current position is
     * previously set by {@link #setCurrentPointerPosition(int, int)} position.
     * Otherwise current position is currently animated position.
     *
     * @return X and Y offsets
     * @see #setSwapAllowance(boolean, boolean)
     */
    public int[] getOffset() {
        int[] Offset = new int[2];
        if (isHorizontalSwapAllowed && isVerticalSwapAllowed) {
            if (mScroller.isFinished()) {
                Offset[0] = CurrentPointerPosition.X - StartPointerPosition.X;
                Offset[1] = CurrentPointerPosition.Y - StartPointerPosition.Y;
            } else {
                int[] InterpolCurrPos = new int[2];
                getInterpolatedCurrentPosition(InterpolCurrPos);
                Offset[0] = InterpolCurrPos[0] - StartPointerPosition.X;
                Offset[1] = InterpolCurrPos[1] - StartPointerPosition.Y;
            }
        } else if (isHorizontalSwapAllowed){
            if (mScroller.isFinished()) {
                Offset[0] = CurrentPointerPosition.X - StartPointerPosition.X;
            } else {
                int[] InterpolCurrPos = new int[2];
                getInterpolatedCurrentPosition(InterpolCurrPos);
                Offset[0] = InterpolCurrPos[0] - StartPointerPosition.X;
            }
        } else if (isVerticalSwapAllowed){
            if (mScroller.isFinished()) {
                Offset[1] = CurrentPointerPosition.Y - StartPointerPosition.Y;
            } else {
                int[] InterpolCurrPos = new int[2];
                getInterpolatedCurrentPosition(InterpolCurrPos);
                Offset[1] = InterpolCurrPos[1] - StartPointerPosition.Y;
            }
        }

        if (Debug)
            Log.i(TAG, "Current Fling Offset: " + Offset[0] + " | " + Offset[1]);
        return Offset;
    }

    /**
     * Get Y offset between start and current pointer positions.
     * Swap restrictions ignored.
     *
     * @return Vertical offset
     */
    public int getVerticalOffset() {
        int Offset;
        if (mScroller.isFinished()) {
            Offset = CurrentPointerPosition.Y - StartPointerPosition.Y;
        } else {
            int[] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Offset = InterpolCurrPos[1] - StartPointerPosition.Y;
        }

        if (Debug)
            Log.i(TAG, "Current Vertical Fling Offset: " + Offset);
        return Offset;
    }

    /**
     * Get X offset between start and current pointer positions.
     * Swap restrictions ignored.
     *
     * @return Horizontal offset
     */
    public int getHorizontalOffset() {
        int Offset;
        if (mScroller.isFinished()) {
            Offset = CurrentPointerPosition.X - StartPointerPosition.X;
        } else {
            int[] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Offset = InterpolCurrPos[1] - StartPointerPosition.X;
        }

        if (Debug)
            Log.i(TAG, "Current Horizontal Fling Offset: " + Offset);
        return Offset;
    }

    /**
     * Get previously set start pointer position
     *
     * @return Start pointer position
     */
    public int[] getStartPointerPosition() {
        int[] SCP = new int[2];
        SCP[0] = StartPointerPosition.X;
        SCP[1] = StartPointerPosition.Y;
        if (Debug)
            Log.i(TAG, "Start Pointer Position: " + SCP[0] + " | " + SCP[1]);
        return SCP;
    }

    /**
     * Get current animation interpolator.
     *
     * @return Interpolator
     * @see #setAnimationInterpolator(Interpolator)
     */
    @Nullable
    public Interpolator getAnimationInterpolator() {
        return AnimationInterpolator;
    }

    /**
     * Set interpolator which used for {@link #finishFling(int, int, int)}
     * animations.
     *
     * @param animationInterpolator Interpolator
     */
    public void setAnimationInterpolator(@Nullable Interpolator animationInterpolator) {
        AnimationInterpolator = animationInterpolator;
    }

    /**
     * Get current pointer position.
     * If {@link #isAnimationFinished()} is <tt>true</tt> current position is
     * previously set by {@link #setCurrentPointerPosition(int, int)} position.
     * Otherwise current position is currently animated position.
     *
     * @return Current pointer position
     */
    public int[] getCurrentPointerPosition() {
        int[] CCP = new int[2];
        if (mScroller.isFinished()) {
            CCP[0] = CurrentPointerPosition.X;
            CCP[1] = CurrentPointerPosition.Y;
        } else {
            getInterpolatedCurrentPosition(CCP);
        }

        if (Debug)
            Log.i(TAG, "Current Pointer Position: " + CCP[0] + " | " + CCP[1]);
        return CCP;
    }

    /**
     * Stop {@link #finishFling(int, int, int)}
     * animations.
     */
    public void abortAnimation() {
        mScroller.abortAnimation();
    }

//    public void setAnimationDuration(int duration){
//        mScroller.extendDuration(duration);
//    }

    /**
     * Get animation duration which set in {@link #finishFling(int, int, int)}
     * methods.
     * If {@link #isAnimationFinished()} is <tt>true</tt> returns 0 instead.
     * 
     * @return Animation duration or 0
     */
    public int getAnimationDuration() {
        if (mScroller.isFinished())
            return 0;
        int dur = mScroller.getDuration();
        if (Debug)
            Log.i(TAG, "Animation Duration: " + dur);
        return dur;
    }

    /**
     * Get animation final pointer position which set in
     * {@link #finishFling(int, int, int)} or calculated in
     * {@link #finishHorizontalFling(int, int, Direction)} methods.
     * If {@link #isAnimationFinished()} is <tt>true</tt> returns [0,0] instead.
     * 
     * @return Final pointer position or [0,0]
     */
    public int[] getAnimationFinalPointerPosition() {
        int[] AFPP = new int[2];
        if (mScroller.isFinished())
            return AFPP;
        AFPP[0] = mScroller.getFinalX();
        AFPP[1] = mScroller.getFinalY();
        if (Debug)
            Log.i(TAG, "Animation Final Position: " + AFPP[0] + " | " + AFPP[1]);
        return AFPP;
    }

    /**
     * Get animation start pointer position which equals {@link #getCurrentPointerPosition()}
     * before {@link #finishFling(int, int, int)} methods call.
     * If {@link #isAnimationFinished()} is <tt>true</tt> returns [0,0] instead.
     *
     * @return start pointer position or [0,0]
     */
    public int[] getAnimationStartPointerPosition() {
        int[] ASPP = new int[2];
        if (mScroller.isFinished())
            return ASPP;
        ASPP[0] = mScroller.getStartX();
        ASPP[1] = mScroller.getStartY();
        if (Debug)
            Log.i(TAG, "Animation Start Pointer Position: " + ASPP[0] + " | " + ASPP[1]);
        return ASPP;
    }

    /**
     * Is animation started by {@link #finishFling(int, int, int)} methods
     * currently not running.
     *
     * @return True if animation is not running
     */
    public boolean isAnimationFinished() {
        return mScroller.isFinished();
    }

    /**
     * Get time passed since {@link #finishFling(int, int, int)} methods call.
     * If {@link #isAnimationFinished()} is <tt>true</tt> returns 0 instead.
     *
     * @return Time passed or 0
     */
    public int timePassed() {
        if (mScroller.isFinished())
            return 0;
        int time = mScroller.timePassed();
        if (Debug)
            Log.i(TAG, "Animation time passed: " + time);
        return time;
    }

    /**
     * Start fling animation from {@link #getCurrentPointerPosition()} to
     * [<tt>destinationX</tt>,<tt>destinationY</tt>] position
     *
     * @param destinationX Final position of X coordinate
     * @param destinationY Final position of Y coordinate
     * @param duration Animation duration
     */
    public void finishFling(int destinationX, int destinationY, int duration) {
        mScroller.fling(CurrentPointerPosition.X, CurrentPointerPosition.Y, 100, 100,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        mScroller.setFinalX(destinationX);
        mScroller.setFinalY(destinationY);
        mScroller.extendDuration(duration);

        CurrentPointerPosition.X = destinationX;
        CurrentPointerPosition.Y = destinationY;
    }

    /**
     * Start fling animation to the point located at the distance
     * <tt>finishPointOffset</tt> from the {@link #getCurrentPointerPosition()}
     * in passed horizontal direction. Do nothing if horizontal fling is not allowed
     * or passed direction is not horizontal.
     *
     * @param finishPointOffset Offset between animation start and finish point
     * @param duration Animation duration
     * @param direction Horizontal direction
     */
    public void finishHorizontalFling(int finishPointOffset, int duration, Direction direction) {
        if (!isHorizontalSwapAllowed || direction == Direction.DOWN_TO_UP
                || direction == Direction.UP_TO_DOWN)
            return;
        int LeftBorder = StartPointerPosition.X - finishPointOffset;
        int RightBorder = StartPointerPosition.X + finishPointOffset;
        mScroller.fling(CurrentPointerPosition.X, CurrentPointerPosition.Y, 100, 100, LeftBorder, RightBorder, CurrentPointerPosition.Y, CurrentPointerPosition.Y);
        switch (direction){
            case LEFT_TO_RIGHT:
                mScroller.setFinalX(RightBorder);
                mScroller.setFinalY(CurrentPointerPosition.Y);
                CurrentPointerPosition.X = RightBorder;
                break;
            case RIGHT_TO_LEFT:
                mScroller.setFinalX(LeftBorder);
                mScroller.setFinalY(CurrentPointerPosition.Y);
                CurrentPointerPosition.X = LeftBorder;
                break;
            case TO_START_POSITION:
            case NO_SWAP:
                mScroller.setFinalX(StartPointerPosition.X);
                mScroller.setFinalY(StartPointerPosition.Y);
                CurrentPointerPosition.X = StartPointerPosition.X;
                CurrentPointerPosition.Y = StartPointerPosition.Y;
                break;
            default:
                mScroller.abortAnimation();
                return;
        }
        mScroller.extendDuration(duration);
    }

    /**
     * Start fling animation to the point located at the distance
     * <tt>finishPointOffset</tt> from the {@link #getCurrentPointerPosition()}
     * in passed vertical direction. Do nothing if vertical fling is not allowed
     * or passed direction is not vertical.
     *
     * @param finishPointOffset Offset between animation start and finish point
     * @param duration Animation duration
     * @param direction Vertical direction
     */
    public void finishVerticalFling(int finishPointOffset, int duration, Direction direction) {
        if (!isVerticalSwapAllowed || direction == Direction.LEFT_TO_RIGHT
                || direction == Direction.RIGHT_TO_LEFT)
            return;
        int TopBorder = StartPointerPosition.Y - finishPointOffset;
        int BottomBorder = StartPointerPosition.Y + finishPointOffset;
        mScroller.fling(CurrentPointerPosition.X, CurrentPointerPosition.Y, 100, 100, CurrentPointerPosition.X, CurrentPointerPosition.X, TopBorder, BottomBorder);
        switch (direction){
            case UP_TO_DOWN:
                mScroller.setFinalX(CurrentPointerPosition.X);
                mScroller.setFinalY(BottomBorder);
                CurrentPointerPosition.Y = BottomBorder;
                break;
            case DOWN_TO_UP:
                mScroller.setFinalX(CurrentPointerPosition.X);
                mScroller.setFinalY(TopBorder);
                CurrentPointerPosition.Y = TopBorder;
                break;
            case TO_START_POSITION:
            case NO_SWAP:
                mScroller.setFinalX(StartPointerPosition.X);
                mScroller.setFinalY(StartPointerPosition.Y);
                CurrentPointerPosition.X = StartPointerPosition.X;
                CurrentPointerPosition.Y = StartPointerPosition.Y;
                break;
            default:
                mScroller.abortAnimation();
                return;
        }
        mScroller.extendDuration(duration);
    }

    /**
     * Get length of {@link #getOffset()}
     *
     * @return Length of {@link #getOffset()}
     */
    public double getOffsetLength(){
        double Return;
        int[] Offset = getOffset();
        if (Offset[0] == 0){ // only vertical
            Return = Math.abs(Offset[1]);
        } else if (Offset[1] == 0){ //only horizontal
            Return = Math.abs(Offset[0]);
        } else {
            Return = Math.sqrt(Math.pow(Offset[0], 2)
                    + Math.pow(Offset[1], 2));
        }
        if (Debug)
            Log.i(TAG, "Offset Length: " + Return);
        return Return;
    }

    /**
     * Get length of {@link #getHorizontalOffset()}
     *
     * @return Horizontal offset length
     */
    public double getHorizontalOffsetLength(){
        double Return;
        if (mScroller.isFinished()) {
            Return = Math.abs(CurrentPointerPosition.X - StartPointerPosition.X);
        } else {
            int [] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Return = Math.abs(InterpolCurrPos[0] - StartPointerPosition.X);
        }

        if (Debug)
            Log.i(TAG, "Horizontal Offset Length: " + Return);
        return Return;
    }

    /**
     * Get length of {@link #getVerticalOffset()}
     *
     * @return Vertical offset length
     */
    public double getVerticalOffsetLength(){
        double Return;
        if (mScroller.isFinished()) {
            Return = Math.abs(CurrentPointerPosition.Y - StartPointerPosition.Y);
        } else {
            int [] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Return = Math.abs(InterpolCurrPos[1] - StartPointerPosition.Y);
        }

        if (Debug)
            Log.i(TAG, "Vertical Offset Length: " + Return);
        return Return;
    }

    /**
     * Get vertical move {@link Direction}. Direction restrictions ignored.
     *
     * @return Vertical {@link Direction}
     */
    public Direction getVerticalMoveDirection(){
        int Difference;
        if (mScroller.isFinished()) {
            Difference = CurrentPointerPosition.Y - StartPointerPosition.Y;
        } else {
            int [] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Difference = InterpolCurrPos[1] - StartPointerPosition.Y;
        }
        if (Difference > 0)
            return Direction.UP_TO_DOWN;
        else if (Difference < 0)
            return Direction.DOWN_TO_UP;
        else return Direction.NO_SWAP;
    }

    /**
     * Get horizontal move {@link Direction}. Direction restrictions ignored.
     *
     * @return Horizontal {@link Direction}
     */
    public Direction getHorizontalMoveDirection(){
        int Difference;
        if (mScroller.isFinished()) {
            Difference = CurrentPointerPosition.X - StartPointerPosition.X;
        } else {
            int [] InterpolCurrPos = new int[2];
            getInterpolatedCurrentPosition(InterpolCurrPos);
            Difference = InterpolCurrPos[0] - StartPointerPosition.X;
        }
        if (Difference > 0)
            return Direction.LEFT_TO_RIGHT;
        else if (Difference < 0)
            return Direction.RIGHT_TO_LEFT;
        else return Direction.NO_SWAP;
    }

    private void getInterpolatedCurrentPosition(int[] Return){
        mScroller.computeScrollOffset();
        if (AnimationInterpolator == null) {
            Return[0] = mScroller.getCurrX();
            Return[1] = mScroller.getCurrY();
        } else {
            float Interpolation;
            Point AnimationStartPos = new Point();
            Point AnimationCurrentPos = new Point();
            Point AnimationFinishPos = new Point();
            AnimationStartPos.set(mScroller.getStartX(), mScroller.getStartY());
            AnimationCurrentPos.set(mScroller.getCurrX(), mScroller.getCurrY());
            AnimationFinishPos.set(mScroller.getFinalX(), mScroller.getFinalY());
            if (AnimationStartPos.X != AnimationFinishPos.X) //Interpolation value same for both
                //directions
                Interpolation = AnimationInterpolator.getInterpolation(
                        (float)(AnimationCurrentPos.X - AnimationStartPos.X) /
                                (AnimationFinishPos.X - AnimationStartPos.X));
            else //ASP.X == AFP.X means that most likely it is vertical animation
                Interpolation = AnimationInterpolator.getInterpolation(
                        (float)(AnimationCurrentPos.Y - AnimationStartPos.Y) /
                                (AnimationFinishPos.Y - AnimationStartPos.Y));
            Return[0] = AnimationStartPos.X +
                    (int)((AnimationCurrentPos.X - AnimationStartPos.X) * Interpolation);
            Return[1] = AnimationStartPos.Y +
                    (int)((AnimationCurrentPos.Y - AnimationStartPos.Y) * Interpolation);
        }
    }

//    public void debug(boolean check) {
//        Debug = check;
//    }

    public enum Direction {
        /**
         * Equals {@link #NO_SWAP}, counts as vertical and horizontal direction.
         */
        TO_START_POSITION,
        /**
         * Equals {@link #TO_START_POSITION}, counts as vertical and horizontal direction.
         */
        NO_SWAP,
        /**
         * Counts as horizontal direction.
         */
        RIGHT_TO_LEFT,
        /**
         * Counts as horizontal direction.
         */
        LEFT_TO_RIGHT,
        /**
         * Counts as vertical direction.
         */
        UP_TO_DOWN,
        /**
         * Counts as vertical direction.
         */
        DOWN_TO_UP;
    }

    private static class Point {
        int X, Y;

        Point() {
        }

        Point(int X, int Y) {
            this.X = X;
            this.Y = Y;
        }

        void set(int X, int Y){
            this.X = X;
            this.Y = Y;
        }
    }
}
