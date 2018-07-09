/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
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
/**
 *
 * @author Frank
 */
public class LoginPane extends AnchorPane {
    protected final PathTransition logonPathTransition;
    protected final PathTransition logonFailedPathTransition;
    protected final Path logonPath;
    protected final Path logonFailedPath;
    protected final ImageView imageView;
    protected final AnchorPane rectPane;
    protected final Rectangle loginBackground;
    protected final TextField usernameField;
    protected final PasswordField passwordField;
    protected final Button logonButton;
    protected final Text text;
    protected final Button testButton;
    protected final CheckBox rememberBox;
    protected final CheckBox updateBox;
    protected Boolean logonSuccess;
    
    protected GuiApplication parentApp;
    
    
    
    public LoginPane(GuiApplication mainApp) {

        imageView = new ImageView();
        loginBackground = new Rectangle();
        usernameField = new TextField();
        passwordField = new PasswordField();
        logonButton = new Button();
        text = new Text();
        logonPathTransition = new PathTransition();
        logonFailedPathTransition = new PathTransition();
        logonPath = new Path();
        logonFailedPath = new Path();
        rectPane = new AnchorPane();
        rememberBox = new CheckBox();
        updateBox = new CheckBox();
        parentApp = mainApp;
        
        logonSuccess = false;

        
        testButton = new Button();
        AnchorPane.setRightAnchor(testButton, 50.0);
        
        setMaxHeight(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);

/*       imageView.setFitHeight(400.0);
        imageView.setFitWidth(600.0);
        imageView.setLayoutX(0);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(false);
        imageView.setImage(new Image(getClass().getResource("/res/img/minecraft-beautiful-landscape-best-decorating-7.jpg").toExternalForm()));
*/

        rectPane.setMaxHeight(USE_PREF_SIZE);
        rectPane.setMaxWidth(USE_PREF_SIZE);
        rectPane.setMinHeight(USE_PREF_SIZE);
        rectPane.setMinWidth(USE_PREF_SIZE);
        rectPane.setPrefHeight(100.0);
        rectPane.setPrefWidth(600.0);
        AnchorPane.setBottomAnchor(rectPane, 0.0);
        AnchorPane.setLeftAnchor(rectPane, 0.0);
        AnchorPane.setRightAnchor(rectPane, 0.0);
        
        loginBackground.setArcHeight(0.0);
        loginBackground.setArcWidth(0.0);
        loginBackground.setHeight(100.0);
        loginBackground.setLayoutY(0.0);
        loginBackground.setStroke(javafx.scene.paint.Color.TRANSPARENT);
        loginBackground.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
        //loginBackground.setWidth(600.0);
        loginBackground.widthProperty().bind(rectPane.widthProperty());
        loginBackground.setFill(javafx.scene.paint.LinearGradient.valueOf("from 0% 0% to 0% 100%, black 100%, rgba(255,0,0,0) 0%"));
        AnchorPane.setBottomAnchor(loginBackground, 0.0);
        
        usernameField.setPromptText("Username");
        usernameField.setStyle("-fx-background-color: black;");
        AnchorPane.setBottomAnchor(usernameField, 30.0);
        AnchorPane.setRightAnchor(usernameField, 250.0);
        
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-background-color: black;");
        AnchorPane.setBottomAnchor(passwordField, 30.0);
        AnchorPane.setRightAnchor(passwordField, 90.0);
        
        logonButton.setMnemonicParsing(false);
        logonButton.setStyle("-fx-background-color: black;");
        logonButton.setText("Login");
        logonButton.setTextFill(javafx.scene.paint.Color.WHITE);
        AnchorPane.setBottomAnchor(logonButton, 30.0);
        AnchorPane.setRightAnchor(logonButton, 25.0);
        
        rememberBox.setText("Remember Password");
        rememberBox.setTextFill(javafx.scene.paint.Color.WHITE);
        AnchorPane.setBottomAnchor(rememberBox, 10.0);
        AnchorPane.setRightAnchor(rememberBox, 105.0);
        
        updateBox.setText("Force Update");
        updateBox.setTextFill(javafx.scene.paint.Color.WHITE);
        AnchorPane.setBottomAnchor(updateBox, 10.0);
        AnchorPane.setRightAnchor(updateBox, 305.0);
        
        
        text.setFill(javafx.scene.paint.Color.WHITE);
        text.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        text.setStrokeWidth(0.0);
        text.setText("<login status goes here>");
        text.setWrappingWidth(267.13671875);
        text.setFont(new Font("Segoe UI", 12.0));
        AnchorPane.setLeftAnchor(text, 100.0);
        AnchorPane.setBottomAnchor(text, 75.0);


//      getChildren().add(imageView);
        rectPane.getChildren().add(loginBackground);
        rectPane.getChildren().add(usernameField);
        rectPane.getChildren().add(passwordField);
        rectPane.getChildren().add(logonButton);
        rectPane.getChildren().add(text);
        rectPane.getChildren().add(rememberBox);
        rectPane.getChildren().add(updateBox);
       // rectPane.getChildren().add(imageView0);
        getChildren().add(rectPane);
        getChildren().add(testButton);
        
        logonPathTransition.setDuration(Duration.millis(500));
        logonPathTransition.setNode(rectPane);
        logonPathTransition.setPath(logonPath);
        
        logonFailedPathTransition.setDuration(Duration.millis(500));
        logonFailedPathTransition.setNode(rectPane);
        logonFailedPathTransition.setPath(logonFailedPath);
        
        logonPathTransition.setCycleCount(1);
        logonPathTransition.setAutoReverse(false);
        logonFailedPathTransition.setCycleCount(1);
        logonFailedPathTransition.setAutoReverse(false);
        
        
        logonButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                text.setText("logging in...");
                logonPath.getElements().clear();
                logonPath.getElements().add(new MoveTo(rectPane.getWidth()*0.5,50f));
                logonPath.getElements().add(new LineTo(rectPane.getWidth()*0.5,150f));
                //logonPath.getElements().add(new LineTo(rectPane.getWidth()*0.5,50f));
                logonPathTransition.play();
                logonPathTransition.setOnFinished(new EventHandler<ActionEvent>(){
                    
                    @Override
                    public void handle(ActionEvent event){
                        logonSuccess = parentApp.login(usernameField.getText(),passwordField.getText());
                        if(logonSuccess){
                            System.out.println("login good!");
                            loginOk();
                        }
                        else{
                            loginBad();
                        }
                    }
                });                               
            }
        });
        
        testButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loginBad();               
            }
        });
        
        readUsername(); // (from file)
        //checkAutoLogin();
        
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
            usernameField.setText(dis.readUTF());
            passwordField.setText(dis.readUTF());
            rememberBox.setSelected(passwordField.getText().length() > 0);
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
            dos.writeUTF(usernameField.getText());
            dos.writeUTF(rememberBox.isSelected() ? passwordField.getText() : "");
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
    
    public void loginBad(){
        text.setText("login failed");
        logonFailedPath.getElements().clear();
        logonFailedPath.getElements().add(new MoveTo(rectPane.getWidth()*0.5,200f));
        logonFailedPath.getElements().add(new LineTo(rectPane.getWidth()*0.5,50f));
        logonFailedPathTransition.play();       
    }
    
    public void loginOk() {
        writeUsername();
        parentApp.continueWithStuff();
    }
/*
    public void setNoNetwork() {
        this.removeAll();
        this.add(buildOfflinePanel());
        this.validate();
    }
*/
    
    public void checkAutologin() {
        if (passwordField.getText().length() > 0) {
            parentApp.login(usernameField.getText(), passwordField.getText());
        }
    }
    
    
}
