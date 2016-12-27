package net.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;

/**
 * This class can be dissolved into GameUpdater, but maybe it will expand later.
 */
public class Configuration {
    private static final Gson GSON = new Gson();
    
    private final String mSavePath;
    private final Class mClazz;
    
    public Configuration(Class clazz, String savePath) {
        mSavePath = savePath;
        mClazz = clazz;
    }
    
    public Object get(String url) throws Exception {
        File dir = new File(mSavePath.substring(0,mSavePath.lastIndexOf("/")));
        if (!dir.exists())
            dir.mkdirs();
        
        String fileName = mSavePath.substring(mSavePath.lastIndexOf("/", mSavePath.lastIndexOf("/")-1)+1);
        URLConnection configSource;
        configSource = new URL(url).openConnection();
        if (configSource instanceof HttpURLConnection) {
            configSource.setRequestProperty("Cache-Control", "no-cache");
            configSource.connect();
        }
        FileOutputStream fos;
        byte[] buffer = new byte[65536];
        int bufferSize;
        long downloadTime;
        try (InputStream inputstream = GameUpdater.getJarInputStream(fileName, configSource)) {
            fos = new FileOutputStream(mSavePath);
            downloadTime = System.currentTimeMillis();
            while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
                fos.write(buffer, 0, bufferSize);
        }
        fos.close();
        downloadTime = System.currentTimeMillis() - downloadTime;
        System.out.println("Got " + fileName + " in "+downloadTime+"ms");
        
        return GSON.fromJson(Util.readFile(new File(mSavePath)), mClazz);
    }
}
