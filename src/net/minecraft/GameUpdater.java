package net.minecraft;

import java.applet.Applet;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.*;

import com.google.gson.Gson;

public class GameUpdater implements Runnable {
    public static final int STATE_INIT = 1;
    public static final int STATE_DETERMINING_PACKAGES = 2;
    public static final int STATE_CHECKING_CACHE = 3;
    public static final int STATE_DOWNLOADING = 4;
    public static final int STATE_EXTRACTING_PACKAGES = 5;
    public static final int STATE_UPDATING_CLASSPATH = 6;
    public static final int STATE_SWITCHING_APPLET = 7;
    public static final int STATE_INITIALIZE_REAL_APPLET = 8;
    public static final int STATE_START_REAL_APPLET = 9;
    public static final int STATE_DONE = 10;
    public int percentage;
    public int currentSizeDownload;
    public int totalSizeDownload;
    public int currentSizeExtract;
    public int totalSizeExtract;
    public static boolean force = false;
    protected URL[] urlList;
    protected LinkedList<String> libraryPathList = new LinkedList();
    protected LinkedList<String> nativesPathList = new LinkedList();
    private static ClassLoader classLoader;
    protected Thread loaderThread;
    protected Thread animationThread;
    public boolean fatalError;
    public String fatalErrorDescription;
    protected String subtaskMessage;
    protected int state;
    protected boolean lzmaSupported;
    protected boolean pack200Supported;
    protected boolean certificateRefused;

    protected static boolean natives_loaded = false;
    private String latestVersion;

    public GameUpdater(String latestVersion) {
        this.subtaskMessage = "";
        this.state = 1;
        this.lzmaSupported = false;
        this.pack200Supported = false;
        this.latestVersion = latestVersion;
    }

    public void init() {
        this.state = 1;
        try {
            Class.forName("LZMA.LzmaInputStream");
            this.lzmaSupported = true;
        } catch (Throwable localThrowable) {
        }
        try {
            Pack200.class.getSimpleName();
            this.pack200Supported = true;
        } catch (Throwable localThrowable1) {
        }
    }

