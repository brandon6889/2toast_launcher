package net.minecraft;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GameUpdater implements Runnable {
    protected int percentage;
    protected static boolean force = false;
    private MinecraftVersion mCurrentVersion;
    private final LinkedList<MinecraftLibrary> mLibraries = new LinkedList();
    private MinecraftModList mMods;
    private MinecraftAssets mAssets;
    protected boolean fatalError;
    protected String fatalErrorDescription;

    private final String latestVersion;
    protected static final String SERVER_URL = "http://2toast.net/minecraft/";
    
    private static GameUpdater INSTANCE;
    
    public static GameUpdater getUpdater(String latestVersion) {
        if (INSTANCE == null)
            INSTANCE = new GameUpdater(latestVersion);
        return INSTANCE;
    }
    
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

    private GameUpdater(String latestVersion) {
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

    protected void loadJarURLs(String path) throws Exception {
        this.state = UpdaterStatus.DL_CONF;
        
        // Offline play not supported. Get legit MC.
        if (this.latestVersion == null || latestVersion.equals("")) throw new Exception("Unknown Version");
        
        System.out.println("Fetching config for Minecraft "+latestVersion);
        ConfigurationFetcher config = new ConfigurationFetcher(path, SERVER_URL);
        
        // Get game config.
        String gameConfigPath = "versions/"+latestVersion+".json";
        mCurrentVersion = (MinecraftVersion)config.get(MinecraftVersion.class, gameConfigPath);
        for (MinecraftLibrary library : mCurrentVersion.libraries)
            if (library.allow())
                mLibraries.add(library);
        if (config.needUpdate())
            force = true;
        percentage = 10;
        
        // Get asset config.
        String assetConfigPath = "assets/indexes/"+mCurrentVersion.getAssets()+".json";
        mAssets = (MinecraftAssets)config.get(MinecraftAssets.class, assetConfigPath);
        mAssets.initialize();
        if (config.needUpdate())
            force = true;
        this.percentage = 20;
        
        // Get mods config.
        String modConfigPath = "mods/"+latestVersion+".json";
        try {
            mMods = (MinecraftModList)config.get(MinecraftModList.class, modConfigPath);
        } catch (Exception e) {
            System.out.println("No mods found.");
        }
        if (config.needUpdate())
            force = true;
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
                    downloadGame(path);
                    extractFiles(path);
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
    
    protected void downloadLibraries(String path) throws Exception {
        this.state = UpdaterStatus.DL_LIBS;
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(path, this);
        downloader.addResources(mLibraries);
        startDownloader(downloader, 50, 350);
    }

    protected void downloadAssets(String path) throws Exception {
        this.state = UpdaterStatus.DL_RES;

        MinecraftResourceDownloader d = mAssets.createDownloader(path, this);
        startDownloader(d, 350, 650);
    }
    
    protected void downloadMods(String path) throws Exception {
        if (mMods == null)
            return;
        
        this.state = UpdaterStatus.DL_MODS;
        
        new File(path+"coremods").mkdirs();
        new File(path+"mods").mkdirs();
        
        MinecraftResourceDownloader downloader = mMods.createDownloader(path, this);
        startDownloader(downloader, 650, 950);
        
        mMods.cleanup(path);
    }
    
    protected void downloadGame(String path) throws Exception {
        this.state = UpdaterStatus.DL_GAME;
        
        LinkedList<MinecraftResource> game = new LinkedList();
        game.add(mCurrentVersion);
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(path, this);
        downloader.addResources(game);
        startDownloader(downloader, 950, 990);
    }

    static protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection) throws Exception {
        final InputStream[] is = new InputStream[1];
        for (int j = 0; (j < 3) && (is[0] == null); j++) {
            Thread t = new Thread() {
                @Override
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
        try (ZipFile zipFile = new ZipFile(in)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    (new File(out + entry.getName())).mkdirs();
                } else {
                    byte[] buffer = new byte[1024];
                    int len;
                    BufferedOutputStream outFile;
                    try (InputStream inStream = zipFile.getInputStream(entry)) {
                        outFile = new BufferedOutputStream(new FileOutputStream(out + entry.getName()));
                        while ((len = inStream.read(buffer)) >= 0) {
                            outFile.write(buffer, 0, len);
                        }
                    }
                    outFile.close();
                }
            }
        }
    }

    protected void extractFiles(String path) throws Exception {
        this.state = UpdaterStatus.EXTRACT;
        
        File nativeDir = new File(path + "bin/" + latestVersion + "-natives");
        nativeDir.delete();
        nativeDir.mkdirs();
        for (MinecraftLibrary l : mLibraries)
            l.extract(path, nativeDir.toString());
        
        if (mCurrentVersion.isLegacy())
            mAssets.buildVirtualDir(path);
    }

    protected void fatalErrorOccured(String error, Exception e) {
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
        if (mMods != null)
            for (String s2 : mMods.getCoremodPaths())
                s += Util.getWorkingDirectory() + "/" + s2 + delim;
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
    
    protected String getLaunchArgs(String user, String token) {
        String uuid = UUID.nameUUIDFromBytes(user.getBytes()).toString();
        String s = mCurrentVersion.getLaunchArgs();
        s = s.replace("${auth_player_name}", user);
        s = s.replace("${auth_uuid}", uuid);
        s = s.replace("${auth_access_token}", token);
        s = s.replace("${auth_session}", token);
        return s;
    }
    
    protected String getMainClass() {
        return mCurrentVersion.mainClass;
    }
}
