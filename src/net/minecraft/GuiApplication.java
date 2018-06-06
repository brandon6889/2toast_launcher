package net.minecraft;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class GuiApplication extends Application {

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
        
        StackPane root = new StackPane();
        root.getChildren().add(btn);
        
        Scene scene = new Scene(root, 800, 440);
        
        stage.setTitle("2Toasty Minecraft Launcher");
        stage.setScene(scene);
        
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        int startWidth = Math.max((int)(primaryScreenBounds.getWidth()*0.5), 800);
        int startHeight = Math.max((int)(primaryScreenBounds.getHeight()*0.5), 440);
        stage.setWidth(startWidth);
        stage.setHeight(startHeight);
        stage.centerOnScreen(); // or setX and setY
        
        stage.show();
    }
    
}
