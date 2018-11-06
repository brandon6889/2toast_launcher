/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    protected FadeTransition fadeTransitionPane, fadeTransitionSpinner;
    protected ImageView launchingIcon;
    protected RotateTransition rotate360Transition;
    
    public Map<String, String> customParameters;
    private GameUpdater gameUpdater;
    private final List<String> stdOpts = new ArrayList();

    
    
    public LauncherPane(GuiApplication mainApp, String[] params) {
        setOpacity(0.0);
        
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
        
        launchingIcon = new ImageView();
        launchingIcon.setFitWidth(80.0);
        launchingIcon.setPreserveRatio(true);
        launchingIcon.setImage(new Image(getClass().getResource("/res/img/loading-transp4.gif").toExternalForm()));
        launchingIcon.setOpacity(0.0);
        
        rotate360Transition = new RotateTransition(Duration.millis(6000), launchingIcon);
        rotate360Transition.setByAngle(360.0);
        rotate360Transition.setCycleCount(Timeline.INDEFINITE);
        rotate360Transition.setInterpolator(Interpolator.LINEAR);
        rotate360Transition.play();

        //show border
        this.setStyle("-fx-border-color: black");
        
        progBox.getChildren().add(downloadAction);
        progBox.getChildren().add(downloadProgress);
        progBox.getChildren().add(new Rectangle(20,20,javafx.scene.paint.Color.grayRgb(255,0.0)));
        progBox.getChildren().add(launchingIcon);
        
        this.setCenter(progBox);
        progBox.setAlignment(Pos.CENTER);
        
        fadeTransitionPane = new FadeTransition();
        fadeTransitionPane.setFromValue(0.0);
        fadeTransitionPane.setToValue(1.0);
        fadeTransitionPane.setNode(this);
        fadeTransitionPane.setDuration(Duration.millis(350));
        fadeTransitionPane.play();
        
        stdOpts.add("-XX:+UseConcMarkSweepGC");
        stdOpts.add("-XX:-UseAdaptiveSizePolicy");
        stdOpts.add("-XX:-OmitStackTraceInFastThrow");
        stdOpts.add("-Xms128m");
        stdOpts.add("-Xmx3968m");
        stdOpts.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        stdOpts.add("-Dfml.ignorePatchDiscrepancies=true");
        stdOpts.add("-Duser.home="+System.getProperty("user.home", "."));
    }

    protected void init() {
        this.gameUpdater = GameUpdater.getUpdater(latestVersion);
    }

    protected void start() {
        Thread t = new Thread() {
            @Override
            public void run() {
                gameUpdater.run();
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
                        launchCommand.addAll(gameUpdater.getLaunchArgs(username, sessionId));
                        launchCommand.add("--height 480 --width 854");
                        
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
                    } catch (Exception ex) {Logger.getLogger(LauncherPane.class.getName()).log(Level.SEVERE, null, ex);}
                }
            }
        };
        t.setDaemon(true);
        t.start();
        t = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if(gameUpdater.percentage < 1000){
                        downloadAction.setText(gameUpdater.getDescriptionForState());
                        downloadProgress.setProgress(gameUpdater.percentage*0.001);
                    }
                    else{
                        downloadAction.setText("Launching game...");
                        downloadProgress.setProgress(1.0);
                        fadeTransitionSpinner = new FadeTransition();
                        fadeTransitionSpinner.setFromValue(0.0);
                        fadeTransitionSpinner.setToValue(1.0);
                        fadeTransitionSpinner.setNode(launchingIcon);
                        fadeTransitionSpinner.setDuration(Duration.millis(500));
                        fadeTransitionSpinner.play();
                        return;
                    }
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
    }
    
    @Override
    public void accept(String line) {
        if (this.isVisible() && line.contains("LWJGL Version: ")) {
            System.out.println("Game started, closing launcher.");
            System.exit(0);
        }
    }
}
