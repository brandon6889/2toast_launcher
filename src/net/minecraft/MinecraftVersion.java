package net.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

public class MinecraftVersion {
    /* JSON fields */
    public String minecraftArguments;
    public String mainClass;
    public String time;
    public String id;
    public String type;
    public String processArguments;
    public String releaseTime;
    public String assets;
    public int minimumLauncherVersion;
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
    protected int getSize() throws MalformedURLException, IOException {
        if (mFileSize == -1) {
            mUrl = GameUpdater.SERVER_URL+"versions/"+id+"/"+getPath();
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
    protected String getPath() {
        return id+".jar";
    }
    
    protected void download(String path) throws Exception { // TODO: FileDownloader class
        int unsuccessfulAttempts = 0;
        int maxUnsuccessfulAttempts = 3;
        boolean downloadFile = true;
        int fileSize = getSize(); // Ensure size proxy is used

        while (downloadFile) {
            downloadFile = false;
            URLConnection urlconnection = new URL(mUrl).openConnection();
            if ((urlconnection instanceof HttpURLConnection)) {
                urlconnection.setRequestProperty("Cache-Control", "no-cache");
                urlconnection.connect();
            }
            String file = getPath();
            FileOutputStream fos;
            int downloadedAmount = 0;
            try (InputStream inputstream = GameUpdater.getJarInputStream(file, urlconnection)) {
                File dir = new File(path + "bin/");
                dir.mkdirs();
                fos = new FileOutputStream(path + "bin/" + file);
                long downloadStartTime = System.currentTimeMillis();
                int bufferSize;
                byte[] buffer = new byte[65536];
                while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, bufferSize);
                    downloadedAmount += bufferSize;
                    // TODO: update gui with percentage
                }
            }
            fos.close();
            if (((urlconnection instanceof HttpURLConnection)) && (fileSize != downloadedAmount) && (fileSize > 0)) {
                unsuccessfulAttempts++;
                if (unsuccessfulAttempts < maxUnsuccessfulAttempts) {
                    downloadFile = true;
                    // Reset GUI percentage for this file
                } else {
                    throw new Exception("failed to download game");
                }
            }
        }
    }
}
