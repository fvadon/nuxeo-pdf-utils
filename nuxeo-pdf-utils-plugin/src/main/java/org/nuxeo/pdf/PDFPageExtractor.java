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
 *     Thiabud Arguillere
 */
package org.nuxeo.pdf;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PageExtractor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Extract pages from a PDF
 *
 * @since 5.9.5
 */
public class PDFPageExtractor {

    private static Log log = LogFactory.getLog(PDFPageExtractor.class);

    protected Blob pdfBlob;

    public PDFPageExtractor(Blob inBlob) {

        pdfBlob = inBlob;
    }

    /**
     * Constructor with a <code>DocumentModel</code>. Default value for
     * <code>inXPath</code> (if passed <code>null</code> or "", if
     * <code>file:content</code>.
     *
     * @param inDoc
     * @param inXPath
     */
    public PDFPageExtractor(DocumentModel inDoc, String inXPath) {

        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        pdfBlob = (Blob) inDoc.getPropertyValue(inXPath);
    }

    public Blob extract(int inStartPage, int inEndPage) {
        return extract(inStartPage, inEndPage, null, null, null, null);
    }

    /**
     * Return a Blob built from page <code>inStartPage</code> to
     * <code>inEndPage</code> (inclusive).
     * <p>
     * If <code>inEndPage</code> is greater than the number of pages in the
     * source document, it will go to the end of the document. If
     * <code>inStartPage</code> is less than 1, it'll start with page 1. If
     * <code>inStartPage</code> is greater than <code>inEndPage</code> or
     * greater than the number of pages in the source document, a blank document
     * will be returned.
     * <p>
     * If fileName is null or "", if is set to the original name + the page
     * range: mydoc.pdf and pages 10-75 +> mydoc-10-75.pdf
     * <p>
     * The mimetype is always set to "application/pdf"
     * <p>
     * Can set the title, subject and author of the resulting PDF.
     * <b>Notice</b>: If the value is null or "", it is just ignored
     *
     * @param inStartPage
     * @param inEndPage
     * @param inFileName
     * @param inTitle
     * @param inSubject
     * @param inAuthor
     * @return FileBlob
     *
     */
    public Blob extract(int inStartPage, int inEndPage, String inFileName,
            String inTitle, String inSubject, String inAuthor) {

        Blob result = null;
        PDDocument pdfDoc = null;
        PDDocument extracted = null;

        try {
            pdfDoc = PDDocument.load(pdfBlob.getStream());

            PageExtractor pe = new PageExtractor(pdfDoc, inStartPage, inEndPage);
            extracted = pe.extract();

            PDFUtils.setInfos(extracted, inTitle, inSubject, inAuthor);

            result = PDFUtils.saveInTempFile(extracted);

            result.setMimeType("application/pdf");

            if (inFileName == null || inFileName.isEmpty()) {
                String originalName = pdfBlob.getFilename();
                if (originalName == null || originalName.isEmpty()) {
                    originalName = "extracted";
                } else {
                    int pos = originalName.toLowerCase().lastIndexOf(".pdf");
                    if (pos > 0) {
                        originalName = originalName.substring(0, pos);
                    }

                }
                inFileName = originalName + "-" + inStartPage + "-" + inEndPage
                        + ".pdf";
            }
            result.setFilename(inFileName);
            extracted.close();

        } catch (IOException | COSVisitorException e) {
            throw new ClientException(e);
        } finally {
            if (pdfDoc != null) {
                try {
                    pdfDoc.close();
                } catch (IOException e) {
                    log.error("Error closing the PDDocument", e);
                }
            }
            if (extracted != null) {
                try {
                    extracted.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        return result;
    }

}
