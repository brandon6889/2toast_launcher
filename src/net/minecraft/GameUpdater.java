package net.minecraft;

import java.applet.Applet;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;
import java.util.ArrayList;

public class GameUpdater implements Runnable {
    protected int percentage;
    protected static boolean force = false;
    private MinecraftVersion mCurrentVersion;
    private final LinkedList<MinecraftLibrary> mLibraries = new LinkedList();
    private final LinkedList<MinecraftResource> mModList = new LinkedList();
    private final LinkedList<MinecraftResource> mCoreModList = new LinkedList();
    private MinecraftAssets mAssets;
    private static ClassLoader classLoader;
    protected boolean fatalError;
    protected String fatalErrorDescription;
    protected Gson gson = new Gson();

    private final String latestVersion;
    protected static final String SERVER_URL = "http://2toast.net/minecraft/";
    
    private enum UpdaterStatus {
        INIT    ("Initializing Loader"),
        DL_CONF ("Fetching Configuration"),
        DL_LIBS ("Downloading Libraries"),
        DL_RES  ("Downloading Resources"),
        DL_MODS ("Downloading Mods"),
        DL_GAME ("Downloading Game"),
        EXTRACT ("Extracting Files"),
        LAUNCH  ("Starting Minecraft");
        
        final private String mDescription;
        
        private UpdaterStatus(String description) {
            mDescription = description;
        }
        
        public String getDescription() {
            return mDescription;
        }
    }
    
    private UpdaterStatus state;

    public GameUpdater(String latestVersion) {
        state = UpdaterStatus.INIT;
        this.percentage = 0;
        this.latestVersion = latestVersion;
    }

    public void init(String path) {
        if (force) {
            File confDir = new File(path + "config");
            try {
                Util.delete(confDir);
            } catch (IOException ex) {}
        }
    }

