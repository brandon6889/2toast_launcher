package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;

public class MinecraftMod implements MinecraftResource {
    /* JSON fields */
    @SerializedName("name")
    public String name;
    @SerializedName("size")
    public Integer size;
    @SerializedName("hash")
    public String hash;
    
    /**
     * Note for future reference: this initialization does not occur if a
     * constructor is defined. What a headache.
     */
    private int mSize = -1;
    
    @Override
    public int getSize() throws MalformedURLException, IOException {
        if (mSize == -1) {
            if (size != null) {
                mSize = size;
            } else {
                URLConnection urlconnection = getUrl().openConnection();
                urlconnection.setDefaultUseCaches(false);
                if ((urlconnection instanceof HttpURLConnection)) {
                    ((HttpURLConnection) urlconnection).setRequestMethod("HEAD");
                }
                mSize = urlconnection.getContentLength();
            }
        }
        return mSize;
    }

    @Override
    public String getPath() {
        return "mods/" + GameUpdater.getUpdater("dummy").getMCVersion() + "/" + name;
    }

    @Override
    public void download(MinecraftResourceDownloader downloader) throws Exception {
        downloader.download(this);
    }

    @Override
    public String getHash() throws Exception {
        if (hash != null)
            return hash;
        throw new Exception("No hash available");
    }

    @Override
    public String getName() {
        return name;
    }
}
