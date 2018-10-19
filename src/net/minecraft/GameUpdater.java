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
import java.util.List;
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

    private final String LATEST_VERSION;
    private final String WORKDIR;
    protected static final String SERVER_URL = "http://kuumba.club/minecraft/";
    
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
        this.LATEST_VERSION = latestVersion;
        WORKDIR = Util.getWorkingDirectory() + File.separator;
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

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        init();
        try {
            try {
                loadJarURLs();

                File versionFile = new File(WORKDIR + "version");
                boolean cacheAvailable = false;
                if ((!force) && (versionFile.exists()) && ((this.LATEST_VERSION.equals("-1")) || (this.LATEST_VERSION.equals(readVersionFile(versionFile))))) {
                    System.out.println("Found cached version " + this.LATEST_VERSION);
                    cacheAvailable = true;
                    //ToDo: Actually check the cache if hashes are available
                    this.percentage = 1000;
                }
                if ((!cacheAvailable) || (force)) {
                    if (versionFile.exists() && !(this.LATEST_VERSION.equals(readVersionFile(versionFile))))
                        System.out.println("Updating from version " + readVersionFile(versionFile) + " to " + this.LATEST_VERSION);
                    else {
                        System.out.println("Downloading version " + this.LATEST_VERSION);
                    }
                    downloadLibraries();
                    downloadAssets();
                    downloadMods();
                    downloadGame();
                    extractFiles();
                    if (this.LATEST_VERSION != null) {
                        this.percentage = 1000;
                        writeVersionFile(versionFile, this.LATEST_VERSION);
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

    private void init() {
        if (force) {
            File confDir = new File(WORKDIR + "config");
            try {
                Util.delete(confDir);
            } catch (IOException ex) {}
        }
    }

    private void loadJarURLs() throws Exception {
        this.state = UpdaterStatus.DL_CONF;
        
        // Offline play not supported. Get legit MC.
        if (this.LATEST_VERSION == null || LATEST_VERSION.equals("")) throw new Exception("Unknown Version");
        
        System.out.println("Fetching config for Minecraft "+LATEST_VERSION);
        ConfigurationFetcher config = new ConfigurationFetcher(WORKDIR, SERVER_URL);
        
        // Get game config.
        String gameConfigPath = "versions/"+LATEST_VERSION+".json";
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
        String modConfigPath = "mods/"+LATEST_VERSION+".json";
        try {
            mMods = (MinecraftModList)config.get(MinecraftModList.class, modConfigPath);
        } catch (Exception e) {
            System.out.println("No mods found.");
        }
        if (config.needUpdate())
            force = true;
        this.percentage = 40;
    }

    private String readVersionFile(File file) throws Exception {
        String version;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            version = dis.readUTF();
        }
        return version;
    }

    private void writeVersionFile(File file, String version) throws Exception {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeUTF(version);
        }
    }
    
    private void downloadLibraries() throws Exception {
        this.state = UpdaterStatus.DL_LIBS;
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(WORKDIR, this);
        downloader.addResources(mLibraries);
        startDownloader(downloader, 50, 350);
    }

    private void downloadAssets() throws Exception {
        this.state = UpdaterStatus.DL_RES;

        MinecraftResourceDownloader d = mAssets.createDownloader(WORKDIR, this);
        startDownloader(d, 350, 650);
    }
    
    private void downloadMods() throws Exception {
        if (mMods == null)
            return;
        
        this.state = UpdaterStatus.DL_MODS;
        
        new File(WORKDIR+"coremods").mkdirs();
        new File(WORKDIR+"mods").mkdirs();
        
        MinecraftResourceDownloader downloader = mMods.createDownloader(WORKDIR, this);
        startDownloader(downloader, 650, 950);
        
        mMods.cleanup(WORKDIR);
    }
    
    private void downloadGame() throws Exception {
        this.state = UpdaterStatus.DL_GAME;
        
        LinkedList<MinecraftResource> game = new LinkedList();
        game.add(mCurrentVersion);
        if (mCurrentVersion.config != null)
            game.add(mCurrentVersion.config);
        if (mCurrentVersion.resources != null)
            game.add(mCurrentVersion.resources);
        if (mCurrentVersion.scripts != null)
            game.add(mCurrentVersion.scripts);
        
        MinecraftResourceDownloader downloader = new MinecraftResourceDownloader(WORKDIR, this);
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
    
    /**
     * Extracts a Minecraft Resource into a folder.
     * @param resource Resource to extract. Currently assumes a zip file.
     * @param outDir Output folder relative to Minecraft appdata folder.
     * @param deleteOutDir If true, delete destination dir before extracting.
     */
    private void extractResource(MinecraftResource resource, String outDir, boolean deleteOutDir) throws Exception {
        String blobFile = WORKDIR + resource.getPath();
        String out = WORKDIR + outDir + File.separator;
        File dir = new File(out);
        if (deleteOutDir)
            Util.delete(dir);
        dir.mkdirs();
        extractZip(blobFile, out);
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

    private void extractFiles() throws Exception {
        this.state = UpdaterStatus.EXTRACT;
        
        File nativeDir = new File(WORKDIR + "bin/" + LATEST_VERSION + "-natives");
        nativeDir.delete();
        nativeDir.mkdirs();
        for (MinecraftLibrary l : mLibraries)
            l.extract(WORKDIR, nativeDir.toString());
        
        if (mCurrentVersion.isLegacy())
            mAssets.buildVirtualDir(WORKDIR);
        
        if (mCurrentVersion.config != null)
            extractResource(mCurrentVersion.config, "config", false);
        
        if (mCurrentVersion.resources != null)
            extractResource(mCurrentVersion.resources, "resources", true);
        
        if (mCurrentVersion.scripts != null)
            extractResource(mCurrentVersion.scripts, "scripts", true);
    }

    private void fatalErrorOccured(String error, Exception e) {
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
        return LATEST_VERSION;
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
    
    protected List<String> getLaunchArgs(String user, String token) {
        String uuid = UUID.nameUUIDFromBytes(user.getBytes()).toString();
        List<String> s = mCurrentVersion.getLaunchArgs();
        int index;
        index = s.indexOf("${auth_player_name}");
        if (index != -1)
            s.set(index, user);
        index = s.indexOf("${auth_uuid}");
        if (index != -1)
            s.set(index, uuid);
        index = s.indexOf("${auth_access_token}");
        if (index != -1)
            s.set(index, token);
        index = s.indexOf("${auth_session}");
        if (index != -1)
            s.set(index, token);
        return s;
    }
    
    protected String getMainClass() {
        return mCurrentVersion.mainClass;
    }
}
