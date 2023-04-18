package com.client.chatwindow;

import com.client.emoji.Emoji;
import com.client.login.MainLauncher;
import com.client.util.VoicePlayback;
import com.client.util.VoiceRecorder;
import com.client.util.VoiceUtil;
import com.messages.Conservation;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import com.messages.User;
import com.messages.bubble.BubbleSpec;
import com.messages.bubble.BubbledLabel;
import com.traynotifications.animations.AnimationType;
import com.traynotifications.notification.TrayNotification;
import java.awt.event.KeyListener;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class ChatController implements Initializable {

    @FXML private TextArea messageBox;
    @FXML private Label usernameLabel;
    @FXML private Label onlineCountLabel;
    @FXML private ListView userList;
    @FXML private ImageView userImageView;
    @FXML private Button recordBtn;
    @FXML private Button sendBtn;
    @FXML ListView chatPage;
    @FXML ListView messageList;
    @FXML ListView contactsList;
    @FXML ListView statusList;
    @FXML BorderPane borderPane;
    @FXML TabPane leftPane;
    @FXML Tab onlineUser;
    @FXML ComboBox statusComboBox;
    @FXML ImageView microphoneImageView;


    Connection connection = DriverManager.getConnection
        ("jdbc:postgresql://localhost:5432/java_chat_user","test","123456789");
    //connect to DATA BASE
    Statement stmt = connection.createStatement();
    private boolean threadFinished = true;

    private int currentConservationID = 0;
    private int currentConservationType = -1;

    private int currentUserID ;

    private int targetID=0;
    private ArrayList<Message> currentMessages = new ArrayList<>();
    private Map<Integer, ArrayList<Message>> messageListMap = new HashMap<>();//map conservation ID to messageHistory
    private Map<Integer,String> conversationTypeMap = new HashMap<>();

    private Map<Integer,Conservation> conservationMap = new HashMap<>();
    private ObservableList<Conservation> conservationObservableList = FXCollections.observableList(new ArrayList<>());
    Image microphoneActiveImage = new Image(getClass().getClassLoader().getResource("images/microphone-active.png").toString());
    Image microphoneInactiveImage = new Image(getClass().getClassLoader().getResource("images/microphone.png").toString());

    private double xOffset;
    private double yOffset;
    Logger logger = LoggerFactory.getLogger(ChatController.class);
    private java.awt.event.KeyEvent keyEvent = null;
    private boolean controlPressed;

    KeyListener keyListener = new KeyListener() {
        @Override
        public void keyTyped(java.awt.event.KeyEvent e) {

        }

        @Override
        public void keyPressed(java.awt.event.KeyEvent e) {
            keyEvent = e;
            controlPressed = true;
        }

        /*
        release pressed key
         */
        @Override
        public void keyReleased(java.awt.event.KeyEvent e) {
            if(e.getKeyCode()==keyEvent.getKeyCode()){
                controlPressed = false;
            }
        }
    };

    public ChatController() throws SQLException {
    }

    public void addToMessageBox(String str){
        messageBox.appendText(str);
    }

    public void sendButtonAction() throws IOException, SQLException {
        String msg = messageBox.getText();
         currentUserID = Integer.parseInt(usernameLabel.getText());
        if (!messageBox.getText().isEmpty()) {
            if(currentConservationType==1) {
                //私聊时按下发送按钮
                 stmt.execute("insert into message_history (text,date,time,type,fromid,target , belong) "
                    + "values ('"+ messageBox.getText()+"',current_date,current_time,1,'"+currentUserID+"','"+targetID+"','"+currentConservationID+"')");
                ResultSet resultSet = stmt.executeQuery("select id,date,time from message_history"
                    + " where (fromid = '"+currentUserID+"' and target = "+targetID+" and type =1)"
                    + "or (fromid = "+targetID+" and target = "+currentUserID+" and type =1 ) ") ;
                //将发送的信息先传到数据库，再从数据库获取该条信息的id
                if (resultSet.next()) {
                    int currentMessageID = resultSet.getInt("id");
                    Date sendDate = Date.from(Instant.now());
                    Listener.send(msg, String.valueOf(targetID),currentMessageID, 1, currentConservationID ,sendDate);
                    messageBox.clear();
                } else {
                    System.out.println("database ERROR!");
                }
            }else {
                //type =2 对应群聊时发送


            }
            //messageBox.setScrollTop(0);
        }
    }

    public void recordVoiceMessage() throws IOException {
        if (VoiceUtil.isRecording()) {
            Platform.runLater(() -> {
                microphoneImageView.setImage(microphoneInactiveImage);
                    }
            );
            VoiceUtil.setRecording(false);
        } else {
            Platform.runLater(() -> {
                microphoneImageView.setImage(microphoneActiveImage);

                    }
            );
            VoiceRecorder.captureAudio();
        }
    }

    public void emojiButtonAction(){
        try {
            Stage primaryStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EmojiList.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 392, 300);
            primaryStage.setTitle("Emojis List");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addToChat(Message msg,boolean isNew) throws InterruptedException {

        Task<HBox> othersMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {


                threadFinished = false;
                Image image = new Image(getClass().getClassLoader().getResource("images/" + msg.getPicture() + ".png").toString());
                ImageView profileImage = new ImageView(image);
                profileImage.setFitHeight(32);
                profileImage.setFitWidth(32);
                BubbledLabel bl = new BubbledLabel();
                if (msg.getType() == MessageType.VOICE){
                    ImageView imageview = new ImageView(new Image(getClass().getClassLoader().getResource("images/sound.png").toString()));
                    bl.setGraphic(imageview);
                    bl.setText("Sent a voice message!");
                    VoicePlayback.playAudio(msg.getVoiceMsg());
                }else {
                    bl.setText(msg.getName() + ": " + msg.getMsg());
                }
                bl.setBackground(new Background(new BackgroundFill(Color.WHITE,null, null)));
                HBox x = new HBox();
                bl.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);
                x.getChildren().addAll(profileImage, bl);
                logger.debug("ONLINE USERS: " + Integer.toString(msg.getUserlist().size()));
                setOnlineLabel(Integer.toString(msg.getOnlineCount()));

                threadFinished = true;
                System.out.println("other m finished");
                return x;
            }
        };

        othersMessages.setOnSucceeded(event -> {
            chatPage.getItems().add(othersMessages.getValue());
        });

        Task<HBox> yourMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {

                Image image = userImageView.getImage();
                ImageView profileImage = new ImageView(image);
                profileImage.setFitHeight(32);
                profileImage.setFitWidth(32);

                BubbledLabel bl = new BubbledLabel();
                if (msg.getType() == MessageType.VOICE){
                    bl.setGraphic(new ImageView(new Image(getClass().getClassLoader().getResource("images/sound.png").toString())));
                    bl.setText("Sent a voice message!");
                    VoicePlayback.playAudio(msg.getVoiceMsg());
                }else {
//                    /*
//                    emoji 还未完善
//                     */
//                    Queue<Object> obs = EmojiOne.getInstance().toEmojiAndText(msg.getMsg());
//                    while(!obs.isEmpty()) {
//                        Object ob = obs.poll();
//                        if(ob instanceof String) {
//                            Text textNode = new Text(msg.getMsg());
//                            textNode.setFont(Font.font(16));
//                            bl.setText(msg.getMsg());
//                         //   flowOutput.getChildren().add(textNode);
//                        }
//                        else if(ob instanceof Emoji) {
//                            Emoji emoji = (Emoji) ob;
//                            bl.setGraphic(createEmojiNode(emoji));
//                            //flowOutput.getChildren().add(createEmojiNode(emoji));
//
//                        }
//                    }
                    bl.setText(msg.getMsg());
                }
                bl.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN,
                        null, null)));
                HBox x = new HBox();
                x.setMaxWidth(chatPage.getWidth() - 20);
                x.setAlignment(Pos.TOP_RIGHT);
                bl.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);
                x.getChildren().addAll(bl, profileImage);

                setOnlineLabel(Integer.toString(msg.getOnlineCount()));
                threadFinished = true;
                System.out.println("your m finished");
                return x;

            }
        };
        yourMessages.setOnSucceeded(event -> chatPage.getItems().add(yourMessages.getValue()));

            if (msg.getName().equals(usernameLabel.getText())) {
                if (isNew) currentMessages.add(msg);
                Thread t2 = new Thread(yourMessages);
                t2.setDaemon(true);
                while (!threadFinished){
                    wait(1);
                }
                threadFinished = false;
                t2.start();
                System.out.println("t2 start");

            } else {
                if (isNew) currentMessages.add(msg);
                Thread t = new Thread(othersMessages);
                t.setDaemon(true);
                while (!threadFinished){
                    wait(1);
                }
                threadFinished = false;
                t.start();
                System.out.println("t1 start");
            }

    }
    private Node createEmojiNode(Emoji emoji) {
        StackPane stackPane = new StackPane();
        stackPane.setPadding(new Insets(3));
        ImageView imageView = new ImageView();
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);
        imageView.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(emoji.getHex())));
        stackPane.getChildren().add(imageView);

        Tooltip tooltip = new Tooltip(emoji.getShortname());
        Tooltip.install(stackPane, tooltip);
        stackPane.setCursor(Cursor.HAND);
        stackPane.setOnMouseEntered(e-> {
            stackPane.setStyle("-fx-background-color: #a6a6a6; -fx-background-radius: 3;");
        });
        stackPane.setOnMouseExited(e-> {
            stackPane.setStyle("");
        });
        return stackPane;
    }

    private String getEmojiImagePath(String hexStr) {
        return getClass().getResource("png_40/" + hexStr + ".png").toExternalForm();
    }


    public void setUsernameLabel(String username) {
        this.usernameLabel.setText(username);
    }

    public void setImageLabel() throws IOException {
        this.userImageView.setImage(new Image(getClass().getClassLoader().getResource("images/Dominic.png").toString()));
    }

    public void setOnlineLabel(String userAccount) {
        Platform.runLater(() -> onlineCountLabel.setText(userAccount));
    }

    public void setUserList(Message msg) {
        logger.info("setUserList() method Enter");
        Platform.runLater(() -> {
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            userList.setItems(users);
            userList.setCellFactory(new CellRenderer());
            setOnlineLabel(String.valueOf(msg.getUserlist().size()));
        });
        logger.info("setUserList() method Exit");
    }

    /* Displays Notification when a user joins */
    public void newUserNotification(Message msg) {
        Platform.runLater(() -> {
            Image profileImg = new Image(getClass().getClassLoader().getResource("images/" + msg.getPicture().toLowerCase() +".png").toString(),50,50,false,false);
            TrayNotification tray = new TrayNotification();
            tray.setTitle("A new user has joined!");
            tray.setMessage(msg.getName() + " has joined the JavaFX Chatroom!");
            tray.setRectangleFill(Paint.valueOf("#2C3E50"));
            tray.setAnimationType(AnimationType.POPUP);
            tray.setImage(profileImg);
            tray.showAndDismiss(Duration.seconds(5));
            try {
                Media hit = new Media(getClass().getClassLoader().getResource("sounds/notification.wav").toString());
                MediaPlayer mediaPlayer = new MediaPlayer(hit);
                mediaPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }
    public void shutdownNotification() {
        Platform.runLater(() -> {
            Image profileImg = new Image(getClass().getClassLoader().getResource("images/sustech.png").toString(),50,50,false,false);
            TrayNotification tray = new TrayNotification();
            tray.setTitle("Disconnected to the sever!");
            tray.setMessage("The server shutdown, please try later");
            tray.setRectangleFill(Paint.valueOf("#2C3E50"));
            tray.setAnimationType(AnimationType.POPUP);
            tray.setImage(profileImg);
            tray.showAndDismiss(Duration.seconds(7));
            try {
                Media hit = new Media(getClass().getClassLoader().getResource("sounds/notification.wav").toString());
                MediaPlayer mediaPlayer = new MediaPlayer(hit);
                mediaPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }
    public void controlCheck(KeyEvent event){
        if (event.getCode()==KeyCode.CONTROL) controlPressed=false;
            }
    public void sendMethod(KeyEvent event) throws IOException {
//        if (event.getCode()==KeyCode.CONTROL) controlPressed=true;
//
//       if(!controlPressed&&event.getCode() == KeyCode.ENTER) {
//           sendButtonAction();
//       }
//     messageBox.addEventFilter(KeyEvent.KEY_PRESSED,key->{
//         if(!key.getCode().equals(KeyCode.CONTROL)&&event.getCode() == KeyCode.ENTER){
//                 try {
//                     sendButtonAction();
//                 } catch (IOException e) {
//                     throw new RuntimeException(e);
//                 }
//             }
//     });

    }

    @FXML
    public void closeApplication() throws IOException {
        //Listener.disconnect();
        Platform.exit();
        System.exit(0);
    }

    /* Method to display server messages */
    public synchronized void addAsServer(Message msg) {
        Task<HBox> task = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setText(msg.getMsg());
                bl6.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE,
                        null, null)));
                HBox x = new HBox();
                bl6.setBubbleSpec(BubbleSpec.FACE_BOTTOM);
                x.setAlignment(Pos.CENTER);
                x.getChildren().addAll(bl6);
                return x;
            }
        };
        task.setOnSucceeded(event -> {
            chatPage.getItems().add(task.getValue());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setImageLabel();
        } catch (IOException e) {
            e.printStackTrace();

        }
        leftPane.getSelectionModel().select(onlineUser);

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


        statusComboBox.getSelectionModel().selectedItemProperty().addListener(
            (ChangeListener<String>) (observable, oldValue, newValue) -> {
                try {
                    Listener.sendStatusUpdate(Status.valueOf(newValue.toUpperCase()));
                } catch (SocketException e){
                    e.printStackTrace();
                    showInternetErrorDialog("Connect Failed!","Internet Error!",
                        "There is something wrong with the Internet connection.\n"
                        + " Please check the Internet and try again later.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        /* Added to prevent  enter from adding a new line to inputMessageBox */
        messageBox.addEventFilter(KeyEvent.KEY_PRESSED, key -> {
            if (key.getCode()==KeyCode.CONTROL) controlPressed=true;
            // press CONTROL + ENTER to start a new line instead of send message
            if (!controlPressed&& key.getCode().equals(KeyCode.ENTER)) {
                try {
                    sendButtonAction();
                } catch (SocketException e){
                    e.printStackTrace();
                    showInternetErrorDialog("Connect Failed!","Internet Error!","There is something wrong with the Internet connection.\n"
                        + " Please check the Internet and try again later.");
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
                key.consume();
            }else if(controlPressed&& key.getCode().equals(KeyCode.ENTER)){
                messageBox.appendText("\n");
            }
        });
        MainLauncher.controllers.put(this.getClass().getSimpleName(),this);
    }

    public void showInternetErrorDialog(String title,String message,String content) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.setContentText(content);
            alert.showAndWait();
        });

    }
    public void setImageLabel(String selectedPicture) {
        switch (selectedPicture) {
            case "Dominic":
                this.userImageView.setImage(new Image(getClass().getClassLoader().getResource("images/Dominic.png").toString()));
                break;
            case "Sarah":
                this.userImageView.setImage(new Image(getClass().getClassLoader().getResource("images/sarah.png").toString()));
                break;
            case "Default":
                this.userImageView.setImage(new Image(getClass().getClassLoader().getResource("images/default.png").toString()));
                break;
        }
    }

    public void logoutScene() {
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent window = null;
            try {
                window = (Pane) fxmlLoader.load();
               // Listener.send("");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Stage stage = MainLauncher.getPrimaryStage();
            Scene scene = new Scene(window);
            stage.setMaxWidth(1024);
            stage.setMaxHeight(620);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.centerOnScreen();
        });
    }


    public void addToRecentConservation(Message message){
        logger.info("addToRecentConservation() method Enter");
        Platform.runLater(() -> {//在recent message新建一个最近聊天
            Conservation conservation =new Conservation();
            conservation.setID(message.getConversationID());
            conservation.setLastTalk(message.getSendDate());
            conservation.setTarget(Integer.parseInt(message.getTarget()));
            conservation.setType(message.getConversationType());

            if(message.getConversationType()==1){
                ArrayList<String>containUsers = new ArrayList<>();
                conservation.setName(message.getTarget());
                containUsers.add(message.getName());
                containUsers.add(message.getTarget());
                conservation.setContainUsers(containUsers);
                conservation.setPicture("images/"+targetID+".png");
            }else {
                conservation.setName("group conservation"+message.getConversationID());
                conservation.setPicture("images/default.png");
            }
            conservationObservableList.add(conservation);
            messageList.setItems(conservationObservableList);
            messageList.setCellFactory(new MessageRenderer());

            //再把该对话加入到本地缓存(map)
            conservationMap.put(conservation.getID(),conservation);

//            ObservableList<User> users = FXCollections.observableList(message.getUsers());
//            userList.setItems(users);
//            userList.setCellFactory(new CellRenderer());
//            setOnlineLabel(String.valueOf(message.getUserlist().size()));
        });
        logger.info("addToRecentConservation() method Exit");
    }

    public void createSingleConversation(MouseEvent mouseEvent) throws SQLException {
        if(mouseEvent.getClickCount()==2) {
            currentUserID = Integer.valueOf(usernameLabel.getText());
            currentConservationType = 1;//双击可以私聊，进入私聊模式，然后判断会话是否已存在
            User targetUser = (User) userList.getSelectionModel().getSelectedItem();
            targetID = Integer.parseInt(targetUser.getName());
            if(!targetUser.getName().equals(usernameLabel.getText())) {
                //type =1  single conservation
                String containUsers = usernameLabel.getText()+","+targetUser.getName();
                try {
                    ResultSet r1 =  stmt.executeQuery("select id from conservation where contain_users like '%"+usernameLabel.getText()+"%' "
                        + "and contain_users like '%"+targetID+"%' and type=1 ");
                    if(!r1.next()) {
                        //改私聊在数据库中不存在，create a new conversation

                        stmt.execute("insert into conservation (type,contain_users,contain_messages)"
                                + "values (1,'"+ containUsers +"',0);");
                        ResultSet resultSet = stmt.executeQuery("select id from conservation where contain_users like '%"+usernameLabel.getText()+"%' "
                            + "and contain_users like '%"+targetID+"%' and type=1 ");
                        messageListMap.put(currentConservationID,currentMessages);//把当前的chatPage里的聊天记录存起来

                        if(resultSet.next()){
                            currentConservationID = resultSet.getInt("id");}
                        chatPage.getItems().clear();
                        Message createConservationMessage = new Message();
                        createConservationMessage.setConversationID(currentConservationID);
                        createConservationMessage.setTarget(String.valueOf(targetID));
                        createConservationMessage.setConversationType(1);
                        createConservationMessage.setID(currentConservationID);
                        createConservationMessage.setName(String.valueOf(currentUserID));
                        createConservationMessage.setMsg("create a conservation local");
                        createConservationMessage.setDate(Date.from(Instant.now()));

                       // createConservationMessage.setType();
                        addToRecentConservation(createConservationMessage);
                    }else {
                        //chatPage change to the corresponding conservation

                        messageListMap.put(currentConservationID,currentMessages);
                        currentConservationID = r1.getInt("id");
                        currentMessages= messageListMap.get(currentConservationID);//聊天记录传过来
                        chatPage.getItems().clear();
                        if(currentMessages==null) {currentMessages = new ArrayList<Message>();}//恢复保存的聊天记录
                        for (Message message : currentMessages) {
                            addToChat(message, false);
                        }
                        //then,
                           // currentMessages.forEach(message -> addToChat(message,false));
                    }
                }catch (SQLException e){
                    e.printStackTrace();
                    e.getCause();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }


            }
        }
//        ObservableList<Message> messages = FXCollections.observableList(new ArrayList<>());
//        chatPage.setItems(messages);
//        chatPage.setCellFactory(new CellRenderer());
//        ListView conversationPage;
    }
}