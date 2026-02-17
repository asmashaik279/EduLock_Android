package com.example.edulock;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PDFViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageButton btnBack;
    private TextView tvTitle;
    private List<Bitmap> pdfPages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_view);

        recyclerView = findViewById(R.id.pdfRecyclerView);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvPdfTitle);

        String fileName = getIntent().getStringExtra("fileName");
        String title = getIntent().getStringExtra("title");
        
        tvTitle.setText(title != null ? title : "Reading Book");
        btnBack.setOnClickListener(v -> finish());

        if (fileName != null) {
            renderPdf(fileName);
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void renderPdf(String fileName) {
        try {
            File file = new File(getCacheDir(), fileName);
            if (!file.exists()) {
                InputStream is = getAssets().open(fileName);
                FileOutputStream os = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
                is.close(); os.close();
            }

            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);

            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                pdfPages.add(bitmap);
                page.close();
            }
            
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new PdfAdapter(pdfPages));
            renderer.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private static class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.ViewHolder> {
        List<Bitmap> pages;
        PdfAdapter(List<Bitmap> pages) { this.pages = pages; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setAdjustViewBounds(true);
            imageView.setPadding(0, 0, 0, 20);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((ImageView) holder.itemView).setImageBitmap(pages.get(position));
        }

        @Override
        public int getItemCount() { return pages.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }
}
