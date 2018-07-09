/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.animation.PathTransition;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Frank
 */
public class LauncherPane extends BorderPane implements Consumer<String>{
    protected GuiApplication parentApp;
    protected String username, latestVersion, downloadTag, sessionId;
    protected VBox progBox;
    protected Text downloadAction;
    protected ProgressBar downloadProgress;
    
    
    private static final long serialVersionUID = 1L;
    public Map<String, String> customParameters;
    private GameUpdater gameUpdater;
    private boolean gameUpdaterStarted;
    private boolean active;
    private int context;
    private final List<String> stdOpts = new ArrayList();

    
    
    public LauncherPane(GuiApplication mainApp, String[] params){
        parentApp = mainApp;
        username = params[2].trim();
        latestVersion = params[0].trim();
        downloadTag = params[1].trim();
        sessionId = params[3].trim();
        
        progBox = new VBox();
        
        downloadAction = new Text();
        downloadAction.setText("Not downloading.");
        downloadAction.setStroke(javafx.scene.paint.Color.WHITE);
        
        downloadProgress = new ProgressBar();
        downloadProgress.setProgress(0.0);
        
        
        //AnchorPane.setBottomAnchor(this, 50.0);
        AnchorPane.setTopAnchor(this, 50.0);
        AnchorPane.setLeftAnchor(this, 50.0);
        AnchorPane.setRightAnchor(this, 50.0);
        AnchorPane.setBottomAnchor(this, 50.0);

        this.setCenter(progBox);
        
        progBox.getChildren().add(downloadAction);
        progBox.getChildren().add(downloadProgress);

        this.gameUpdaterStarted = false;
        this.active = false;
        this.context = 0;
        
        stdOpts.add("-XX:+UseConcMarkSweepGC");
        stdOpts.add("-XX:-UseAdaptiveSizePolicy");
        stdOpts.add("-XX:-OmitStackTraceInFastThrow");
        stdOpts.add("-Xms128m");
        stdOpts.add("-Xmx3968m");
        stdOpts.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        stdOpts.add("-Dfml.ignorePatchDiscrepancies=true");
        stdOpts.add("-Duser.home="+System.getProperty("user.home", "."));
        init();
        start();
    }

    public void init() {
        this.gameUpdater = GameUpdater.getUpdater(latestVersion);
    }

    public void start() {
        Thread t = new Thread() {
            @Override
            public void run() {
                gameUpdater.run();
                System.out.println("done downloading!");
                if (!gameUpdater.fatalError) { // Needs more testing... prints Executing game?
                    try {
                        List<String> launchCommand = new ArrayList();
                        launchCommand.add(Util.getJavaBin());
                        launchCommand.addAll(stdOpts);
                        launchCommand.add("-Djava.library.path=" + Util.getWorkingDirectory().toString() +
                                "/bin/" + gameUpdater.getMCVersion() + "-natives");
                        launchCommand.add("-cp");
                        launchCommand.add(gameUpdater.getClassPath());
                        launchCommand.add(gameUpdater.getMainClass());
                        String token = sessionId;
                        for (String s : gameUpdater.getLaunchArgs(username, token))
                            launchCommand.add(s);
                        launchCommand.add("--height 480 --width 854");
                        /*System.out.print("COMMAND: ");
                        for (String s : launchCommand) {
                        System.out.println(s);
                        }
                        System.out.println();*/
                        System.out.println("Executing game..");
                        ProcessBuilder pb = new ProcessBuilder(launchCommand);
                        pb.directory(Util.getWorkingDirectory());
                        pb.environment().put("APPDATA", Util.getWorkingDirectory().getAbsoluteFile().getParent());
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        try {
                            Process p = pb.start();
                            if (p == null)
                                throw new Exception("Failed to start game.");
                            else {
                                new Thread(new StreamHandler(p.getInputStream(),System.out,LauncherPane.this)).start();
                            }
                        } catch (Exception e) {e.printStackTrace();}
                    } catch (Exception ex) {Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);}
                }
            }
        };
        t.setDaemon(true);
        t.start();
        t = new Thread() {
            @Override
            public void run() {
                while (true) {
                    downloadProgress.setProgress(gameUpdater.percentage*0.001);
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
    
    @Override
    public void accept(String line) {
        if (this.isVisible() && line.contains("LWJGL Version: ")) {
            System.out.println("Game started, closing launcher.");
            System.exit(0);
            //this.getParent().setVisible(false);
        }
    }
}
