package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

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
    @SerializedName("size")
    public Integer size;
    @SerializedName("hash")
    public String hash;
    @SerializedName("config")
    public MinecraftConfig config;
    @SerializedName("resources")
    public MinecraftResourcesBlob resources;
    @SerializedName("scripts")
    public MinecraftResourcesBlob scripts;
    
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
                String url = GameUpdater.SERVER_URL + getPath();
                URLConnection urlconnection = new URL(url).openConnection();
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
        if (hash != null)
            return hash;
        throw new Exception("Hash does not exist for Minecraft JAR");
    }

    @Override
    public String getName() {
        return id + ".jar";
    }
    
    protected List<String> getLaunchArgs() {
        List<String> launchArgs = new LinkedList();
        
        String assetRoot = Util.getWorkingDirectory().toString()+"/assets";
        if (isLegacy())
            assetRoot += "/virtual";
        
        for (String s : minecraftArguments.split(" ")) {
            switch(s) {
                case "${game_directory}":
                    launchArgs.add(Util.getWorkingDirectory().toString());
                    break;
                case "${assets_index_name}":
                    launchArgs.add(getAssets());
                    break;
                case "${user_type}":
                    launchArgs.add("Legacy");
                    break;
                case "--version":
                case "--versionType":
                    if (!isLegacy())
                        launchArgs.add(s);
                    break;
                case "${version_name}":
                    if (!isLegacy())
                        launchArgs.add("Kuumba");
                    break;
                case "${version_type}":
                    if (!isLegacy())
                        launchArgs.add("Forge");
                    break;
                case "${assets_root}":
                case "${game_assets}":
                    launchArgs.add(assetRoot);
                    break;
                default:
                    launchArgs.add(s);
            }
        }
        return launchArgs;
    }
    
    protected String getAssets() {
        return (assets == null) ? id : assets;
    }
}
