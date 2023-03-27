package com.linuxgods.kreiger.intellij.idea.har;

import com.intellij.json.JsonLanguage;

public class HarLanguage extends JsonLanguage {
    
    public final static HarLanguage INSTANCE = new HarLanguage();
    
    private HarLanguage() {
        super("HAR");
    }
    
    
}
