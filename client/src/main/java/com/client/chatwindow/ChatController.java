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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import javafx.scene.control.Alert.AlertType;
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
    @FXML private ImageView MultiConservation;
    @FXML private Button recordBtn;
    @FXML private Button sendBtn;
    @FXML ListView chatPage;
    @FXML ListView messageList;
    @FXML ListView contactsList;
    @FXML ListView statusList;
    @FXML BorderPane borderPane;
    @FXML TabPane leftPane;
    @FXML Tab recentMessageTab;
    @FXML Tab onlineUserTab;
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
    private ArrayList<User> users = new ArrayList<>();

    private ArrayList<Message> currentMessages = new ArrayList<>();
    private Map<Integer, ArrayList<Message>> messageListMap = new HashMap<>();//map conservation ID to messageHistory
    private Map<Integer,String> conversationTypeMap = new HashMap<>();
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String picture;
   // private Map<String,User>userMap = new HashMap<>();
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
            Date sendDate = Date.from(Instant.now());
            String sendTime = df.format(Date.from(Instant.now()));
            String date = sendTime.substring(0,10);
            String time = sendTime.substring(11);
            if(currentConservationType==1) {
                //私聊时按下发送按钮
                 stmt.execute("insert into message_history (text,date,time,type,fromid,target , belong) "
                    + "values ('"+ messageBox.getText()+"','"+date+"','"+time+"',1,'"
                     +currentUserID+"','"+targetID+"','"+currentConservationID+"')");
                ResultSet resultSet = stmt.executeQuery("select id,date,time from message_history"
                    + " where (date = '"+date+"' and time='"+time+"' and fromid = '"+currentUserID+"' and target = "+targetID+" and type =1)") ;
                //将发送的信息先传到数据库，再从数据库获取该条信息的id
                if (resultSet.next()) {
                    int currentMessageID = resultSet.getInt("id");
                    Listener.send(msg, String.valueOf(targetID),currentMessageID, 1, currentConservationID ,sendDate);
                    messageBox.clear();
                    //send
                    //update conservation in DB
                    stmt.execute("update conservation set contain_messages = concat(contain_messages,',"+currentMessageID +"') where id = '"+currentConservationID+"'");
                    stmt.execute("update conservation set last_talk_date = current_date where id ='"+currentConservationID+"'");
                    stmt.execute("update conservation set last_talk_time = current_time where id ='"+currentConservationID+"'");
                } else {
                    System.out.println("database ERROR!");
                }
            }else {
                //type =2 对应群聊时发送
                stmt.execute("insert into message_history (text,date,time,type,fromid,target , belong) "
                    + "values ('"+ messageBox.getText()+"','"+date+"','"+time+"',2,'"
                    +currentUserID+"','"+currentConservationID+"','"+currentConservationID+"')");
                ResultSet resultSet = stmt.executeQuery("select id,date,time from message_history"
                    + " where (date = '"+date+"' and time='"+time+"' and fromid = '"+currentUserID+"' and belong = "+currentConservationID+" and type =2)") ;
                if(resultSet.next()){
                    int currentMessageID = resultSet.getInt("id");
                    Listener.send(msg,String.valueOf(currentConservationID),currentMessageID,2,currentConservationID,sendDate);
                    messageBox.clear();
                    stmt.execute("update conservation set contain_messages = concat(contain_messages,',"+currentMessageID +"') where id = '"+currentConservationID+"'");
                    stmt.execute("update conservation set last_talk_date = current_date where id ='"+currentConservationID+"'");
                    stmt.execute("update conservation set last_talk_time = current_time where id ='"+currentConservationID+"'");
                }
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
                //logger.debug("ONLINE USERS: " + Integer.toString(msg.getUserlist().size()));
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

            } else if((msg.getName().equals(String.valueOf(targetID))&&currentConservationType==1)||
                (msg.getConversationType()==2&&msg.getConversationID()==currentConservationID&&currentConservationType==2) ){
                if (isNew) currentMessages.add(msg);
                Thread t1 = new Thread(othersMessages);
                t1.setDaemon(true);
                while (!threadFinished){
                    wait(1);
                }
                threadFinished = false;
                t1.start();
                System.out.println("t1 start");
            }else  {
                //有新消息但是不是当前对话的新消息，弹出提醒
                if(isNew){
                    newMessageNotification(msg);
                }
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
        this.userImageView.setImage(new Image(getClass().getClassLoader().getResource("images/default.png").toString()));
    }

    public void setOnlineLabel(String userAccount) {
        Platform.runLater(() -> onlineCountLabel.setText(userAccount));
    }

    public void setUserList(Message msg) {
        logger.info("setUserList() method Enter");
        Platform.runLater(() -> {
            users.clear();
            users.addAll(msg.getUserlist());
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            userList.setItems(users);
            userList.setCellFactory(new CellRenderer());
            setOnlineLabel(String.valueOf(msg.getUserlist().size()));
        });
        logger.info("setUserList() method Exit");
    }

    public void newMessageNotification(Message msg){
        Platform.runLater(() -> {

            leftPane.getSelectionModel().select(recentMessageTab);

            Image profileImg = new Image(getClass().getClassLoader().getResource("images/" + msg.getPicture().toLowerCase() +".png").toString(),50,50,false,false);
            TrayNotification tray = new TrayNotification();
            tray.setTitle("New Message");
            tray.setMessage("you have a new message from "+msg.getName());
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
        leftPane.getSelectionModel().select(onlineUserTab);

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
                picture="default";
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
                conservation.setPicture(String.valueOf(targetID));
            }else {
                conservation.setName("group conservation"+message.getConversationID());
                conservation.setPicture("default");
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

    public void changeToConservation(MouseEvent mouseEvent)
        throws SQLException, InterruptedException, ParseException {
        currentUserID = Integer.parseInt(usernameLabel.getText());
        Conservation targetConservation = (Conservation) messageList.getSelectionModel()
            .getSelectedItem();
        currentConservationType = targetConservation.getType();
        currentConservationID = targetConservation.getID();
        if (currentConservationType == 1) {
            //在recent message中点击私聊，切换到指定的私聊
            ArrayList<String> containUsers = targetConservation.getContainUsers();
            User targetUser = new User();
            if (containUsers.get(0).equals(usernameLabel.getText())) {
                targetUser.setName(containUsers.get(1));
            } else {
                targetUser.setName(containUsers.get(0));
            }
            targetUser.setPicture("default");
            changeToSingleConservation(targetUser);
        } else {
            //切换到群聊
            ArrayList<String> containUsers = targetConservation.getContainUsers();

            ResultSet r1 = stmt.executeQuery(
                "select * from conservation where id = '" + currentConservationID + "'");
            if (r1.next()) {
                //该群聊存在,从数据库获取
                currentConservationID = r1.getInt("id");

                    String containMessages = r1.getString("contain_messages");
                    if(containMessages!=null){
                    String[] messages = containMessages.split(",");//存的是message的ID
                    currentMessages.clear();
                    chatPage.getItems().clear();
                    for (String s : messages) {
                        if (!s.equals("")) {
                            ResultSet resultSet = stmt.executeQuery(
                                "select * from message_history where id =" + Integer.parseInt(s)
                                    + " ");
                            if (resultSet.next()) {
                                Message message = new Message();
                                message.setID(resultSet.getInt("id"));
                                message.setMsg(resultSet.getString("text"));
                                message.setType(MessageType.USER);
                                message.setConversationID(resultSet.getInt("belong"));
                                message.setName(resultSet.getString("fromid"));
                                message.setTarget(resultSet.getString("target"));
                                message.setConversationType(2);

                                String date = resultSet.getString("date");
                                String time = resultSet.getString("time");
                                message.setSnedDateDate(df.parse(date + " " + time));
                                ResultSet r2 = stmt.executeQuery(
                                    "select picture from user_table where account ='"
                                        + message.getName() + "'  ");
                                if (r2.next())
                                    message.setPicture(r2.getString("picture"));
                                addToChat(message, false);
                            }
                        }
                    }
                }else {
                    currentMessages.clear();
                    chatPage.getItems().clear();
                }
            }
        }
    }
    public void startSingleConversation(MouseEvent mouseEvent) {
        //该方法仅限于私聊模式双击对方
        if(mouseEvent.getClickCount()==2) {
            currentUserID = Integer.parseInt(usernameLabel.getText());
            currentConservationType = 1;//双击可以私聊，进入私聊模式，然后判断会话是否已存在
            User targetUser = (User) userList.getSelectionModel().getSelectedItem();
            targetID = Integer.parseInt(targetUser.getName());
            changeToSingleConservation(targetUser);

        }
    }
    public void changeToSingleConservation(User targetUser){
        if(!targetUser.getName().equals(usernameLabel.getText())) {
            //type =1  single conservation
            targetID = Integer.parseInt(targetUser.getName());
            String containUsers = usernameLabel.getText() + "," + targetUser.getName();
            try {
                ResultSet r1 = stmt.executeQuery(
                    "select id,contain_messages from conservation where contain_users like '%"
                        + usernameLabel.getText() + "%' "
                        + "and contain_users like '%" + targetID + "%' and type=1 ");
                if (!r1.next()) {
                    //该私聊在数据库中不存在，create a new conversation

                    stmt.execute(
                        "insert into conservation (type,contain_users,picture,last_talk_date,last_talk_time)"
                            + "values (1,'" + containUsers + "','" + targetUser.getPicture()
                            + "',current_date,current_time);");
                    ResultSet resultSet = stmt.executeQuery(
                        "select id from conservation where contain_users like '%"
                            + usernameLabel.getText() + "%' "
                            + "and contain_users like '%" + targetID + "%' and type=1 ");
                    messageListMap.put(currentConservationID,
                        currentMessages);//把当前的chatPage里的聊天记录存起来

                    if (resultSet.next()) {
                        currentConservationID = resultSet.getInt("id");
                        chatPage.getItems().clear();
                        Message createConservationMessage = new Message();
                        createConservationMessage.setConversationID(currentConservationID);
                        createConservationMessage.setTarget(String.valueOf(targetID));
                        createConservationMessage.setConversationType(1);
                        createConservationMessage.setID(currentConservationID);
                        createConservationMessage.setName(String.valueOf(currentUserID));
                        createConservationMessage.setMsg("create a conservation local");
                        createConservationMessage.setSnedDateDate(Date.from(Instant.now()));

                        // createConservationMessage.setType();
                        addToRecentConservation(createConservationMessage);
                    }
                } else {
                    //chatPage change to the corresponding conservation
                    currentConservationID = r1.getInt("id");
                    //从数据库获取聊天记录
                    String containMessages = r1.getString("contain_messages");
                    if(containMessages!=null) {

                        String[] messages = containMessages.split(",");//存的是message的ID
                        currentMessages.clear();
                        chatPage.getItems().clear();
                        for (String s : messages) {
                            if (!s.equals("")) {
                                ResultSet resultSet = stmt.executeQuery(
                                    "select * from message_history where id =" + Integer.parseInt(s)
                                        + " ");
                                if (resultSet.next()) {
                                    Message message = new Message();
                                    message.setID(resultSet.getInt("id"));
                                    message.setMsg(resultSet.getString("text"));
                                    message.setType(MessageType.USER);
                                    message.setConversationID(resultSet.getInt("belong"));
                                    message.setName(resultSet.getString("fromid"));
                                    message.setTarget(resultSet.getString("target"));
                                    message.setConversationType(1);
                                    if (message.getTarget().equals(usernameLabel.getText())) {
                                        message.setPicture(picture);
                                    } else {
                                        message.setPicture(targetUser.getPicture());
                                    }
                                    String date = resultSet.getString("date");
                                    String time = resultSet.getString("time");
                                    message.setSnedDateDate(df.parse(date + " " + time));
                                    addToChat(message, false);
                                }
                            }
                        }
                    }else {
                        currentMessages.clear();
                        chatPage.getItems().clear();
                    }
                    //从本地获取聊天记录
//                        messageListMap.put(currentConservationID,currentMessages);
//                        currentConservationID = r1.getInt("id");
//                        currentMessages= messageListMap.get(currentConservationID);//聊天记录传过来
//                        chatPage.getItems().clear();
//                        if(currentMessages==null) {currentMessages = new ArrayList<Message>();}//恢复保存的聊天记录
//                        for (Message message : currentMessages) {
//                            addToChat(message, false);
//                        }
                    //then,
                    // currentMessages.forEach(message -> addToChat(message,false));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                e.getCause();
            } catch (InterruptedException | ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void recentMessageClicked() throws SQLException, ParseException {
        if(usernameLabel!=null){//选中了recentMessage
            currentUserID = Integer.parseInt(usernameLabel.getText());
           ResultSet resultSet = stmt.executeQuery("select *from conservation where contain_users like '%"+currentUserID+"%' ");
            System.out.println("click recentMessage");
            ArrayList<Conservation> conservationArrayList = new ArrayList<>();
            while (resultSet.next()){
                Conservation conservation = new Conservation();
                conservation.setType(resultSet.getInt("type"));
                conservation.setID(resultSet.getInt("id"));
                conservation.setPicture(resultSet.getString("picture"));

                //setLastTalk
                String date = resultSet.getString("last_talk_date");
                String time = resultSet.getString("last_talk_time");
                Date lastTalk = df.parse(date+" "+time);
                conservation.setLastTalk(lastTalk);

                //Users
                String containUsers = resultSet.getString("contain_users");
                String[]users =  containUsers.split(",");

                ArrayList<String> containUserArraylist = new ArrayList<>(Arrays.asList(users));
                conservation.setContainUsers(containUserArraylist);
                if(conservation.getType()==1){//会话名称就是对方id
                    if(users[0].equals(usernameLabel.getText())){
                        conservation.setTarget(Integer.parseInt(users[1]));
                        conservation.setName(users[1]);
                    }else {
                        conservation.setTarget(Integer.parseInt(users[0]));
                        conservation.setName(users[0]);
                    }
                }else {
                    //群聊记录
                    conservation.setName("group "+conservation.getID());
                    conservation.setTarget(conservation.getID());//群聊的target就是ID
                }
                conservationArrayList.add(conservation);
            }
            conservationArrayList.sort(Comparator.comparing(Conservation::getLastTalk).reversed());
            conservationObservableList.clear();

            conservationObservableList.addAll(conservationArrayList);
            messageList.setItems(conservationObservableList);
            messageList.setCellFactory(new MessageRenderer());
//            System.out.println(recentMessageTab);
//            System.out.println(onlineUserTab);
        }

    }

    public void createGroupConservationClicked() throws SQLException {
        logger.info("click create group conservation");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Choose Users");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        ButtonType OK = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().add(OK);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
       // ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        Map <CheckBox,User> map = new HashMap<>();

        for (int i = 0; i < users.size(); i++) {
            if (!users.get(i).getName().equals(usernameLabel.getText())) {
                CheckBox tmp = new CheckBox(users.get(i).getName());
         //       checkBoxes.add(tmp);
                map.put(tmp,users.get(i));
                grid.add(tmp,0,i);
            }
        }

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if(result.isPresent()&&result.get()==ButtonType.OK){

            ArrayList<User> selectedUsers = new ArrayList<>();
            for (Map.Entry<CheckBox,User> entry : map.entrySet()){
                if (entry.getKey().isSelected()){
                    selectedUsers.add(entry.getValue());
                }
            }
            if(!selectedUsers.isEmpty()){
                createGroup(selectedUsers);
            }else {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Choose Empty");
                alert.setHeaderText("Waring");
                alert.setContentText("You should choose at least one person");
                alert.showAndWait();
            }

        }

    }
    public void createGroup(ArrayList<User> selectedUsers) throws SQLException {
        logger.info("start create group");
        ArrayList<String> selectedString = new ArrayList<>();
        selectedString.add(usernameLabel.getText());
        StringBuilder containUsers = new StringBuilder(usernameLabel.getText());
        for (User selectedUser : selectedUsers) {
            selectedString.add(selectedUser.getName());
            containUsers.append(",").append(selectedUser.getName());
        }
        Date createDate = Date.from(Instant.now());

        String sendTime = df.format(Date.from(Instant.now()));
        String date = sendTime.substring(0,10);
        String time = sendTime.substring(11);
        Conservation conservation = new Conservation();
        conservation.setContainUsers(selectedString);
        conservation.setType(2);
        conservation.setPicture("default");
        conservation.setLastTalk(createDate);
        stmt.execute("insert into conservation (type, contain_users, picture, last_talk_date, last_talk_time)"
            + " values (2,'"+containUsers+"','default','"+date+"','"+time+"') ");
        ResultSet resultSet = stmt.executeQuery("select id from conservation where contain_users = '"+containUsers+"'"
            + "and type=2 and last_talk_date = '"+date+"' and last_talk_time = '"+time+"'");
        if(resultSet.next()) {conservation.setID(resultSet.getInt("id"));}
        chatPage.getItems().clear();
        targetID = currentConservationID;
        Message createConservationMessage = new Message();
        createConservationMessage.setConversationID(currentConservationID);
        createConservationMessage.setTarget(String.valueOf(targetID));
        createConservationMessage.setConversationType(2);
        createConservationMessage.setID(currentConservationID);
        createConservationMessage.setName("group "+currentConservationID);
        createConservationMessage.setMsg("create a conservation local");
        createConservationMessage.setSnedDateDate(createDate);

        // createConservationMessage.setType();
        addToRecentConservation(createConservationMessage);}



    }
