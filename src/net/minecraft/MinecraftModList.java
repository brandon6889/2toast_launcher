package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedList;
import java.util.List;

public class MinecraftModList {
    /* JSON fields */
    @SerializedName("mods")
    public LinkedList<MinecraftMod> mods;
    @SerializedName("coremods")
    public LinkedList<MinecraftCoreMod> coremods;
    
    /**
     * Generate list of coremod paths. Used to generate classpath.
     * @return 
     */
    protected List<String> getCoremodPaths() {
        LinkedList<String> l = new LinkedList();
        if (coremods != null)
            for (MinecraftCoreMod r : coremods)
                l.add(r.getPath());
        return l;
    }
}
