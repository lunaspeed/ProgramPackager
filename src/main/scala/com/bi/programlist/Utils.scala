package com.bi.programlist

import java.nio.file.Paths
import org.apache.commons.lang3.StringUtils

object Utils {

  final val root = Paths.get("/")
  final val separator = '\\'
  
  def normalizePath(path: String): String = StringUtils.replaceChars(path, '/', separator)
  
  val toPathList = (normalizePath _).andThen((s) => s.split(separator).toList.dropWhile(_ == ""))
}