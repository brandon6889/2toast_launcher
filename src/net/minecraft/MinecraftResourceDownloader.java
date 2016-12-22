package net.minecraft;

/**
 * MinecraftResourceDownloader.
 */
public class MinecraftResourceDownloader {
    private int totalSize = 0;
    private int downloadedSize = 0;
    /**
     * Get progress of downloader.
     * @return 0 to 100
     */
    protected int getProgress() {
        if (downloadedSize == 0)
            return 0;
        return 0;
    }
}
