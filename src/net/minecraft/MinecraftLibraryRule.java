package net.minecraft;

import com.google.gson.annotations.SerializedName;

public class MinecraftLibraryRule {
    /* JSON fields */
    @SerializedName("action")
    public String action;
    @SerializedName("os")
    public OperatingSystem os;
    
    /**
     * Determine whether this rule applies to the provided operating system.
     * @param os Operating system
     * @return 
     */
    protected boolean supportsOS(String os) {
        return this.os != null && this.os.supportsOS(os);
    }
}
