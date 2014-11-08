import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.pdf.PDFUtils;

/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */

/**
 *
 * Setters return the PDFWatermark object so they can be chained
 *
 * @since 6.0
 */
public class PDFWatermark {

    public static final String DEFAULT_FONT_FAMILY = "Helvetica";

    public static final float DEFAULT_FONT_SIZE = 36f;

    public static final int DEFAULT_TEXT_ROTATION = 0;

    public static final String DEFAULT_TEXT_COLOR = "#ffffff";

    public static final float DEFAULT_X_POSITION = 0f;

    public static final float DEFAULT_Y_POSITION = 0f;

    public static final boolean DEFAULT_INVERT_Y = false;

    protected Blob blob = null;

    protected String stampString = null;

    protected String fontFamily = DEFAULT_FONT_FAMILY;

    protected float fontSize = DEFAULT_FONT_SIZE;

    protected int textRotation = DEFAULT_TEXT_ROTATION;

    protected String hex255Color = DEFAULT_TEXT_COLOR;

    protected float xPosition = DEFAULT_X_POSITION;

    protected float yPosition = DEFAULT_Y_POSITION;

    protected boolean invertY = DEFAULT_INVERT_Y;

    public PDFWatermark(Blob inBlob) {

        blob = inBlob;
    }

    public PDFWatermark(DocumentModel inDoc, String inXPath) {

        blob = (Blob) inDoc.getPropertyValue(PDFUtils.checkXPath(inXPath));
    }

    public Blob watermark() throws ClientException {

        Blob result = null;
        PDDocument pdfDoc = null;
        PDPageContentStream contentStream = null;

        try {

            pdfDoc = PDDocument.load(blob.getStream());
            PDFont font = PDType1Font.getStandardFont(fontFamily);
            int[] rgb = PDFUtils.hex255ToRGB(hex255Color);

            List allPages = pdfDoc.getDocumentCatalog().getAllPages();
            for (int i = 0; i < allPages.size(); i++) {
                contentStream = null;

                // create an empty page and a geo object to use for calcs
                PDPage page = (PDPage) allPages.get(i);
                PDRectangle pageSize = page.findMediaBox();

                // are we inverting the y axis?
                if (invertY) {
                    yPosition = pageSize.getHeight() - yPosition;
                }

                // calculate the width of the string according to the font
                float stringWidth = font.getStringWidth(stampString) * fontSize
                        / 1000f;

                // determine the rotation stuff. Is the the loaded page in
                // landscape mode? (for axis and string dims)
                int pageRot = page.findRotation();
                boolean pageRotated = pageRot == 90 || pageRot == 270;

                // are we rotating the text?
                boolean textRotated = textRotation != 0 || textRotation != 360;

                // calc the diff of rotations so the text stamps
                int totalRot = pageRot - textRotation;

                // calc the page dimensions
                float pageWidth = pageRotated ? pageSize.getHeight()
                        : pageSize.getWidth();
                float pageHeight = pageRotated ? pageSize.getWidth()
                        : pageSize.getHeight();

                // determine the axis of rotation
                double centeredXPosition = pageRotated ? pageHeight / 2f
                        : (pageWidth - stringWidth) / 2f;
                double centeredYPosition = pageRotated ? (pageWidth - stringWidth) / 2f
                        : pageHeight / 2f;

                // append the content to the existing stream
                contentStream = new PDPageContentStream(pdfDoc, page, true,
                        true, true);
                contentStream.beginText();

                // set font and font size
                contentStream.setFont(font, fontSize);

                // set the stroke (text) hex255Color
                contentStream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);

                // if we are rotating, do it
                if (pageRotated) {

                    // rotate the text according to the calculations above
                    contentStream.setTextRotation(Math.toRadians(totalRot),
                            centeredXPosition, centeredYPosition);

                } else if (textRotated) {

                    // rotate the text according to the calculations above
                    contentStream.setTextRotation(Math.toRadians(textRotation),
                            xPosition, yPosition);

                } else {

                    // no rotate, just move it.
                    contentStream.setTextTranslation(xPosition, yPosition);
                }

                // stamp the damned text already
                contentStream.drawString(stampString);

                // close and clean up
                contentStream.endText();
                contentStream.close();
            }

        } catch (IOException e) {
            throw new ClientException(e);
        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (pdfDoc != null) {
                try {
                    pdfDoc.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return result;
    }

    public String getStampString() {
        return stampString;
    }

    public PDFWatermark setStampString(String inValue) {
        stampString = inValue;
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public PDFWatermark setFontFamily(String inValue) {
        fontFamily = inValue == null || inValue.isEmpty() ? DEFAULT_FONT_FAMILY
                : inValue;
        return this;
    }

    public float getFontSize() {
        return fontSize;
    }

    public PDFWatermark setFontSize(float inValue) {
        fontSize = inValue < 1f ? DEFAULT_FONT_SIZE : inValue;
        return this;
    }

    public int getTextRotation() {
        return textRotation;
    }

    public PDFWatermark setTextRotation(int inValue) {
        fontSize = inValue < 1 ? DEFAULT_TEXT_ROTATION : inValue;
        return this;
    }

    public String getHex255Color() {
        return hex255Color;
    }

    public PDFWatermark setHex255Color(String inValue) {
        hex255Color = inValue == null || inValue.isEmpty() ? DEFAULT_FONT_FAMILY
                : inValue;
        return this;
    }

    public float getXPosition() {
        return xPosition;
    }

    public PDFWatermark setXPosition(float inValue) {
        xPosition = inValue < 0 ? DEFAULT_X_POSITION : inValue;
        return this;
    }

    public float getYPosition() {
        return yPosition;
    }

    public PDFWatermark setYPosition(float inValue) {
        yPosition = inValue < 0 ? DEFAULT_Y_POSITION : yPosition;
        return this;
    }

    public boolean isInvertY() {
        return invertY;
    }

    public PDFWatermark setInvertY(boolean inValue) {
        invertY = inValue;
        return this;
    }
}