    private String generateStacktrace(Exception exception) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        return result.toString();
    }

    protected String getDescriptionForState() {
        switch (this.state) {
        case 1:
            return "Initializing loader";
        case 2:
            return "Determining packages to load";
        case 3:
            return "Checking cache for existing files";
        case 4:
            return "Downloading packages";
        case 5:
            return "Extracting downloaded packages";
        case 6:
            return "Updating classpath";
        case 7:
            return "Switching applet";
        case 8:
            return "Initializing real applet";
        case 9:
            return "Starting real applet";
        case 10:
            return "Done loading";
        }
        return "unknown state";
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is);
        s.useDelimiter("\\A");
        String out = s.hasNext() ? s.next() : "";
        s.close();
        return out;
    }

    protected String trimExtensionByCapabilities(String file) {
        if (!this.pack200Supported) {
            file = file.replaceAll(".pack", "");
        }
        if (!this.lzmaSupported) {
            file = file.replaceAll(".lzma", "");
        }
        return file;
    }

    protected void loadJarURLs() throws Exception {
        this.state = 2;
        byte[] buffer = new byte[65536];
        int bufferSize;
        long downloadTime;
        
        // TODO: Mark/get latest version in offline mode, only fetch data when online.
        if (this.latestVersion == null || this.latestVersion.equals("")) throw new Exception("Unknown Version");
        //String json = Util.readFile(new File(jsonPath));
        //MinecraftVersion currentVersion = new Gson().fromJson(json, MinecraftVersion.class);
        
        // TODO: We also need to fetch assets for new MC.. make new function for this..
        //modSource = new URL("http://2toast.net/minecraft/assets/indexes/"+this.latestVersion+".json").openConnection();
        
        String jsonPath = Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
        File dir = new File(jsonPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        jsonPath += this.latestVersion + ".json";
        
        // Get json config
        URLConnection modSource;
        modSource = new URL("http://2toast.net/minecraft/versions/"+this.latestVersion+"/"+this.latestVersion+".json").openConnection();
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
        System.out.println("Got "+this.latestVersion+".json in "+downloadTime+"ms");
        
        // Parse json config. Paths in format org/apache/commons/.../.jar
        String versionJson = Util.readFile(new File(jsonPath));
        MinecraftVersion currentVersion = new Gson().fromJson(versionJson, MinecraftVersion.class);
        for (MinecraftLibrary library : currentVersion.libraries) {
            if (library.allow()) {
                String libPath = library.name.substring(0, library.name.indexOf(":"));
                libPath = libPath.replace(".", "/").replace(":", "/");
                String libName = library.name.substring(library.name.indexOf(":")+1, library.name.lastIndexOf(":"));
                String libVer = library.name.substring(library.name.lastIndexOf(":")+1);
                String newPath;
                if (library.natives != null) {
                    newPath = libPath+"/"+libName+"/"+libVer+"/"+libName+"-"+libVer+"-natives-"+Util.getPlatform().toString()+".jar";
                    nativesPathList.add(newPath);
                } else {
                    newPath = libPath+"/"+libName+"/"+libVer+"/"+libName+"-"+libVer+".jar";
                    libraryPathList.add(newPath);
                }
            }
        }
    if (true)
    throw new Exception("bye");

        String jarList = "minecraft.jar, otherPlaceholder.jar";
        
        modSource = new URL("http://2toast.net/minecraft/versions/"+this.latestVersion+"/modlist.txt").openConnection();
        if (modSource instanceof HttpURLConnection) {
            modSource.setRequestProperty("Cache-Control", "no-cache");
            modSource.connect();
        }
        InputStream modListStream = modSource.getInputStream();
        String modList = convertStreamToString(modListStream);

        StringTokenizer mod = new StringTokenizer(modList, ":");
        int modCount = mod.countTokens();
        for (int i = 0; i < modCount; i++) {
            jarList += ", " + mod.nextToken();
        }

        jarList = trimExtensionByCapabilities(jarList);
        StringTokenizer jar = new StringTokenizer(jarList, ", ");
        int jarCount = jar.countTokens() + 1;

        this.urlList = new URL[jarCount];
        
        URL path = new URL("http://2toast.net/minecraft/"+this.latestVersion+"/");
        URL modPath = new URL("http://2toast.net/minecraft/mods/"+this.latestVersion+"/");
        
        for (int i = 0; i < jarCount - 1; i++) {
            String currentFile = jar.nextToken();
            if (currentFile.endsWith(".mod.jar")) {
                this.urlList[i] = new URL(modPath, currentFile);
            } else if (currentFile.endsWith(".jar")) {
                this.urlList[i] = new URL(path, currentFile);
            } else {
                this.urlList[i] = new URL(modPath, currentFile);
            }
        }

        String osName = System.getProperty("os.name");
        String nativeJar = null;
        if (osName.startsWith("Win")) {
            nativeJar = "windows_natives.jar";
        } else if (osName.startsWith("Linux")) {
            nativeJar = "linux_natives.jar";
        } else if (osName.startsWith("Mac")) {
            nativeJar = "macosx_natives.jar";
        } else if ((osName.startsWith("Solaris")) || (osName.startsWith("SunOS"))) {
            nativeJar = "solaris_natives.jar";
        } else {
            fatalErrorOccured("OS (" + osName + ") not supported", null);
        }
        if (nativeJar == null) {
            fatalErrorOccured("no lwjgl natives files found", null);
        } else {
            nativeJar = trimExtensionByCapabilities(nativeJar);
            this.urlList[(jarCount - 1)] = new URL(path, nativeJar);
        }
    }

    @SuppressWarnings("unchecked")
    public void run() {
        init();
        this.state = 3;
        this.percentage = 5;
        try {
            try {
                loadJarURLs();
                @SuppressWarnings("rawtypes")
                String path = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
                    }
                });
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File modDir = new File(path + "../mods");
                File coreModDir = new File(path + "../coremods");
                if (!modDir.exists())
                    modDir.mkdirs();
                if (!coreModDir.exists())
                    coreModDir.mkdirs();
                if (force) {
                    delete(new File(path + "../coremods"));
                    delete(new File(path + "../mods"));
                }
                if (!modDir.exists())
                    modDir.mkdirs();
                if (!coreModDir.exists())
                    coreModDir.mkdirs();
                if (this.latestVersion != null) {
                    File versionFile = new File(dir, "version");
                    boolean cacheAvailable = false;
                    if ((!force) && (versionFile.exists()) && ((this.latestVersion.equals("-1")) || (this.latestVersion.equals(readVersionFile(versionFile))))) {
                        System.out.println("Found cached version " + this.latestVersion);
                        cacheAvailable = true;
                        this.percentage = 90;
                    }
                    if ((!cacheAvailable) || (force)) {
                        if (versionFile.exists() && !(this.latestVersion.equals(readVersionFile(versionFile))))
                            System.out.println("Updating from version " + readVersionFile(versionFile) + " to " + this.latestVersion);
                        else {
                            System.out.println("Downloading version " + this.latestVersion);
                        }
                        downloadJars(path);
                        extractJars(path);
                        extractNatives(path);
                        if (this.latestVersion != null) {
                            this.percentage = 100;
                            writeVersionFile(versionFile, this.latestVersion);
                        }
                    }
                }
                updateClassPath(dir);
                this.state = 10;
            } catch (AccessControlException ace) {
                fatalErrorOccured(ace.getMessage(), ace);
                this.certificateRefused = true;

                this.loaderThread = null;
            }
        } catch (Exception e) {
            fatalErrorOccured(e.getMessage(), e);

            this.loaderThread = null;

            return;
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

    protected void updateClassPath(File dir) throws Exception {
        this.state = 6;
        this.percentage = 100;
        URL[] urls = new URL[this.urlList.length];
        for (int i = 0; i < this.urlList.length; i++) {
            urls[i] = new File(dir, getJarName(this.urlList[i])).toURI().toURL();
        }

        if (classLoader == null) {
            classLoader = new URLClassLoader(urls) {
                protected PermissionCollection getPermissions(CodeSource codesource) {
                    PermissionCollection perms = null;
                    try {
                        Method method = SecureClassLoader.class.getDeclaredMethod("getPermissions", new Class[] { CodeSource.class });

                        method.setAccessible(true);
                        perms = (PermissionCollection) method.invoke(getClass().getClassLoader(), new Object[] { codesource });

                        String host = "2toast.net";
                        if ((host != null) && (host.length() > 0)) {
                            perms.add(new SocketPermission(host, "connect,accept"));
                        } else {
                            codesource.getLocation().getProtocol().equals("file");
                        }
                        perms.add(new FilePermission("<<ALL FILES>>", "read"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return perms;
                }

            };
        }

        String path = dir.getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        unloadNatives(path);
        System.setProperty("org.lwjgl.librarypath", path + "natives");
        System.setProperty("net.java.games.input.librarypath", path + "natives");
        natives_loaded = true;
    }

    @SuppressWarnings("rawtypes")
    private void unloadNatives(String nativePath) {
        if (!natives_loaded) {
            return;
        }
        try {
            Field field = ClassLoader.class.getDeclaredField("loadedLibraryNames");
            field.setAccessible(true);
            Vector libs = (Vector) field.get(getClass().getClassLoader());
            String path = new File(nativePath).getCanonicalPath();
            for (int i = 0; i < libs.size(); i++) {
                String s = (String) libs.get(i);
                if (s.startsWith(path)) {
                    libs.remove(i);
                    i--;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        @SuppressWarnings("rawtypes")
        Class appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
        //Class appletClass = classLoader.loadClass("net.minecraft.client.main.Main");
        return (Applet) appletClass.newInstance();
    }

    protected void downloadJars(String path) throws Exception {
        this.state = 4;
        int[] fileSizes = new int[this.urlList.length];
        for (int i = 0; i < this.urlList.length; i++) {
            //System.out.println(this.urlList[i]);
            URLConnection urlconnection = this.urlList[i].openConnection();
            urlconnection.setDefaultUseCaches(false);
            if ((urlconnection instanceof HttpURLConnection)) {
                ((HttpURLConnection) urlconnection).setRequestMethod("HEAD");
            }
            fileSizes[i] = urlconnection.getContentLength();
            this.totalSizeDownload += fileSizes[i];
        }

        int initialPercentage = this.percentage = 10;
        byte[] buffer = new byte[65536];

        for (int i = 0; i < this.urlList.length; i++) {
            int unsuccessfulAttempts = 0;
            int maxUnsuccessfulAttempts = 3;
            boolean downloadFile = true;

            while (downloadFile) {
                downloadFile = false;
                URLConnection urlconnection = this.urlList[i].openConnection();
                if ((urlconnection instanceof HttpURLConnection)) {
                    urlconnection.setRequestProperty("Cache-Control", "no-cache");
                    urlconnection.connect();
                }
                String currentFile = getFileName(this.urlList[i]);
                InputStream inputstream = getJarInputStream(currentFile, urlconnection);
                FileOutputStream fos;
                if (currentFile.endsWith(".mod.jar")) {
                    fos = new FileOutputStream(path + "../coremods/" + currentFile.replaceAll(".mod.jar", ".jar"));
                } else if (currentFile.endsWith(".x.zip")) {
                    fos = new FileOutputStream(path + "../" + currentFile);
                } else if (currentFile.endsWith(".zip")) {
                    fos = new FileOutputStream(path + "../mods/" + currentFile);
                } else {
                    fos = new FileOutputStream(path + currentFile);
                }
                long downloadStartTime = System.currentTimeMillis();
                int downloadedAmount = 0;
                int fileSize = 0;
                String downloadSpeedMessage = "";
                int bufferSize;
                while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, bufferSize);
                    this.currentSizeDownload += bufferSize;
                    fileSize += bufferSize;
                    this.percentage = (initialPercentage + this.currentSizeDownload * 70 / this.totalSizeDownload);
                    this.subtaskMessage = ("Retrieving resource: " + this.currentSizeDownload * 100 / this.totalSizeDownload + "%");
                    downloadedAmount += bufferSize;
                    long timeLapse = System.currentTimeMillis() - downloadStartTime;
                    if (timeLapse >= 1000L) {
                        float downloadSpeed = downloadedAmount / (float) timeLapse;
                        downloadSpeed = (int) (downloadSpeed * 100.0F) / 100.0F;
                        downloadSpeedMessage = " @ " + downloadSpeed + " KB/sec";
                        downloadedAmount = 0;
                        downloadStartTime += 1000L;
                    }
                    this.subtaskMessage += downloadSpeedMessage;
                }

                inputstream.close();
                fos.close();
                if (((urlconnection instanceof HttpURLConnection)) && (fileSize != fileSizes[i]) && (fileSizes[i] > 0)) {
                    unsuccessfulAttempts++;
                    if (unsuccessfulAttempts < maxUnsuccessfulAttempts) {
                        downloadFile = true;
                        this.currentSizeDownload -= fileSize;
                    } else {
                        throw new Exception("failed to download " + currentFile);
                    }
                }
            }
        }

        this.subtaskMessage = "";
    }

    protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection) throws Exception {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void extractLZMA(String in, String out) throws Exception {
        File f = new File(in);
        FileInputStream fileInputHandle = new FileInputStream(f);
        Class clazz = Class.forName("LZMA.LzmaInputStream");
        Constructor constructor = clazz.getDeclaredConstructor(new Class[] { InputStream.class });

        InputStream inputHandle = (InputStream) constructor.newInstance(new Object[] { fileInputHandle });

        OutputStream outputHandle = new FileOutputStream(out);
        byte[] buffer = new byte[16384];
        for (int ret = inputHandle.read(buffer); ret >= 1; ret = inputHandle.read(buffer)) {
            outputHandle.write(buffer, 0, ret);
        }

        inputHandle.close();
        outputHandle.close();
        outputHandle = null;
        inputHandle = null;
        f.delete();
    }

    protected void extractZip(String in, String out) throws Exception {
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

    protected void extractPack(String in, String out) throws Exception {
        File f = new File(in);
        FileOutputStream fostream = new FileOutputStream(out);
        JarOutputStream jostream = new JarOutputStream(fostream);
        Pack200.Unpacker unpacker = Pack200.newUnpacker();
        unpacker.unpack(f, jostream);
        jostream.close();
        f.delete();
    }

    void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    protected void extractJars(String path) throws Exception {
        this.state = 5;
        float increment = 10.0F / this.urlList.length;
        for (int i = 0; i < this.urlList.length; i++) {
            this.percentage = (80 + (int) (increment * (i + 1)));
            String filename = getFileName(this.urlList[i]);
            if (filename.endsWith(".pack.lzma")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replaceAll(".lzma", ""));
                extractLZMA(path + filename, path + filename.replaceAll(".lzma", ""));
                this.subtaskMessage = ("Extracting: " + filename.replaceAll(".lzma", "") + " to " + filename.replaceAll(".pack.lzma", ""));
                extractPack(path + filename.replaceAll(".lzma", ""), path + filename.replaceAll(".pack.lzma", ""));

            } else if (filename.endsWith(".pack")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".pack", ""));
                extractPack(path + filename, path + filename.replace(".pack", ""));

            } else if (filename.endsWith(".lzma")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".lzma", ""));
                extractLZMA(path + filename, path + filename.replace(".lzma", ""));

            } else if (filename.endsWith(".x.zip")) {
                this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".x.zip", ""));
                extractZip(path + "../" + filename, path + "../");
                (new File(path + "../" + filename)).delete();
            }
        }
    }

    protected void extractNatives(String path) throws Exception {
        this.state = 5;
        int initialPercentage = this.percentage = 90; // added 90
        String nativeJar = getJarName(this.urlList[(this.urlList.length - 1)]);
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
        File nativeFolder = new File(path + "natives");
        if (!nativeFolder.exists()) {
            nativeFolder.mkdir();
        }
        JarFile jarFile = new JarFile(path + nativeJar, true);
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
                File f = new File(path + "natives" + File.separator + entry.getName());
                if ((!f.exists()) || (f.delete())) {
                    InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
                    OutputStream out = new FileOutputStream(path + "natives" + File.separator + entry.getName());
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
        File f = new File(path + nativeJar);
        f.delete();
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
        if (fileName.endsWith(".pack.lzma")) {
            fileName = fileName.replaceAll(".pack.lzma", "");
        } else if (fileName.endsWith(".pack")) {
            fileName = fileName.replaceAll(".pack", "");
        } else if (fileName.endsWith(".lzma")) {
            fileName = fileName.replaceAll(".lzma", "");
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

    @SuppressWarnings("unchecked")
    public boolean canPlayOffline() {
        try {
            @SuppressWarnings("rawtypes")
            String path = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
                }
            });
            File dir = new File(path);
            if (!dir.exists()) {
                return false;
            }
            dir = new File(dir, "version");
            if (!dir.exists()) {
                return false;
            }
            String version = readVersionFile(dir);
            if ((version != null) && (version.length() > 0))
                return true;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
