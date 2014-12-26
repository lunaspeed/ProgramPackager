package com.bi.programlist.mega

import scala.language.implicitConversions
import com.bi.programlist._
import com.bi.programlist.Utils._
import com.bi.programlist.excel.{SheetData, Excel}
import java.io.File
import org.apache.commons.lang3.StringUtils
import java.nio.file._
import java.io.FilenameFilter
import com.typesafe.scalalogging.slf4j.LazyLogging
//import java.nio.file.attribute.{FileAttribute, PosixFilePermissions, PosixFilePermission}
import scala.collection.JavaConversions._

object Parser extends LazyLogging {
  
//  private final val filePermissions = PosixFilePermissions.asFileAttribute(Set(PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE))
 

  def parse(filepath:String, gi: GenerateInfo, defaultProjName: Option[String])(prb: ProcessResultBuilder): Either[Seq[Throwable], List[SheetData[ProgInfo]]] = {
    
    val defProjName = defaultProjName.getOrElse("MEGA")
    val setting = gi.setting
    
    logger.info(s"generateInfo.pathStartsWithProjectName: ${setting.pathStartsWithProjectName}")
    
    def toProgInfo(data: List[(String, Option[String], Option[String])]): List[ProgInfo] = {
      prb.addInitialFiles(data.length) 
      data map { 
        case (filepath, action, projName) => {
          val paths = toPathList(filepath) match {
            case fn @ (s :: Nil) => fn
            case l => setting.projectRoot ++: (setting.pathStartsWithProjectName match {
              case true => l.drop(1)
              case false => l
            })
          }
          val wholeFilename = paths.last
          val lastIndex = wholeFilename.lastIndexOf('.');
          val (filename, extension) = if(lastIndex != -1) {
            (wholeFilename.substring(0, lastIndex), wholeFilename.substring(lastIndex + 1).toLowerCase)
          }
          else {
            (wholeFilename, "")
          }
          
          val pi = ProgInfo(filename, paths, extension, paths.contains(setting.webContentName), action.getOrElse(gi.defaultAction), projName.getOrElse(defProjName))
          //println(pi)
          pi
        }
        
      }
    }
    
    val addClass: List[ProgInfo] => List[ProgInfo] = setting.sourceOnly match {
      case true => identity   
      case false =>
        (progInfos) => progInfos flatMap { 
          p => {
            
            if(p.extension == "java") {
              val packagePath = p.path.dropWhile(_ != "com");
              val classPackagePath = setting.classRoot ++: packagePath
              p :: findClasses(p.filename, classPackagePath, p.action, p.project)(prb)
            }
            else {
              List(p)
            }
          }
        }
    }
    
    Excel.parse(filepath, (Excel.sheetToList(Excel.simpleRowToList _) _).andThen(toProgInfo).andThen(addClass))
  }
  
  def toFile(filepath:String, gi: GenerateInfo, sheets: List[SheetData[ProgInfo]])(prb: ProcessResultBuilder): Unit = {

    val sourceTargetData = sheets.map(sheet => {
      
          val sourceTarget = sheet.data.flatMap(pi => createTarget(pi, gi, sheet.name) map {(pi, _)})
          
          prb.addFinalFiles(sourceTarget.length)
          sheet.copy(data = sourceTarget)
        })
        
        if(gi.packProgram) {
          
          logger.info("start copying files")
          
          for(sheet <- sourceTargetData;
              (source, target) <- sheet.data) {
            if (source.path.length > 1)
              copyFile(source.path, target.path)(prb)
            else prb.addFilesNotCopied(1)
          }
          
          logger.info("finished copying files")
        }
        
        def pairToList(p: Tuple2[ProgInfo, ProgInfo]): List[Option[String]] = p match {
          case (_, t) => {
            val path = t.path match {
              case _ :: Nil => Nil
              case p => (p.drop(gi.setting.projectTargetRoot.length + 1) dropRight 1) :+ ""
            }
            None :: None :: None :: None :: None :: None :: 
            Some(t.project) :: Some(t.extension) :: Some(path.mkString(separator.toString)) :: 
            Some(t.path.last) :: Some(gi.userName) :: Some(t.action) :: Some(gi.verifierName) :: Some(gi.testerName) :: Nil
          }
        }
        logger.info("start writing to excel")
        val headers = "異動日期" :: "項目編號" :: "模組":: "功能" :: "問題說明" :: "更版原因說明" :: 
                      "主機別" :: "檔案類型" :: "程式目錄" :: "程式名稱" :: "異動人員" :: "異動狀態" :: "覆核人員" :: "測試人員" :: "備註" :: Nil
        Excel.write(filepath, sourceTargetData, pairToList, Excel.simpleToRow, headers)
        logger.info("finished writing to excel")
//    }
  }
  
