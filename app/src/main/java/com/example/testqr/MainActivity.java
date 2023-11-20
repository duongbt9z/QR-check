package com.example.testqr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    Button btnGenreateQR, btnSaveQR, btnScanCamera, btnScanImage;
    ImageView imgQRcode;
    EditText edtData;
    Bitmap bitmapQRImage;

    ActivityResultLauncher QuetQRimage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    try {

                        InputStream inputStream = getContentResolver().openInputStream(result);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            int widgth = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            int[] pixels = new int[widgth * height];
//                                bitmap.getPixel(widgth,height);
                            bitmap.getPixels(pixels, 0, widgth, 0, 0, widgth, height);
                            bitmap.recycle();
                            RGBLuminanceSource source = new RGBLuminanceSource(widgth, height, pixels);
                            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                            MultiFormatReader reader = new MultiFormatReader();
                            Result readResult = reader.decode(binaryBitmap);
                            Toast.makeText(MainActivity.this, "check qr image", Toast.LENGTH_SHORT).show();
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Quét mã QR from image ");
                            builder.setMessage(readResult.toString());
                            builder.setPositiveButton("Sao chép", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("qr_contnet", readResult.toString());
                                    clipboardManager.setPrimaryClip(clipData);
                                    dialog.dismiss();
                                }
                            });
                            builder.create().show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edtData = findViewById(R.id.edtData);
        btnScanImage = findViewById(R.id.btnScanImage);
        btnScanCamera = findViewById(R.id.btnScanCamera);
        btnSaveQR = findViewById(R.id.btnSaveQR);
        btnGenreateQR = findViewById(R.id.btnGenreateQR);
        imgQRcode = findViewById(R.id.imgQRcode);

        // quet mã thu viện
        btnScanImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuetQRimage.launch("image/*");
            }
        });

        // btnScanCamera
        btnScanCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("bug-clcik","camera");
                ScancodefromCamera();
            }
        });

        //save image qr
        btnSaveQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveImgae();
            }
        });

        // tạo mã qrimage
        btnGenreateQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = edtData.getText().toString().trim();
                if (data.length() > 0) {
                    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                    try {
                        BitMatrix bitMatrix = multiFormatWriter.encode(
                                data,
                                BarcodeFormat.QR_CODE,
                                300, 300
                        );
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        bitmapQRImage = barcodeEncoder.createBitmap(bitMatrix);
                        imgQRcode.setImageBitmap(bitmapQRImage);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Vui lòng nhập dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // save vào file
    protected void SaveImgae() {
        Uri uriimage;
        ContentResolver contentResolver = getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uriimage = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            uriimage = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        String timeStamp = new SimpleDateFormat("dd_MM_YYYY_HH_mm_ss").format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, timeStamp + ".jpg");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        uriimage = contentResolver.insert(uriimage, contentValues);
        try {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imgQRcode.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uriimage));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            Objects.requireNonNull(outputStream);
            Toast.makeText(this, "Đã lưu" + timeStamp, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu ảnh", Toast.LENGTH_SHORT).show();
        }

    }

    // check qr bằng camera
    ActivityResultLauncher ScancodefromCameraLaucher = registerForActivityResult(
            new ScanContract(),
            new ActivityResultCallback<ScanIntentResult>() {
                @Override
                public void onActivityResult(ScanIntentResult result) {
                    if (result.getContents().length() > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Quét mã QR");
                        builder.setMessage(result.getContents());
                        builder.setPositiveButton("Sao chép", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText("qr_contnet", result.getContents());
                                clipboardManager.setPrimaryClip(clipData);
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }
            });

    protected void ScancodefromCamera() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volumn up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(MyCaptureActivity.class);
        ScancodefromCameraLaucher.launch(options);
        Log.d("bug","camera");
    }
}
