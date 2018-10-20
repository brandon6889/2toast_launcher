/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft;

import javafx.animation.PathTransition;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
/**
 *
 * @author Frank
 */
public class LoginPane extends AnchorPane {
    protected final FadeTransition fadeTransition;
    protected final PathTransition logonPathTransition;
    protected final PathTransition logonFailedPathTransition;
    protected final Path logonPath;
    protected final Path logonFailedPath;
    protected final AnchorPane rectPane;
    protected final Rectangle loginBackground;
    protected final TextField usernameField;
    protected final PasswordField passwordField;
    protected final Button logonButton;
    protected final Text logonStatus;
    protected final CheckBox rememberBox;
    protected final CheckBox updateBox;
    protected final ImageView loginSymbol;
    protected String logonSuccess;
    protected GuiApplication parentApp;
    
    
    
    public LoginPane(GuiApplication mainApp) {

        loginBackground = new Rectangle();
        usernameField = new TextField();
        passwordField = new PasswordField();
        logonButton = new Button();
        logonStatus = new Text();
        logonPathTransition = new PathTransition();
        logonFailedPathTransition = new PathTransition();
        fadeTransition = new FadeTransition();
        logonPath = new Path();
        logonFailedPath = new Path();
        rectPane = new AnchorPane();
        rememberBox = new CheckBox();
        updateBox = new CheckBox();
        parentApp = mainApp;
        logonSuccess = "";
        loginSymbol = new ImageView();

        setMaxHeight(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);

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
                
        logonStatus.setFill(javafx.scene.paint.Color.WHITE);
        logonStatus.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        logonStatus.setStrokeWidth(0.0);
        logonStatus.setText("");
        logonStatus.wrappingWidthProperty().bind(rectPane.widthProperty().subtract(400.0));
        logonStatus.setFont(new Font("Segoe UI", 16.0));
        AnchorPane.setTopAnchor(logonStatus, 20.0);
        AnchorPane.setLeftAnchor(logonStatus, 25.0);

        loginSymbol.setFitWidth(150.0);
        loginSymbol.setPreserveRatio(true);
        loginSymbol.setImage(new Image(getClass().getResource("/res/img/loading-transp3.gif").toExternalForm()));
        loginSymbol.setOpacity(0.0);
        AnchorPane.setBottomAnchor(loginSymbol,0.0);
        AnchorPane.setRightAnchor(loginSymbol,0.0);
        getChildren().add(loginSymbol);
        fadeTransition.setNode(loginSymbol);
        fadeTransition.setDuration(Duration.millis(150));        
        
        rectPane.getChildren().add(loginBackground);
        rectPane.getChildren().add(usernameField);
        rectPane.getChildren().add(passwordField);
        rectPane.getChildren().add(logonButton);
        rectPane.getChildren().add(logonStatus);
        rectPane.getChildren().add(rememberBox);
        rectPane.getChildren().add(updateBox);
        // rectPane.getChildren().add(imageView0);
        getChildren().add(rectPane);
        
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
        
        usernameField.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent ke)
            {
                if (ke.getCode().equals(KeyCode.ENTER))
                {
                    passwordField.requestFocus();
                }
            }
        });
        
        passwordField.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent ke)
            {
                if (ke.getCode().equals(KeyCode.ENTER))
                {
                    doLogin();
                }
            }
        });
        
        logonButton.setOnAction(new EventHandler<ActionEvent>() 
        {
            @Override
            public void handle(ActionEvent event) 
            {
                doLogin();
            }                                   
        });
        
        readUsername(); // (from file)
        
        //TODO: Implement auto login, if applicable
        //checkAutoLogin();
        
    }
    
    public void doLogin(){
        GameUpdater.force = updateBox.isSelected();
        fadeTransition.setFromValue(loginSymbol.getOpacity());
        fadeTransition.setToValue(1.0);
        fadeTransition.play();

        logonStatus.setText("Logging in to Kuumba.club ...");
        logonPath.getElements().clear();
        logonPath.getElements().add(new MoveTo(rectPane.getWidth()*0.5,50f));
        logonPath.getElements().add(new LineTo(rectPane.getWidth()*0.5,150f));
        //logonPath.getElements().add(new LineTo(rectPane.getWidth()*0.5,50f));
        logonPathTransition.play();
        logonPathTransition.setOnFinished(new EventHandler<ActionEvent>(){

            @Override
            public void handle(ActionEvent event){
                fadeTransition.setFromValue(loginSymbol.getOpacity());
                fadeTransition.setToValue(0.0);
                fadeTransition.play();

                logonSuccess = parentApp.login(usernameField.getText(),passwordField.getText());
                if(logonSuccess == "success"){
                    loginOk();
                }
                else{
                    loginBad();
                }
            }
        });
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
        logonStatus.setText(logonSuccess);
        logonFailedPath.getElements().clear();
        logonFailedPath.getElements().add(new MoveTo(rectPane.getWidth()*0.5,200f));
        logonFailedPath.getElements().add(new LineTo(rectPane.getWidth()*0.5,50f));
        logonFailedPathTransition.play();       
    }
    
    public void loginOk() {
        writeUsername();
        //parentApp.continueWithStuff();
    }
    
    public void checkAutologin() {
        if (passwordField.getText().length() > 0) {
            parentApp.login(usernameField.getText(), passwordField.getText());
        }
    }
    
    
}
