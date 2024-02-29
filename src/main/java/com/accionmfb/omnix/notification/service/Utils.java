package com.accionmfb.omnix.notification.service;


import com.accionmfb.omnix.notification.payload.Attachment;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.EmailAttachment;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.accionmfb.omnix.notification.constant.ApiPaths.TOKEN_PREFIX;

@Slf4j
public class Utils {

    private static final int MAX_JSOUP_PARSE_TIMEOUT = 10000;
    private static final String PDF_EXTENSION = ".pdf";

    public static String convertHtmlToPdf(String htmlString, String pdfDocName){
        Document doc = Jsoup.parse(htmlString, Strings.EMPTY, Parser.htmlParser());
        File currentFolder = new File(".");
        String folderAbsPath = currentFolder.getAbsolutePath();
        String pdfAbsPath = folderAbsPath.concat(pdfDocName);
        try (OutputStream os = new FileOutputStream(pdfAbsPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withUri(pdfAbsPath);
            builder.toStream(os);
            builder.withW3cDocument(new W3CDom().fromJsoup(doc), "/");
            builder.run();
        }catch (Exception exception){
            log.error("Exception message: {}", exception.getMessage());
            exception.printStackTrace();
            throw new RuntimeException(exception.getMessage());
        }

        return pdfAbsPath;
    }

    public static String downLoadHtmlLinkAsPdf(String link, String pdfDocName){
        try {
            URL url = new URL(link);
            Document htmlDoc = Jsoup.parse(url, MAX_JSOUP_PARSE_TIMEOUT);
            String htmlString = htmlDoc.outerHtml();
            return convertHtmlToPdf(htmlString, pdfDocName);
        }catch (Exception exception){
            exception.printStackTrace();
            throw new RuntimeException(exception.getMessage());
        }
    }

    public static String downLoadContextualHtmlLinkAsPdf(String link, Map<String, Object> contextData, String pdfDocName){
        try {
            URL url = new URL(link);
            Document htmlDoc = Jsoup.parse(url, MAX_JSOUP_PARSE_TIMEOUT);
            String htmlString = htmlDoc.outerHtml();
            return convertHtmlToPdf(formatHtmlStringWithContext(htmlString, contextData), pdfDocName);
        }catch (Exception exception){
            exception.printStackTrace();
            throw new RuntimeException(exception.getMessage());
        }
    }

    public static File downLoadContextualHtmlLinkAsPdfFile(String link, Map<String, Object> contextData, String pdfDocName){
        return new File(downLoadContextualHtmlLinkAsPdf(link, contextData, pdfDocName));
    }

    private static String formatHtmlStringWithContext(String htmlString, Map<String, Object> contextData){
        String result = htmlString;
        for(Map.Entry<String, Object> entry : contextData.entrySet()){
            String key = entry.getKey();
            String replaceable = "\\{".concat(key).concat("}");
            String replacement = entry.getValue() + Strings.EMPTY;
            result = result.replaceAll(replaceable, replacement);
        }
        return result;
    }

    public static EmailAttachment convertAttachmentToEmailAttachment(Attachment attachment){
        String link = attachment.getLink();
        Map<String, Object> contextData = attachment.getAttachmentContextData();
        String localFileName = "ATT_".concat(String.valueOf(System.currentTimeMillis()));
        File file = Utils.downLoadContextualHtmlLinkAsPdfFile(link, contextData, localFileName);
        String name = attachment.getName().endsWith(PDF_EXTENSION) ? attachment.getName() : attachment.getName().concat(PDF_EXTENSION);
        EmailAttachment emailAttachment = new EmailAttachment();
        emailAttachment.setDescription(attachment.getDescription());
        emailAttachment.setName(name);
        emailAttachment.setDisposition(EmailAttachment.ATTACHMENT);
        emailAttachment.setPath(file.getAbsolutePath());
        return emailAttachment;
    }

    public static <T> T returnOrDefault(T value, T defaultValue){
        return Objects.isNull(value) ? defaultValue : value;
    }

    public static boolean isNullOrEmpty(Object object){
        return Objects.isNull(object) || String.valueOf(object).isEmpty();
    }

    public static boolean nonNullOrEmpty(Object object){
        return !isNullOrEmpty(object);
    }
}
