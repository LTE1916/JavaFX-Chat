package com.client.chatwindow;

import static com.client.login.LoginController.con;

import com.messages.Conservation;
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
class MessageRenderer implements Callback<ListView<Conservation>,ListCell<Conservation>> {
  @Override
  public ListCell<Conservation> call(ListView<Conservation> p) {

    ListCell<Conservation> cell = new ListCell<Conservation>(){
      @Override
      protected void updateItem(Conservation conservation, boolean bln) {
        super.updateItem(conservation, bln);
        setGraphic(null);
        setText(null);
        if(conservation!=null){
          HBox hBox = new HBox();
          Text name;
          if(conservation.getType()==1){
            name = new Text(String.valueOf(conservation.getTarget()));   //私聊
          }else  {
            name = new Text(conservation.getName());//2人以上群聊
          }
          ImageView statusImageView = new ImageView();

            ImageView picture = new ImageView();
            Image image = new Image(getClass().getClassLoader().getResource("images/"+ conservation.getPicture().toLowerCase()+".png").toString(),50,50,true,true);
            picture.setImage(image);

            if(conservation.getType()==1&&conservation.getStatus()!=null) {

            Image statusImage = new Image(getClass().getClassLoader().getResource(
                    "images/" + conservation.getStatus().toLowerCase() + ".png")
                .toString(), 16, 16, true, true);
            statusImageView.setImage(statusImage);
            hBox.getChildren().addAll(statusImageView,name,picture);
          }else {
              hBox.getChildren().addAll(name,picture);
            }

            hBox.setAlignment(Pos.CENTER_LEFT);
            setGraphic(hBox);

          }
        }
      };


  return cell;
  }
}