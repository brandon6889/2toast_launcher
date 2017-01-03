package net.minecraft;

public class OperatingSystem {
    /* JSON fields */
    public String name;
    
    /**
     * Determine whether the provided operating system matches that in JSON.
     * @param os Operating system
     * @return 
     */
    protected boolean supportsOS(String os) {
        return name == null || name.trim().equals("") || name.toLowerCase().equals(os);
    }
}