  private def copyFile(source: List[String], target: List[String])(prb: ProcessResultBuilder): Unit = {
    
    val sp = listToPath(source)
    val sf = sp.toFile
    if(sf.exists) {
      if(sf.isDirectory()) {
        logger.warn(s"file [${sf.getCanonicalPath()}] is a directory, file copy skipped")
        
      }
      else {
        val tp = listToPath(target)
        Files.createDirectories(tp.getParent())//, filePermissions)
        Files.copy(sp, tp, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        prb.addFilesCopied(1)
      }
      
    }
    else {
      logger.warn(s"file [${sf.getCanonicalPath()}] does not exist, file copy skipped")
      prb.addFilesNotFound(1)
    }
    
  }
  
  private def createTarget(pi: ProgInfo, gi: GenerateInfo, sheetName: String): List[ProgInfo] = {

    val setting = gi.setting
    val project = if(setting.replaceWebContent) pi.project + ".war" else setting.webContentName

    val base = setting.projectTargetRoot :+ sheetName
    def fromJavaToClassPath = base ++: (project :: setting.relativeClassTargetPath ++: pi.path.drop(setting.projectRoot.length + 1)) :: Nil
    def sourceToTargetPath = {
      val p = pi.path.drop(setting.projectRoot.length)
      val relpath = p.indexOf(setting.webContentName) match {
        case -1 => p
        case i => project :: p.drop(i + 1)
      }
      base ++: relpath :: Nil
    }
    def sourceClassToTargetClassPath = base ++: (project :: setting.relativeClassTargetPath ++: pi.path.drop(setting.classRoot.length)) :: Nil
    val newPaths = if(pi.path.length == 1) {
      pi.path :: Nil
    }
    else if(!pi.isWebResource) {
      if(pi.extension == "java") {
        if(setting.javaClassTogether) {
          fromJavaToClassPath
        }
        else {
          sourceToTargetPath
        }
      }
      else if(pi.extension == "class") {
        sourceClassToTargetClassPath
      }
      else {
        if(setting.javaClassTogether) {
          fromJavaToClassPath
        }
        else if(setting.sourceOnly) {
          sourceToTargetPath
        }
        else {
          sourceToTargetPath ++: fromJavaToClassPath
        }
      }
    }
    else {
      sourceToTargetPath
    }
    
    logger.debug(s"new path [$newPaths]")
    newPaths map {p => pi.copy(path = p)}
  }
  
  implicit def toFilenameFilter(fn: (File, String) => Boolean): FilenameFilter = new FilenameFilter() {
    def accept(dir:File, name: String): Boolean = fn(dir, name)
  }
  
  private def findClasses(filename: String, path: List[String], action: String, projName: String)(prb: ProcessResultBuilder): List[ProgInfo] = {
    
    val parent = path.dropRight(1)
    val dir = listToPath(parent).toFile
    dir.exists match {
      case true => {
        val innerClass = filename + "$"
        val innerClasses = dir.listFiles((dir: File, name: String) => name.startsWith(innerClass)).toList
        
        prb.addAddedInnrClasses(innerClasses.size)
        
        val pi = ProgInfo(filename, parent :+ (filename + ".class"), "class", false, action, projName) :: innerClasses.map(c => {
          val wholeFileName = c.getName() 
          val filename = wholeFileName.substring(0, wholeFileName.length - 6)
          ProgInfo(filename, parent :+ wholeFileName, "class", false, action, projName)
        })
        
        logger.debug(s"program info list: $pi")
        prb.addAddedClasses(pi.length)
        pi
      }
      case false => logger.warn(s"directory [${dir.getAbsolutePath}] does not exist, cannot find class"); Nil
    }
    
  }
  
  def listToPath(list: List[String]): Path = list match {
    case Nil => root
    case p :: Nil => Paths.get(p)
    case _ => root.resolve(Paths.get(list.head, list.tail: _*))
  }
}
