/*
 * Copyright 2016 Longri
 * Copyright 2016-2017 devemux86
 * Copyright 2017 nebular
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.ios_moe.backend;

import apple.corefoundation.c.CoreFoundation;
import apple.corefoundation.enums.CFStringBuiltInEncodings;
import apple.corefoundation.opaque.CFAttributedStringRef;
import apple.corefoundation.opaque.CFStringRef;
import apple.coregraphics.enums.CGBlendMode;
import apple.coregraphics.enums.CGLineCap;
import apple.coregraphics.enums.CGLineJoin;
import apple.coregraphics.opaque.CGContextRef;
import apple.coretext.c.CoreText;
import apple.coretext.opaque.CTLineRef;
import apple.uikit.UIColor;
import apple.uikit.UIFont;
import apple.uikit.c.UIKit;
import org.oscim.backend.canvas.Paint;

import java.util.HashMap;

import static apple.coregraphics.c.CoreGraphics.*;

/**
 * iOS specific implementation of {@link Paint}.
 */
public class IosPaint implements Paint {

    private static final String DEFAULT_FONT_NAME = UIFont.systemFontOfSizeWeight(1, UIKit.UIFontWeightSemibold()).fontDescriptor().postscriptName();
    private static final String DEFAULT_FONT_NAME_BOLD = UIFont.systemFontOfSizeWeight(1, UIKit.UIFontWeightBold()).fontDescriptor().postscriptName();
    private static final String DEFAULT_FONT_NAME_ITALIC = UIFont.italicSystemFontOfSize(1).fontDescriptor().postscriptName();
    private final static HashMap<String, UIFont> fontHashMap = new HashMap<>();
    FontProperties attribs = new FontProperties();
    float strokeWidth;
    private Align align;
    private int cap = CGLineCap.Butt;
    private int join = CGLineJoin.Miter;
    private Style style;
    private float textSize;
    private FontFamily fontFamily;
    private FontStyle fontStyle;
    private int colorInt;
    private int strokeColorInt;
    private CTLineRef ctLine;
    private boolean ctLineIsDirty = true;
    private String lastText = "";
    private float descent;
    private float fontHeight;

    private static int getLineCap(Cap cap) {
        switch (cap) {
            case BUTT:
                return CGLineCap.Butt;
            case ROUND:
                return CGLineCap.Round;
            case SQUARE:
                return CGLineCap.Square;
        }
        return CGLineCap.Butt;
    }

    private static int getLineJoin(Join join) {
        switch (join) {
            case MITER:
                return CGLineJoin.Miter;
            case ROUND:
                return CGLineJoin.Round;
            case BEVEL:
                return CGLineJoin.Bevel;
        }
        return CGLineJoin.Miter;
    }

    @Override
    public int getColor() {
        return this.colorInt;
    }

    @Override
    public void setColor(int color) {
        if (colorInt == color) return;
        this.colorInt = color;
        synchronized (attribs) {
            attribs.setForgroundColor(getUiColor(color));
        }
        ctLineIsDirty = true;
    }

    public void setStrokeColor(int color) {
        if (strokeColorInt == color) return;
        this.strokeColorInt = color;
        synchronized (attribs) {
            attribs.setStrokeColor(getUiColor(color));
        }
        ctLineIsDirty = true;
    }

    private UIColor getUiColor(int color) {
        float colorA = ((color & 0xff000000) >>> 24) / 255f;
        float colorR = ((color & 0xff0000) >>> 16) / 255f;
        float colorG = ((color & 0xff00) >>> 8) / 255f;
        float colorB = (color & 0xff) / 255f;
        return UIColor.colorWithRedGreenBlueAlpha(colorR, colorG, colorB, colorA);
    }

    public int getIosStrokeCap() {
        return this.cap;
    }

    @Override
    public void setStrokeCap(Cap cap) {
        this.cap = getLineCap(cap);
    }

    public int getIosStrokeJoin() {
        return this.join;
    }

    @Override
    public void setStrokeJoin(Join join) {
        this.join = getLineJoin(join);
    }

    @Override
    public void setTextAlign(Align align) {
        // TODO never read
        this.align = align;
    }

    @Override
    public void setTextSize(float textSize) {
        if (this.textSize != textSize) {
            this.textSize = textSize;
            createIosFont();
            ctLineIsDirty = true;
        }
    }

