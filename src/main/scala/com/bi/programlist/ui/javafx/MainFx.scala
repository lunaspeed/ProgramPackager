package com.bi.programlist.ui.javafx

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.stage._
import javafx.scene._
import javafx.event.EventHandler

object MainFx {

  def main(args: Array[String]): Unit = {
    Application launch (classOf[MainFx], args: _*)
  }

}

class MainFx extends Application {

  def start(primaryStage: Stage): Unit = {

    val loader = new FXMLLoader(getClass().getResource("mainFx.fxml"))
    val root: Parent = loader.load()
    val controller: MainFxController = loader.getController()
    controller.stage = primaryStage
    controller.init

    primaryStage.setOnCloseRequest(new EventHandler[WindowEvent]() {
      override def handle(e: WindowEvent): Unit = {
        controller.onClose()
      }
    })

    val scene = new Scene(root, 750, 550);

    primaryStage.setTitle("程式打包")
    primaryStage.setScene(scene)
    primaryStage.show()
  }

}
