package com.bi.programlist.ui.javafx

import com.bi.programlist.{GenerateInfo, Settings}
import com.bi.programlist.Utils._

object Model {

}

case class UiInfo(packProgram: Boolean, userName: String, action: String, verifierName: String, testerName: String, sourceExcel: String, targetExcel: String, 
    settings: UISettings, svnUser: Option[String]) {
  def toGenerateInfo: GenerateInfo = GenerateInfo(packProgram, userName, action, verifierName, testerName, settings.toSettings)
}

case class UISettings(projectRoot: String, classRelativePath: String, projectTargetRoot: String, classRelativeTargetPath: String,
    replaceWebContent:Boolean, sourceOnly: Boolean, javaClassTogether: Boolean, pathStartsWithProjectName: Boolean, webContentName: String = "WebContent") {
  
  def toSettings: Settings = Settings(toPathList(projectRoot), toPathList(classRelativePath), toPathList(projectTargetRoot), toPathList(classRelativeTargetPath),
    replaceWebContent, sourceOnly, javaClassTogether, pathStartsWithProjectName, webContentName)
}