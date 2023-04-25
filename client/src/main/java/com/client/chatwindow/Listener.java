package com.client.chatwindow;

import com.client.login.LoginController;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Date;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

import static com.messages.MessageType.CONNECTED;

public class Listener implements Runnable{

    private static final String HASCONNECTED = "has connected";

    private static String picture;
    private Socket socket;
    public String hostname;
    public int port;
    public static String username;
    public ChatController controller;
    private static ObjectOutputStream oos;
    private InputStream is;
    private ObjectInputStream input;
    private OutputStream outputStream;

    private static FileInputStream fis;

    private static DataOutputStream dos;
    private DataInputStream dis;
    private FileOutputStream fos;
    Logger logger = LoggerFactory.getLogger(Listener.class);

    public Listener(String hostname, int port, String username, String picture, ChatController controller) {
        this.hostname = hostname;
        this.port = port;
        Listener.username = username;
        Listener.picture = picture;
        this.controller = controller;
    }

    public void run() {
        try {
            socket = new Socket(hostname, port);
            LoginController.getInstance().showScene();
            outputStream = socket.getOutputStream();
            oos = new ObjectOutputStream(outputStream);
            is = socket.getInputStream();
            input = new ObjectInputStream(is);



        } catch (IOException e) {
            LoginController.getInstance().showErrorDialog("Could not connect to server",
                "\"Please check for firewall issues and check if the server is running.");
            logger.error("Could not Connect");
        }
        logger.info("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());

        try {
            connect();
            logger.info("Sockets in and out ready!");
            while (socket.isConnected()) {
                Message message =  (Message) input.readObject();

                if (message != null) {
                    logger.debug("Message received:" + message.getMsg() + " MessageType:" + message.getType() + "Name:" + message.getName());
                    switch (message.getType()) {
                        case USER:
                            controller.addToChat(message,true);
                            break;
                        case VOICE:
                            controller.addToChat(message,true);
                            break;
                        case File:
                            String msg = message.getMsg();

                            controller.saveFile(msg,message.getID());

                            message.setMsg("send a File ");
                            controller.addToChat(message,true);
                            break;
                        case NOTIFICATION:
                            controller.newUserNotification(message);
                            break;
                        case SERVER:
                            controller.addAsServer(message);
                            break;
                        case CONNECTED:
                            controller.setUserList(message);

                            break;
                        case DISCONNECTED:
                            controller.setUserList(message);
                            break;
                        case STATUS:
                            controller.setUserList(message);
                            break;
                        case SHUTDOWN:
                            controller.shutdownNotification();
                            break;
                        case LOGOUT:
                            controller.showInternetErrorDialog("Taken offline",
                            "Your id login on another device",
                            "Please change your password if it was not your operation");
                            disconnect();
                            controller.logoutScene();

                            //controller.setUserList(message);
                            break;
                    }
                }
            }
        } catch (SocketException e){
            e.printStackTrace();
            controller.shutdownNotification();
          //  controller.logoutScene();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
            controller.logoutScene();
        }
    }

    /* This method is used for sending a normal Message
     * @param msg - The message which the user generates
     */
    public static void send(String msg,String targetID,int messageID,int conservationType, int conservationID , Date date , MessageType messageType) throws IOException {
        Message newMessage = new Message();
        newMessage.setName(username);
        newMessage.setType(messageType);//意思是来自用户发送的文字消息或者文件
       // newMessage.setStatus(Status.AWAY);
        newMessage.setMsg(msg);
        newMessage.setPicture(picture);
        newMessage.setID(messageID);
        newMessage.setConversationType(conservationType);//1=私聊，2=群聊
        newMessage.setConversationID(conservationID);
        newMessage.setSnedDateDate(date);
        newMessage.setTarget(targetID);
        oos.writeObject(newMessage);
        oos.flush();
    }

    /* This method is used for sending a voice Message
 * @param msg - The message which the user generates
 */
    public static void sendVoiceMessage(byte[] audio) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.VOICE);
        createMessage.setStatus(Status.RECORDING);
        createMessage.setVoiceMsg(audio);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
        oos.flush();
    }

    /* This method is used for sending a normal Message
 * @param msg - The message which the user generates
 */

//    public static void sendFile(File file) throws IOException {
//        try {
//            fis = new FileInputStream(file);
//            //client.getOutputStream()返回此Socket的输出流
////            dos.writeUTF(file.getName());
////            dos.flush();
////            dos.writeLong(file.length());
////            dos.flush();.
//         //   System.out.println("开始传输文件-----");
//            byte[] bytes = new byte[1024];
//            int length = 0;
//
//            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
//                dos.write(bytes, 0, length);
//                dos.flush();
//            }
//         //   System.out.println("文件传输成功-----");
//        }catch(IOException e) {
//            e.printStackTrace();
//            // System.out.println("文件传输异常");
//            // 传输完关闭流
//        }
//    }

    public static void sendStatusUpdate(Status status) throws IOException {
        Message message = new Message();
        message.setName(username);
        message.setType(MessageType.STATUS);
        message.setStatus(status);
        message.setPicture(picture);
        oos.writeObject(message);
        oos.flush();
    }
    public static void disconnect() throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.DISCONNECTED);
        createMessage.setStatus(Status.LOGOUT);
        //createMessage.setMsg(msg);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
        oos.flush();
    }

    /* This method is used to send a connecting message */
    public static void connect() throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(CONNECTED);
        createMessage.setMsg(HASCONNECTED);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
    }

}
