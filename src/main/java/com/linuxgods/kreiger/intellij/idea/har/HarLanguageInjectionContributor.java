package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.lang.Language;
import com.intellij.lang.injection.general.Injection;
import com.intellij.lang.injection.general.LanguageInjectionContributor;
import com.intellij.lang.injection.general.SimpleInjection;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.intellij.json.JsonUtil.getPropertyValueOfType;

public class HarLanguageInjectionContributor implements LanguageInjectionContributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HarLanguageInjectionContributor.class);

    @Override
    public @Nullable Injection getInjection(@NotNull PsiElement context) {
        if (!context.getContainingFile().getLanguage().is(HarLanguage.INSTANCE)) return null;
        if (!(context instanceof JsonStringLiteral stringLiteral)) return null;
        if (!(stringLiteral.getParent() instanceof JsonProperty textProperty)) return null;
        if (!"text".equals(textProperty.getName())) return null;
        if (!(textProperty.getParent() instanceof JsonObject contentObject)) return null;
        if (!(contentObject.getParent() instanceof JsonProperty contentProperty)) return null;
        if (!"content".equals(contentProperty.getName())) return null;
        if (!(contentProperty.getParent() instanceof JsonObject responseObject)) return null;
        if (!(responseObject.getParent() instanceof JsonProperty responseProperty)) return null;
        if (!"response".equals(responseProperty.getName())) return null;
        if (!(responseProperty.getParent() instanceof JsonObject entryObject)) return null;
        JsonObject requestObject = getPropertyValueOfType(entryObject, "request", JsonObject.class);
        if (null == requestObject) return null;
        JsonStringLiteral mimeTypeLiteral = getPropertyValueOfType(contentObject, "mimeType", JsonStringLiteral.class);
        if (null == mimeTypeLiteral) return null;
        String mimeType = mimeTypeLiteral.getValue();

        Language language = Language.findInstancesByMimeType(mimeType).stream()
                .findFirst().orElse(null);
        if (language == null) return null;

        LOGGER.warn("inject {} {}", context, language);
        
        return new SimpleInjection(language, "", "", null);

    }
}
