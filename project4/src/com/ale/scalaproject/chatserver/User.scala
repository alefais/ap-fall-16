/*
    Alessandra Fais - mat. 481017
    AP - Advanced Programming
    Computer Science and Networking 2016/17
    Homework 4
 */

package com.ale.scalaproject.chatserver

import java.io.PrintWriter

/**
  * The user type.
  * Characterizes the user by its name and the socket used to connect to the server.
  * The user can be in two different states: logged in and logged out.
  * Using this class the server can write to the user.
  */
class User(val name: String) {
  private var connection: Option[PrintWriter] = None

  def isLogged: Boolean = connection.isDefined

  def login(out: PrintWriter): Boolean = if(!isLogged) { connection = Some(out); true } else false

  def logout(): Boolean = if(isLogged) { connection = None; true } else false

  def write(msg: String): Unit = if(isLogged) connection.get.println(msg)

  override def toString: String = {
    "Name: " + name + ", State: " + (if(isLogged) "online" else "offline")
  }
}
