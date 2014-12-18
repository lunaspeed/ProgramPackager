package com.bi.programlist.ui.javafx

import scala.language.implicitConversions
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.util._
import org.apache.commons.lang3.StringUtils
import org.parboiled.common.FileUtils
import com.bi.programlist.GenerateInfo
import com.bi.programlist.mega.Processor
import com.typesafe.scalalogging.slf4j.LazyLogging
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.{Button, CheckBox, ChoiceBox, TextField}
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.Stage
import javafx.css.PseudoClass
import org.apache.commons.lang3.SystemUtils
import javafx.scene.text.Font
import javafx.geometry.Pos
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.EventHandler
import javafx.scene.layout.{VBox, HBox}
import javafx.beans.binding.Bindings
import com.bi.programlist.mega.Svn
import com.bi.programlist.db.squeryl.DB
import javafx.scene.Node

object MainFxController {

  implicit def textFieldToString(tf: TextField): String = tf.getText()
  implicit def checkboxToBool(cb: CheckBox): Boolean = cb.isSelected()
  implicit def choiceBox[A](cb: ChoiceBox[A]): A = cb.getValue()
  implicit def toChangeListener[A](f: Function3[ObservableValue[_ <: A], A, A, Unit]) = new ChangeListener[A]() {
    def changed(ov: ObservableValue[_ <: A], oldValue: A, newValue: A): Unit = f(ov, oldValue, newValue)
  }
  
  implicit def textFieldToOptionString(tf: TextField): Option[String] = if(StringUtils.isEmpty(tf.getText())) None else Some(tf.getText())
}

class MainFxController extends LazyLogging {

  private final val config = Paths.get("config.json")
  private final val desktop = java.awt.Desktop.getDesktop()

  var stage: Stage = null

  @FXML
  var actiontarget: Text = null
  @FXML
  var actiontarget2: Text = null
  @FXML
  var projectRoot: TextField = null
  @FXML
  var classRelativePath: TextField = null
  @FXML
  var projectTargetRoot: TextField = null
  @FXML
  var classRelativeTargetPath: TextField = null
  @FXML
  var sourceOnly: CheckBox = null
  @FXML
  var javaClassTogether: CheckBox = null
  @FXML
  var packProgram: CheckBox = null
  @FXML
  var pathStartsWithProjectName: CheckBox = null
  @FXML
  var sourceExcel: TextField = null
  @FXML
  var targetExcel: TextField = null
  
  @FXML
  var userName: TextField = null
  @FXML
  var userSvn: TextField = null
  @FXML
  var svnButton: Button = null
  @FXML
  var submitButton: Button = null
  @FXML
  var fileAction: ChoiceBox[String] = null
  @FXML
  var webContentName: TextField = null
  @FXML
  var replaceWebContent: CheckBox = null
  @FXML
  var verifierName: TextField = null
  @FXML
  var testerName: TextField = null
  @FXML
  var openTargetExcel: Button = null
  @FXML
  var openTargetPath: Button = null
  
  @FXML
  var successButtons: VBox = null
  @FXML
  var failureButtons: VBox = null
  
  @FXML
  var gridPane1: javafx.scene.layout.GridPane = null
  
  /*  SVN tab   */
  @FXML
  var svnMessageBox: HBox = null
  @FXML
  var svnMessage: Text = null
  
  import MainFxController._
  private def inputsToValidate = projectRoot :: classRelativePath :: projectTargetRoot :: classRelativeTargetPath :: sourceExcel :: targetExcel :: Nil
  
  private def saveConfig: UiInfo = {
    val setting = UISettings(projectRoot, classRelativePath, projectTargetRoot, classRelativeTargetPath,
    replaceWebContent, sourceOnly, javaClassTogether, pathStartsWithProjectName, webContentName)
    val ui = UiInfo(packProgram, userName, fileAction, verifierName, testerName, sourceExcel, targetExcel, setting, userSvn)
    saveConfig(ui) match {
      case Failure(t) => logger.warn("error while saving config", t)
      case _ =>
    }
    ui
  }
  
  @FXML
  def handleSubmitButtonAction(event: ActionEvent): Unit = {
    validate(inputsToValidate) match {
      case Nil => {
        syncCheckBoxes
        val ui = saveConfig
        process(sourceExcel, ui.toGenerateInfo, targetExcel)
      }
      case errors => {
          val es = errors.mkString("\n\r")
          actiontarget.setText(es)
      }
    }
  }
  
