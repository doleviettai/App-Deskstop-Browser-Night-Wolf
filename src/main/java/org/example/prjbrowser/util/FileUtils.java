package org.example.prjbrowser.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FileUtils {

    public static String extractTextFromFile(byte[] fileData, String fileName) throws IOException {

        String lower = fileName.toLowerCase();

        // ➤ Xử lý PDF
        if (lower.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileData))) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }

        // ➤ Xử lý DOCX
        else if (lower.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileData))) {
                return doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .collect(Collectors.joining("\n"));
            }
        }

        // ➤ Xử lý TXT (UTF-8)
        else if (lower.endsWith(".txt")) {
            return new String(fileData, StandardCharsets.UTF_8);
        }

        // ➤ Không hỗ trợ
        else {
            return "Unsupported file type: " + fileName;
        }
    }
}
