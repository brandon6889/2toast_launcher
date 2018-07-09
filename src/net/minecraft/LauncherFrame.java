package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URLEncoder;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;

import javax.imageio.ImageIO;

import javax.swing.JOptionPane;

public class LauncherFrame {
    public static final int VERSION = 14;
    public static final String USERVERSION = "1.3";
    private static final long serialVersionUID = 1L;
    private Launcher launcher;

    public LauncherFrame(Frame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bullshit) {
        System.out.println();
        System.out.println("== 2Toasty Launcher v" + USERVERSION + " ==");
        System.out.println();
        System.out.println("Java Runtime: "+System.getProperty("java.runtime.name"));
        System.out.println("Java Version: "+System.getProperty("java.vm.name")+" "+System.getProperty("java.version")+" " +System.getProperty("sun.arch.data.model")+" ("+System.getProperty("java.specification.version")+")");
        float heapSizeMegs = (float) (Runtime.getRuntime().maxMemory() / 1024L / 1024L);
        System.out.println("Heap size:    " + heapSizeMegs);
        System.out.println();
        if (System.getProperty("sun.arch.data.model").equals("32")) {
            System.out.println("Quitting due to 32-bit Java runtime.");
            JOptionPane.showMessageDialog(frame, "32-bit Java not supported. Please update to at least Java 8 64-bit.");
            frame.dispose();
            System.exit(2);
            return;
        }
        javafx.application.Application.launch(GuiApplication.class);
    }
    
    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec proxyHostOption = parser.accepts("proxyHost").withRequiredArg();
        ArgumentAcceptingOptionSpec proxyPortOption = parser.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
        ArgumentAcceptingOptionSpec workDirOption = parser.accepts("workDir").withRequiredArg().ofType(File.class).defaultsTo(Util.getWorkingDirectory(), new File[0]);
        NonOptionArgumentSpec nonOption = parser.nonOptions();
        OptionSet optionSet = parser.parse(args);
        List leftoverArgs = optionSet.valuesOf((OptionSpec)nonOption);
        String hostName = (String)optionSet.valueOf((OptionSpec)proxyHostOption);
        Proxy proxy = Proxy.NO_PROXY;
        if(hostName != null) {
            try {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, ((Integer)optionSet.valueOf((OptionSpec)proxyPortOption))));
            } catch (Exception var14) {}
        }
        File workingDirectory = (File)optionSet.valueOf((OptionSpec)workDirOption);
        workingDirectory.mkdirs();
        
        LauncherFrame frame = new LauncherFrame(new Frame(), workingDirectory, proxy, (PasswordAuthentication)null, (String[])leftoverArgs.toArray(new String[leftoverArgs.size()]), 0);
    }
}
