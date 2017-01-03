package net.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * MinecraftResourceDownloader.
 */
public class MinecraftResourceDownloader {
    /* Download progress */
    private long mTotalSize = 0;
    private long mDownloadedSize = 0;
    
    /* Minecraft root folder */
    private final String mPath;
    
    /* Download queue */
    private final LinkedList<MinecraftResource> mWaiting = new LinkedList();
    private final List<MinecraftResource> mInProgress = new ArrayList(); // Later replace this & sort with a queuer class
    
    /* Lock objects */
    private final Object mLockQueue = new Object();
    private final Object mLockProgress = new Object();
    
    /* Notify caller */
    private final Object mCaller;
    
    /* Download errors */
    private Exception e;
    
    private int mConcurrentDownloads = 1;
    
    public MinecraftResourceDownloader(String path, Object caller) {
        mPath = path;
        mCaller = caller;
    }
    
    protected void setConcurrentDownloads(int i) {
        mConcurrentDownloads = i;
    }
    
    /**
     * Add all resources before starting downloader.
     * @param resources
     * @throws IOException 
     */
    protected void addResources(List<MinecraftResource> resources) throws IOException {
        mWaiting.addAll(resources);
        
        /* Remove any duplicate assets, esp. in legacy. Since we store files per hash this is an issue. */
        //LinkedList<MinecraftResource> dedup = new LinkedList(new LinkedHashSet(mWaiting)); // Can use this to preserve order
        Set<MinecraftResource> dedup = new LinkedHashSet(mWaiting);
        mWaiting.clear();
        mWaiting.addAll(dedup);
        
        mTotalSize = 0;
        for (MinecraftResource r : mWaiting)
            mTotalSize += r.getSize();
    }
    
    /**
     * Sort queued resources. Call before starting unless synchronized.
     * @param comparator 
     */
    protected void sortResources(Comparator<MinecraftResource> comparator) {
        mWaiting.sort(comparator);
    }
    
    /**
     * Get progress of downloader.
     * @return 0 to 1000
     * @throws java.lang.Exception
     */
    protected int getProgress() throws Exception {
        synchronized (this) {
            if (e != null)
                throw e;
        }
        synchronized (mLockProgress) {
            if (mTotalSize == mDownloadedSize) {
                synchronized (mLockQueue) {
                    if (mInProgress.isEmpty())
                        return 1000;
                    else
                        return 0;
                }
            } else {
                synchronized (mLockQueue) {
                    // This sometimes false-positives in failure conditions
                    if (mInProgress.isEmpty() && mWaiting.isEmpty()) {
                        synchronized (this) {
                            if (e != null)
                                throw e;
                            else
                                throw new Exception("Download: filesize mismatch: got " + mDownloadedSize + " bytes, expected " + mTotalSize);
                        }
                    }
                }
            }
            int progress = (int)((1000*mDownloadedSize)/mTotalSize);
            if (progress == 1000)
                progress--;
            return progress;
        }
    }
    
    /**
     * Download queued resources.
     * @throws java.lang.Exception
     */
    protected void download() throws Exception {
        synchronized (mLockQueue) {
            while (mInProgress.size() < mConcurrentDownloads && mWaiting.size() > 0) {
                MinecraftResource resource = mWaiting.remove();
                mInProgress.add(resource);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            resource.download(MinecraftResourceDownloader.this);
                        } catch (Exception ex) {
                            synchronized (MinecraftResourceDownloader.this) {
                                if (MinecraftResourceDownloader.this.e == null)
                                    MinecraftResourceDownloader.this.e = ex;
                            }
                        }
                    }
                }.start();
            }
        }
    }
    
    protected void download(MinecraftResource resource) throws Exception {
        int unsuccessfulAttempts = 0;
        int maxUnsuccessfulAttempts = 3;
        boolean downloadFile = true;
        int fileSize = resource.getSize(); // Ensure size proxy is used
        String filePath = resource.getPath();
        
        File localFile = new File(mPath + filePath);
        try {
            if (localFile.exists() && calcSHA1(localFile).equals(resource.getHash().toUpperCase())) {
                synchronized (mLockProgress) {
                    mDownloadedSize += fileSize;
                }
                synchronized (mLockQueue) {
                    mInProgress.remove(resource);
                    this.download();
                }
                synchronized (mCaller) {
                    mCaller.notify();
                }
                return; // We good
            } else {
                System.out.println("SHA1 "+resource.getName()+" calc:"+calcSHA1(localFile)+" prov:"+resource.getHash());
            }
        } catch (Exception e) {
            // The hash could not be checked, so force update.
            System.out.println("Failed check for "+resource.getName()+" "+localFile.toString());
        }

        while (downloadFile) {
            downloadFile = false;
            URLConnection urlconnection = resource.getUrl().openConnection();
            if ((urlconnection instanceof HttpURLConnection)) {
                urlconnection.setRequestProperty("Cache-Control", "no-cache");
                urlconnection.connect();
            }
            FileOutputStream fos;
            int downloadedAmount = 0;
            try (InputStream inputstream = GameUpdater.getJarInputStream(filePath, urlconnection)) {
                File dir = new File(mPath + filePath.substring(0,filePath.lastIndexOf("/")));
                dir.mkdirs();
                fos = new FileOutputStream(localFile);
                int bufferSize;
                byte[] buffer = new byte[65536];
                while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, bufferSize);
                    downloadedAmount += bufferSize;
                    synchronized (mLockProgress) {
                        mDownloadedSize += bufferSize;
                    }
                }
            }
            fos.close();
            if (((urlconnection instanceof HttpURLConnection)) && (fileSize != downloadedAmount) && (fileSize > 0)) {
                synchronized (mLockProgress) {
                    mDownloadedSize -= downloadedAmount;
                }
                unsuccessfulAttempts++;
                if (unsuccessfulAttempts < maxUnsuccessfulAttempts) {
                    downloadFile = true;
                } else {
                    throw new Exception("Failed to download " + resource.getName() + " from " + urlconnection.toString() + ": got " + downloadedAmount + " bytes, expected " + fileSize);
                }
            }
            /*synchronized (mLockProgress) {
                mDownloadedSize += fileSize;
            }*/
            synchronized (mLockQueue) {
                mInProgress.remove(resource);
                this.download();
            }
            synchronized (mCaller) {
                mCaller.notify();
            }
        }
    }
    
    protected static String calcSHA1(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len = input.read(buffer);
            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }
            return new HexBinaryAdapter().marshal(sha1.digest());
        }
    }
}
