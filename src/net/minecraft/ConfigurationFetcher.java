package net.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;

public class ConfigurationFetcher {
    private final Gson mGson = new Gson();
    
    /* Local base path */
    private final String mPath;
    /* Remote base path */
    private final String mServer;
    
    public ConfigurationFetcher(String path, String serverUrl) {
        mPath = path;
        mServer = serverUrl;
    }
    
    public Object get(Class clazz, String path) throws Exception {
        File dir = new File(mPath + path.substring(0,path.lastIndexOf("/")));
        if (!dir.exists())
            dir.mkdirs();
        
        String fileName = path.substring(path.lastIndexOf("/", path.lastIndexOf("/")-1)+1);
        URLConnection configSource;
        configSource = new URL(mServer + path).openConnection();
        if (configSource instanceof HttpURLConnection) {
            configSource.setRequestProperty("Cache-Control", "no-cache");
            configSource.connect();
        }
        FileOutputStream fos;
        byte[] buffer = new byte[65536];
        int bufferSize;
        long downloadTime;
        try (InputStream inputstream = GameUpdater.getJarInputStream(fileName, configSource)) {
            fos = new FileOutputStream(mPath + path);
            downloadTime = System.currentTimeMillis();
            while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
                fos.write(buffer, 0, bufferSize);
        }
        fos.close();
        downloadTime = System.currentTimeMillis() - downloadTime;
        System.out.println("Got " + fileName + " in "+downloadTime+"ms");
        
        return mGson.fromJson(Util.readFile(new File(mPath + path)), clazz);
    }
}
