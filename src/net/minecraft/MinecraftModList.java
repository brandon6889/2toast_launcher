package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MinecraftModList {
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
    
    protected MinecraftResourceDownloader createDownloader(String path, Object caller) throws Exception {
        mDownloader = new MinecraftResourceDownloader(path, caller);
        if (mods != null)
            mDownloader.addResources(mods);
        if (coremods != null)
            mDownloader.addResources(coremods);
        return mDownloader;
    }
    
    /**
     * Delete mods that should no longer be installed.
     * @param path Minecraft home folder.
     * @throws IOException 
     */
    protected void cleanup(String path) throws IOException {
        File modDir = new File(path+"mods/"+GameUpdater.getUpdater("dummy").getMCVersion()+"/");
        File coremodDir = new File(path+"coremods/"+GameUpdater.getUpdater("dummy").getMCVersion()+"/");
        modDir.mkdirs();
        coremodDir.mkdirs();
        if (coremods == null)
            Util.delete(coremodDir);
        if (mods == null) {
            Util.delete(modDir);
        } else {
            List<File> currentFiles = Util.enumFiles(modDir);
            List<String> modFiles = new ArrayList();
            for (File f : currentFiles)
                modFiles.add(f.getPath().substring(modDir.getPath().length()+1));

            /* Delete outdated mods */
            List<String> desiredFiles = new ArrayList();
            for (MinecraftResource o : mods)
                desiredFiles.add(o.getName());
            for (String s : modFiles)
                if (!desiredFiles.contains(s))
                    new File(modDir + File.separator + s).delete();
        }
    }
}
