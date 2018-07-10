package net.minecraft;

import java.text.MessageFormat;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;
import java.io.IOException;
import java.net.URLEncoder;


public class GuiApplication extends Application {
    
    private MediaPlayer mediaPlayer = null;
    
    protected LoginPane loginPane;
    protected LauncherPane launcherPane;
    protected Scene rootScene;
    protected AnchorPane rootPane;
    
    @Override
    public void start(Stage stage) throws Exception {
        
        //Define Root AnchorPane and Scene
        rootPane = new AnchorPane();
        rootScene = new Scene(rootPane, 800, 550);
                
        //Crappy Background Imageview
        //TODO: Make this preserve ratio, but also fully fill window
        //TODO 2: stop using a shitty image for the background
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.fitWidthProperty().bind(rootScene.widthProperty());
        imageView.fitHeightProperty().bind(rootScene.heightProperty());
        imageView.setImage(new Image(getClass().getResource("/res/img/minecraft-beautiful-landscape-best-decorating-7.jpg").toExternalForm()));
        rootPane.getChildren().add(imageView);
        
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
            // Unable to play due to codec issue, probably on Linux.
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
                return "Logon Failed or Improper response received:\n"+result;
            }

            String[] values = result.split(":");
            
            //ERROR 3: Need to post in forum introduction
            if (values.length <3 /*padma*/+2) {
                return "Authenticated, but you need to post on the forum!";
            }
            
            System.out.println("Logged in as " + values[2]);
            
            //this.loginForm.stopMusicThread();           
            
            LauncherPane launcherPane = new LauncherPane(this,values);      
            rootPane.getChildren().add(launcherPane);

            return "success";

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println(e.toString());
            return e.toString();
        }        
    }    
}
