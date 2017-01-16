package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MinecraftAssets {
    /* JSON fields */
    @SerializedName("objects")
    public Map<String, MinecraftAssetsObject> objects;
    
    private MinecraftResourceDownloader mDownloader;
    
    /**
     * Must be called after instantiation by Gson. Copies keys into objects as names.
     */
    protected void initialize() {
        for (Entry<String, MinecraftAssetsObject> e : objects.entrySet())
            e.getValue().name = e.getKey();
    }
    
    /**
     * Build virtual asset dir for legacy games. Populates assets/virtual with
     * copies of assets given vanity names. Deletes virtual assets that don't
     * belong.
     * @param path Minecraft home folder.
     */
    protected void buildVirtualDir(String path) throws IOException {
        File virtualDir = new File(path+"assets/virtual/");
        virtualDir.mkdirs();
        List<File> currentFiles = Util.enumFiles(virtualDir);
        List<String> virtualFiles = new ArrayList();
        for (File f : currentFiles)
            virtualFiles.add(f.getPath().substring(virtualDir.getPath().length()));
        
        /* Delete unneeded assets */
        List<String> assetFiles = new ArrayList();
        for (MinecraftAssetsObject o : objects.values())
            assetFiles.add(o.getName());
        for (String s : virtualFiles)
            if (!assetFiles.contains(s))
                new File(virtualDir + File.separator + s).delete();
        
        /* Add missing assets */
        for (MinecraftAssetsObject o : objects.values())
            if (!virtualFiles.contains(o.getName())) {
                File dest = new File(virtualDir+File.separator+o.getName());
                dest.mkdirs();
                Files.copy(new File(path + o.getPath()).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
    }
    
    protected MinecraftResourceDownloader createDownloader(String path, Object caller) throws Exception {
        ArrayList<MinecraftResource> o = new ArrayList();
        o.addAll(objects.values());
        mDownloader = new MinecraftResourceDownloader(path, caller);
        mDownloader.addResources(o);
        mDownloader.sortResources(MinecraftResource.SIZESORT);
        mDownloader.setConcurrentDownloads(4);
        return mDownloader;
    }
}
