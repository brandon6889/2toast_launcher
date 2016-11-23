package net.minecraft;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MinecraftLibrary {
    /* JSON fields */
    public String name;
    public ArrayList<Rule> rules;
    public String url;
    public String formatted;
    public Natives natives;
    
    /* Internal data */
    private int mFileSize = -1;
    private String mUrl = "";
    private String mLibName = "";
    
    /**
     * Determine whether the library should be loaded in the current environment.
     * @return boolean flag
     */
    protected boolean allow() {
        boolean flag = false;
        if ((this.rules == null) || (this.rules.isEmpty())) {
            flag = true;
        } else {
            for (int j = 0; j < this.rules.size(); j++) {
                Rule r = (Rule)this.rules.get(j);
                if (r.action.equals("disallow")) {
                    if (r.os != null && (r.os.name == null || r.os.name.trim().equals("") || r.os.name.toLowerCase().equals(Util.getPlatform().toString()))) {
                        flag = false;
                        break;
                    }
                }
                else {
                    if (r.os != null && (r.os.name == null || r.os.name.trim().equals("") || r.os.name.toLowerCase().equals(Util.getPlatform().toString()))) {
                        flag = true;
                    }
                    else if (r.os == null) {
                        flag = true;
                    }
                }
            }
        }
        return flag;
    }
    
    /**
     * Proxy for file size. Fetches size of file from server on first call.
     * @return Size of library file.
     * @throws java.net.MalformedURLException
     */
    protected int getSize() throws MalformedURLException, IOException {
        if (mFileSize == -1) {
            mUrl = GameUpdater.SERVER_URL+"libraries/"+getPath();
            URLConnection urlconnection = new URL(mUrl).openConnection();
            urlconnection.setDefaultUseCaches(false);
            if ((urlconnection instanceof HttpURLConnection)) {
                ((HttpURLConnection) urlconnection).setRequestMethod("HEAD");
            }
            mFileSize = urlconnection.getContentLength();
        }
        return mFileSize;
    }
    
    /**
     * Generates a path for fetching/storing the library. Used for launch command.
     * @return path
     */
    protected String getPath() {
        String libPath = name.substring(0, name.indexOf(":"));
        libPath = libPath.replace(".", "/").replace(":", "/");
        String libName = mLibName = name.substring(name.indexOf(":")+1, name.lastIndexOf(":"));
        String libVer = name.substring(name.lastIndexOf(":")+1);
        String newPath;
        if (natives != null) {
            return libPath+"/"+libName+"/"+libVer+"/"+libName+"-"+libVer+"-natives-"+Util.getPlatform().toString()+".jar";
        } else {
            return libPath+"/"+libName+"/"+libVer+"/"+libName+"-"+libVer+".jar";
        }
    }
    
    /**
     * Download the library to the Minecraft folder.
     * @param path Minecraft root folder
     * @throws java.lang.Exception
     */
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
                fos = new FileOutputStream(path + "libraries/" + file);
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
                    throw new Exception("failed to download " + mLibName);
                }
            }
        }
    }
}
