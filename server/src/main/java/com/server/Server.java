package com.server;

import com.exception.DuplicateUsernameException;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import com.messages.User;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    /* Setting up variables */
    private static final int PORT = 9001;
    private static final HashMap<String, User> names = new HashMap<>();
    private static Hashtable<String, ObjectOutputStream> writers = new Hashtable<>();

    private static ArrayList<User> users = new ArrayList<>();
    static Logger logger = LoggerFactory.getLogger(Server.class);



    public Server() throws SQLException {
    }

    public static void main(String[] args) throws Exception {
        logger.info("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);

        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listener.close();
        }
    }


    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private Logger logger = LoggerFactory.getLogger(Handler.class);
        private User user;
        private ObjectInputStream input;
        private OutputStream os;
        private ObjectOutputStream output;
        private InputStream is;
        private DataInputStream dis;
        private FileOutputStream fos;
        private final Connection connection = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/java_chat_user",
            "test" ,"123456789");
        //connect to DATA BASE
        private Statement stmt = connection.createStatement();

        public Handler(Socket socket) throws IOException, SQLException {
            this.socket = socket;
            System.out.println(socket.toString());
        }

        public void run() {
            logger.info("Attempting to connect a user...");
            try {
                is = socket.getInputStream();
                input = new ObjectInputStream(is);
                os = socket.getOutputStream();
                output = new ObjectOutputStream(os);
                dis = new DataInputStream(is);

                Message firstMessage = (Message) input.readObject();
                if (checkDuplicateUsername(firstMessage)) {//异地登陆挤下线
                    Message logoutMessage = new Message();
                    logoutMessage.setName(firstMessage.getName());
                    logoutMessage.setStatus(Status.LOGOUT);
                    logoutMessage.setType(MessageType.LOGOUT);
                    ObjectOutputStream writer = writers.get(firstMessage.getName());
                    writer.writeObject(logoutMessage);
                    Thread.sleep(100);
                    writer = output;

                }
                //新用户上线
                    this.name = firstMessage.getName();
                    user = new User();
                    user.setName(firstMessage.getName());
                    user.setStatus(Status.ONLINE);
                    user.setPicture(firstMessage.getPicture());
                    users.add(user);
                    names.put(name, user);
                    writers.put(firstMessage.getName(), output);
                    logger.info(name + " has been added to the list");
            //    writers.add(output);
                sendNewUserNotification(firstMessage);
                addToList();
                while (socket.isConnected()) {
                        Message inputmsg = (Message) input.readObject();
                        //waiting the client message
                        if (inputmsg != null) {
                            logger.info(inputmsg.getType() + " - " + inputmsg.getName() + ": "
                                + inputmsg.getMsg());
                            switch (inputmsg.getType()) {
                                case USER:
                                case File:
                                    if (inputmsg.getConversationType() == 1) {
                                        writeSingleConversation(inputmsg);
                                    } else {
                                        writeGroup(inputmsg);
                                    }
                                    break;

                                case VOICE:
                                    write(inputmsg);
                                    break;
                                case CONNECTED:
                                    addToList();
                                    break;
                                case STATUS:
                                    changeStatus(inputmsg);
                                    break;
                                case DISCONNECTED:
                                    closeConnections();
                                    socket.close();
                                    break;
                            }
                        }
                    }

            } catch (SocketException socketException) {
                logger.error("Socket Exception for user " + name);

            } catch (DuplicateUsernameException duplicateException){
                logger.error("Duplicate Username : " + name);
            } catch (Exception e){
                logger.error("Exception in run() method for user: " + name, e);
            } finally {
                closeConnections();
            }
        }

        private Message changeStatus(Message inputmsg) throws IOException {
            logger.debug(inputmsg.getName() + " has changed status to  " + inputmsg.getStatus());
            Message msg = new Message();
            msg.setName(user.getName());
            msg.setType(MessageType.STATUS);
            msg.setMsg("");
            User userObj = names.get(name);
            userObj.setStatus(inputmsg.getStatus());
            write(msg);
            return msg;
        }

        private synchronized boolean checkDuplicateUsername(Message firstMessage) throws DuplicateUsernameException {
            logger.info(firstMessage.getName() + " is trying to connect");
            return names.containsKey(firstMessage.getName());

          //  } else {

           //     logger.error(firstMessage.getName() + " is already connected");
           //     throw new DuplicateUsernameException(firstMessage.getName() + " is already connected");
            //}
        }

        private Message sendNewUserNotification(Message firstMessage) throws IOException {
            Message msg = new Message();
            msg.setMsg("has joined the chat.");
            msg.setType(MessageType.NOTIFICATION);
            msg.setName(firstMessage.getName());
            msg.setPicture(firstMessage.getPicture());
            write(msg);
            return msg;
        }

        private Message sendShutdownNotification(Message firstMessage) throws IOException {
            Message msg = new Message();
            msg.setMsg("Sever shutdown!");
            msg.setType(MessageType.NOTIFICATION);
            msg.setName(firstMessage.getName());
            msg.setPicture(firstMessage.getPicture());
            write(msg);
            return msg;
        }

        private Message removeFromList() throws IOException {
            logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left the chat.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setName("SERVER");
            msg.setUserlist(names);
            write(msg);
            logger.debug("removeFromList() method Exit");
            return msg;
        }

        /*
         * For displaying that a user has joined the server
         */
        private Message addToList() throws IOException {
            Message msg = new Message();
            msg.setMsg("Welcome, You have now joined the server! Enjoy chatting!");
            msg.setType(MessageType.CONNECTED);
            msg.setName("SERVER");
            write(msg);
            return msg;
        }

        private void writeSingleConversation(Message message)throws IOException{
            //服务器给私聊对象发消息，通知发送方客户端已发送，通知接收方客户端有新消息
            ObjectOutputStream sender = writers.get(message.getName());
            ObjectOutputStream target = writers.get(message.getTarget());
            message.setUserlist(names);
            message.setUsers(users);
            message.setOnlineCount(names.size());
            sender.writeObject(message);
            sender.reset();
            if (target != null) {
                target.writeObject(message);
                target.reset();
            }
        }

        private void writeGroup(Message msg) throws IOException, SQLException {
            ObjectOutputStream sender = writers.get(msg.getName());

            msg.setUserlist(names);
            msg.setUsers(users);
            msg.setOnlineCount(names.size());
            sender.writeObject(msg);
            sender.reset();
            ResultSet resultSet = stmt.executeQuery("select contain_users from conservation "
                + "where id = '" + msg.getConversationID() + "' ");
            if (resultSet.next()) {
                String containUsers = resultSet.getString("contain_users");
                String []users = containUsers.split(",");
                for (int i = 0; i <users.length ; i++) {
                    if (!users[i].equals(msg.getName())) {
                        ObjectOutputStream receiver = writers.get(users[i]);
                        if(receiver != null){
                            receiver.writeObject(msg);
                            receiver.reset();
                        }

                    }
                }

            }
        }

        private void write(Message msg) throws IOException {
            //给全体在线用户发一条消息，（某人上线，服务器掉线......）

            for(Map.Entry<String, ObjectOutputStream> entry: writers.entrySet()){
             ObjectOutputStream writer = entry.getValue();
                msg.setUserlist(names);
                msg.setUsers(users);
                msg.setOnlineCount(names.size());
                writer.writeObject(msg);
                writer.reset();
            }
        }

        /*
         * Once a user has been disconnected, we close the open connections and remove the writers
         */
        private synchronized void closeConnections()  {
            logger.debug("closeConnections() method Enter");
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size()
                + " usersList size:" + users.size());
            if (name != null) {
                names.remove(name);
                logger.info("User: " + name + " has been removed!");
            }
            if (user != null){
                users.remove(user);
                logger.info("User object: " + user + " has been removed!");
            }
            if (output != null){
                writers.remove(name);
                logger.info("Writer object: " + user + " has been removed!");
            }
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                removeFromList();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size()
                + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
        }
    }
}