    @Override
    public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
        if (fontFamily != this.fontFamily
                || fontStyle != this.fontStyle) {

            this.fontFamily = fontFamily;
            this.fontStyle = fontStyle;
            createIosFont();
            ctLineIsDirty = true;
        }
    }

    @Override
    public float measureText(String text) {
        if (ctLineIsDirty || !text.equals(lastText)) {
            ctLineIsDirty = true;
            createCTLine(text);
        }
//        return (float) ctLine.getWidth();
        return 0;
    }

    private void createCTLine(String text) {
        if (ctLineIsDirty) {
            synchronized (attribs) {
                /*
                The sign of the value for NSStrokeWidthAttributeName is interpreted as a mode;
                it indicates whether the attributed string is to be filled, stroked, or both.
                Specifically, a zero value displays a fill only, while a positive value displays a stroke only.
                A negative value allows displaying both a fill and stroke.

                !!!!!
                NOTE: The value of NSStrokeWidthAttributeName is interpreted as a percentage of the font point size.
                */
                double strokeWidthPercent = -(this.strokeWidth / this.textSize * 40);
                attribs.setStrokeWidth(strokeWidthPercent);

                CFStringRef stringRef = CoreFoundation.CFStringCreateWithCString(null, text, CFStringBuiltInEncodings.UTF8);
                CFAttributedStringRef attributedStringRef = CoreFoundation.CFAttributedStringCreate(null, stringRef, attribs);
                ctLine = CoreText.CTLineCreateWithAttributedString(attributedStringRef);
            }
            lastText = text;
            ctLineIsDirty = false;
        }
    }

    private void createIosFont() {
        /*
          DEVICE_DEFAULT = [iOS == getDeviceDefault()], [Android == 'Roboto']
          MONOSPACE      = [iOS == 'Courier'], [Android == 'Droid Sans Mono']
          SANS_SERIF     = [iOS == 'Verdena'], [Android == 'Droid Sans']
          SERIF          = [iOS == 'Georgia'], [Android == 'Droid Serif']
         */

        String fontname = DEFAULT_FONT_NAME;
        switch (this.fontFamily) {
            case DEFAULT:
                // set Style
                switch (this.fontStyle) {
                    case NORMAL:
                        fontname = DEFAULT_FONT_NAME;
                        break;
                    case BOLD:
                        fontname = DEFAULT_FONT_NAME_BOLD;
                        break;
                    case BOLD_ITALIC:
                        fontname = DEFAULT_FONT_NAME_BOLD;
                        break;
                    case ITALIC:
                        fontname = DEFAULT_FONT_NAME_ITALIC;
                        break;
                }
                break;
            case DEFAULT_BOLD:
                // ignore style
                fontname = DEFAULT_FONT_NAME_BOLD;
                break;
            case MONOSPACE:
                // set Style
                switch (this.fontStyle) {
                    case NORMAL:
                        fontname = "CourierNewPS-BoldMT";
                        break;
                    case BOLD:
                        fontname = "CourierNewPS-BoldMT";
                        break;
                    case BOLD_ITALIC:
                        fontname = "CourierNewPS-BoldMT";
                        break;
                    case ITALIC:
                        fontname = "CourierNewPS-BoldMT";
                        break;
                }
                break;
            case SANS_SERIF:
                // set Style
                switch (this.fontStyle) {
                    case NORMAL:
                        fontname = "Verdana";
                        break;
                    case BOLD:
                        fontname = "Verdana-Bold";
                        break;
                    case BOLD_ITALIC:
                        fontname = "Verdana-BoldItalic";
                        break;
                    case ITALIC:
                        fontname = "Verdana-Italic";
                        break;
                }
                break;
            case SERIF:
                // set Style
                switch (this.fontStyle) {
                    case NORMAL:
                        fontname = "Georgia";
                        break;
                    case BOLD:
                        fontname = "Georgia-Bold";
                        break;
                    case BOLD_ITALIC:
                        fontname = "Georgia-BoldItalic";
                        break;
                    case ITALIC:
                        fontname = "Georgia-Italic";
                        break;
                }
                break;
        }

        synchronized (attribs) {
            String key = fontname + this.textSize;

            //try to get buffered font
            UIFont font = fontHashMap.get(key);

            if (font == null) {
                font = UIFont.fontWithNameSize(fontname, this.textSize);
                fontHashMap.put(key, font);
            }

            descent = (float) font.descender();
            fontHeight = (float) (font.descender() + font.capHeight());
            attribs.setFont(font);

        }
    }

    public void drawLine(CGContextRef cgBitmapContext, String text, float x, float y) {
        if (ctLineIsDirty || !text.equals(lastText)) {
            ctLineIsDirty = true;
            createCTLine(text);
        }

        CGContextSaveGState(cgBitmapContext);
        CGContextSetShouldAntialias(cgBitmapContext, true);
        CGContextSetTextPosition(cgBitmapContext, x, y + descent);
        CGContextSetBlendMode(cgBitmapContext, CGBlendMode.Normal);

        CoreText.CTLineDraw(ctLine, cgBitmapContext);
        CGContextRestoreGState(cgBitmapContext);
    }

    @Override
    public float getFontHeight() {
        return fontHeight;
    }

    @Override
    public float getFontDescent() {
        return descent;
    }

    @Override
    public float getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public void setStrokeWidth(float width) {
        if (this.strokeWidth == width) return;
        this.strokeWidth = width;
        this.ctLineIsDirty = true;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public float getTextHeight(String text) {
        return this.fontHeight;
    }

    @Override
    public float getTextWidth(String text) {
        return measureText(text);
    }
}
