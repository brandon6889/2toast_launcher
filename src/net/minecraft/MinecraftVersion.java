package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

public class MinecraftVersion implements MinecraftResource {
    /* JSON fields */
    @SerializedName("minecraftArguments")
    public String minecraftArguments;
    @SerializedName("mainClass")
    public String mainClass;
    @SerializedName("time")
    public String time;
    @SerializedName("id")
    public String id;
    @SerializedName("type")
    public String type;
    @SerializedName("processArguments")
    public String processArguments;
    @SerializedName("releaseTime")
    public String releaseTime;
    @SerializedName("assets")
    public String assets;
    @SerializedName("minimumLauncherVersion")
    public int minimumLauncherVersion;
    @SerializedName("libraries")
    public LinkedList<MinecraftLibrary> libraries;
    
    /* Internal data */
    private int mFileSize = -1;
    private String mUrl = "";
    
    /**
     * Proxy for file size. If the size was provided by the asset index, use
     * given value. Else fetch size from server on first call.
     * @return Size of library file in bytes.
     * @throws java.net.MalformedURLException
     */
    @Override
    public int getSize() throws MalformedURLException, IOException {
        if (mFileSize == -1) {
            mUrl = GameUpdater.SERVER_URL + getPath();
            /*if (size != null) {
                mFileSize = size;
            } else {*/
                URLConnection urlconnection = new URL(mUrl).openConnection();
                urlconnection.setDefaultUseCaches(false);
                if ((urlconnection instanceof HttpURLConnection)) {
                    ((HttpURLConnection) urlconnection).setRequestMethod("HEAD");
                }
                mFileSize = urlconnection.getContentLength();
            //}
        }
        return mFileSize;
    }
    
    /**
     * Generates a path for fetching/storing the library. Used for launch command.
     * @return path
     */
    @Override
    public String getPath() {
        return "bin/" + id + ".jar";
    }
    
    /**
     * Determine whether the Minecraft version is legacy. Legacy versions need
     * virtual asset folders.
     * @return 
     */
    public boolean isLegacy() {
        return type.equals("legacy");
    }

    @Override
    public void download(MinecraftResourceDownloader downloader) throws Exception {
        downloader.download(this);
    }

    @Override
    public String getHash() throws Exception {
        throw new Exception("Hash does not exist for Minecraft JAR");
    }

    @Override
    public String getName() {
        return id + ".jar";
    }
    
    protected String getLaunchArgs() {
        String s = minecraftArguments.replace("${game_directory}", Util.getWorkingDirectory().toString());
        if (isLegacy()) {
            s = s.replace("--version ${version_name}", "");
            s = s.replace("--versionType ${version_type}", "");
        } else {
            s = s.replace("${version_name}", "2Toasty Minecraft");
            s = s.replace("${version_type}", "Forge");
            s += " --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker";
        }
        s = s.replace("${assets_index_name}", getAssets());
        s = s.replace("${user_type}", "Legacy");
        String assetRoot = Util.getWorkingDirectory().toString()+"/assets";
        if (isLegacy())
            assetRoot += "/virtual";
        s = s.replace("${assets_root}", assetRoot);
        s = s.replace("${game_assets}", assetRoot);
        return s;
    }
    
    protected String getAssets() {
        return (assets == null) ? id : assets;
    }
}
