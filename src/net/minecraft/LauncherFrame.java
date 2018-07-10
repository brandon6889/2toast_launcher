package net.minecraft;

//Bootstrapper expects to pass in a Frame
//Need to keep Frame for legacy reasons
import java.awt.Frame;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;

import javax.swing.JOptionPane;

public class LauncherFrame {
    public static final int VERSION = 14;
    public static final String USERVERSION = "1.3";
    private static final long serialVersionUID = 1L;
    
    //Bootstrapper expects to pass in a Frame
    //Need to keep Frame for legacy reasons
    public LauncherFrame(Frame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bullshit) {
        System.out.println();
        System.out.println("== Kuumba Launcher v" + USERVERSION + " ==");
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                frame.dispose();
            }
        }).start();
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
