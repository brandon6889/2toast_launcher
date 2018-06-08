package net.minecraft;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class GuiApplication extends Application {
    
    private MediaPlayer mediaPlayer = null;

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
        
        GridPane grid = new GridPane();
        grid.add(btn, 0, 0);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setPadding(new Insets(0, 0, 0, 0));
        
        Scene scene = new Scene(grid, 800, 440);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        
        stage.setTitle("2Toasty Minecraft Launcher");
        stage.setScene(scene);
        
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        int startWidth = Math.max((int)(primaryScreenBounds.getWidth()*0.5), 800);
        int startHeight = Math.max((int)(primaryScreenBounds.getHeight()*0.5), 440);
        stage.setWidth(startWidth);
        stage.setHeight(startHeight);
        stage.centerOnScreen(); // or setX and setY
        
        stage.show();
        
        String musicURL = getClass().getResource("music.mp3").toExternalForm();
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
    
}
