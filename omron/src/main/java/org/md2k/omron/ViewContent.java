package org.md2k.omron;

/**
 * Created by monowar on 6/22/16.
 */
public class ViewContent {
    private String name;
    private String platformType;

    public ViewContent(String name, String platformType) {
        this.name = name;
        this.platformType = platformType;
    }

    public String getName() {
        return name;
    }

    public String getPlatformType() {
        return platformType;
    }
}
