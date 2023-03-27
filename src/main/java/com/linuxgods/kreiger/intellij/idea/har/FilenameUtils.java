package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameUtils {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"?([^\";\\\\/]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\.\\w+$");

    public static String parseFilenameFromContentDisposition(String contentDisposition) {
        Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String decoded = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
            return Path.of(decoded).normalize().getFileName().toString();
            
        }
        return null;
    }

    public static String inferFilenameFromUrl(String url, String mimeType) {
        String path = URI.create(url).getPath();
        String[] pathParts = path.split("/");
        String filename = pathParts[pathParts.length - 1];

        // If the filename doesn't have an extension, attempt to add one from the mimeType
        if (!EXTENSION_PATTERN.matcher(filename).find() && mimeType != null && !mimeType.isEmpty()) {
            FileTypeManagerEx fileTypeManagerEx = FileTypeManagerEx.getInstanceEx();
            Optional<String> ext = Language.findInstancesByMimeType(mimeType).stream()
                    .map(fileTypeManagerEx::findFileTypeByLanguage)
                    .filter(Objects::nonNull)
                    .map(FileType::getDefaultExtension)
                    .findFirst();
            if (ext.isPresent()) return filename+"."+ext.get();
        }

        return filename;
    }
}
