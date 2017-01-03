package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MinecraftMod implements MinecraftResource {
    /* JSON fields */
    @SerializedName("name")
    public String name;
    @SerializedName("size")
    public Integer size;
    @SerializedName("hash")
    public String hash;
    
    private String mUrl;
    private int mSize = -1;
    
    public MinecraftMod(String name) {
        this.name = name;
    }
    
    @Override
    public int getSize() throws MalformedURLException, IOException {
        if (mUrl == null)
            mUrl = GameUpdater.SERVER_URL+getPath();
        if (mSize == -1) {
            URLConnection urlconnection = new URL(mUrl).openConnection();
            urlconnection.setDefaultUseCaches(false);
            if ((urlconnection instanceof HttpURLConnection)) {
                ((HttpURLConnection) urlconnection).setRequestMethod("HEAD");
            }
            mSize = urlconnection.getContentLength();
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        return name;
    }
}
