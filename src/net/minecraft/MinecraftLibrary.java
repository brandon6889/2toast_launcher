package net.minecraft;

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import static net.minecraft.GameUpdater.validateCertificateChain;

/**
 * Minecraft Library. Provides information and functions to fetch/manage a jar
 * library file.
 * TODO: Need to extract natives in legacy games.
 */
public class MinecraftLibrary {
    /* JSON fields */
    @SerializedName("name")
    public String name;
    @SerializedName("size")
    public Integer size;
    @SerializedName("sha1")
    public String sha1;
    @SerializedName("rules")
    public LinkedList<MinecraftLibraryRule> rules;
    @SerializedName("formatted")
    public String formatted;
    @SerializedName("natives")
    public MinecraftLibraryNatives natives; /* If not null, get size/sha here */
    @SerializedName("extract")
    public String extract; /* If not null, extract this library */
    
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
        if ((rules == null) || (rules.isEmpty())) {
            flag = true;
        } else {
            for (MinecraftLibraryRule r : rules) {
                if (r.action.equals("disallow")) {
                    if (r.os != null && (r.os.name == null || r.os.name.trim().equals("") || r.os.name.toLowerCase().equals(Util.getPlatform().toString()))) {
                        flag = false;
                        break;
                    }
                } else {
                    if (r.os == null)
                        flag = true;
                    else if (r.os.name == null || r.os.name.trim().equals("") || r.os.name.toLowerCase().equals(Util.getPlatform().toString()))
                        flag = true;
                }
            }
        }
        return flag;
    }
    
    /**
     * Proxy for file size. If the size was provided by the asset index, use
     * given value. Else fetch size from server on first call.
     * @return Size of library file in bytes.
     * @throws java.net.MalformedURLException
     */
    protected int getSize() throws MalformedURLException, IOException {
        if (mFileSize == -1) {
            mUrl = GameUpdater.SERVER_URL+"libraries/"+getPath();
            if (size != null) {
                mFileSize = size;
            } else if (natives != null) {
                mFileSize = (natives.size() != null) ? natives.size() : -1;
                sha1 = natives.sha1();
            }
            if (mFileSize == -1) {
                URLConnection urlconnection = new URL(mUrl).openConnection();
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
    protected String getPath() {
        String libPath = name.substring(0, name.indexOf(":"));
        libPath = libPath.replace(".", "/").replace(":", "/");
        String libName = mLibName = name.substring(name.indexOf(":")+1, name.lastIndexOf(":"));
        String libVer = name.substring(name.lastIndexOf(":")+1);
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
        
        File yeboi = new File(path + "libraries/" + getPath());
        if (sha1 != null && yeboi.exists()) {
            if (GameUpdater.calcSHA1(yeboi).equals(sha1.toUpperCase())) {
                return; // We good
            }
        }

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
                File dir = new File(path + "libraries/" + file.substring(0,file.lastIndexOf("/")));
                dir.mkdirs();
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
    
    /**
     * Extract files from jar. Expected use is extracting natives.
     * @param rootPath Game folder, library source.
     * @param nativePath Destination folder.
     * @throws IOException 
     */
    protected void extract(String rootPath, String nativePath) throws IOException, Exception {
        if (natives != null) {
            Certificate[] certificate = Launcher.class.getProtectionDomain().getCodeSource().getCertificates();
            if (certificate == null) {
                URL location = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
                JarURLConnection jurl = (JarURLConnection) new URL("jar:" + location.toString() + "!/net/minecraft/Launcher.class").openConnection();
                jurl.setDefaultUseCaches(true);
                try {
                    certificate = jurl.getCertificates();
                } catch (Exception localException) {
                }
            }
            try (JarFile jarFile = new JarFile(rootPath + "libraries/" + getPath(), true)) {
                Enumeration<JarEntry> entities = jarFile.entries();
                int totalSizeExtract = 0; // ToDo: Progress bars..
                while (entities.hasMoreElements()) {
                    JarEntry entry = entities.nextElement();
                    if ((!entry.isDirectory()) && (entry.getName().indexOf('/') == -1)) { // only extract files in root. No subdirs
                        totalSizeExtract += entry.getSize();
                    }
                }
                int currentSizeExtract = 0;
                for (entities = jarFile.entries(); entities.hasMoreElements();) {
                    JarEntry entry = entities.nextElement();
                    if ((!entry.isDirectory()) && (entry.getName().indexOf('/') == -1)) {
                        File f = new File(nativePath + File.separator + entry.getName());
                        if ((!f.exists()) || (f.delete())) {
                            InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
                            OutputStream out = new FileOutputStream(f);
                            byte[] buffer = new byte[65536];
                            int bufferSize;
                            while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
                                out.write(buffer, 0, bufferSize);
                                currentSizeExtract += bufferSize;
                            }
                            validateCertificateChain(certificate, entry.getCertificates());
                            in.close();
                            out.close();
                        }
                    }
                }
            }
        }
    }
}
