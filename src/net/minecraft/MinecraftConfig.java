package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.MalformedURLException;

public class MinecraftConfig implements MinecraftResource {
    /* JSON fields */
    @SerializedName("configBlobName")
    public String configBlobName;
    @SerializedName("configBlobSize")
    public Integer configBlobSize;
    @SerializedName("configBlobHash")
    public String configBlobHash;
    // These can become JSON objects in the future of the same type. One enforced, the other not. Make an apply method.
    @SerializedName("defaultSettings")
    public String defaultSettings;
    @SerializedName("overrideSettings")
    public String overrideSettings;
    
    @Override
    public int getSize() throws MalformedURLException, IOException {
        return configBlobSize;
    }
    
    /**
     * Generates a path for fetching/storing the library. Used for launch command.
     * @return path
     */
    @Override
    public String getPath() {
        return "blob/" + configBlobName;
    }

    @Override
    public void download(MinecraftResourceDownloader downloader) throws Exception {
        downloader.download(this);
    }

    @Override
    public String getHash() throws Exception {
        return configBlobHash;
    }

    @Override
    public String getName() {
        return configBlobName;
    }
}
