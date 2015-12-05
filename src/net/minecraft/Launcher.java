package net.minecraft;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class Launcher extends Applet implements Runnable, AppletStub {
    private static final long serialVersionUID = 1L;
    public Map<String, String> customParameters;
    private GameUpdater gameUpdater;
    private boolean gameUpdaterStarted;
    private Applet applet;
    private Image bgImage;
    private Image nptoastLogo;
    private boolean active;
    private int context;
    private VolatileImage img;

    public Launcher() {
        this.customParameters = new HashMap<String, String>();
        this.gameUpdaterStarted = false;
        this.active = false;
        this.context = 0;
    }

    public boolean isActive() {
        if (this.context == 0) {
            this.context = -1;
            try {
                if (getAppletContext() != null) {
                    this.context = 1;
                }
            } catch (Exception localException) {
            }
        }
        if (this.context == -1) {
            return this.active;
        }

        return super.isActive();
    }

    public void init(String userName, String latestVersion, String downloadTag, String sessionId) {
        try {
            this.bgImage = ImageIO.read(LoginForm.class.getResource("bgimg.jpg"));
            this.nptoastLogo = ImageIO.read(LoginForm.class.getResource("logo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.customParameters.put("username", userName);
        this.customParameters.put("sessionid", sessionId);
        this.customParameters.put("downloadTag", downloadTag);
        this.gameUpdater = new GameUpdater(latestVersion);
    }

    public boolean canPlayOffline() {
        return this.gameUpdater.canPlayOffline();
    }

    public void init() {
        if (this.applet != null) {
            this.applet.init();
            return;
        }

        init(getParameter("userName"), getParameter("latestVersion"), getParameter("downloadTag"), getParameter("sessionId"));
    }

    public void start() {
        if (this.applet != null) {
            this.applet.start();
            return;
        }
        if (this.gameUpdaterStarted) {
            return;
        }

        Thread t = new Thread() {
            public void run() {
                Launcher.this.gameUpdater.run();
                try {
                    if (!Launcher.this.gameUpdater.fatalError) {
                        Launcher.this.replace(Launcher.this.gameUpdater.createApplet());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();
        t = new Thread() {
            public void run() {
                while (Launcher.this.applet == null) {
                    Launcher.this.repaint();
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
        this.gameUpdaterStarted = true;
    }

    public void stop() {
        if (this.applet != null) {
            this.active = false;
            this.applet.stop();
            return;
        }
    }

    public void destroy() {
        if (this.applet != null) {
            this.applet.destroy();
            return;
        }
    }

    public void replace(Applet applet) {
        this.applet = applet;
        applet.setStub(this);
        applet.setSize(getWidth(), getHeight());
        setLayout(new BorderLayout());
        add(applet, "Center");
        applet.init();
        this.active = true;
        applet.start();
        validate();
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g2) {
        if (this.applet != null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if ((this.img == null) || (this.img.getWidth() != w) || (this.img.getHeight() != h)) {
            this.img = createVolatileImage(w, h);
        }
        Graphics g = this.img.getGraphics();
        //for (int x = 0; x <= w / 200; x++) {
        //    for (int y = 0; y <= h / 200; y++) {
        //        g.drawImage(this.bgImage, x * 200, y * 200, null);
        //    }
        //}
        g.drawImage(this.bgImage, 0, 0, w, h, null);
        
        g.drawImage(this.nptoastLogo, 30, 20, null);

        g.setColor(new Color(225, 232, 236));
        
        g.fillRect(20, h-100, w - 40, 60);

        g.setColor(new Color(29, 77, 126));
        String msg = "Updating Minecraft";
        if (this.gameUpdater.fatalError) {
            msg = "Failed to launch";
        }
        g.setFont(new Font(null, 1, 24));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h - 76);
        g.setFont(new Font(null, 0, 18));
        fm = g.getFontMetrics();
        msg = this.gameUpdater.getDescriptionForState();
        if (this.gameUpdater.fatalError) {
            msg = this.gameUpdater.fatalErrorDescription;
        }
        //g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 1);
        msg = this.gameUpdater.subtaskMessage;
        //g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 2);
        if (!this.gameUpdater.fatalError) {
            g.setColor(Color.black);
            g.fillRect(64, h - 60, w - 128 + 1, 9);
            g.setColor(new Color(29, 77, 126));
            g.fillRect(64, h - 60, this.gameUpdater.percentage * (w - 128) / 100, 8);
            g.setColor(new Color(58, 154, 252));
            g.fillRect(65, h - 60 + 1, this.gameUpdater.percentage * (w - 128) / 100 - 2, 3);
        }
        g.dispose();
        g2.drawImage(this.img, 0, 0, w, h, null);
    }

    public void run() {
    }

    public String getParameter(String name) {
        String custom = (String) this.customParameters.get(name);
        if (custom != null) {
            return custom;
        }
        try {
            return super.getParameter(name);
        } catch (Exception e) {
            this.customParameters.put(name, null);
        }
        return null;
    }

    public void appletResize(int i, int j) {
    }
}
