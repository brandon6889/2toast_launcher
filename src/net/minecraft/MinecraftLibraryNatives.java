package net.minecraft;

import com.google.gson.annotations.SerializedName;

public class MinecraftLibraryNatives {
    /* JSON fields */
    @SerializedName("windowsSha1")
    public String windowsSha1;
    @SerializedName("windowsSize")
    public Integer windowsSize;
    @SerializedName("osxSha1")
    public String osxSha1;
    @SerializedName("osxSize")
    public Integer osxSize;
    @SerializedName("linuxSha1")
    public String linuxSha1;
    @SerializedName("linuxSize")
    public Integer linuxSize;
    
    public String sha1() {
        switch(Util.getPlatform()) {
            case windows:
                return windowsSha1;
            case macos:
                return osxSha1;
            case linux:
                return linuxSha1;
            default:
                return "";
        }
    }
    
    public Integer size() {
        switch(Util.getPlatform()) {
            case windows:
                return windowsSize;
            case macos:
                return osxSize;
            case linux:
                return linuxSize;
            default:
                return -1;
        }
    }
}
