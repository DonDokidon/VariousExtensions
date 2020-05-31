package ru.mail.dondokidon.extensions;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Class to create scaled Bitmap and copy Bitmap into another Bitmap.
 */
public class BitmapCreator {

    /**
     * Pass it as width or height parameter to adjust it value to height or width changes
     * in same percentage value.
     */
    public final static int SIZE_PROPORTIONAL = -1;

    /**
     * Create new Bitmap with scaled according ScaleType param Bitmap from resources within
     *
     * @param res The resources object containing the image data
     * @param resourceId The resource id of the image data
     * @param width Returned bitmap's width
     * @param height Returned bitmap's height
     * @param scaleType Scale type to decoded bitmap to fit into created bitmap
     * @return Created Bitmap, or null if the image could not be decoded from resources
     *
     * @throws OutOfMemoryError if resource is too large to decode
     */
    @Nullable
    public static Bitmap createScaleTyped(@NotNull Resources res, int resourceId, int width, int height, ScaleType scaleType) {
        BitmapFactory.Options Options = new BitmapFactory.Options();
        Options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resourceId, Options);

        int scale;
        if (width == SIZE_PROPORTIONAL && height == SIZE_PROPORTIONAL)
            scale = 1;
        else if (width == SIZE_PROPORTIONAL){
            scale = Math.max(1, Options.outHeight / height);
        } else if (height == SIZE_PROPORTIONAL){
            scale = Math.max(1, Options.outWidth / width);
        } else {
            scale = Math.max(1, Math.min(Options.outWidth / width, Options.outHeight / height));
        }
        Options.inJustDecodeBounds = false;
        Options.inSampleSize = scale;
        Bitmap tempBitmap = BitmapFactory.decodeResource(res, resourceId, Options);
        if (tempBitmap == null){
            return null;
        }
        Bitmap result = createScaleTyped(tempBitmap, width, height, scaleType);
        tempBitmap.recycle();
        return result;
    }

    /**
     * Create new Bitmap with scaled according ScaleType param Bitmap from resources within
     *
     * @param byteArray The byte array representing the image data
     * @param width Returned bitmap's width
     * @param height Returned bitmap's height
     * @param scaleType Scale type to decoded bitmap to fit into created bitmap
     * @return Created Bitmap, or null if the image could not be decoded from byte array
     *
     * @throws OutOfMemoryError if resource is too large to decode
     */
    @Nullable
    public static Bitmap createScaleTyped(@NotNull byte[] byteArray, int width, int height, ScaleType scaleType) {
        BitmapFactory.Options Options = new BitmapFactory.Options();
        Options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, Options);

        int scale;
        if (width == SIZE_PROPORTIONAL && height == SIZE_PROPORTIONAL)
            scale = 1;
        else if (width == SIZE_PROPORTIONAL){
            scale = Math.max(1, Options.outHeight / height);
        } else if (height == SIZE_PROPORTIONAL){
            scale = Math.max(1, Options.outWidth / width);
        } else {
            scale = Math.max(1, Math.min(Options.outWidth / width, Options.outHeight / height));
        }
        Options.inJustDecodeBounds = false;
        Options.inSampleSize = scale;
        Bitmap tempBitmap = BitmapFactory.decodeByteArray(byteArray,0, byteArray.length, Options);
        if (tempBitmap == null){
            return null;
        }
        Bitmap result = createScaleTyped(tempBitmap, width, height, scaleType);
        tempBitmap.recycle();
        return result;
    }

    /**
     * Create new Bitmap with scaled according ScaleType param source Bitmap within
     *
     * @param source The bitmap which should be scaled
     * @param width Returned bitmap's width
     * @param height Returned bitmap's height
     * @param scaleType Scale type to decoded bitmap to fit into created bitmap
     * @return Created Bitmap
     *
     * @throws IllegalArgumentException if width, height exceed the
     *         bounds of the source bitmap
     * @throws IllegalStateException if the source bitmap's config is {@link Config#HARDWARE}
     */
    public static Bitmap createScaleTyped(@NotNull Bitmap source, int width, int height, @NotNull ScaleType scaleType){
        if (width == SIZE_PROPORTIONAL && height == SIZE_PROPORTIONAL)
            return Bitmap.createBitmap(source);
        else if (width == SIZE_PROPORTIONAL){
            width = (int)((double)height / source.getHeight() * source.getWidth());
        } else if (height == SIZE_PROPORTIONAL){
            height = (int)((double)width / source.getWidth() * source.getHeight());
        }
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());
        copyScaleTyped(source, result, scaleType);
        return result;
    }

    /**
     * Copy scaled according ScaleType param source Bitmap in target Bitmap
     *
     * @param source The bitmap which should be scaled
     * @param target The bitmap which will contain scaled source bitmap in result
     * @param scaleType Scale type to decoded bitmap to fit into created bitmap
     *
     * @throws IllegalStateException if the source bitmap's config is {@link Config#HARDWARE},
     * if the target bitmap is not mutable
     */
    public static void copyScaleTyped(@NotNull Bitmap source, @NotNull Bitmap target, @NotNull ScaleType scaleType){
        Canvas SizeCanvas;
        Matrix SizeMatrix;
        int Width = source.getWidth(), Height = source.getHeight();
        int width = target.getWidth(), height = target.getHeight();
        int startX, startY;
        float scaleX, scaleY, scaleResult;
        switch (scaleType){
            case CENTER:
                startY = Height / 2 - height / 2;
                startX = Width / 2 - width / 2;
                copy(source, target, startX, startY, width, height);
                break;

            case CENTER_CROP:
                scaleX = width / (float) Width;
                scaleY = height / (float) Height;
                if (scaleX > scaleY){
                    scaleResult = scaleX;
                    startX = 0;
                    startY = (int)(Height * scaleResult / 2 - height / 2);
                } else {
                    scaleResult = scaleY;
                    startX = (int)(Width * scaleResult / 2 - width / 2);
                    startY = 0;
                }
                SizeCanvas = new Canvas(target);
                SizeMatrix = new Matrix();
                SizeMatrix.setTranslate(-startX, -startY);
                SizeMatrix.preScale(scaleResult, scaleResult);
                SizeCanvas.drawBitmap(source, SizeMatrix, new Paint());
                break;

            case CENTER_INSIDE:
                scaleX = width / (float) Width;
                scaleY = height / (float) Height;
                scaleResult = Math.min(scaleX, scaleY);
                if (scaleX > scaleY){
                    startX = (int)(Width * scaleResult / 2 - width / 2);
                    startY = 0;
                } else {
                    startX = 0;
                    startY = (int)(Height * scaleResult / 2 - height / 2);
                }
                SizeCanvas = new Canvas(target);
                SizeMatrix = new Matrix();
                SizeMatrix.setTranslate(-startX, -startY);
                SizeMatrix.preScale(scaleResult, scaleResult);
                SizeCanvas.drawBitmap(source, SizeMatrix, new Paint());
                break;
        }
    }

    /**
     * Copy source bitmap into target bitmap
     *
     * @param source The bitmap which should be copied
     * @param target The bitmap which will contain copied source bitmap in result
     *
     * @throws IllegalStateException if the source bitmap's config is {@link Config#HARDWARE},
     * if the target bitmap is not mutable
     */
    public static void copy(@NotNull Bitmap source,@NotNull Bitmap target){
        copy(source, target, 0,0, Math.min(source.getWidth(), target.getWidth()),
                Math.min(source.getHeight(), target.getHeight()));
    }

    /**
     * Copy source bitmap into target bitmap
     *
     * @param source The bitmap which should be copied
     * @param target The bitmap which will contain copied source bitmap in result
     * @param startX The x coordinate of the first pixel to read from the source bitmap
     * @param startY The y coordinate of the first pixel to read from the source bitmap
     * @param width The number of pixels to read from each row
     * @param height The number of rows to read
     *
     * @throws IllegalArgumentException if startX, startY, width, height exceed the
     *         bounds of the source bitmap; if width, height exceed the bounds of target
     *         bitmap
     * @throws IllegalStateException if the source bitmap's config is {@link Config#HARDWARE},
     * if the target bitmap is not mutable
     */
    public static void copy(@NotNull Bitmap source,@NotNull Bitmap target, int startX, int startY, int width, int height){
        int [] Pixels = new int [width * height];
        source.getPixels(Pixels, 0, width, startX, startY, width, height);
        target.setPixels(Pixels, 0, width, 0, 0, width, height);
    }

    public enum ScaleType{
        CENTER,
        CENTER_CROP,
        CENTER_INSIDE;
    }
}
