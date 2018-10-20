package net.minecraft;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class GuiApplication extends Application {
    
    private MediaPlayer mediaPlayer = null;
    
    protected LoginPane loginPane;
    protected LauncherPane launcherPane;
    protected Scene rootScene;
    protected AnchorPane rootPane;
    
    @Override
    public void start(Stage stage) throws Exception {
        
        // Define Root AnchorPane and Scene
        rootPane = new AnchorPane();
        rootScene = new Scene(rootPane, 800, 550);

        // Apply background image.
        rootScene.getStylesheets().add(getClass().getResource("/res/css/style.css").toExternalForm());
        
        //Instantiate LoginPane to interact with user:
        LoginPane loginPane = new LoginPane(this);
        rootPane.getChildren().add(loginPane);
        
        stage.setMinWidth(550.0);
        stage.setMinHeight(200.0);
        stage.getIcons().add(new Image(getClass().getResource("/res/img/favicon.png").toExternalForm()));
        stage.setTitle("Kuumba Minecraft Launcher");
        stage.setScene(rootScene);
        stage.show();
        
        //TODO: Make Friendlier mediaplayer
        String musicURL = getClass().getResource("/res/music.mp3").toExternalForm();
        Media music = new Media(musicURL);
        try {
            mediaPlayer = new MediaPlayer(music);
            mediaPlayer.play();
        }
        catch (RuntimeException ex) {
            // Unable to play due to codec issue. Likely OpenJDK on Linux.
            System.out.println("Warning: Unable to play menu music.");
        }
    }
    
    //Returns "success" if successful, otherwise returns error message
    //allows LoginPane to react to login success/failure
    //GuiApplication handles logging in
    protected String login(String username, String password){    
        try {
            String parameters = "user=" + URLEncoder.encode(username, "UTF-8") + "&pass=" + URLEncoder.encode(password, "UTF-8") + "&version=" + 14;
            String result = Util.executePost("https://kuumba.club/minecraft/login.php", parameters);
            
            //ERROR 1: Can't connect to Kuumba:
            if (result == null) {
                return "Can't connect to kuumba.club.";
            }
            
            //ERROR 2: Improper Response or failed logon
            if (!result.contains(":")) {
                return "Login failed or login server error:\n"+result;
            }

            String[] values = result.split(":");
            
            //ERROR 3: Need to post in forum introduction
            if (values.length < 5) {
                return "Logged in, but first post an intro on the forum!";
            }
            
            System.out.println("Logged in as " + values[2]);
            
            //this.loginForm.stopMusicThread();           
            
            LauncherPane launcherPane = new LauncherPane(this,values);
            AnchorPane.setTopAnchor(launcherPane, 25.0);
            AnchorPane.setLeftAnchor(launcherPane, 25.0);
            AnchorPane.setRightAnchor(launcherPane, 25.0);
            AnchorPane.setBottomAnchor(launcherPane, 25.0);
            rootPane.getChildren().add(launcherPane);
            launcherPane.init();
            launcherPane.start();

            return "success";

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println(e.toString());
            return e.toString();
        }        
    }    
}
