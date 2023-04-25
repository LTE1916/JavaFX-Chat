package com.client.login;

import com.client.chatwindow.ChatController;
import com.client.chatwindow.Listener;
import com.client.settings.SettingController;
import com.client.util.ResizeHelper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;


public class LoginController implements Initializable {
    @FXML private ArrayList<ImageView> imageViews = new ArrayList<>();//已保存的头像列表，从数据库接收或本地获取
    @FXML private ImageView DefaultView;
    @FXML private ImageView SarahView;
    @FXML private ImageView DominicView;
    @FXML public  TextField hostNameTextField;
    @FXML private TextField portTextField;
    @FXML private TextField usernameTextField;
    @FXML private TextField passwordTextField;

    @FXML private Label selectedPicture;
    public static ChatController con;
    public static SettingController settingController;
    @FXML private BorderPane borderPane;
    private double xOffset;
    private double yOffset;
    private String hostname = "localhost";
    private int port = 9001;
    private Scene scene;

    private static LoginController instance;
    private Connection connection = DriverManager.getConnection
        ("jdbc:postgresql://localhost:5432/java_chat_user","test","123456789");
    //connect to DATA BASE

    private Statement stmt = connection.createStatement();

    public LoginController() throws SQLException {
        instance = this;
    }

    public static LoginController getInstance() {
        return instance;
    }

    public void settingButtonAction()throws IOException{

        FXMLLoader fXMlLoader = new FXMLLoader(getClass().getResource("/views/SettingView.fxml"));
        Parent window =  fXMlLoader.load();
        settingController = fXMlLoader.getController();
       // settingController.setPort(port);
       // settingController.setHostname(hostname);
        this.scene = new Scene(window);
        Stage stage = (Stage) usernameTextField.getScene().getWindow();

        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

    }
    public void setHostname(String hostname){
        this.hostname = hostname;
    }
    public void setPort(int port){
        this.port = port;
    }
    public void login(KeyEvent keyEvent) throws SQLException, IOException {
        if(keyEvent.getCode()== KeyCode.ENTER)loginButtonAction();
    }
    public void loginButtonAction() throws SQLException, IOException {

        try {
            String username = usernameTextField.getText();
            String password = passwordTextField.getText();
            /*
             check format
             account,password can't be null
             6<password<32
            */

            if(username.equals("")){
                showWaringDialog("Account number can not be null.","Please input account number.");
                return ;
            }
            if(password.length()>0&&(password.length()<6||password.length()>32)){
                showWaringDialog("Password length wrong.","The length of password should between 6 to 32.");
                return;
            }else if(password.length()==0){
                showWaringDialog("Password can not be null.","Please input password.");
                return;
            }
            /*
            TODO:
             对密码要加密，避免明文传输和存储
             */

            ResultSet resultSet = stmt.executeQuery("select account from user_table "
                + "where account ='"+Integer.parseInt(username)+"' and password = '"+password+"'");
            /*
            resultSet contains the set of the (account and password) if exists
            对数据库返回结果进行判断
             */
            if(resultSet.next()){
               // String picture = selectedPicture.getText();

                FXMLLoader fXMlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
                Parent window =  fXMlLoader.load();
                con = fXMlLoader.getController();

                Listener listener = new Listener(hostname, port, username, "default", con);
                Thread x = new Thread(listener);
                x.start();
                this.scene = new Scene(window);
            }
            else {
                ResultSet userSearch = stmt.executeQuery("select account from user_table "
                    + "where account ='"+Integer.parseInt(username)+"'");
                //whether account exists in DataBase

                if(userSearch.next()){
                showErrorDialog("Password wrong.","Please check password.");
                }else {
                    showErrorDialog("Account not exists.","this account do not exists,please check the account number");
                }
            }
        }catch (NumberFormatException e){
            showErrorDialog("Account format wrong!","Account only contains number.\nPlease check the account format.");
           // throw new RuntimeException(e);
        }

    }

