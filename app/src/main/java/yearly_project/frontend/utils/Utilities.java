package yearly_project.frontend.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

import timber.log.Timber;
import yearly_project.frontend.waitScreen.CalculateResults;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class Utilities {

    @NonNull
    public static MappedByteBuffer loadMappedFile(@NonNull Context context, @NonNull String filePath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(filePath);

        MappedByteBuffer var9;
        try {
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

            try {
                FileChannel fileChannel = inputStream.getChannel();
                long startOffset = fileDescriptor.getStartOffset();
                long declaredLength = fileDescriptor.getDeclaredLength();
                var9 = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            } catch (Throwable var12) {
                try {
                    inputStream.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }

                throw var12;
            }

            inputStream.close();
        } catch (Throwable var13) {
            try {
                fileDescriptor.close();
            } catch (Throwable var10) {
                var13.addSuppressed(var10);
            }

            throw var13;
        }

        fileDescriptor.close();

        return var9;
    }

    public static void createAlertDialog(Context context, String title, String content, DialogInterface.OnClickListener clickListener){
        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public static void createQuestionDialog(Context context, String title, String content, DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton("Yes", yesListener)
                .setNegativeButton("No", noListener)
                .show();
    }

    public static Bitmap convertMatToBitMap(Mat input) {
        Bitmap bmp = null;

        try {
            bmp = Bitmap.createBitmap(input.cols(), input.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(input, bmp);
        } catch (CvException e) {
            Timber.d(e);
        }

        return bmp;
    }

    public static void bitmapToFile(Bitmap bmp,String path, String filename) {
        File dir = new File(path);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, filename + ".png");
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(String path){
        File file = new File(path);
        if (file.isDirectory())
            for (File child : Objects.requireNonNull(file.listFiles()))
                deleteFile(child.getAbsolutePath());

        file.delete();
    }

    public static Mat onImageAvailable(Image image) {
        try {
            if (image != null) {
                byte[] nv21;
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                nv21 = new byte[ySize + uSize + vSize];

                //U and V are swapped
                yBuffer.get(nv21, 0, ySize);
                vBuffer.get(nv21, ySize, vSize);
                uBuffer.get(nv21, ySize + vSize, uSize);

                return getYUV2Mat(nv21, image);
            }
        } catch (Exception ignored) {
        } finally {
            assert image != null;
            image.close();// don't forget to close
        }

        return null;
    }

    public static Mat getYUV2Mat(byte[] data, Image image) {
        Mat mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CV_8UC1);
        mYuv.put(0, 0, data);
        Mat mRGB = new Mat();
        cvtColor(mYuv, mRGB, Imgproc.COLOR_YUV2RGB, 3);
        return mRGB;
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public static void activityResult(int result, Activity activity, int ID) {
        Intent data = new Intent(activity, CalculateResults.class);
        data.putExtra("ID", ID);
        activity.setResult(result, data);
    }
}
