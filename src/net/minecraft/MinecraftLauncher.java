package net.minecraft;

import javax.swing.JOptionPane;

public class MinecraftLauncher {

    public static void main(String[] args) throws Exception {
        if (System.getProperty("java.specification.version").equals("1.6") || System.getProperty("java.specification.version").equals("1.7")) {
            System.out.println("Java too old; quitting.");
            JOptionPane.showMessageDialog(null, "Please update to the 64-bit version of Java 8 or newer.");
        } else {
            LauncherFrame.main(args);
        }
    }
}