    public void showScene() throws IOException {
        Platform.runLater(() -> {
            Stage stage = (Stage) usernameTextField.getScene().getWindow();
            stage.setResizable(true);
            stage.setWidth(1040);
            stage.setHeight(620);

            stage.setOnCloseRequest((WindowEvent e) -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(this.scene);
            stage.setMinWidth(800);
            stage.setMinHeight(300);
            ResizeHelper.addResizeListener(stage);
            stage.centerOnScreen();
            con.setUsernameLabel(usernameTextField.getText());
           // con.setImageLabel(selectedPicture.getText());
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        /* Drag and Drop */
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


        int numberOfSquares = 30;
        while (numberOfSquares > 0){
            generateAnimation();
            numberOfSquares--;
        }
        try {
            InputStream inputStream = getClass().getClassLoader().getResource("").openStream();
            String filepath = "client/src/main/resources/settings.ini";
            FileInputStream fis = new FileInputStream(filepath);
            Properties pps = new Properties();
            pps.load(fis);
            port = Integer.parseInt(pps.getProperty("port"));
            hostname = pps.getProperty("hostname");
//            port = 9001;
//            hostname = "192.168.137.1";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }





        MainLauncher.controllers.put(this.getClass().getSimpleName(),this);
    }



    /* This method is used to generate the animation on the login window, It will generate random ints to determine
     * the size, speed, starting points and direction of each square.
     */
    public void generateAnimation(){
        Random rand = new Random();
        int sizeOfSquare = rand.nextInt(50) + 1;
        int speedOfSquare = rand.nextInt(10) + 5;
        int startXPoint = rand.nextInt(420);
        int startYPoint = rand.nextInt(350);
        int direction = rand.nextInt(5) + 1;

        KeyValue moveXAxis = null;
        KeyValue moveYAxis = null;
        Rectangle r1 = null;

        switch (direction){
            case 1 :
                // MOVE LEFT TO RIGHT
                r1 = new Rectangle(0,startYPoint,sizeOfSquare,sizeOfSquare);
                moveXAxis = new KeyValue(r1.xProperty(), 350 -  sizeOfSquare);
                break;
            case 2 :
                // MOVE TOP TO BOTTOM
                r1 = new Rectangle(startXPoint,0,sizeOfSquare,sizeOfSquare);
                moveYAxis = new KeyValue(r1.yProperty(), 420 - sizeOfSquare);
                break;
            case 3 :
                // MOVE LEFT TO RIGHT, TOP TO BOTTOM
                r1 = new Rectangle(startXPoint,0,sizeOfSquare,sizeOfSquare);
                moveXAxis = new KeyValue(r1.xProperty(), 350 -  sizeOfSquare);
                moveYAxis = new KeyValue(r1.yProperty(), 420 - sizeOfSquare);
                break;
            case 4 :
                // MOVE BOTTOM TO TOP
                r1 = new Rectangle(startXPoint,420-sizeOfSquare ,sizeOfSquare,sizeOfSquare);
                moveYAxis = new KeyValue(r1.xProperty(), 0);
                break;
            case 5 :
                // MOVE RIGHT TO LEFT
                r1 = new Rectangle(420-sizeOfSquare,startYPoint,sizeOfSquare,sizeOfSquare);
                moveXAxis = new KeyValue(r1.xProperty(), 0);
                break;
            case 6 :
                //MOVE RIGHT TO LEFT, BOTTOM TO TOP
                r1 = new Rectangle(startXPoint,0,sizeOfSquare,sizeOfSquare);
                moveXAxis = new KeyValue(r1.xProperty(), 350 -  sizeOfSquare);
                moveYAxis = new KeyValue(r1.yProperty(), 420 - sizeOfSquare);
                break;

            default:
                System.out.println("default");
        }

        r1.setFill(Color.web("#F89406"));
        r1.setOpacity(0.1);

        KeyFrame keyFrame = new KeyFrame(Duration.millis(speedOfSquare * 1000), moveXAxis, moveYAxis);
        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
        borderPane.getChildren().add(borderPane.getChildren().size()-1,r1);
    }

    /* Terminates Application */
    public void closeSystem(){

        Platform.exit();
        System.exit(0);
    }

    public void minimizeWindow(){
        MainLauncher.getPrimaryStage().setIconified(true);
    }

    /* This displays an alert message to the user */
    public void showErrorDialog(String message,String content) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning!");
            alert.setHeaderText(message);
            alert.setContentText(content);
            alert.showAndWait();
        });

    }
    public void showWaringDialog(String message,String content) {
        Platform.runLater(()-> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Warning!");
            alert.setHeaderText(message);
            alert.setContentText(content);
            alert.showAndWait();

        });

    }

    public void SignUpButtonAction() throws SQLException {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sign up");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        Label usernameLabel = new Label();
        Label passwordLabel = new Label();
        usernameLabel.setText("username");
        passwordLabel.setText("passwordLabel");
        TextField textField = new TextField();
        PasswordField passwordField = new PasswordField();
        grid.add(usernameLabel,0,0);
        grid.add(textField,1,0);
        grid.add(passwordLabel,0,1);
        grid.add(passwordField,1,1);
        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> buttonType = dialog.showAndWait();
        if(buttonType.get().equals(ButtonType.OK)){
            String name = textField.getText();
            String password = passwordField.getText();
            if(name.equals("")){
                showWaringDialog("Account number can not be null.","Please input account number.");
                SignUpButtonAction();
                return ;
            }
            if(password.length()>0&&(password.length()<6||password.length()>32)){
                showWaringDialog("Password length wrong.","The length of password should between 6 to 32.");
                SignUpButtonAction();
                return;
            }else if(password.length()==0){
                showWaringDialog("Password can not be null.","Please input password.");
                SignUpButtonAction();
                return;
            }
            //connect to DATA BASE

            ResultSet r = stmt.executeQuery("select account from user_table where account = '"+name+"'");
            if(r.next()){
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("ID existed");
                alert.setHeaderText("name occupied");
                alert.setContentText("This name already be occupied.\nPlease try another name and tey again!");
                alert.showAndWait();
            }else {

                stmt.execute("insert into user_table (account, password, picture) VALUES ('"+name+"','"+password+"','"+name+"')");
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Successful");
                alert.setTitle("Welcome");
                alert.setContentText("Sign up successfully,Welcome to Sustech Chat.\nplease login in");
                alert.showAndWait();
            }
        }

    }

}
