package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.MalformedURLException;

public class MinecraftResourcesBlob implements MinecraftResource {
    /* JSON fields */
    @SerializedName("resourceBlobName")
    public String resourceBlobName;
    @SerializedName("resourceBlobSize")
    public Integer resourceBlobSize;
    @SerializedName("resourceBlobHash")
    public String resourceBlobHash;
    
    @Override
    public int getSize() throws MalformedURLException, IOException {
        return resourceBlobSize;
    }
    
    /**
     * Generates a path for fetching/storing the library. Used for launch command.
     * @return path
     */
    @Override
    public String getPath() {
        return "blob/" + resourceBlobName;
    }

    @Override
    public void download(MinecraftResourceDownloader downloader) throws Exception {
        downloader.download(this);
    }

    @Override
    public String getHash() throws Exception {
        return resourceBlobHash;
    }

    @Override
    public String getName() {
        return resourceBlobName;
    }
}
