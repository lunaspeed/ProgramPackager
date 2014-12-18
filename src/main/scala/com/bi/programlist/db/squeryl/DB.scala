package com.bi.programlist.db.squeryl

import org.squeryl._
import org.squeryl.adapters.H2Adapter
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import java.util.Date
import java.sql.Timestamp

object DB extends Schema {

  def initDB(dbPath: String): Unit = {

    Class.forName("org.sqlite.JDBC")

    SessionFactory.concreteFactory = Some(() =>
      Session.create(
        java.sql.DriverManager.getConnection(s"jdbc:sqlite:$dbPath"), new H2Adapter))
  }

  val nodesCurrents = table[NodesCurrent]("NODES_CURRENT")

}

object NodesCurrent {
  
  def findByLocalRelPath(localRelpath: String): Option[NodesCurrent] = inTransaction {
    DB.nodesCurrents.where(n => n.localRelpath === localRelpath).headOption
  }
}

class NodesCurrent(@Column("wc_id") val wcId: Int, @Column("local_relpath") val localRelpath: String, @Column("op_depth") val opDepth: Int,
  @Column("parent_relpath") val parentRelpath: Option[String], @Column("repos_id") val reposId: Option[Int], @Column("repos_path") val reposPath: Option[String],
  val revision: Option[Int], val presence: Option[String], @Column("moved_here") val movedHere: Option[Int], @Column("moved_to") val movedTo: Option[String],
  val kind: Option[String], val depth: Option[String],
  val checksum: Option[String], @Column("symlink_target") val symlinkTarget: Option[String], @Column("changed_revision") val changedRevision: Option[Int],
  @Column("changed_date") val changedDate: Option[Long], @Column("changed_author") val changedAuthor: Option[String],
  @Column("translated_size") val translatedSize: Option[Int], @Column("last_mod_time") val lastModTime: Option[Long],
  @Column("file_external") val fileExternal: Option[String]) {

  def changedDateAsDate = changedDate.map(longToDate)

  def lastModTimeAsDate = lastModTime map longToDate

  private def longToDate(long: Long) = new Date(long / 1000)
}

object Test {

  
  def main(args: Array[String]): Unit = {

    val t = 1352196881144375L

    val d1 = new Date(t/ 1000)
    println(d1)
    
    val t1 = 1353296881144375L
    val d2 = new Date(t1 / 1000)
    println(d2)
    
    val t1d = t / 1000 / 1000 / 60 / 60 / 24
    val t2d = t1 / 1000 / 1000/ 60 / 60 / 24
    
    println(t2d - t1d)
    
//    DB.initDB("/Users/Lunaspeed/Documents/wc.db")
//    val nc = transaction {
//      DB.nodesCurrents.where(n => n.localRelpath === "PRO/com/bi/wms/pro/conf/web/ProRiskConfAction_zh_TW.utf-8").headOption
//    }
//    
//    nc.foreach(n => println(n.localRelpath))
  }
}