package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonElementGenerator;
import com.intellij.json.psi.JsonElementVisitor;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElementVisitor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static com.intellij.codeInspection.ProblemHighlightType.*;
import static com.intellij.json.JsonUtil.*;
import static com.intellij.psi.PsiElementVisitor.*;

public class HarContentInspection extends LocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!holder.getFile().getLanguage().is(HarLanguage.INSTANCE)) return EMPTY_VISITOR;
        int lineWidthLimit = AdvancedSettings.getInt("editor.soft.wrap.force.limit");

        Document document = PsiDocumentManager.getInstance(holder.getProject()).getDocument(holder.getFile());
        if (document == null) return EMPTY_VISITOR;

        return new JsonElementVisitor() {

            @Override
            public void visitObject(@NotNull JsonObject o) {
                if (!(o.getParent() instanceof JsonProperty property)) return;
            }

            @Override
            public void visitStringLiteral(@NotNull JsonStringLiteral stringLiteral) {
                if (stringLiteral.isPropertyName()) return;
                int lineWidth = getLineWidth(stringLiteral);
                if (lineWidth > lineWidthLimit && lineWidth - stringLiteral.getTextLength() < lineWidthLimit) {
                    holder.registerProblem(stringLiteral, "String exceeds soft wrap length limit",
                            WARNING, new LocalQuickFix() {
                                @Override
                                public @IntentionFamilyName @NotNull String getFamilyName() {
                                    return "Replace with empty string";
                                }

                                @Override
                                public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                                    descriptor.getPsiElement().replace(new JsonElementGenerator(project).createStringLiteral(""));
                                }
                            });
                } else if (!isOnTheFly) {
                    return;
                }
                if (!(stringLiteral.getParent() instanceof JsonProperty textProperty)) return;
                if (!"text".equals(textProperty.getName())) return;
                if (!(textProperty.getParent() instanceof JsonObject contentObject)) return;
                if (!(contentObject.getParent() instanceof JsonProperty contentProperty)) return;
                if (!"content".equals(contentProperty.getName())) return;
                if (!(contentProperty.getParent() instanceof JsonObject responseObject)) return;
                if (!(responseObject.getParent() instanceof JsonProperty responseProperty)) return;
                if (!"response".equals(responseProperty.getName())) return;
                if (!(responseProperty.getParent() instanceof JsonObject entryObject)) return;
                JsonObject requestObject = getPropertyValueOfType(entryObject, "request", JsonObject.class);
                if (null == requestObject) return;
                JsonStringLiteral urlLiteral = getPropertyValueOfType(requestObject, "url", JsonStringLiteral.class);
                if (null == urlLiteral) return;
                String url = urlLiteral.getValue();
                String mimeType = getStringProperty(contentObject, "mimeType").orElse(null);
                String encoding = getStringProperty(contentObject, "encoding").orElse(null);
                
                String filename = getContentDisposition(responseObject)
                        .map(FilenameUtils::parseFilenameFromContentDisposition)
                        .orElseGet(() -> FilenameUtils.inferFilenameFromUrl(url, mimeType));
                VirtualFile virtualFile = holder.getFile().getVirtualFile();

                VirtualFile fileByRelativePath = virtualFile.findFileByRelativePath(filename);
                if (fileByRelativePath != null) return;
                VirtualFile parent = virtualFile.getParent();
                
                holder.registerProblem(stringLiteral, "Content can be saved to file", isOnTheFly ? INFORMATION : WARNING,
                        new LocalQuickFix() {
                            @Override
                            public @IntentionFamilyName @NotNull String getFamilyName() {
                                return "Save to file and replace with JSON reference (non-standard)";
                            }

                            @Override
                            public @IntentionName @NotNull String getName() {
                                return "Save to file '"+filename+"' and replace with JSON reference (non-standard)";
                            }

                            @Override
                            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                                JsonStringLiteral stringLiteral = (JsonStringLiteral) descriptor.getPsiElement();
                                String value = stringLiteral.getValue();
                                byte[] bytes = "base64".equals(encoding) ? 
                                        Base64.getDecoder().decode(value) :
                                        value.getBytes(StandardCharsets.UTF_8);
                                try {
                                    VirtualFile file = parent.createChildData(this, filename);
                                    try (OutputStream outputStream = file.getOutputStream(this)) {
                                        outputStream.write(bytes);
                                    }
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                                stringLiteral.replace(new JsonElementGenerator(project).createObject("\"$ref\":\"./" + StringEscapeUtils.escapeJson(filename) + "\""));
                            }
                        });
            }

            private Optional<String> getContentDisposition(JsonObject responseObject) {
                JsonArray headersArray = getPropertyValueOfType(responseObject, "headers", JsonArray.class);
                return Stream.ofNullable(headersArray)
                        .flatMap(jsonArray -> jsonArray.getValueList().stream())
                        .filter(jsonValue -> jsonValue instanceof JsonObject)
                        .map(jsonValue -> (JsonObject) jsonValue)
                        .flatMap(jsonObject -> Stream.ofNullable(getPropertyValueOfType(jsonObject, "name", JsonStringLiteral.class))
                                .filter(nameLiteral -> "content-disposition".equals(nameLiteral.getValue().toLowerCase(Locale.ROOT)))
                                .map(nameLiteral -> jsonObject))
                        .flatMap(jsonObject -> Stream.ofNullable(getPropertyValueOfType(jsonObject, "value", JsonStringLiteral.class))
                        .map(JsonStringLiteral::getValue))
                        .findFirst();
            }

            private int getLineWidth(@NotNull JsonStringLiteral stringLiteral) {
                int textOffset = stringLiteral.getTextOffset();
                int lineNumber = document.getLineNumber(textOffset);
                int lineStart = document.getLineStartOffset(lineNumber);
                int lineEnd = document.getLineEndOffset(lineNumber);
                return lineEnd - lineStart;
            }
        };
    }

    private static Optional<String> getStringProperty(JsonObject jsonObject, String name) {
        return Optional.ofNullable(getPropertyValueOfType(jsonObject, name, JsonStringLiteral.class))
                .map(JsonStringLiteral::getValue);
    }

}
