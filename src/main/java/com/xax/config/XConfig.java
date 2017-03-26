package com.xax.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class XConfig extends Configuration {

    public static final String COMMENT_SEPARATOR = "##############################################################################";

    public XConfig(File file, String configVersion, boolean caseSensitiveCustomCategories) {
        super(file, configVersion, caseSensitiveCustomCategories);
    }
}
