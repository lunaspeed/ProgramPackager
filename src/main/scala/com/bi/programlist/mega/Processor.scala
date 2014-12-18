package com.bi.programlist.mega

import java.io.IOException
import java.nio.file.{Files, Path, SimpleFileVisitor, FileVisitResult}
import java.nio.file.attribute.BasicFileAttributes
import com.bi.programlist.{GenerateInfo, ProcessResult}
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.util.{Try, Success, Failure}
import java.util.concurrent.atomic.AtomicInteger
import com.bi.programlist.Utils

protected[mega] class ProcessResultBuilder {
  
  private val initialFiles = new AtomicInteger
  private val finalFiles = new AtomicInteger
  private val addedClasses = new AtomicInteger
  private val addedInnerClasses = new AtomicInteger
  private val fileCounts = new scala.collection.mutable.HashMap[String, Int]
  private val filesCopied = new AtomicInteger
  private val filesNotFound = new AtomicInteger
  private val filesNotCopied = new AtomicInteger
  
  private val start = System.currentTimeMillis
  def toResult = ProcessResult(initialFiles.get, finalFiles.get, addedClasses.get, addedInnerClasses.get, fileCounts.toMap, filesCopied.get, filesNotFound.get, filesNotCopied.get, System.currentTimeMillis - start)
  
  def addInitialFiles = add(initialFiles) _
  def addFinalFiles = add(finalFiles) _
  def addAddedClasses = add(addedClasses) _
  def addAddedInnrClasses = add(addedInnerClasses) _
  def addFilesCopied = add(filesCopied) _
  def addFilesNotFound = add(filesNotFound) _
  def addFilesNotCopied = add(filesNotCopied) _
  
  private def add(ai: AtomicInteger)(i: Int) {

    if(i == 1) ai.incrementAndGet()
    else ai.addAndGet(i)
  }
}

object Processor extends LazyLogging {

  def process(filepath:String, generateInfo: GenerateInfo, toFilepath: String, cleanGeneratedFiles: Boolean = true): Either[Seq[Throwable], ProcessResult] = {
    
    val prb = new ProcessResultBuilder
    logger.info("start parsing excel")
    val sheets = Parser.parse(filepath, generateInfo, Some("MEGA"))(prb)
    
    logger.info("excel parsed")
    
    sheets match {
      case Left(errors) => Left(errors)
      case Right(data) => {
        Try({
          if(cleanGeneratedFiles) {
            cleanFilepath(Utils.toPathList(toFilepath))
            cleanFilepath(generateInfo.setting.projectTargetRoot)
          }
          
          Parser.toFile(toFilepath, generateInfo, data)(prb)
        }) match {
          case Success(_) => Right(prb.toResult)
          case Failure(t) => Left(t :: Nil)
        }
      }
    }
  }

  private def cleanFilepath(filepath: List[String]): Unit = {

    val directory = Parser.listToPath(filepath)
    if (directory.toFile().exists) {
      Files.walkFileTree(directory, new SimpleFileVisitor[Path]() {

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
//          logger.info(s"file deleting [${file.toString()}]")
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
          
          if(directory != dir) {//if its a directory, only clean and not delete itself
//            logger.info(s"dir deleting [${dir.toString()}]")
            Files.delete(dir)
          }
          FileVisitResult.CONTINUE
        }

      })
    }
  }
  
//  def main(args: Array[String]): Unit = {
//    
//    val projectRoot: String = "/Users/Lunaspeed/Projects/BI/MEGA-SRC" 
//    val relativeClassPath: String = "WEB-INF/classes"
//    val projectTargetRoot: String = "/Users/Lunaspeed/Documents/BI/72/target"
//    val relativeClassTargetPath: String = "WEB-INF/classes"
//    val javaClassTogether: Boolean = true
//    val packProgram: Boolean = true
//    val pathStartsWithProjectName: Boolean = true
//    val userName = "Steven"
//    val action = "新增"
//    val replaceWebContent = true
//    val gi = new GenerateInfo(projectRoot, relativeClassPath, projectTargetRoot, relativeClassTargetPath, replaceWebContent, true, javaClassTogether, packProgram, pathStartsWithProjectName, userName, action, "", "", "WebContent")
//    val filename = "/Users/Lunaspeed/Documents/BI/72/test.xlsx"
//    val toFilename = "/Users/Lunaspeed/Documents/BI/72/test-result.xlsx"
//    process(filename, gi, toFilename)
//    //cleanFilepath(Parser.toPathList(projectTargetRoot))
//  }
}