    private String generateStacktrace(Exception exception) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        return result.toString();
    }

    protected String getDescriptionForState() {
        return state.getDescription();
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is);
        s.useDelimiter("\\A");
        String out = s.hasNext() ? s.next() : "";
        s.close();
        return out;
    }

    protected void loadJarURLs(String path) throws Exception {
        this.state = UpdaterStatus.DL_CONF;
        
        // Offline play not supported. Get legit MC.
        if (this.latestVersion == null || this.latestVersion.equals("")) throw new Exception("Unknown Version");
        
        System.out.println("Fetching config for Minecraft "+this.latestVersion);
        
        // Get game config
        String gameConfigPath = "versions/"+this.latestVersion+"/"+this.latestVersion+".json";
        Configuration gameConfig = new Configuration(MinecraftVersion.class, path + gameConfigPath);
        mCurrentVersion = (MinecraftVersion)gameConfig.get(SERVER_URL + gameConfigPath);
        for (MinecraftLibrary library : mCurrentVersion.libraries)
            if (library.allow())
                mLibraries.add(library);
        percentage = 10;
        
        // Get asset config
        String assetConfigPath = "assets/indexes/"+this.latestVersion+".json";
        Configuration assetConfig = new Configuration(MinecraftAssets.class, path + assetConfigPath);
        mAssets = (MinecraftAssets)assetConfig.get(SERVER_URL + assetConfigPath);
        this.percentage = 20;
        
        // Fetch mods list. Only stores filenames from colon-delimited file.
        try {
            long downloadTime = System.currentTimeMillis();
            URLConnection modSource = new URL(SERVER_URL+"mods/"+this.latestVersion+".txt").openConnection();
            if (modSource instanceof HttpURLConnection) {
                modSource.setRequestProperty("Cache-Control", "no-cache");
                modSource.connect();
            }
            InputStream modListStream = modSource.getInputStream();
            String modList = convertStreamToString(modListStream);
            StringTokenizer mod = new StringTokenizer(modList, ":");
            int modCount = mod.countTokens();
            for (int i = 0; i < modCount-1; i++) {
                mModList.add(new MinecraftMod(mod.nextToken(), latestVersion));
            }
            modListStream.close();
            downloadTime = System.currentTimeMillis() - downloadTime;
            System.out.println("Got mod index in "+downloadTime+"ms");
        } catch (FileNotFoundException e) {
            System.out.println("No mods found for version "+this.latestVersion);
        }
        this.percentage = 30;
        
        // Fetch coremods list. Only stores filenames from colon-delimited file.
        try {
            long downloadTime = System.currentTimeMillis();
            URLConnection modSource = new URL(SERVER_URL+"coremods/"+this.latestVersion+".txt").openConnection();
            if (modSource instanceof HttpURLConnection) {
                modSource.setRequestProperty("Cache-Control", "no-cache");
                modSource.connect();
            }
            InputStream modListStream = modSource.getInputStream();
            String modList = convertStreamToString(modListStream);
            StringTokenizer mod = new StringTokenizer(modList, ":");
            int modCount = mod.countTokens();
            for (int i = 0; i < modCount-1; i++) {
                mCoreModList.add(new MinecraftCoreMod(mod.nextToken(), latestVersion));
            }
            modListStream.close();
            downloadTime = System.currentTimeMillis() - downloadTime;
            System.out.println("Got coremod index in "+downloadTime+"ms");
        } catch (FileNotFoundException e) {
            System.out.println("No coremods found for version "+this.latestVersion);
        }
        this.percentage = 40;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        String path =  Util.getWorkingDirectory() + File.separator;
        init(path);
        try {
            try {
                loadJarURLs(path);

                File versionFile = new File(path + "version");
                boolean cacheAvailable = false;
                if ((!force) && (versionFile.exists()) && ((this.latestVersion.equals("-1")) || (this.latestVersion.equals(readVersionFile(versionFile))))) {
                    System.out.println("Found cached version " + this.latestVersion);
                    cacheAvailable = true;
                    //ToDo: Actually check the cache if hashes are available
                    this.percentage = 1000;
                }
                if ((!cacheAvailable) || (force)) {
                    if (versionFile.exists() && !(this.latestVersion.equals(readVersionFile(versionFile))))
                        System.out.println("Updating from version " + readVersionFile(versionFile) + " to " + this.latestVersion);
                    else {
                        System.out.println("Downloading version " + this.latestVersion);
                    }
                    downloadLibraries(path);
                    downloadAssets(path);
                    downloadMods(path);
                    downloadCoreMods(path);
                    downloadGame(path);
                    extractJars(path);
                    extractNatives(path);
                    if (this.latestVersion != null) {
                        this.percentage = 1000;
                        writeVersionFile(versionFile, this.latestVersion);
                    }
                }
                this.state = UpdaterStatus.LAUNCH;
            } catch (AccessControlException ace) {
                fatalErrorOccured(ace.getMessage(), ace);
            }
        } catch (Exception e) {
            fatalErrorOccured(e.getMessage(), e);
        }

    }

    protected String readVersionFile(File file) throws Exception {
        String version;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            version = dis.readUTF();
        }
        return version;
    }

    protected void writeVersionFile(File file, String version) throws Exception {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeUTF(version);
        }
    }

    public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        @SuppressWarnings("rawtypes")
        Class appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
        //Class appletClass = classLoader.loadClass("net.minecraft.client.main.Main");
        return (Applet) appletClass.newInstance();
    }
    
    protected void downloadLibraries(String path) throws Exception {
        this.state = UpdaterStatus.DL_LIBS;
        
        ArrayList<MinecraftResource> o = new ArrayList();
        o.addAll(mLibraries);
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(path, this);
        downloader.addResources(o);
        startDownloader(downloader, 50, 300);
    }

    protected void downloadAssets(String path) throws Exception {
        this.state = UpdaterStatus.DL_RES;

        MinecraftResourceDownloader d = mAssets.createDownloader(path);
        startDownloader(d, 300, 550);
    }
    
    protected void downloadCoreMods(String path) throws Exception {
        this.state = UpdaterStatus.DL_MODS;
        
        /* For now, delete the folder to purge stale mods... */
        new File(path+"coremods").delete();
        new File(path+"coremods").mkdirs();
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(path, this);
        downloader.addResources(mCoreModList);
        downloader.download();
        startDownloader(downloader, 800, 850);
    }
    
    protected void downloadMods(String path) throws Exception {
        this.state = UpdaterStatus.DL_MODS;
        
        /* For now, delete the folder to purge stale mods... */
        new File(path+"mods").delete();
        new File(path+"mods").mkdirs();
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(path, this);
        downloader.addResources(mModList);
        startDownloader(downloader, 550, 800);
    }
    
    protected void downloadGame(String path) throws Exception {
        this.state = UpdaterStatus.DL_GAME;
        
        LinkedList<MinecraftResource> game = new LinkedList();
        game.add(mCurrentVersion);
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(path, this);
        downloader.addResources(game);
        startDownloader(downloader, 850, 950);
    }

    static protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection) throws Exception {
        final InputStream[] is = new InputStream[1];
        for (int j = 0; (j < 3) && (is[0] == null); j++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        is[0] = urlconnection.getInputStream();
                    } catch (IOException localIOException) {}
                }
            };
            t.setName("JarInputStreamThread");
            t.start();
            for (int iterationCount = 0; (is[0] == null) && (iterationCount++ < 5);) {
                try {
                    t.join(1000L);
                } catch (InterruptedException localInterruptedException) {}
            }
            if (is[0] == null) {
                try {
                    t.interrupt();
                    t.join();
                } catch (InterruptedException localInterruptedException1) {}
            }
        }
        if (is[0] == null) {
            throw new Exception("Unable to download " + currentFile + " from " + urlconnection.getURL());
        }
        return is[0];
    }

    static protected void extractZip(String in, String out) throws Exception {
        ZipFile zipFile = new ZipFile(in);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                (new File(out + entry.getName())).mkdirs();
            } else {
                byte[] buffer = new byte[1024];
                int len;
                InputStream inStream = zipFile.getInputStream(entry);
                BufferedOutputStream outFile = new BufferedOutputStream(new FileOutputStream(out + entry.getName()));
                while ((len = inStream.read(buffer)) >= 0) {
                    outFile.write(buffer, 0, len);
                }
                inStream.close();
                outFile.close();
            }
        }
        zipFile.close();
    }

    protected void extractJars(String path) throws Exception {
        this.state = UpdaterStatus.EXTRACT;
        if (mCurrentVersion.isLegacy())
            mAssets.buildVirtualDir(path);
        /*float increment = 10.0F / this.urlList.length;
        for (int i = 0; i < this.urlList.length; i++) {
            this.percentage = (80 + (int) (increment * (i + 1)));
            String filename = getFileName(this.urlList[i]);
            if (filename.endsWith(".x.zip")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".x.zip", ""));
                extractZip(path + filename, path);
                (new File(path + filename)).delete();
            }
        }*/
    }

    protected void extractNatives(String path) throws Exception {
        this.state = UpdaterStatus.EXTRACT;
        File nativeDir = new File(path + "bin/" + latestVersion + "-natives");
        nativeDir.delete();
        nativeDir.mkdirs();
        for (MinecraftLibrary l : mLibraries)
            l.extract(path, nativeDir.toString());
    }

    protected static void validateCertificateChain(Certificate[] ownCerts, Certificate[] native_certs) throws Exception {
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

    protected void fatalErrorOccured(String error, Exception e) {
        //e.printStackTrace();
        this.fatalError = true;
        this.fatalErrorDescription = ("Fatal error occured (" + this.state + "): " + error);
        System.out.println(this.fatalErrorDescription);
        if (e != null) {
            System.out.println(generateStacktrace(e));
        }
    }
    
    protected String getClassPath() {
        String delim = ":";
        if (Util.getPlatform() == Util.OS.windows)
            delim = ";";
        String s = "";
        for (MinecraftResource r : this.mCoreModList)
            s += Util.getWorkingDirectory() + "/" + r.getPath() + delim;
        for (MinecraftLibrary l : mLibraries)
            s += Util.getWorkingDirectory() + "/" + l.getPath() + delim;
        s += Util.getWorkingDirectory() + "/" + mCurrentVersion.getPath();
        return s;
    }
    
    protected String getMCVersion() {
        return latestVersion;
    }
    
    private void startDownloader(MinecraftResourceDownloader downloader, int initialPercentage, int finalPercentage) throws Exception {
        downloader.download();
        int i;
        while ((i = downloader.getProgress()) != 1000) {
            this.percentage = (int) (initialPercentage + (finalPercentage - initialPercentage)*((double)i/1000.0D));
            synchronized (this) {
                wait(50L);
            }
        }
    }
}
