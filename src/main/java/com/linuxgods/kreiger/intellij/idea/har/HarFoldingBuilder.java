package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.json.editor.folding.JsonFoldingBuilder;
import com.intellij.json.psi.JsonNumberLiteral;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.json.JsonUtil.getPropertyValueOfType;

public class HarFoldingBuilder extends JsonFoldingBuilder {

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
        FoldingDescriptor[] foldingDescriptors = super.buildFoldRegions(node, document);
        for (FoldingDescriptor foldingDescriptor : foldingDescriptors) {
            ASTNode element = foldingDescriptor.getElement();
            PsiElement psiElement = element.getPsi();
            if (!(psiElement instanceof JsonObject jsonObject)) {
                continue;
            }
            JsonStringLiteral startedDateTime = getPropertyValueOfType(jsonObject, "startedDateTime", JsonStringLiteral.class);
            JsonObject request = getPropertyValueOfType(jsonObject, "request", JsonObject.class);
            JsonObject response = getPropertyValueOfType(jsonObject, "response", JsonObject.class);
            if (null == startedDateTime || null == request || null == response) continue;
            JsonStringLiteral methodLiteral = getPropertyValueOfType(request, "method", JsonStringLiteral.class);
            JsonStringLiteral urlLiteral = getPropertyValueOfType(request, "url", JsonStringLiteral.class);
            JsonNumberLiteral statusLiteral = getPropertyValueOfType(response, "status", JsonNumberLiteral.class);
            JsonStringLiteral statusTextLiteral = getPropertyValueOfType(response, "statusText", JsonStringLiteral.class);
            JsonStringLiteral redirectURLLiteral = getPropertyValueOfType(response, "redirectURL", JsonStringLiteral.class);
            JsonObject content = getPropertyValueOfType(response, "content", JsonObject.class);
            if (null == methodLiteral || null == urlLiteral || null == statusLiteral || null == statusTextLiteral) {
                continue;
            }
            String url = truncate(urlLiteral.getValue());
            String responseStatus = statusLiteral.getText() + " " + statusTextLiteral.getValue();
            if (null != content) {
                JsonNumberLiteral contentSize = getPropertyValueOfType(content, "size", JsonNumberLiteral.class);
                if (null != contentSize) {
                    long size = Long.parseLong(contentSize.getText());
                    if (size > 0)
                        responseStatus += " " + toHumanReadableSize(size);
                }
            }
            String placeholderText = startedDateTime.getValue() + "  " + responseStatus + " <- " + methodLiteral.getValue() + " " + url;
            if (redirectURLLiteral != null) {
                String redirectURL = redirectURLLiteral.getValue();
                if (!redirectURL.isBlank())
                    placeholderText += " -> "+truncate(redirectURL);
            }
            foldingDescriptor.setPlaceholderText(placeholderText);
        }
        return foldingDescriptors;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        ASTNode parent1 = node.getTreeParent();
        if (parent1 == null) return false;
        ASTNode parent2 = parent1.getTreeParent();
        return parent2 != null;
    }

    @NotNull
    private static String truncate(String url) {
        if (url.length() > 70) {
            int i = url.indexOf('?');
            if (-1 < i && i < 70) {
                return url.substring(0, i+1)+"...";
            }
            
            url = url.substring(0, 37) + "..."+url.substring(url.length()-30);
        }
        return url;
    }

    private final static List<String> SIZE_SUFFIXES = List.of("B ", "kB", "MB", "GB", "TB");
    public static String toHumanReadableSize(long size) {
        for (String suffix : SIZE_SUFFIXES) {
            if (size < 1024L) {
                return String.format("%4d %s", size, suffix);
            }
            size >>= 10;
        }
        return String.format("%4d %s", size, "PB");
    }
}
