package net.minecraft;

//import java.util.ArrayList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Image;
import java.awt.image.VolatileImage;

public class MinecraftLauncher {

    public static void main(String[] args) throws Exception {
        if (System.getProperty("java.specification.version").equals("1.6") || System.getProperty("java.specification.version").equals("1.7")) {
            Frame frame = new Frame() {
                private VolatileImage render;
                
                public void update(Graphics g) {
                    paint(g);
                }
                
                public void paint(Graphics g2) {
                    int w = getWidth();
                    int h = getHeight();
                    
                    if ((render == null) || (render.getWidth() != w) || (render.getHeight() != h)) {
                        this.render = createVolatileImage(w, h);
                        
                        Graphics g = this.render.getGraphics();
                        
                        g.setFont(new Font(null, 1, 24));
                        g.setColor(Color.white);
                        FontMetrics fm = g.getFontMetrics();
                        String message = "Please update to Java 8 or later.";
                        g.drawString(message, w / 2 - fm.stringWidth(message) / 2, h/2);
                        g.dispose();
                        g2.drawImage(this.render, 0, 0, w, h, null);
                        g2.dispose();
                    }
                }
            };
            frame.setPreferredSize(new Dimension(180, 60));
            frame.setBackground(new Color(102, 0, 0));
            
            frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
            
            //frame.pack();
            frame.setLocationRelativeTo(null);
            
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent arg0) {
                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(800L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            System.out.println("FORCING EXIT!");
                            System.exit(0);
                        }
                    }.start();
                    System.exit(0);
                }
            });
            
            frame.setVisible(true);
        } else {
            LauncherFrame.main(args);
        }
        
        //float heapSizeMegs = (float) (Runtime.getRuntime().maxMemory() / 1024L / 1024L);
        
        //if (heapSizeMegs > 511.0F) {
            //LauncherFrame.main(args);
        /*} else {
            System.err.println("WARNING: Heap size too small. Attempting to relaunch...");
            try {
                String pathToJar = MinecraftLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                ArrayList<String> params = new ArrayList<String>();
                params.add("java");
                params.add("-Xms512m");
                params.add("-Xmx1024m");
                params.add("-Xss256k");
                //params.add("-Dsun.java2d.noddraw=true");
                //params.add("-Dsun.java2d.d3d=false");
                //params.add("-Dsun.java2d.opengl=false");
                //params.add("-Dsun.java2d.pmoffscreen=false");
                params.add("-classpath");
                params.add(pathToJar);
                params.add("net.minecraft.LauncherFrame");
                ProcessBuilder pb = new ProcessBuilder(params);
                Process process = pb.start();
                if (process == null) {
                    throw new Exception("Unable to start process.");
                }
                System.exit(0);
            } catch (Exception e) {
            	System.err.println(e.getMessage());
                e.printStackTrace();
                LauncherFrame.main(args);
            }
        }*/
    }
}
