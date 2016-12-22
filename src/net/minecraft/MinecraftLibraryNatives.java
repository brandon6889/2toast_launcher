package net.minecraft;

public class MinecraftLibraryNatives {
    /* JSON fields */
    public String windowsSha1;
    public Integer windowsSize;
    public String osxSha1;
    public Integer osxSize;
    public String linuxSha1;
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
    
    public int size() {
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
