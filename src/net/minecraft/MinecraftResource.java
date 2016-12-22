package net.minecraft;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Common interface for downloadable resources.
 */
public interface MinecraftResource {
    /**
     * Return the file size of this resource. Expected to serve as a proxy when
     * the size is not known from an index file.
     * @return File size.
     * @throws MalformedURLException Invalid URL generated trying to fetch size.
     * @throws IOException Unable to fetch size due to IO error.
     */
    public int getSize() throws MalformedURLException, IOException;
    
    /**
     * Return the path of the file. For most cases, used both server and client
     * side. Exception would be legacy games which may expect vanity asset names.
     * @return File path.
     */
    public String getPath();
    
    /**
     * Download the file using a resource downloader.
     * @param downloader Resource downloader.
     * @throws Exception Any exception that may unexpectedly occur.
     */
    public void download(MinecraftResourceDownloader downloader) throws Exception;
}
