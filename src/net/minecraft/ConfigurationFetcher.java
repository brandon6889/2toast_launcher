package net.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.security.NoSuchAlgorithmException;

public class ConfigurationFetcher {
    private final Gson mGson = new Gson();
    
    /* Local base path */
    private final String mPath;
    /* Remote base path */
    private final String mServer;
    /* Track whether config is new/needs update. */
    private boolean mLastConfigNeedsUpdate = false;
    
    public ConfigurationFetcher(String path, String serverUrl) {
        mPath = path;
        mServer = serverUrl;
    }
    
    public Object get(Class clazz, String path) throws Exception {
        mLastConfigNeedsUpdate = false;
        String localPath = mPath + path;
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
        if (new File(localPath).exists()) {
            path += ".new";
            mLastConfigNeedsUpdate = true;
        }
        FileOutputStream fos;
        byte[] buffer = new byte[65536];
        int bufferSize;
        long downloadTime;
        try (InputStream inputstream = GameUpdater.getJarInputStream(fileName, configSource)) {
            fos = new FileOutputStream(localPath);
            downloadTime = System.currentTimeMillis();
            while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
                fos.write(buffer, 0, bufferSize);
        }
        fos.close();
        downloadTime = System.currentTimeMillis() - downloadTime;
        System.out.println("Got " + fileName + " in "+downloadTime+"ms");
        
        File newConf = new File(localPath + ".new");
        File oldConf = new File(localPath);
        if (mLastConfigNeedsUpdate) {
            try {
                mLastConfigNeedsUpdate = !MinecraftResourceDownloader.calcSHA1(oldConf)
                                 .equals(MinecraftResourceDownloader.calcSHA1(newConf));
                Files.move(newConf.toPath(), oldConf.toPath(), REPLACE_EXISTING);
            } catch (IOException | NoSuchAlgorithmException e) {/* Default to true, sure. */}
        }
        
        return mGson.fromJson(Util.readFile(new File(localPath)), clazz);
    }
    
    public boolean needUpdate() {
        return mLastConfigNeedsUpdate;
    }
}
