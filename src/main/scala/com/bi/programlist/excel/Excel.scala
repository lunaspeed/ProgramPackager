package com.bi.programlist.excel

import org.apache.poi.ss.usermodel._
import scala.collection.JavaConversions._
import org.apache.poi.openxml4j.opc.OPCPackage
import resource._
import org.apache.poi.xssf.usermodel._
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.File
import com.typesafe.scalalogging.slf4j.LazyLogging
import java.nio.file.Paths
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.FileInputStream

object Excel extends LazyLogging {
  
  def parse[A](filepath: String, sheetFunc: Sheet => List[A]): Either[Seq[Throwable], List[SheetData[A]]] = 
    (if(filepath.endsWith(".xlsx")) {
      managed(OPCPackage.open(filepath))            
      .map(new XSSFWorkbook(_))
      .map(_.iterator: Iterator[Sheet])
    }
    else {
      managed(new FileInputStream(filepath))
      .map(new HSSFWorkbook(_))
      .map(wb => {
        (0 to (wb.getNumberOfSheets() - 1) map {wb.getSheetAt(_)}).iterator
      })
    })
    .map(_.map(s => SheetData(s.getSheetName(), sheetFunc(s))).toList )
    .either
  
  def sheetToList[A](converter: Row => A)(sheet: Sheet): List[A] = {
    val it: scala.collection.Iterator[Row] = sheet.iterator
    
    (it map converter) 
    .toList
  }
  
  def simpleRowToList(row: Row): (String, Option[String], Option[String]) = {
    val fp = row.getCell(0).getStringCellValue()
    val action = Option(row.getCell(1)).map(_.getStringCellValue())
    val proj = Option(row.getCell(2)).map(_.getStringCellValue())
   //logger.info(fp + ":" + action.getOrElse("-none-"))
    (fp, action, proj)
  }
    
//  def sheetToList(sheet: Sheet): List[(String, Option[String])] = {
//    
//    val it: scala.collection.Iterator[Row] = sheet.iterator
//    
//    it.map(r => {
//      val fp = r.getCell(0).getStringCellValue()
//      val proj = Option(r.getCell(1)).map(_.getStringCellValue())
//      (fp, proj)
//    })
//    .toList
//  }
  
  
  def toWorkbook[A](data: Seq[SheetData[A]], toList: A => List[Option[String]], writeToRow: (Row, List[Option[String]]) => Unit, headers: List[String] = Nil): Workbook = {
    
//    val template = Paths.get("template.xlsx").toFile
//    val wb = 
//      if(template.exists) {
//        
//        managed(OPCPackage.open(template))
//          .map(new XSSFWorkbook(_)).either match {
//            case Left(errors) => logger.warn("error loading template", errors(0)); new XSSFWorkbook
//            case Right(wb) => wb
//          }
//          
//      } 
//      else new XSSFWorkbook
    
    val wb = new XSSFWorkbook

    data.foreach(sd => {
      val sheet = wb.createSheet(sd.name)//Option(wb.getSheet(sd.name)).getOrElse(wb.createSheet(sd.name))
      val rowOffset = headers match {
        case Nil => 0
        case _ => {
          val row = sheet.createRow(0)
          headers.zipWithIndex.foreach {
            case (h, i) => row.createCell(i).setCellValue(h)
          }
          1
        }
      }
      
      sd.data.zipWithIndex.foreach {
        case (d, i) => {
          val row = sheet.createRow(i + rowOffset)
          val dataList = toList(d)
          writeToRow(row, dataList)
        }
      }
    })
    
    wb
  }
  
  val simpleToRow: (Row, List[Option[String]]) => Unit = (r, l) => l.zipWithIndex.foreach({
    case (Some(s), i) => r.createCell(i).setCellValue(s)
    case _ =>
  })
  
  def write[A](filepath: String, data: Seq[SheetData[A]], toList: A => List[Option[String]], writeToRow: (Row, List[Option[String]]) => Unit, headers: List[String] = Nil): Unit = {
    
    val wb = toWorkbook(data, toList, writeToRow, headers)
    
    createDirectory(filepath)
    
    for(fos <- managed(new FileOutputStream(filepath));
        bos <- managed(new BufferedOutputStream(fos))) {
      wb.write(bos)
    }
    
  }
  
  private def createDirectory(filepath: String) {
    val f = new File(filepath)
    val p = f.getParentFile()
    if(!p.exists) {
      if(!p.mkdirs()) {
        throw new RuntimeException(s"Cannot create directory: ${p.getCanonicalPath()}")
      }
    }
      
  }
}

case class SheetData[A](name: String, data: List[A])
//case class ProgInfo(filename: String, path: String, ftype: String, project: String)