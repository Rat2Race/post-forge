package dev.iamrat.collector.item.domain;

public class CollectedItemTextSanitizer {

    public String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replaceAll("<[^>]*>", "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replaceAll("&#\\d+;", "")
            .replaceAll("&[a-zA-Z]+;", " ")
            .trim();
    }
}
