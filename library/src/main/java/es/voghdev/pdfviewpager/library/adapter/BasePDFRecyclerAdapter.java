package es.voghdev.pdfviewpager.library.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import es.voghdev.pdfviewpager.library.R;

public class BasePDFRecyclerAdapter extends RecyclerView.Adapter<BasePDFRecyclerAdapter.BasePDFRecyclerViewHolder> {

    protected static final int FIRST_PAGE = 0;
    protected static final float DEFAULT_QUALITY = 2.0f;
    protected static final int DEFAULT_OFFSCREENSIZE = 1;

    protected String pdfPath;
    protected Context context;
    protected PdfRenderer renderer;
    protected BitmapContainer bitmapContainer;
    protected LayoutInflater inflater;

    protected float renderQuality;
    protected int offScreenSize;

    public BasePDFRecyclerAdapter(Context context, String pdfPath) {
        this.pdfPath = pdfPath;
        this.context = context;
        this.renderQuality = DEFAULT_QUALITY;
        this.offScreenSize = DEFAULT_OFFSCREENSIZE;

        init();
    }

    /**
     * This constructor was added for those who want to customize ViewPager's offScreenSize attr
     */
    public BasePDFRecyclerAdapter(Context context, String pdfPath, int offScreenSize) {
        this.pdfPath = pdfPath;
        this.context = context;
        this.renderQuality = DEFAULT_QUALITY;
        this.offScreenSize = offScreenSize;

        init();
    }

    @SuppressWarnings("NewApi")
    protected void init() {
        try {
            renderer = new PdfRenderer(getSeekableFileDescriptor(pdfPath));
            inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            PdfRendererParams params = extractPdfParamsFromFirstPage(renderer, renderQuality);
            bitmapContainer = new SimpleBitmapPool(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("NewApi")
    protected PdfRendererParams extractPdfParamsFromFirstPage(PdfRenderer renderer, float renderQuality) {
        PdfRenderer.Page samplePage = getPDFPage(renderer, FIRST_PAGE);
        PdfRendererParams params = new PdfRendererParams();

        params.setRenderQuality(renderQuality);
        params.setOffScreenSize(offScreenSize);
        params.setWidth((int) (samplePage.getWidth() * renderQuality));
        params.setHeight((int) (samplePage.getHeight() * renderQuality));

        samplePage.close();

        return params;
    }

    protected ParcelFileDescriptor getSeekableFileDescriptor(String path) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor;

        File pdfCopy = new File(path);

        if (pdfCopy.exists()) {
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfCopy, ParcelFileDescriptor.MODE_READ_ONLY);
            return parcelFileDescriptor;
        }

        if (isAnAsset(path)) {
            pdfCopy = new File(context.getCacheDir(), path);
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfCopy, ParcelFileDescriptor.MODE_READ_ONLY);
        } else {
            URI uri = URI.create(String.format("file://%s", path));
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(Uri.parse(uri.toString()), "rw");
        }

        return parcelFileDescriptor;
    }

    protected boolean isAnAsset(String path) {
        return !path.startsWith("/");
    }


    @SuppressWarnings("NewApi")
    protected PdfRenderer.Page getPDFPage(PdfRenderer renderer, int position) {
        return renderer.openPage(position);
    }

    @SuppressWarnings("NewApi")
    public void close() {
        releaseAllBitmaps();
        if (renderer != null) {
            renderer.close();
        }
    }

    protected void releaseAllBitmaps() {
        if (bitmapContainer != null) {
            bitmapContainer.clear();
        }
    }

    @NonNull
    @Override
    public BasePDFRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BasePDFRecyclerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_pdf_page, parent, false));
    }

    @Override
    @SuppressWarnings("NewApi")
    public void onBindViewHolder(@NonNull BasePDFRecyclerViewHolder holder, int position) {

        if (renderer == null || getItemCount() < position) {
            return;
        }

        PdfRenderer.Page page = getPDFPage(renderer, position);

        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();

        holder.iv.setImageBitmap(bitmap);
//        ((ViewPager) container).addView(v, 0);

//        return v;
    }

    @Override
    @SuppressWarnings("NewApi")
    public int getItemCount() {
        return renderer != null ? renderer.getPageCount() : 0;
    }

    class BasePDFRecyclerViewHolder extends RecyclerView.ViewHolder {
        ImageView iv;
        public SubsamplingScaleImageView ssiv;

        public BasePDFRecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            iv = (ImageView) itemView.findViewById(R.id.imageView);
            ssiv = itemView.findViewById(R.id.subsamplingImageView);
        }
    }
}
