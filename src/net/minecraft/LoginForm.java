package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.VolatileImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.imageio.ImageIO;

import org.mp3transform.Decoder;

public class LoginForm extends Panel {
    private static final long serialVersionUID = 1L;
    private Image bgImage;
    //private Image bgImageBig;
    private Image toastLogo;
    private TextField userName;
    private TextField password;
    private Checkbox rememberBox;
    private Checkbox updateBox;
    private Button launchButton;
    private Button retryButton;
    private Button offlineButton;
    private Label errorLabel;
    private boolean outdated;
    private VolatileImage img;
    private static final GridBagConstraints loginPanelConstraints;
    private LauncherFrame launcher;
    private InputStream menuMusicInputStream;
    public Decoder menuMusicPlayer = new Decoder();
    
    static {
        loginPanelConstraints = new GridBagConstraints();
        loginPanelConstraints.fill = GridBagConstraints.NONE;
        loginPanelConstraints.weightx = 1.0;
        loginPanelConstraints.weighty = 1.0;
        loginPanelConstraints.insets = new Insets (20, 20, 20, 20);
        loginPanelConstraints.anchor = GridBagConstraints.SOUTHEAST;
    }

    public LoginForm(final LauncherFrame launcherFrame) {
        this.userName = new TextField(20);
        this.password = new TextField(20);
        this.rememberBox = new Checkbox("Remember password");
        this.updateBox = new Checkbox("Force update");
        this.launchButton = new Button("Login");
        this.retryButton = new Button("Try again");
        this.offlineButton = new Button("Play offline");
        this.errorLabel = new Label("", 1);
        this.outdated = false;
        this.launcher = launcherFrame;
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        add(buildLoginPanel(), loginPanelConstraints);
        try {
            this.bgImage = ImageIO.read(LoginForm.class.getResource("bgimg.jpg"));
            this.toastLogo = ImageIO.read(LoginForm.class.getResource("logo.png"));
            this.menuMusicInputStream = new BufferedInputStream(LoginForm.class.getResourceAsStream("music.mp3"), 2*1024*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        readUsername();
        this.retryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                LoginForm.this.errorLabel.setText("");
                LoginForm.this.removeAll();
                LoginForm.this.add(LoginForm.this.buildLoginPanel(), loginPanelConstraints);
                LoginForm.this.validate();
            }
        });
        this.offlineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                launcher.playCached(LoginForm.this.userName.getText());
            }
        });
        this.launchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (LoginForm.this.updateBox.getState())
                    GameUpdater.force = true;
                launcher.login(LoginForm.this.userName.getText(), LoginForm.this.password.getText());
            }
        });
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    LoginForm.this.menuMusicPlayer.play("menu music", LoginForm.this.menuMusicInputStream);
                } catch (IOException e) {}
            }
        };
        t.start();
    }

    private void readUsername() {
        try {
            File lastLogin = new File(Util.getWorkingDirectory(), "lastlogin");
            Cipher cipher = getCipher(2, "passwordfile");
            DataInputStream dis;
            if (cipher != null) {
                dis = new DataInputStream(new CipherInputStream(new FileInputStream(lastLogin), cipher));
            } else {
                dis = new DataInputStream(new FileInputStream(lastLogin));
            }
            this.userName.setText(dis.readUTF());
            this.password.setText(dis.readUTF());
            this.rememberBox.setState(this.password.getText().length() > 0);
            dis.close();
        } catch (Exception e) {
            // do nothing..
        }
    }

    private void writeUsername() {
        try {
            File lastLogin = new File(Util.getWorkingDirectory(), "lastlogin");
            Cipher cipher = getCipher(1, "passwordfile");
            DataOutputStream dos;
            if (cipher != null) {
                dos = new DataOutputStream(new CipherOutputStream(new FileOutputStream(lastLogin), cipher));
            } else {
                dos = new DataOutputStream(new FileOutputStream(lastLogin));
            }
            dos.writeUTF(this.userName.getText());
            dos.writeUTF(this.rememberBox.getState() ? this.password.getText() : "");
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Cipher getCipher(int mode, String password) throws Exception {
        Random random = new Random(43287234L);
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);
        SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(mode, pbeKey, pbeParamSpec);
        return cipher;
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g2) {
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
        //System.out.println("start at w = "+w+", h = "+h);
        
        //int defW, defH, scaledW, scaledH, offsetW, offsetH;
        //double scaleFactorW, scaleFactorH;
        
        //if (w <= 854 && h <= 480) {
            /*defW = this.bgImage.getWidth(null);
            defH = this.bgImage.getHeight(null);
            scaleFactorW = defW / w;
            scaleFactorH = defH / h;
            if (scaleFactorW >= scaleFactorH) {
                scaledW = w;
                scaledH = (int) (defH * scaleFactorW);
                offsetH = (int) ((scaledH - h) / 2);
                g.drawImage(this.bgImage, 0, offsetH, scaledW, scaledH, null);
            } else {
                scaledW = (int) (defW * scaleFactorH);
                scaledH = h;
                offsetW = (int) ((scaledW - w) / 2);
                g.drawImage(this.bgImage, offsetW, 0, scaledW, scaledH, null);
            }*/
        /*} else {
            g.drawImage(this.bgImageBig, 0, 0, w, h, null);
        }*/
        double logoScaleFactor = (double)w / 854;
        int logoWidth = (int) (logoScaleFactor * (this.toastLogo.getWidth(null) / 4));
        int logoHeight = (int) (logoScaleFactor * (this.toastLogo.getHeight(null) / 4));

        g.drawImage(this.bgImage, 0, 0, w, h, null);
        g.drawImage(this.toastLogo, w/28, h/20, logoWidth, logoHeight, null);
        g.dispose();
        g2.drawImage(this.img, 0, 0, w, h, null);
    }

    private Panel buildLoginPanel() {
        Panel panel = new Panel() {
            private static final long serialVersionUID = 1L;
            private Insets insets = new Insets(0, 24, 16, 32);

            public Insets getInsets() {
                return this.insets;
            }

            public void update(Graphics g) {
                paint(g);
            }

            public void paint(Graphics g) {
                super.paint(g);
            }
        };
        panel.setBackground(new Color(237, 240, 242));
        BorderLayout layout = new BorderLayout();
        layout.setHgap(0);
        layout.setVgap(8);
        panel.setLayout(layout);
        GridLayout gl1 = new GridLayout(0, 1);
        GridLayout gl2 = new GridLayout(0, 1);
        gl1.setVgap(2);
        gl2.setVgap(2);
        Panel titles = new Panel(gl1);
        Panel values = new Panel(gl2);
        titles.add(new Label("Username:", 2));
        titles.add(new Label("Password:", 2));
        titles.add(new Label("", 2));
        this.password.setEchoChar('*');
        values.add(this.userName);
        values.add(this.password);
        //values.add(this.rememberBox);
        values.add(this.updateBox);
        panel.add(titles, "West");
        panel.add(values, "Center");
        Panel loginPanel = new Panel(new BorderLayout());
        Panel registerPanel = new Panel(new BorderLayout());
        try {
            if (this.outdated) {
                Label accountLink = new Label("You need to update the launcher!") {
                    private static final long serialVersionUID = 0L;

                    public void paint(Graphics g) {
                        super.paint(g);
                        int x = 0;
                        int y = 0;
                        FontMetrics fm = g.getFontMetrics();
                        int width = fm.stringWidth(getText());
                        int height = fm.getHeight();
                        if (getAlignment() == 0) {
                            x = 0;
                        } else if (getAlignment() == 1) {
                            x = getBounds().width / 2 - width / 2;
                        } else if (getAlignment() == 2) {
                            x = getBounds().width - width;
                        }
                        y = getBounds().height / 2 + height / 2 - 1;
                        g.drawLine(x + 2, y, x + width - 2, y);
                    }

                    public void update(Graphics g) {
                        paint(g);
                    }
                };
                accountLink.setCursor(Cursor.getPredefinedCursor(12));
                accountLink.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent arg0) {
                        try {
                            Desktop.getDesktop().browse(new URL("http://2toast.net").toURI());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                accountLink.setForeground(Color.BLUE);
                registerPanel.add(accountLink, "West");
                registerPanel.add(new Panel(), "Center");
            } else {
                Label accountLink = new Label("New player?") {
                    private static final long serialVersionUID = 0L;

                    public void paint(Graphics g) {
                        super.paint(g);
                        int x = 0;
                        int y = 0;
                        FontMetrics fm = g.getFontMetrics();
                        int width = fm.stringWidth(getText());
                        int height = fm.getHeight();
                        if (getAlignment() == 0) {
                            x = 0;
                        } else if (getAlignment() == 1) {
                            x = getBounds().width / 2 - width / 2;
                        } else if (getAlignment() == 2) {
                            x = getBounds().width - width;
                        }
                        y = getBounds().height / 2 + height / 2 - 1;
                        g.drawLine(x + 2, y, x + width - 2, y);
                    }

                    public void update(Graphics g) {
                        paint(g);
                    }
                };
                accountLink.setCursor(Cursor.getPredefinedCursor(12));
                accountLink.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent arg0) {
                        try {
                            Desktop.getDesktop().browse(new URL("http://2toast.net").toURI());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                accountLink.setForeground(Color.BLUE);
                registerPanel.add(accountLink, "West");
                registerPanel.add(new Panel(), "Center");
            }
        } catch (Error localError) {
        }
        loginPanel.add(registerPanel, "Center");
        loginPanel.add(this.launchButton, "East");
        panel.add(loginPanel, "South");
        this.errorLabel.setFont(new Font(null, 2, 16));
        this.errorLabel.setForeground(new Color(29, 77, 126));
        panel.add(this.errorLabel, "North");
        return panel;
    }

    private Panel buildOfflinePanel() {
        Panel panel = new Panel() {
            private static final long serialVersionUID = 1L;
            private Insets insets = new Insets(12, 24, 16, 32);

            public Insets getInsets() {
                return this.insets;
            }

            public void update(Graphics g) {
                paint(g);
            }

            public void paint(Graphics g) {
                super.paint(g);
            }
        };
        panel.setBackground(new Color(237, 240, 242));
        BorderLayout layout = new BorderLayout();
        panel.setLayout(layout);
        Panel loginPanel = new Panel(new BorderLayout());
        loginPanel.add(new Panel(), "Center");
        panel.add(new Panel(), "Center");
        loginPanel.add(this.retryButton, "East");
        loginPanel.add(this.offlineButton, "West");
        boolean canPlayOffline = this.launcher.canPlayOffline(this.userName.getText());
        this.offlineButton.setEnabled(canPlayOffline);
        if (!canPlayOffline) {
            panel.add(new Label("Play online once to enable offline"), "Center");
        }
        panel.add(loginPanel, "South");
        this.errorLabel.setFont(new Font(null, 1, 16));
        this.errorLabel.setForeground(new Color(29, 77, 126));
        panel.add(this.errorLabel, "North");
        return panel;
    }

    public void setError(String errorMessage) {
        this.removeAll();
        this.add(buildLoginPanel(), loginPanelConstraints);
        this.errorLabel.setText(errorMessage);
        this.validate();
    }

    public void loginOk() {
        writeUsername();
    }

    public void setNoNetwork() {
        this.removeAll();
        this.add(buildOfflinePanel());
        this.validate();
    }

    public void checkAutologin() {
        if (this.password.getText().length() > 0) {
            this.launcher.login(this.userName.getText(), this.password.getText());
        }
    }

    public void setOutdated() {
        this.outdated = true;
    }
}
