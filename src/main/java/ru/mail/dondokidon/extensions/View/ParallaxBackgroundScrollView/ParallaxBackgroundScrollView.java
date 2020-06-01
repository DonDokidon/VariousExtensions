package ru.mail.dondokidon.extensions.View.ParallaxBackgroundScrollView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ScrollView;

import androidx.core.widget.NestedScrollView;


/**
 * {@link ScrollView} which allows to set several background {@link Bitmap} and move them
 * with every change of scroll positions to achieve parallax effect.
 */
public abstract class ParallaxBackgroundScrollView extends ScrollView {

    private int BackgroundColor = Color.TRANSPARENT;
    private Drawable BackgroundDrawable = null;

    private SparseArray<ColorFilter> BackgroundLayersColorFilterArray = new SparseArray<>(5);

    private SparseArray<Bitmap> BackgroundLayersArray = new SparseArray<>(5);

    private SparseArray<Matrix> BackgroundLayersMatrixArray = new SparseArray<>(5);

    private Paint LayerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ParallaxBackgroundScrollView(Context context) {
        super(context);
        init(context);
    }

    public ParallaxBackgroundScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ParallaxBackgroundScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                if (BackgroundDrawable != null){
                    setBackgroundDrawablePosition(getScrollX(), getScrollY());
                }

                for (int n : getLayersIndexList()){
                    setBackgroundLayerMatrix(n, BackgroundLayersMatrixArray.get(n), getScrollX(), getScrollY());
                }
            }
        });
    }

    /**
     * Set background layer for passed layer index. Layers will be drawn in order of increasing
     * their index
     */
    protected void setBackgroundLayer(int LayerIndex ,Bitmap pic){
        if (pic == null){
            BackgroundLayersArray.remove(LayerIndex);
            BackgroundLayersMatrixArray.remove(LayerIndex);
            BackgroundLayersColorFilterArray.remove(LayerIndex);
            return;
        }
        BackgroundLayersArray.append(LayerIndex, pic);
        Matrix matrix = BackgroundLayersMatrixArray.get(LayerIndex);
        if (BackgroundLayersMatrixArray.get(LayerIndex) == null) {
            matrix = new Matrix();
            BackgroundLayersMatrixArray.append(LayerIndex, matrix);
        }
        setBackgroundLayerMatrix(LayerIndex, matrix, getScrollX(), getScrollY());
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        if (BackgroundColor != Color.TRANSPARENT){
            canvas.drawColor(BackgroundColor);
        }
        if (BackgroundDrawable != null){
            BackgroundDrawable.draw(canvas);
        }

        for (int n : getLayersIndexList()){
            LayerPaint.setColorFilter(BackgroundLayersColorFilterArray.get(n));
            canvas.drawBitmap(BackgroundLayersArray.get(n), BackgroundLayersMatrixArray.get(n), LayerPaint);
        }

        super.draw(canvas);
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);

        if (BackgroundDrawable != null){
            setBackgroundDrawablePosition(x, y);
        }

        for (int n : getLayersIndexList()){
            setBackgroundLayerMatrix(n, BackgroundLayersMatrixArray.get(n), getScrollX(), getScrollY());
        }
    }

    /**
     * Set position matrix for passed layer index
     *
     * @param LayerIndex Index of layer
     * @param matrix Matrix which should be set
     * @param x Current scroll X position
     * @param y Current scroll Y position
     */
    protected abstract void setBackgroundLayerMatrix(int LayerIndex, Matrix matrix, int x, int y);

    private void setBackgroundDrawablePosition(int x, int y){
        BackgroundDrawable.setBounds(x, y, x + getWidth(), y + getHeight());
    }

    @Override
    public void setBackgroundColor(int color) {
        BackgroundColor = color;
        invalidate();
    }

    @Override
    public void setBackground(Drawable background) {
        BackgroundDrawable = background;
        if (BackgroundDrawable != null){
            setBackgroundDrawablePosition(getScrollX(), getScrollY());
        }
        invalidate();
    }

    /**
     * Get background layer previously set for passed index or null if no such layer.
     *
     * @param LayerIndex Index of layer
     * @return Background layer previously set for passed index or null
     */
    protected Bitmap getBackgroundLayer(int LayerIndex) {
        return BackgroundLayersArray.get(LayerIndex);
    }

    /**
     * Set {@link ColorFilter} for background layer with passed index. Nothing will be set if
     * there is no layer with passed index.
     *
     * @param LayerIndex Index of layer
     * @param backgroundBackLayerColorFilter {@link ColorFilter} for passed layer
     */
    protected void setBackgroundLayerColorFilter(int LayerIndex, ColorFilter backgroundBackLayerColorFilter) {
        if (backgroundBackLayerColorFilter == null){
            BackgroundLayersColorFilterArray.remove(LayerIndex);
            return;
        }
        if (BackgroundLayersArray.get(LayerIndex) == null)
            return;
        BackgroundLayersColorFilterArray.append(LayerIndex, backgroundBackLayerColorFilter);
        invalidate();
    }

    /**
     * Force {@link #setBackgroundLayerMatrix(int, Matrix, int, int)} call for layer with passed
     * index.
     *
     * @param LayerIndex Index of layer
     */
    protected void prepareBackgroundLayerMatrix(int LayerIndex){
        if (BackgroundLayersArray.get(LayerIndex) != null){
            setBackgroundLayerMatrix(LayerIndex, BackgroundLayersMatrixArray.get(LayerIndex), getScrollX(), getScrollY());
        }
    }

    @Override
    public Drawable getBackground() {
        return BackgroundDrawable;
    }

    /**
     * Get count of layers
     *
     * @return Count of layers
     */
    protected int getLayersCount(){
        return BackgroundLayersArray.size();
    }

    /**
     * Get list of layers indexes currently set
     *
     * @return List of layers indexes currently set
     */
    protected int[] getLayersIndexList(){
        int Size = BackgroundLayersArray.size();
        int[] list = new int[Size];
        for (int i = 0; i < Size; i++)
            list[i] = BackgroundLayersArray.keyAt(i);
        return list;
    }
}
