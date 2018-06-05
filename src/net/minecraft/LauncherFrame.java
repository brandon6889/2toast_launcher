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

public class LauncherFrame extends Frame {
    public static final int VERSION = 14;
    public static final String USERVERSION = "1.3";
    private static final long serialVersionUID = 1L;
    private Launcher launcher;
    private LoginForm loginForm;

    public LauncherFrame(Frame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bullshit) {
        super("2Toasty Minecraft Launcher");
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
        this.setBackground(new Color(0, 0, 0));
        this.loginForm = new LoginForm(this);
        this.setLayout(new BorderLayout());
        this.add(this.loginForm, "Center");
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        this.loginForm.setPreferredSize(new Dimension(width*17/20, height*4/5));

        pack();
        setLocationRelativeTo(null);
        try {
            setIconImage(ImageIO.read(LauncherFrame.class.getResource("favicon.png")));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                new Thread() {
                    @Override
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
        
        this.loginForm.startMusicThread();
    }

    public void playCached(String userName) {
        try {
            if ((userName == null) || (userName.length() <= 0)) {
                userName = "Player"; // lol
            }
            this.launcher = new Launcher();
            this.launcher.customParameters.put("userName", userName);
            this.launcher.init();
            this.removeAll();
            this.add(this.launcher, "Center");
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
            String result = Util.executePost("https://auth.2toast.net/minecraft/login.php", parameters);
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
            System.out.println("Logged in as " + values[2]);
            //this.loginForm.stopMusicThread();
            this.launcher = new Launcher();
            this.launcher.customParameters.put("userName", values[2].trim());
            this.launcher.customParameters.put("latestVersion", values[0].trim());
            this.launcher.customParameters.put("downloadTag", values[1].trim());
            this.launcher.customParameters.put("sessionId", values[3].trim());
            this.launcher.init();
            this.removeAll();
            this.add(this.launcher, "Center");
            this.launcher.start();
            this.loginForm.loginOk();
            this.loginForm = null;
            this.setTitle("2Toasty Minecraft");
            this.validate();
        } catch (UnsupportedEncodingException e) {
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
        //JFrame frame = new JFrame();
        //frame.setTitle("2Toast Minecraft Launcher");
        //frame.setPreferredSize(new Dimension(900, 580));
        
        //try {
        //    InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
        //    if(in != null) {
        //        launcherFrame.setIconImage(ImageIO.read(in));
        //    }
        //} catch (IOException var13) {}
        
        LauncherFrame frame = new LauncherFrame(new Frame(), workingDirectory, proxy, (PasswordAuthentication)null, (String[])leftoverArgs.toArray(new String[leftoverArgs.size()]), 0);
        frame.setLocationRelativeTo((Component)null);
        frame.setVisible(true);
    }
}
