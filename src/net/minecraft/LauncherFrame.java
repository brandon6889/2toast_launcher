package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

public class LauncherFrame extends Frame {
    public static final int VERSION = 14;
    private static final long serialVersionUID = 1L;
    private Launcher launcher;
    private LoginForm loginForm;

    public LauncherFrame(Frame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bullshit) {
        super("2Toasty Minecraft Launcher");
        System.out.println("WeaselWare Minecraft Launcher");
        System.out.println("------------------------------");
        System.out.println("Java Runtime: "+System.getProperty("java.runtime.name"));
        System.out.println("Java Version: "+System.getProperty("java.runtime.version") + " ("+System.getProperty("java.specification.version")+")");
        float heapSizeMegs = (float) (Runtime.getRuntime().maxMemory() / 1024L / 1024L);
        System.out.println("Heap size:    " + heapSizeMegs);
        this.setBackground(new Color(0, 0, 0));
        this.loginForm = new LoginForm(this);
        this.setLayout(new BorderLayout());
        this.add(this.loginForm, "Center");
        this.loginForm.setPreferredSize(new Dimension(900, 580));
        
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
        pack();
        setLocationRelativeTo(null);
        //frame.removeAll();
        //frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH); //maximize
        //frame.setLocationRelativeTo(null);
        //frame.setLayout(new BorderLayout());
        //frame.add(this, "Center");
        //frame.pack(); // does it do for this too?
        //frame.validate();
        try {
            /*frame.*/setIconImage(ImageIO.read(LauncherFrame.class.getResource("favicon.png")));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        /*frame.*/addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(30000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("FORCING EXIT!");
                        System.exit(0);
                    }
                }.start();

                if (LauncherFrame.this.launcher != null) {
                    LauncherFrame.this.launcher.stop();
                    LauncherFrame.this.launcher.destroy();
                }
                System.exit(0);
            }
        });
        
        setVisible(true);
        frame.dispose();
    }

    public void playCached(String userName) {
        try {
            if ((userName == null) || (userName.length() <= 0)) {
                userName = "Player"; // lol
            }
            this.launcher = new Launcher();
            this.launcher.customParameters.put("userName", userName);
            this.launcher.init();
            //frame.removeAll();
            this.removeAll();
            this.add(this.launcher, "Center");
            //frame.validate();
            this.launcher.start();
            this.loginForm = null;
            this.setTitle("2Toasty Minecraft");
            this.validate();
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.toString());
        }
    }

    public void login(String userName, String password) {
        try {
            String parameters = "user=" + URLEncoder.encode(userName, "UTF-8") + "&pass=" + URLEncoder.encode(password, "UTF-8") + "&version=" + 14;
            String result = Util.executePost("https://2toast.net/minecraft/login.php", parameters);
            if (result == null) {
                showError("Can't connect to 2toast.net.");
                this.loginForm.setNoNetwork();
                return;
            }
            if (!result.contains(":")) {
                showError(result);
                this.loginForm.setNoNetwork();
                return;
            }

            String[] values = result.split(":");
            if (values.length <3 /*padma*/+2) {
                showError(result);
                this.loginForm.setNoNetwork();
                return;
            }
            System.out.println("Logged in as " + values[2]);// + "', sid "+values[3]);
            this.launcher = new Launcher();
            this.launcher.customParameters.put("userName", values[2].trim());
            this.launcher.customParameters.put("latestVersion", values[0].trim());
            this.launcher.customParameters.put("downloadTag", values[1].trim());
            this.launcher.customParameters.put("sessionId", values[3].trim());
            this.launcher.init();
            //frame.removeAll();
            this.removeAll();
            this.add(this.launcher, "Center");
            //frame.validate();
            this.launcher.start();
            this.loginForm.loginOk();
            this.loginForm = null;
            this.setTitle("2Toasty Minecraft Launcher");
            this.validate();
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.toString());
            this.loginForm.setNoNetwork();
        }
    }

    private void showError(String error) {
        removeAll();
        //frame.getContentPane().removeAll();
        add(this.loginForm);
        this.loginForm.setError(error);
        //frame.getContentPane().repaint();
        validate();
    }

    public boolean canPlayOffline(String userName) {
        Launcher launcher = new Launcher();
        launcher.init(userName, null, null, null);
        return launcher.canPlayOffline();
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
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, ((Integer)optionSet.valueOf((OptionSpec)proxyPortOption)).intValue()));
            } catch (Exception var14) {}
        }
        File workingDirectory = (File)optionSet.valueOf((OptionSpec)workDirOption);
        workingDirectory.mkdirs();
        //JFrame frame = new JFrame();
        //frame.setTitle("2Toast Minecraft Launcher");
        //frame.setPreferredSize(new Dimension(900, 580));
        
        //try {
        //    InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
        //    if(in != null) {
        //        launcherFrame.setIconImage(ImageIO.read(in));
        //    }
        //} catch (IOException var13) {}
        
        //frame.pack();
        //frame.setLocationRelativeTo((Component)null);
        //frame.setVisible(true);
        
        LauncherFrame frame = new LauncherFrame(new Frame(), workingDirectory, proxy, (PasswordAuthentication)null, (String[])leftoverArgs.toArray(new String[leftoverArgs.size()]), 0);
        frame.setLocationRelativeTo((Component)null);
        frame.setVisible(true);
        
        //LauncherFrame launcherFrame = new LauncherFrame();
        //launcherFrame.setVisible(true);
    }
    
    //protected void clearFrame() {
    //    frame.getContentPane().removeAll();
    //}
}
