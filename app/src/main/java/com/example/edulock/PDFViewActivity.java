package com.example.edulock;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PDFViewActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvTitle;
    private ImageButton btnBack;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;

    private int currentPageIndex = 0;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_view);

        imageView = findViewById(R.id.pdfImageView);
        tvTitle = findViewById(R.id.tvPdfTitle);
        btnBack = findViewById(R.id.btnBack);

        String fileName = getIntent().getStringExtra("fileName");
        String title = getIntent().getStringExtra("title");

        if (title != null) {
            tvTitle.setText(title.toUpperCase());
        }

        btnBack.setOnClickListener(v -> finish());

        gestureDetector = new GestureDetector(this, new GestureListener());

        imageView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        if (fileName != null) {
            openPdfFromAssets(fileName);
        }
    }

    private void openPdfFromAssets(String fileName) {
        try {
            File file = new File(getCacheDir(), fileName);

            if (!file.exists()) {
                InputStream asset = getAssets().open(fileName);
                FileOutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int size;
                while ((size = asset.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
                asset.close();
                output.close();
            }

            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            showPage(currentPageIndex);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showPage(int index) {
        if (pdfRenderer == null) return;
        if (index < 0 || index >= pdfRenderer.getPageCount()) return;

        if (currentPage != null) {
            currentPage.close();
        }

        currentPageIndex = index;
        currentPage = pdfRenderer.openPage(currentPageIndex);

        Bitmap bitmap = Bitmap.createBitmap(
                currentPage.getWidth(),
                currentPage.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        bitmap.eraseColor(Color.WHITE);

        currentPage.render(bitmap, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        imageView.setImageBitmap(bitmap);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {

            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                if (diffX > 0) {
                    // Swipe Right → Previous Page
                    showPage(currentPageIndex - 1);
                } else {
                    // Swipe Left → Next Page
                    showPage(currentPageIndex + 1);
                }
                return true;
            }
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (currentPage != null) currentPage.close();
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
