package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;

public class MinecraftAssetsObject implements MinecraftResource {
    /* JSON fields */
    @SerializedName("name")
    public String name;
    @SerializedName("hash")
    public String hash;
    @SerializedName("size")
    public Integer size;
    
    /* Internal data */
    private int mFileSize = -1;
    
    /**
     * Proxy for file size. If the size was provided by the asset index, use
     * given value. Else fetch size from server on first call.
     * @return Size of library file in bytes.
     * @throws java.net.MalformedURLException
     */
    @Override
    public int getSize() throws MalformedURLException, IOException {
        if (mFileSize == -1) {
            if (size != null) {
                mFileSize = size;
            } else {
                URLConnection urlconnection = getUrl().openConnection();
                urlconnection.setDefaultUseCaches(false);
                if ((urlconnection instanceof HttpURLConnection)) {
                    ((HttpURLConnection) urlconnection).setRequestMethod("HEAD");
                }
                mFileSize = urlconnection.getContentLength();
            }
        }
        return mFileSize;
    }
    
    /**
     * Generates a path for fetching/storing the asset.
     * @return path
     */
    @Override
    public String getPath() {
        return "assets/objects/" + hash.substring(0,2) + "/" + hash;
    }
    
    /**
     * Get legacy/vanity name for resource.
     * @return file path/name
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHash() throws Exception {
        return hash;
    }

    @Override
    public void download(MinecraftResourceDownloader downloader) throws Exception {
        downloader.download(this);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        try {
            return getHash().equals(((MinecraftAssetsObject)o).getHash());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}
