package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.json.JsonLanguage;
import com.intellij.json.JsonParserDefinition;
import com.intellij.json.psi.impl.JsonFileImpl;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;

public class HarParserDefinition extends JsonParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(HarLanguage.INSTANCE);

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider fileViewProvider) {
        return new JsonFileImpl(fileViewProvider, HarLanguage.INSTANCE);
    }

}
