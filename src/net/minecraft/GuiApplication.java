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
    LoginPane loginPane;
    LauncherPane launcherPane;
    Scene rootScene;
    AnchorPane rootPane;
    
    @Override
    public void start(Stage stage) throws Exception {
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });
        
        /*
        GridPane grid = new GridPane();
        grid.add(btn, 0, 0);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setPadding(new Insets(0, 0, 0, 0));
        */
        
         
        rootPane = new AnchorPane();
        rootScene = new Scene(rootPane, 800, 440);
        
        //Crappy Imageview
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.fitWidthProperty().bind(rootScene.widthProperty());
        imageView.fitHeightProperty().bind(rootScene.heightProperty());
        imageView.setImage(new Image(getClass().getResource("/res/img/minecraft-beautiful-landscape-best-decorating-7.jpg").toExternalForm()));
        rootPane.getChildren().add(imageView);
        //want to preserve ratio
        
        LoginPane loginPane = new LoginPane(this);
        rootPane.getChildren().add(loginPane);
        
        rootScene.getStylesheets().add("/res/css/style.css");
        
        stage.getIcons().add(new Image("/res/img/favicon.png"));
        stage.setTitle("2Toasty Minecraft Launcher");
        stage.setScene(rootScene);
        
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        int startWidth = Math.max((int)(primaryScreenBounds.getWidth()*0.5), 800);
        int startHeight = Math.max((int)(primaryScreenBounds.getHeight()*0.5), 440);
        stage.setWidth(startWidth);
        stage.setHeight(startHeight);
        stage.centerOnScreen(); // or setX and setY
                
        stage.show();
        
        /*String musicURL = getClass().getResource("/res/music.mp3").toExternalForm();
        Media music = new Media(musicURL);
        try {
            mediaPlayer = new MediaPlayer(music);
            mediaPlayer.play();
        }
        catch (RuntimeException ex) {
            // Unable to play due to codec issue, probably on Linux.
            System.out.println("Warning: Unable to play menu music.");
        }*/
    }
    
    //returns true if successful
    protected boolean login(String username, String password){    
        try {
            String parameters = "user=" + URLEncoder.encode(username, "UTF-8") + "&pass=" + URLEncoder.encode(password, "UTF-8") + "&version=" + 14;
            String result = Util.executePost("https://yimbot.net/minecraft/login.php", parameters);
            //Can't connect to 2toast:
            if (result == null) {
                System.out.println("Can't connect to 2toast.net.");
                //this.loginForm.setNoNetwork();
                return false;
            }
            
            //Improper Response or failed logon
            if (!result.contains(":")) {
                System.out.println(result);
                //this.loginForm.setNoNetwork();
                return false;
            }

            String[] values = result.split(":");
            //Need to post in forum introduction
            if (values.length <3 /*padma*/+2) {
                System.out.println(result);
                //this.loginForm.setNoNetwork();
                return false;
            }
            System.out.println("Logged in as " + values[2]);
            
            //this.loginForm.stopMusicThread();
            
            
            LauncherPane launcherPane = new LauncherPane(this,values);      
            rootPane.getChildren().add(launcherPane);
            
            
            /*            
            this.launcher.customParameters.put("username", values[2].trim());
            this.launcher.customParameters.put("latestVersion", values[0].trim());
            this.launcher.customParameters.put("downloadTag", values[1].trim());
            this.launcher.customParameters.put("sessionId", values[3].trim());
            this.launcher.init();
            */
            
            //this.removeAll();
            //this.add(this.launcher, "Center");
            //this.launcher.start();
            return true;
            //this.loginForm = null;
            //this.setTitle("2Toasty Minecraft");
            //this.validate();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println(e.toString());
            return false;
            //this.loginForm.setNoNetwork();
        }        
    }
    
    protected void continueWithStuff(){
       System.out.println("closing screen...");
       System.out.println("closed!");
    }
    
}
