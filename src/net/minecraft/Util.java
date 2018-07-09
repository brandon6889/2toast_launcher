package net.minecraft;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Util {
    
    private static File workDir = null;

    protected static File getWorkingDirectory() {
        if (workDir == null) {
            workDir = getWorkingDirectory("minecraftKuumba");
        }
        return workDir;
    }

    private static File getWorkingDirectory(String applicationName) {
        String userHome = System.getProperty("user.home", ".");
        File workingDirectory;
        switch (getPlatform()) {
        case linux:
        case solaris:
            workingDirectory = new File(userHome, '.' + applicationName + '/');
            break;
        case windows:
            String applicationData = System.getenv("LOCALAPPDATA");
            if (applicationData != null) {
                workingDirectory = new File(applicationData, "." + applicationName + '/');
            } else {
                workingDirectory = new File(userHome, '.' + applicationName + '/');
            }
            break;
        case macos:
            workingDirectory = new File(userHome, "Library/Application Support/" + applicationName);
            break;
        default:
            workingDirectory = new File(userHome, applicationName + '/');
        }

        if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs())) {
            throw new RuntimeException("The working directory could not be created: " + workingDirectory);
        }

        return workingDirectory;
    }

    protected static OS getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.windows;
        }
        if (osName.contains("mac")) {
            return OS.macos;
        }
        if (osName.contains("solaris")) {
            return OS.solaris;
        }
        if (osName.contains("sunos")) {
            return OS.solaris;
        }
        if (osName.contains("linux")) {
            return OS.linux;
        }
        if (osName.contains("unix")) {
            return OS.linux;
        }

        return OS.unknown;
    }

    protected static String executePost(String targetURL, String urlParameters) {
    	if (System.getProperty("java.runtime.name").equals("Java(TM) SE Runtime Environment")) {
    	    try { // Bypass cryptographic limitations
        	/*
                 * Do the following, but with reflection to bypass access checks:
                 *
                 * JceSecurity.isRestricted = false;
                 * JceSecurity.defaultPolicy.perms.clear();
                 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
                 */
    	        
        	    // Enable restricted ciphers
    	        final Field restrictedField = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
                restrictedField.setAccessible(true);
                restrictedField.set(null, java.lang.Boolean.FALSE);
            
                // Get default cipher policy
                final Field policyField = Class.forName("javax.crypto.JceSecurity").getDeclaredField("defaultPolicy");
                policyField.setAccessible(true);
                final PermissionCollection defaultPolicy = (PermissionCollection) policyField.get(null);
            
                // Clear existing (restricted) policy
                final Field permsField = Class.forName("javax.crypto.CryptoPermissions").getDeclaredField("perms");
                final Field instanceField = Class.forName("javax.crypto.CryptoAllPermission").getDeclaredField("INSTANCE");
                permsField.setAccessible(true);
                ((Map<?, ?>) permsField.get(defaultPolicy)).clear();
                
                instanceField.setAccessible(true);
                defaultPolicy.add((Permission) instanceField.get(null));
            } catch (Exception e) {}
    	}
        
        HttpsURLConnection connection = null;
        try { // Load root CA
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            ByteArrayInputStream inStream = new ByteArrayInputStream(
                    read(MinecraftLauncher.class.getResourceAsStream("/res/sanders2020.ks")));
            trustStore.load(inStream, "LabourIsEntitledToAllItCreates".toCharArray());
            inStream.close();

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(trustStore, "LabourIsEntitledToAllItCreates".toCharArray()); // key store password

            SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            URL url = new URL(targetURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslCtx.getSocketFactory());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected enum OS {
        linux, solaris, windows, macos, unknown;
    }
    
    protected static String readFile(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String data = "";
            String line;
            while ((line = br.readLine()) != null) {
                data = data + line;
            }
            br.close();
            return data;
        } catch (IOException e) {
            System.err.println("Unable to read file: " + f.toString());
        }
        return "";
    }

    private static byte[] read(InputStream src) throws IOException {
        ByteArrayOutputStream dest = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        for (int n = 0; n != -1; n = src.read(buffer))
            dest.write(buffer, 0, n);

        src.close();
        dest.close();

        return dest.toByteArray();
    }
    
    protected static String getJavaBin() throws Exception {
        String javabin = System.getProperty("java.home") + File.separator + "bin" + File.separator;
        if (getPlatform() == OS.windows && new File(javabin+"javaw.exe").exists())
            return javabin += "java.exe";
        else if (new File (javabin+"java").exists())
            return javabin += "java";
        throw new Exception("Could not locate java");
    }

    static protected void delete(File f) throws IOException {
        Files.walkFileTree(f.toPath(), new FileVisitor() {
            @Override
            public FileVisitResult preVisitDirectory(Object t, BasicFileAttributes bfa) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Object t, BasicFileAttributes bfa) throws IOException {
                Files.delete((Path)t);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Object t, IOException ioe) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Object t, IOException ioe) throws IOException {
                Files.delete((Path)t);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Create list of files in directory recursively.
     * @param dir Directory to search.
     * @return List of files in subfolders.
     */
    static List<File> enumFiles(File dir) {
        List<File> files = new ArrayList();
        for (File f : dir.listFiles()) {
            if (f.isDirectory())
                files.addAll(enumFiles(f));
            else
                files.add(f);
        }
        return files;
    }
}