  def handleSvnButtonAction(event: ActionEvent): Unit = {
    validate(projectRoot :: userSvn :: sourceExcel :: Nil) match {
      case Nil => {
        syncCheckBoxes
        val ui = saveConfig
        
        setButtonsState(true)
        clearMessages
        
        new Thread() {
          override def run(): Unit = {
            ui.svnUser match {
              case Some(svnAccount) => {
                DB.initDB(ui.settings.projectRoot +"/.svn/wc.db")
                logger.info(s"svn name: ${ui.svnUser}")
                val result = Svn.checkSvn(ui.sourceExcel, svnAccount, ui.settings.projectRoot) 
                Platform.runLater(new Runnable() {
                  override def run() {
                    result match {
                      case Right(report) if(report.diffModifier.isEmpty && report.notFound.isEmpty && report.tooOld.isEmpty) => {
                        svnMessage.setText("SVN check passed.")
                      }
                      case Right(report) => {
                        
                        var rs = s"""SVN Report
Total Files: ${report.total}
Files With Different modifiers (${ui.svnUser.getOrElse("")}):
${report.diffModifier.mkString("\n")}

Files not synced within last 1 day:
${report.tooOld.mkString("\n")}

Files not found in Local SVN:
${report.notFound.mkString("\n")}
"""
                        FileUtils.writeAllText(rs, new File("svnReport.txt"))
                        svnMessage.setText("SVN check not passed, please see report.")
                        svnMessageBox.setVisible(true)
                      }
                      case Left(errors) => displayErrors(svnMessage, errors, svnMessageBox)
                    }
                    setButtonsState(false)
                  }
                })
              }
              case None => logger.error("svnUser does not have a value")
            }
          }
        }.start()
        
      }
      case errors => {
        val es = errors.mkString("\n\r")
        actiontarget.setText(es)
      }
      
    }
  }
  
  private def validate(inputs: List[TextField]): List[String] = {
   
    val errorClass = PseudoClass.getPseudoClass("error")
    
    inputs.filter(i => {
      val notValid = StringUtils.isEmpty(i.getText())
      i.pseudoClassStateChanged(errorClass, notValid)
      notValid
    }) match {
      case Nil => Nil
      case h :: tail => h.requestFocus();"請輸入所有欄位" :: Nil
    }
  }

  @FXML
  protected def handleProjectRootButtonAction(event: ActionEvent): Unit = openDirectoryChooser(projectRoot)

  @FXML
  protected def handleClassSourceButtonAction(event: ActionEvent): Unit = openDirectoryChooser(projectTargetRoot)

  @FXML
  protected def handleSourceExcelButtonAction(event: ActionEvent): Unit = openExcelFileChooser(sourceExcel, false)

  @FXML
  protected def handleTargetExcelButtonAction(event: ActionEvent): Unit = openExcelFileChooser(targetExcel, true)
  
  @FXML
  protected def handleOpenTargetExcelButtonAction(event: ActionEvent): Unit = openFile(targetExcel.getText())
  @FXML
  protected def handleOpenTargetPathButtonAction(event: ActionEvent): Unit = openFile(projectTargetRoot.getText())
  @FXML
  protected def handleOpenLogButtonAction(event: ActionEvent): Unit = openFile("logs/log.log")
  
  @FXML
  protected def handleOpenSvnReportButtonAction(event: ActionEvent): Unit = openFile("svnReport.txt")

  private def setButtonsState(disable: Boolean): Unit = {
     submitButton.setDisable(disable)
     svnButton.setDisable(disable)
  }
  
  private def process(sourceExcel: String, gi: GenerateInfo, targetExcel: String): Unit = {

    setButtonsState(true)
    
    clearMessages
    
    new Thread() {
      
      override def run(): Unit = {
        
        val result = Processor.process(sourceExcel, gi, targetExcel, true)
        Platform.runLater(new Runnable() {
          def run(): Unit = {
            result match {
              case Right(pr) => {
                
                val log = f"""產生完畢
Initial Files:         ${pr.initialFiles}%4d
Total Classes Added:   ${pr.addedClasses}%4d
Inner Classes Added:   ${pr.addedInnerClasses}%4d
Extra files duplicated:${(pr.finalFiles - pr.initialFiles - pr.addedClasses)}%4d
Final Files:           ${pr.finalFiles}%4d"""
                actiontarget.setText(log)
                logger.info(log)
                val log2 = f"""
Files Copied:       ${pr.filesCopied}%7d
Files Not Found:    ${pr.filesNotFound}%7d
Files Ignored:      ${pr.filesNotCopied}%7d
Process took:       ${pr.time}%7d ms"""
                actiontarget2.setText(log2)
                logger.info(log2)
                successButtons.setVisible(true)
              }
              case Left(errors) => displayErrors(actiontarget, errors, failureButtons)
            }
            setButtonsState(false)
          }
        })
      }
    }.start()
  }
  
  private def displayErrors(textField: Text, errors: Seq[Throwable], showNode: Node): Unit = {
    textField.setText("產生錯誤，請看log")
    errors.zipWithIndex.foreach(e => logger.error(s"error ${e._2 + 1} while running process", e._1))
    showNode.setVisible(true)
  }
  
  private def clearMessages(): Unit = {
    actiontarget.setText("")
    actiontarget2.setText("")
    svnMessage.setText("")
    successButtons.setVisible(false)
    failureButtons.setVisible(false)
    svnMessageBox.setVisible(false)
  }

