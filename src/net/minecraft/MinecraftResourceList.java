package net.minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class MinecraftResourceList {
    /**
     * Create a resource downloader for this set of resources.
     * @param path
     * @param caller
     * @return new resource downloader.
     * @throws Exception 
     */
    protected abstract MinecraftResourceDownloader createDownloader(String path, Object caller) throws Exception;
    
    /**
     * Create list of files in directory recursively.
     * @param dir Directory to search.
     * @return List of files in subfolders.
     */
    static List<File> enumFiles(File dir) {
        List<File> files = new ArrayList();
        for (File f : dir.listFiles()) {
            if (f.isDirectory())
                files.addAll(enumFiles(f));
            else
                files.add(f);
        }
        return files;
    }
}
