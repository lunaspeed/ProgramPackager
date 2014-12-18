package com.bi.programlist.mega

import com.bi.programlist.excel.{Excel, SheetData}
import scala.util.{Try, Success, Failure}
import com.bi.programlist.db.squeryl.NodesCurrent
import org.apache.commons.lang3.StringUtils
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.collection.immutable.VectorBuilder

object Svn extends LazyLogging {

  case class UpdateReport(total: Int, diffModifier: List[String], tooOld: List[String], notFound: List[String])
  
  def checkSvn(filepath: String, modifier: String, projectRoot: String): Either[Seq[Throwable], UpdateReport] = {
    
    val func = Excel.sheetToList(Excel.simpleRowToList) _ 
    Excel.parse(filepath, func) match {
      case Left(errors) => Left(errors)
      case Right(sheets) => {
        Try(checkAgainstSvn(sheets, modifier, projectRoot)) match {
          case Success(ur) => Right(ur)
          case Failure(e) => Left(List(e))
        }
      }
    }
  }
    
  private def checkAgainstSvn(sheets: List[SheetData[(String, Option[String], Option[String])]], modifier: String, projectRoot: String): UpdateReport = {
    
    var total = 0
    var diffModifier = new VectorBuilder[String]
    var tooOld = new VectorBuilder[String]
    var notFound = new VectorBuilder[String]
    
    for(sheet <- sheets;
      data <- sheet.data) {
      var p = StringUtils.replaceChars(data._1, '\\', '/')
      if(p.startsWith("/")) {
        p = p.substring(1)
      }
      
      val i = p.indexOf('/')
      p = p.substring(i + 1)
      
      NodesCurrent.findByLocalRelPath(p) match {
        case Some(nc) => {
          if(nc.changedAuthor != modifier) {
            diffModifier += data._1
          }
          else {
            nc.lastModTime match {
              case Some(t) if isTooOld(t / 1000) => tooOld += data._1
              case None => logger.warn(s"${data._1} does not have a modify time in svn")
            }
          }
        }
        case None => notFound += data._1
      }
      total += 1
    }
    UpdateReport(total, diffModifier.result.toList, tooOld.result.toList, notFound.result.toList)
  }
  
  
  
  private val toDayFactor = 1000 * 60 * 60 * 24
  private def isTooOld(millis: Long): Boolean = {
    
    val current = System.currentTimeMillis / toDayFactor
    
    current - (millis / toDayFactor) > 2
  }
}