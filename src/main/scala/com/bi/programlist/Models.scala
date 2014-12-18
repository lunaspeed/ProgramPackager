package com.bi.programlist

import com.bi.programlist.mega.Parser._

case class ProgInfo(filename: String, path: List[String], extension: String, isWebResource: Boolean, action: String, project: String)

//sealed trait FileAction { def name: String }
//case object NewFile extends FileAction { def name = "新增" }
//case object UpdateFile extends FileAction { def name = "修改" }
//case object DeleteFile extends FileAction { def name = "刪除" }


case class GenerateInfo(packProgram: Boolean,
    userName:String, defaultAction: String, verifierName: String, testerName:String, setting: Settings)

case class Settings(projectRoot: List[String], relativeClassPath: List[String], projectTargetRoot: List[String], relativeClassTargetPath: List[String],
    replaceWebContent:Boolean, sourceOnly: Boolean, javaClassTogether: Boolean, pathStartsWithProjectName: Boolean, webContentName: String = "WebContent") {
  
  def classRoot = projectRoot ++: relativeClassPath
  
  def classTargetRoot = projectTargetRoot ++: relativeClassTargetPath
}

case class ProcessResult(initialFiles: Int, finalFiles: Int, addedClasses:Int, addedInnerClasses:Int, fileCounts: Map[String, Int], filesCopied: Int, filesNotFound:Int, filesNotCopied: Int, time: Long)
