package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HarFileType extends JsonFileType {
    private final static HarFileType INSTANCE = new HarFileType();

    private HarFileType() {
        super(HarLanguage.INSTANCE);
    }
    
    
    @Override
    public @NonNls @NotNull String getName() {
        return "HAR";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "HTTP Archive file";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "har";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Json;
    }

}
