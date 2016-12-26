package net.minecraft;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MinecraftMod implements MinecraftResource {
    final String mName;
    final String mVer;
    private String mUrl;
    private int mSize = -1;
    
    public MinecraftMod(String name, String ver) {
        mName = name;
        mVer = ver;
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
        return "mods/" + mVer + "/" + mName;
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
        return mName;
    }
}
