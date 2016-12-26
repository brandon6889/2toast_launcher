package net.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MinecraftResourceDownloader.
 */
public class MinecraftResourceDownloader {
    /* Download progress */
    private int mTotalSize = 0;
    private int mDownloadedSize = 0;
    
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
    
    private static final int CONCURRENT_DOWNLOADS = 4;
    
    public MinecraftResourceDownloader(String path, Object caller) {
        mPath = path;
        mCaller = caller;
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
     * @return 0 to 100
     */
    protected int getProgress() {
        synchronized (mLockProgress) {
            if (mTotalSize == 0)
                return 0;
            if (mTotalSize == mDownloadedSize)
                return 100;
            return 100*mDownloadedSize/mTotalSize;
        }
    }
    
    /**
     * Download queued resources.
     * @throws java.lang.Exception
     */
    protected void download() throws Exception {
        synchronized (mLockQueue) {
            while (mInProgress.size() < CONCURRENT_DOWNLOADS && mWaiting.size() > 0) {
                MinecraftResource resource = mWaiting.remove();
                mInProgress.add(resource);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            resource.download(MinecraftResourceDownloader.this);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }.start();
            }
        }
        System.out.println("break");
    }
    
    protected void download(MinecraftResource resource) throws Exception {
        System.out.println("Downloading "+resource.getName());
        int unsuccessfulAttempts = 0;
        int maxUnsuccessfulAttempts = 3;
        boolean downloadFile = true;
        int fileSize = resource.getSize(); // Ensure size proxy is used
        String filePath = resource.getPath();
        
        File localFile = new File(mPath + filePath);
        if (localFile.exists() && GameUpdater.calcSHA1(localFile).equals(resource.getHash().toUpperCase())) {
            synchronized (mLockProgress) {
                mDownloadedSize += fileSize;
            }
            synchronized (mLockQueue) {
                mInProgress.remove(resource);
                this.download();
            }
            System.out.println("Done "+resource.getName());
            return; // We good
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
                        mDownloadedSize += downloadedAmount;
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
                    throw new Exception("Failed to download " + resource.getName());
                }
            }
            synchronized (mLockQueue) {
                mInProgress.remove(resource);
                this.download();
            }
            System.out.println("Done "+resource.getName());
        }
    }
}
