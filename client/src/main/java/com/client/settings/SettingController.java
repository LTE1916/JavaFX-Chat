package com.client.settings;

import com.client.chatwindow.ChatController;
import com.client.login.LoginController;
import com.client.login.MainLauncher;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SettingController implements Initializable {
  @FXML private BorderPane borderPane;
  private double xOffset;
  private double yOffset;

  @FXML
  public TextField hostnameTextfield;
  private String hostname;
  @FXML private TextField portTextfield;
  private String port;
  private Scene scene;
  private static SettingController settingController;
  public SettingController() {
    settingController = this;
  }
  public static LoginController loginController;

  @FXML
  private void confirmButtonAction() throws IOException {
    try {
      String filepath = "client/src/main/resources/settings.ini";
      FileInputStream fis = new FileInputStream(filepath);
      Properties pps = new Properties();
      pps.load(fis);
      pps.setProperty("port",portTextfield.getText());
      pps.setProperty("hostname",hostnameTextfield.getText());
      OutputStream opt = new FileOutputStream(filepath);
      pps.store(opt,null);
      opt.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
    Parent window =  fxmlLoader.load();
    loginController = fxmlLoader.getController();
    loginController.setPort(Integer.parseInt(portTextfield.getText()));
    loginController.setHostname(hostnameTextfield.getText());
    this.scene = new Scene(window);
    Stage stage =(Stage) hostnameTextfield.getScene().getWindow();
    stage.setScene(scene);
    stage.show();
  }

//  public void setHostname(String hostname){
//    this.hostname = hostname;
//  }
//  public void setPort(int port){
//    this.port = String.valueOf(port);
//  }

  public void minimizeWindow(){
    MainLauncher.getPrimaryStage().setIconified(true);
  }

  public void cancelButtonAction() throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
    Parent window =  fxmlLoader.load();
    loginController = fxmlLoader.getController();
    this.scene = new Scene(window);
    Stage stage =(Stage) hostnameTextfield.getScene().getWindow();
    stage.setScene(scene);
    stage.show();
  }



  public void closeSystem() throws IOException {
    cancelButtonAction();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    borderPane.setOnMousePressed(event -> {
      xOffset = MainLauncher.getPrimaryStage().getX() - event.getScreenX();
      yOffset = MainLauncher.getPrimaryStage().getY() - event.getScreenY();
      borderPane.setCursor(Cursor.CLOSED_HAND);
    });

    borderPane.setOnMouseDragged(event -> {
      MainLauncher.getPrimaryStage().setX(event.getScreenX() + xOffset);
      MainLauncher.getPrimaryStage().setY(event.getScreenY() + yOffset);

    });

    borderPane.setOnMouseReleased(event -> {
      borderPane.setCursor(Cursor.DEFAULT);
    });
    try {
      String filepath = "client/src/main/resources/settings.ini";
      FileInputStream fis = new FileInputStream(filepath);
      Properties pps = new Properties();
      pps.load(fis);
      port = pps.getProperty("port");
      hostname = pps.getProperty("hostname");

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    portTextfield.setText(port);
    hostnameTextfield.setText(hostname);
    MainLauncher.controllers.put(this.getClass().getSimpleName(),this);
  }
}
