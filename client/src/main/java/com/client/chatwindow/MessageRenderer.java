package com.client.chatwindow;

import com.messages.Message;
import com.messages.User;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
/*
chatPage 渲染器

 */
class MessageRenderer implements Callback<ListView<Message>,ListCell<Message>> {
  @Override
  public ListCell<Message> call(ListView<Message> p) {

    ListCell<Message> cell = new ListCell<Message>(){
      @Override
      protected void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(null);
        setText(null);
        if(item!=null){
          HBox hBox = new HBox();
        }
      }
    };
  return null;
  }
}