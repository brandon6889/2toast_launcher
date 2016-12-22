package net.minecraft;

import com.google.gson.annotations.SerializedName;

public class MinecraftLibraryRule {
    @SerializedName("action")
    public String action;
    @SerializedName("os")
    public OperatingSystem os;
}
