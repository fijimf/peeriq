package com.fijimf

import java.sql.DriverManager

trait HasId[A] {
  def getId(a:A):String
}

case class User(id:String, name:String)

object User {
  implicit val userHasId:HasId[User]= new HasId[User] {
    override def getId(a: User): String = a.id
  }
}


object Tester {
  def printLabel[A](a:A)(implicit hia:HasId[A]) = {
    println(hia.getId(a))
  }

  def main(args: Array[String]): Unit = {

    DriverManager.getDriver("jdbc:postgresql://fijimf.com:5432/postgres")
    val u = User("fijimf", "Jim")
    val k = User("keri", "Keri")
    printLabel(u)
    printLabel(k)
  }
}