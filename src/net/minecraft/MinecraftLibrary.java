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

/**
 * Minecraft Library. Provides information and functions to fetch/manage a jar
 * library file.
 * TODO: Need to extract natives in legacy games.
 */
public class MinecraftLibrary implements MinecraftResource {
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
                    if (r.supportsOS(Util.getPlatform().toString())) {
                        flag = false;
                        break;
                    }
                } else {
                    if (r.os == null)
                        flag = true;
                    else if (r.supportsOS(Util.getPlatform().toString()))
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
    @Override
    public int getSize() throws MalformedURLException, IOException {
        if (mFileSize == -1) {
            mUrl = GameUpdater.SERVER_URL+getPath();
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
    @Override
    public String getPath() {
        String libPath = name.substring(0, name.indexOf(":"));
        libPath = libPath.replace(".", "/").replace(":", "/");
        String libName = mLibName = name.substring(name.indexOf(":")+1, name.lastIndexOf(":"));
        String libVer = name.substring(name.lastIndexOf(":")+1);
        if (natives != null) {
            return "libraries/"+libPath+"/"+libName+"/"+libVer+"/"+libName+"-"+libVer+"-natives-"+Util.getPlatform().toString()+".jar";
        } else {
            return "libraries/"+libPath+"/"+libName+"/"+libVer+"/"+libName+"-"+libVer+".jar";
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
                } catch (IOException le) {
                }
            }
            try (JarFile jarFile = new JarFile(rootPath + getPath(), true)) {
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
                            OutputStream out;
                            try (InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()))) {
                                out = new FileOutputStream(f);
                                byte[] buffer = new byte[65536];
                                int bufferSize;
                                while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
                                    out.write(buffer, 0, bufferSize);
                                    currentSizeExtract += bufferSize;
                                }
                                validateCertificateChain(certificate, entry.getCertificates());
                            }
                            out.close();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void download(MinecraftResourceDownloader downloader) throws Exception {
        downloader.download(this);
    }

    @Override
    public String getHash() throws Exception {
        if (sha1 == null)
            throw new Exception("Hash does not exist for library "+mLibName);
        return sha1;
    }

    @Override
    public String getName() {
        return "library " + mLibName;
    }

    private static void validateCertificateChain(Certificate[] ownCerts, Certificate[] native_certs) throws Exception {
        if (ownCerts == null) {
            return;
        }
        if (native_certs == null) {
            throw new Exception("Unable to validate certificate chain. Native entry did not have a certificate chain at all");
        }
        if (ownCerts.length != native_certs.length) {
            throw new Exception("Unable to validate certificate chain. Chain differs in length [" + ownCerts.length + " vs " + native_certs.length + "]");
        }
        for (int i = 0; i < ownCerts.length; i++) {
            if (!ownCerts[i].equals(native_certs[i])) {
                throw new Exception("Certificate mismatch: " + ownCerts[i] + " != " + native_certs[i]);
            }
        }
    }
}
