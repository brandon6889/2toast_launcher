package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MinecraftModList extends MinecraftResourceList {
    /* JSON fields */
    @SerializedName("mods")
    public LinkedList<MinecraftMod> mods;
    @SerializedName("coremods")
    public LinkedList<MinecraftCoreMod> coremods;
    
    private MinecraftResourceDownloader mDownloader;
    
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
    
    @Override
    protected MinecraftResourceDownloader createDownloader(String path, Object caller) throws Exception {
        mDownloader = new MinecraftResourceDownloader(path, caller);
        if (mods != null)
            mDownloader.addResources(mods);
        if (coremods != null)
            mDownloader.addResources(coremods);
        return mDownloader;
    }
}
