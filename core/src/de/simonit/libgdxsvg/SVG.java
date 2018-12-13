package de.simonit.libgdxsvg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SVG implements Disposable {

    private byte[] src;

    private float origAspectRatio;
    private int width = 0;
    private int height = 0;

    private Texture svg;

    public SVG(String internalPath) {
        this(Gdx.files.internal(internalPath));
    }

    public SVG(FileHandle file) {
        this.src = file.readBytes();
        refreshTexture(true);
    }

    private void refreshTexture() {
        refreshTexture(false);
    }

    private void refreshTexture(final boolean saveAspectRatio) {
        InputStream stream = new ByteArrayInputStream(src);
        try {
            Bitmap bitmap = CanvasAdapter.decodeSvgBitmap(stream, width, height, 100);

            byte[] bitmapData = bitmap.getPngEncodedData();
            svg = new Texture(new Pixmap(bitmapData, 0, bitmapData.length));
            if (saveAspectRatio) {
                origAspectRatio = (float) svg.getWidth() / (float) svg.getHeight();
            }
            height = svg.getHeight();
            width = svg.getWidth();
        } catch (IOException e) {
            throw new GdxRuntimeException(e);
        }
    }

    public float getOrigAspectRatio() {
        return origAspectRatio;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        setWidth(width, false);
    }

    public void setWidth(int width, boolean keepAspectRatio) {
        if (width != this.width) {
            if (keepAspectRatio) {
                this.height = MathUtils.round(width / this.origAspectRatio);
            }
            this.width = width;
            refreshTexture();
        }
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        setHeight(height, false);
    }

    public void setHeight(int height, boolean keepAspectRatio) {
        if (height != this.height) {
            if (keepAspectRatio) {
                this.width = MathUtils.round(width * this.origAspectRatio);
            }
            this.height = height;
            refreshTexture();
        }
    }

    public void draw(Batch batch, float x, float y) {
        draw(batch, x, y, this.width, this.height);
    }

    public void draw(Batch batch, float x, float y, float width, float height) {
        if (this.svg != null) {
            batch.draw(this.svg, x, y, width, height);
        }
    }

    @Override
    public void dispose() {
        this.svg.dispose();
    }
}