  private def openExcelFileChooser(text: TextField, newFile: Boolean): Unit = {

    val file = text.getText()
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Choose an Excel (.xlsx)")
    if (StringUtils.isNotEmpty(file)) {
      val f = new File(file)
      if (f.exists()) {
        if (f.isFile()) {
          fileChooser.setInitialDirectory(f.getParentFile())
          fileChooser.setInitialFileName(f.getName())
        } else if (f.isDirectory()) {
          fileChooser.setInitialDirectory(f)
        }
      }
    }
    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Excel Workbook", "*.xlsx"), new ExtensionFilter("Excel 97 - 2004 Workbook", "*.xls"))

    val chosenFile = if (newFile) fileChooser.showSaveDialog(stage) else fileChooser.showOpenDialog(stage)
    Option(chosenFile) match {
      case Some(_) =>
        Try(text.setText(chosenFile.getCanonicalPath())) match {
          case Failure(t) => logger.error(s"error set chosen file path", t)
          case _ =>
        }
      case None => 
    }
    
  }

  private def openDirectoryChooser(text: TextField): Unit = {

    val dir = text.getText()
    val directory = new DirectoryChooser()
    directory.setTitle("Choose a directory")
    if (StringUtils.isNotEmpty(dir)) {
      val df = new File(dir)
      if (df.exists()) {
        directory.setInitialDirectory(df)
      }
    }

    val chosenDir = directory.showDialog(stage)
    Option(chosenDir).foreach {dir =>
      Try(text.setText(dir.getCanonicalPath())) match {
        case Failure(t) => logger.error(s"error set chosen directory path", t)
        case _ =>
      }
    }
  }

  def onClose(): Unit = {

  }

  def init(): Unit = {
    
    //sample.eventtest.EventTest.addClick(sourceExcel, gridPane1)
    
//    if(Font.getFontNames("Consolas").size > 0) {
//      val font = Font.font("Consolas")
//      List(actiontarget, actiontarget2, svnMessage) foreach {_.setFont(font)}
//    }
    
    import java.lang.{Boolean => JBool}
    List(sourceExcel, targetExcel) foreach { tf => 
      tf.focusedProperty().addListener((observable: ObservableValue[_ <: JBool], oldValue: JBool, newValue: JBool) => 
          if(newValue) {
            tf.positionCaret(tf.getText().length())
          })
    }
    
    javaClassTogether.disableProperty().bind(sourceOnly.selectedProperty())
    
    loadConfig match {
      case Success(Some(ui)) => {
        val setting = ui.settings;
        projectRoot.setText(setting.projectRoot)
        classRelativePath.setText(setting.classRelativePath)

        projectTargetRoot.setText(setting.projectTargetRoot)
        classRelativeTargetPath.setText(setting.classRelativeTargetPath)

        javaClassTogether.setSelected(setting.javaClassTogether)
        packProgram.setSelected(ui.packProgram)
        pathStartsWithProjectName.setSelected(setting.pathStartsWithProjectName)

        sourceExcel.setText(ui.sourceExcel)
        targetExcel.setText(ui.targetExcel)
        userName.setText(ui.userName)
        verifierName.setText(ui.verifierName)
        testerName.setText(ui.testerName)
        
        fileAction.setValue(ui.action)
        
        replaceWebContent.setSelected(setting.replaceWebContent)
        
        webContentName.setText(setting.webContentName)
        
        ui.svnUser foreach {userSvn.setText(_)}
      }
      case Success(None) => {
        val s = File.separator
        val wcn = "WebContent" 
        val path = s"WEB-INF${s}classes"
        classRelativePath.setText(wcn + s + path)
        classRelativeTargetPath.setText(path)
        webContentName.setText(wcn)
        fileAction.setValue("新增")
        replaceWebContent.setSelected(true)
        sourceOnly.setSelected(true)
      }
      case Failure(t) => {
        logger.warn("error loading config, previous config ignored", t)
      }
    }
    syncCheckBoxes
  }
  
  private def syncCheckBoxes: Unit = {
    if(sourceOnly.isSelected()) javaClassTogether.setSelected(false)
  }
  
  private def openFile(path: String): Unit = {
    
    val file = new File(path)
    if(file.exists()) {
      
      Try(desktop.open(file)) match {
        case Failure(e) => logger.error("error opening file", e)
        case _ =>
      }
    }
  }
  

  import spray.json._

  object MyJsonProtocol extends DefaultJsonProtocol {
    
    implicit val uiSettingFormat = jsonFormat9(UISettings)
    implicit val uiFormat = jsonFormat9(UiInfo)
    
  }
  
  import MyJsonProtocol._
  private def saveConfig(ui: UiInfo): Try[Unit] = {
    //config
    Try({
      val wj = ui.toJson
      FileUtils.writeAllText(wj.prettyPrint, config.toFile(), StandardCharsets.UTF_8)
    })

  }

  private def loadConfig(): Try[Option[UiInfo]] = {

    val cf = config.toFile()
    if (cf.exists()) {
      Try({
        val js = FileUtils.readAllText(config.toFile(), StandardCharsets.UTF_8)
        Some(js.parseJson.convertTo[UiInfo])
      })
    } 
    else {
      Success(None)
    }
  }

}