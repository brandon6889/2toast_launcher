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
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.*;

import com.google.gson.Gson;

public class GameUpdater implements Runnable {
    public int percentage;
    public int currentSizeExtract;
    public int totalSizeExtract;
    public static boolean force = false;
    private MinecraftVersion mCurrentVersion;
    protected LinkedList<MinecraftLibrary> mLibraries = new LinkedList();
    protected LinkedList<String> modPathList = new LinkedList();
    protected MinecraftAssets mAssets;
    private static ClassLoader classLoader;
    protected Thread loaderThread;
    protected Thread animationThread;
    public boolean fatalError;
    public String fatalErrorDescription;
    protected String subtaskMessage = "";
    protected boolean pack200Supported = false;
    protected boolean certificateRefused;
    protected Gson gson = new Gson();

    protected static boolean natives_loaded = false;
    private String latestVersion;
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

    /**
     * Detect supported pack files and create bin/mod folders.
     * 
     * @param path 
     */
    public void init(String path) {
        try {
            Pack200.class.getSimpleName();
            this.pack200Supported = true;
        } catch (Throwable localThrowable1) {}
        
        File dir = new File(path + "bin");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File modDir = new File(path + "mods");
        if (force) {
            try {
                delete(modDir);
            } catch (IOException ex) {}
        }
        if (!modDir.exists())
            modDir.mkdirs();
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

    protected void loadJarURLs() throws Exception {
        this.state = UpdaterStatus.DL_CONF;
        byte[] buffer = new byte[65536];
        int bufferSize;
        long downloadTime;
        
        // Offline play not supported. Get legit MC.
        if (this.latestVersion == null || this.latestVersion.equals("")) throw new Exception("Unknown Version");
        
        String jsonPath = Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
        File dir = new File(jsonPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        jsonPath += this.latestVersion + ".json";
        
        String assetJsonPath = Util.getWorkingDirectory() + File.separator + "assets" + File.separator + "indexes" + File.separator;
        dir = new File(assetJsonPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        assetJsonPath += this.latestVersion+".json";
        
        System.out.println("Fetching config for Minecraft "+this.latestVersion);
        
        // Get json config
        URLConnection modSource;
        modSource = new URL(SERVER_URL+"versions/"+this.latestVersion+"/"+this.latestVersion+".json").openConnection();
        if (modSource instanceof HttpURLConnection) {
            modSource.setRequestProperty("Cache-Control", "no-cache");
            modSource.connect();
        }
        InputStream inputstream = getJarInputStream(this.latestVersion+".json", modSource);
        FileOutputStream fos = new FileOutputStream(jsonPath);
        downloadTime = System.currentTimeMillis();
        while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
            fos.write(buffer, 0, bufferSize);
        inputstream.close();
        fos.close();
        downloadTime = System.currentTimeMillis() - downloadTime;
        System.out.println("Got library index in "+downloadTime+"ms");
        this.percentage = 1;
        
        // Get asset json
        modSource = new URL(SERVER_URL+"assets/indexes/"+this.latestVersion+".json").openConnection();
        if (modSource instanceof HttpURLConnection) {
            modSource.setRequestProperty("Cache-Control", "no-cache");
            modSource.connect();
        }
        inputstream = getJarInputStream(this.latestVersion+".json", modSource);
        fos = new FileOutputStream(assetJsonPath);
        downloadTime = System.currentTimeMillis();
        while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
            fos.write(buffer, 0, bufferSize);
        inputstream.close();
        fos.close();
        downloadTime = System.currentTimeMillis() - downloadTime;
        System.out.println("Got asset index in "+downloadTime+"ms");
        this.percentage = 2;
        
        // Parse json config.
        String versionJson = Util.readFile(new File(jsonPath));
        mCurrentVersion = gson.fromJson(versionJson, MinecraftVersion.class);
        for (MinecraftLibrary library : mCurrentVersion.libraries) {
            if (library.allow()) {
                mLibraries.add(library);
            }
        }
        
        // Parse asset json.
        versionJson = Util.readFile(new File(assetJsonPath));
        mAssets = gson.fromJson(versionJson, MinecraftAssets.class);
        
        // Fetch mods list. Only stores filenames from colon-delimited file.
        try {
            downloadTime = System.currentTimeMillis();
            modSource = new URL(SERVER_URL+"mods/"+this.latestVersion+".txt").openConnection();
            if (modSource instanceof HttpURLConnection) {
                modSource.setRequestProperty("Cache-Control", "no-cache");
                modSource.connect();
            }
            InputStream modListStream = modSource.getInputStream();
            String modList = convertStreamToString(modListStream);
            StringTokenizer mod = new StringTokenizer(modList, ":");
            int modCount = mod.countTokens();
            for (int i = 0; i < modCount; i++) {
                modPathList.add(mod.nextToken());
            }
            modListStream.close();
            downloadTime = System.currentTimeMillis() - downloadTime;
            System.out.println("Got mod index in "+downloadTime+"ms");
        } catch (FileNotFoundException e) {
            System.out.println("No mods found for version "+this.latestVersion);
        }
        this.percentage = 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        String path =  Util.getWorkingDirectory() + File.separator;
        init(path);
        try {
            try {
                loadJarURLs();

                File versionFile = new File(path + "version");
                boolean cacheAvailable = false;
                if ((!force) && (versionFile.exists()) && ((this.latestVersion.equals("-1")) || (this.latestVersion.equals(readVersionFile(versionFile))))) {
                    System.out.println("Found cached version " + this.latestVersion);
                    cacheAvailable = true;
                    //ToDo: Actually check the cache if hashes are available
                    this.percentage = 100;
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
                    extractJars(path);
                    extractNatives(path);
                    if (this.latestVersion != null) {
                        this.percentage = 100;
                        writeVersionFile(versionFile, this.latestVersion);
                    }
                }
                this.state = UpdaterStatus.LAUNCH;
            } catch (AccessControlException ace) {
                fatalErrorOccured(ace.getMessage(), ace);
                this.certificateRefused = true;
            }
        } catch (Exception e) {
            fatalErrorOccured(e.getMessage(), e);
        } finally {
            this.loaderThread = null;
        }

    }

    protected String readVersionFile(File file) throws Exception {
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        String version = dis.readUTF();
        dis.close();
        return version;
    }

    protected void writeVersionFile(File file, String version) throws Exception {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
        dos.writeUTF(version);
        dos.close();
    }

    public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        @SuppressWarnings("rawtypes")
        Class appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
        //Class appletClass = classLoader.loadClass("net.minecraft.client.main.Main");
        return (Applet) appletClass.newInstance();
    }
    
    protected void downloadLibraries(String path) throws Exception {
        this.state = UpdaterStatus.DL_LIBS;
        
        int sizeLibraryTotal = 0;
        int sizeLibraryDownloaded = 0;
        for (MinecraftLibrary library : mLibraries)
            sizeLibraryTotal += library.getSize();
        
        int initialPercentage = this.percentage = 5;
        int finalPercentage = 30;
        for (MinecraftLibrary library : mLibraries) {
            library.download(path);
            sizeLibraryDownloaded += library.getSize();
            this.percentage = (int) (initialPercentage + (finalPercentage - initialPercentage)*((double)sizeLibraryDownloaded/(double)sizeLibraryTotal));
            //System.out.println(sizeLibraryDownloaded + "/" + sizeLibraryTotal + ": " + percentage);
        }
    }

    protected void downloadAssets(String path) throws Exception {
        this.state = UpdaterStatus.DL_RES;
        
        int sizeAssetTotal = 0;
        int sizeAssetDownloaded = 0;
        for (MinecraftAssetsObject asset : mAssets.objects)
            sizeAssetTotal += asset.getSize();

        int initialPercentage = this.percentage = 30;
        int finalPercentage = 55;
        for (MinecraftAssetsObject asset : mAssets.objects) {
            asset.download(path); // ToDo: Replace path argument with downloader visitor object. Visitor can have DLer threads :)
            sizeAssetDownloaded += asset.getSize();
            this.percentage = (int) (initialPercentage + (finalPercentage - initialPercentage)*((double)sizeAssetDownloaded/(double)sizeAssetTotal));
        }
        
        /*
        if (currentFile.endsWith(".mod.jar")) {
            fos = new FileOutputStream(path + "coremods/" + currentFile.replaceAll(".mod.jar", ".jar"));
        } else if (currentFile.endsWith(".x.zip")) {
            fos = new FileOutputStream(path + currentFile);
        } else if (currentFile.endsWith(".zip")) {
            fos = new FileOutputStream(path + "mods/" + currentFile);
        } else {
            fos = new FileOutputStream(path + "bin/" + currentFile);
        }
        */
    }
    
    protected void downloadMods(String path) throws Exception {
        this.state = UpdaterStatus.DL_MODS;
        
        int initialPercentage = this.percentage = 55;
        int finalPercentage = 80;
        for (String s : modPathList) {
            System.out.println("Want to get mod "+s);
        }
    }
    
    protected void downloadGame(String path) throws Exception {
        this.state = UpdaterStatus.DL_GAME;
        
        int initialPercentage = this.percentage = 80;
        mCurrentVersion.download(path);
        int finalPercentage = this.percentage = 90;
    }

    static protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection) throws Exception {
        final InputStream[] is = new InputStream[1];
        for (int j = 0; (j < 3) && (is[0] == null); j++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        is[0] = urlconnection.getInputStream();
                    } catch (IOException localIOException) {
                    }
                }
            };
            t.setName("JarInputStreamThread");
            t.start();
            for (int iterationCount = 0; (is[0] == null) && (iterationCount++ < 5);) {
                try {
                    t.join(1000L);
                } catch (InterruptedException localInterruptedException) {
                }
            }
            if (is[0] == null) {
                try {
                    t.interrupt();
                    t.join();
                } catch (InterruptedException localInterruptedException1) {
                }
            }
        }
        if (is[0] == null) {
            if (currentFile.equals("minecraft.jar")) {
                throw new Exception("Unable to download " + currentFile);
            }

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

    static protected void extractPack(String in, String out) throws Exception {
        File f = new File(in);
        FileOutputStream fostream = new FileOutputStream(out);
        JarOutputStream jostream = new JarOutputStream(fostream);
        Pack200.Unpacker unpacker = Pack200.newUnpacker();
        unpacker.unpack(f, jostream);
        jostream.close();
        f.delete();
    }

    static protected void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    protected void extractJars(String path) throws Exception {
        this.state = UpdaterStatus.EXTRACT;
        if (mCurrentVersion.isLegacy())
            mAssets.buildVirtualDir(path);
        /*float increment = 10.0F / this.urlList.length;
        for (int i = 0; i < this.urlList.length; i++) {
            this.percentage = (80 + (int) (increment * (i + 1)));
            String filename = getFileName(this.urlList[i]);
            if (filename.endsWith(".pack")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".pack", ""));
                extractPack(path + "bin/" + filename, path + "bin/" + filename.replace(".pack", ""));

            } else if (filename.endsWith(".x.zip")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".x.zip", ""));
                extractZip(path + filename, path);
                (new File(path + filename)).delete();
            }
        }*/
    }

    protected void extractNatives(String path) throws Exception {
        this.state = UpdaterStatus.EXTRACT;
        /*int initialPercentage = this.percentage = 90; // added 90
        String nativeJar = mCurrentVersion.id+".jar";
        //String nativeJar = getJarName(this.urlList[(this.urlList.length - 1)]);
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
        File nativeFolder = new File(path + "bin/natives");
        if (!nativeFolder.exists()) {
            nativeFolder.mkdir();
        }
        JarFile jarFile = new JarFile(path + "bin/" + nativeJar, true);
        Enumeration<JarEntry> entities = jarFile.entries();
        this.totalSizeExtract = 0;
        while (entities.hasMoreElements()) {
            JarEntry entry = entities.nextElement();
            if ((!entry.isDirectory()) && (entry.getName().indexOf('/') == -1)) {
                this.totalSizeExtract = ((int) (this.totalSizeExtract + entry.getSize()));
            }
        }
        this.currentSizeExtract = 0;
        for (entities = jarFile.entries(); entities.hasMoreElements();) {
            JarEntry entry = entities.nextElement();
            if ((!entry.isDirectory()) && (entry.getName().indexOf('/') == -1)) {
                File f = new File(path + "bin/natives" + File.separator + entry.getName());
                if ((!f.exists()) || (f.delete())) {
                    InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
                    OutputStream out = new FileOutputStream(path + "bin/natives" + File.separator + entry.getName());
                    byte[] buffer = new byte[65536];
                    int bufferSize;
                    while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
                        out.write(buffer, 0, bufferSize);
                        this.currentSizeExtract += bufferSize;
                        this.percentage = (initialPercentage + this.currentSizeExtract * 10 / this.totalSizeExtract); // 20
                        this.subtaskMessage = ("Extracting: " + entry.getName() + " " + this.currentSizeExtract * 100 / this.totalSizeExtract + "%");
                    }
                    validateCertificateChain(certificate, entry.getCertificates());
                    in.close();
                    out.close();
                }
            }
        }

        this.subtaskMessage = "";
        jarFile.close();
        File f = new File(path + "bin/" + nativeJar);
        f.delete();*/
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

    protected String getJarName(URL url) {
        String fileName = url.getFile();
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        if (fileName.endsWith(".pack")) {
            fileName = fileName.replaceAll(".pack", "");
        }
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    protected String getFileName(URL url) {
        String fileName = url.getFile();
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    protected void fatalErrorOccured(String error, Exception e) {
        e.printStackTrace();
        this.fatalError = true;
        this.fatalErrorDescription = ("Fatal error occured (" + this.state + "): " + error);
        System.out.println(this.fatalErrorDescription);
        if (e != null) {
            System.out.println(generateStacktrace(e));
        }
    }
}
