package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class MinecraftAssets {
    /* JSON fields */
    @SerializedName("objects")
    public ArrayList<MinecraftAssetsObject> objects;
    
    private MinecraftResourceDownloader mDownloader;
    
    /**
     * Build virtual asset dir for legacy games. Populates assets/virtual with
     * copies of assets given vanity names. Deletes virtual assets that don't
     * belong.
     * @param path Minecraft home folder.
     */
    protected void buildVirtualDir(String path) throws IOException {
        File virtualDir = new File(path+"assets/virtual/");
        virtualDir.mkdirs();
        List<File> currentFiles = enumFiles(virtualDir);
        List<String> virtualFiles = new ArrayList();
        for (File f : currentFiles)
            virtualFiles.add(f.getPath().substring(virtualDir.getPath().length()));
        
        /* Delete unneeded assets */
        List<String> assetFiles = new ArrayList();
        for (MinecraftAssetsObject o : objects)
            assetFiles.add(o.getName());
        for (String s : virtualFiles)
            if (!assetFiles.contains(s))
                new File(virtualDir + File.separator + s).delete();
        
        /* Add missing assets */
        for (MinecraftAssetsObject o : objects)
            if (!virtualFiles.contains(o.getName())) {
                File dest = new File(virtualDir+File.separator+o.getName());
                dest.mkdirs();
                Files.copy(new File(path + o.getPath()).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
    }
    
    /**
     * Create list of files in directory recursively.
     * @param dir Directory to search.
     * @return List of files in subfolders.
     */
    private List<File> enumFiles(File dir) {
        List<File> files = new ArrayList();
        for (File f : dir.listFiles()) {
            if (f.isDirectory())
                files.addAll(enumFiles(f));
            else
                files.add(f);
        }
        return files;
    }
    
    protected void download(String path) throws Exception {
        ArrayList<MinecraftResource> o = new ArrayList();
        o.addAll(objects);
        mDownloader = new MinecraftResourceDownloader(path, this);
        mDownloader.addResources(o);
        mDownloader.sortResources(MinecraftResource.SIZESORT);
        mDownloader.setConcurrentDownloads(10);
        mDownloader.download();
    }
    
    protected int getProgress() throws Exception {
        return mDownloader.getProgress();
    }
}
