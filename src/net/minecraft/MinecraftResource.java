package net.minecraft;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common interface for Minecraft resources.
 */
public interface MinecraftResource {
    
    static final Comparator<MinecraftResource> SIZESORT = new SizeSort();
    
    /**
     * Return the file size of this resource. Expected to serve as a proxy when
     * the size is not known from an index file.
     * @return File size.
     * @throws MalformedURLException Invalid URL generated trying to fetch size.
     * @throws IOException Unable to fetch size due to IO error.
     */
    public int getSize() throws MalformedURLException, IOException;
    
    /**
     * Return the relative path of the file. With respect to download root or
     * local Minecraft folder root.
     * @return File path.
     */
    public String getPath();
    
    /**
     * Download the file using a resource downloader.
     * @param downloader Resource downloader.
     * @throws Exception Any exception that may unexpectedly occur.
     */
    public void download(MinecraftResourceDownloader downloader) throws Exception;
    
    /**
     * Get expected hash value of file.
     * @return Hexadecimal representation of SHA1 hash.
     * @throws Exception Hash is unknown.
     */
    public String getHash() throws Exception;
    
    /**
     * Get name of object. This isn't particularly well-defined. Just go with it.
     * @return Name of object.
     */
    public String getName();
    
    /**
     * Get URL of object. Used to downloaded the resource.
     * @return URL string.
     * @throws java.net.MalformedURLException
     */
    default public URL getUrl() throws MalformedURLException {
        return new URL(GameUpdater.SERVER_URL + getPath());
    }
    
    /**
     * Size sort comparator. Used to optimize download order.
     */
    static class SizeSort implements Comparator<MinecraftResource> {
        /**
         * Compare assets by file size. All assets must be initialized by calling
         * getSize() before using the comparator.
         * @param t1
         * @param t2
         * @return 
         */
        @Override
        public int compare(MinecraftResource t1, MinecraftResource t2) {
            try {
                int size1 = t1.getSize();
                int size2 = t2.getSize();
                if (size1 > size2)
                    return 1;
                else if (size1 == size2)
                    return 0;
                else
                    return -1;
            } catch (IOException ex) {
                Logger.getLogger(MinecraftResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        }
    }
}
