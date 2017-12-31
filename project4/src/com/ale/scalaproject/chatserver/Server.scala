/*
    Alessandra Fais - mat. 481017
    AP - Advanced Programming
    Computer Science and Networking 2016/17
    Homework 4
 */

package com.ale.scalaproject.chatserver

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket, SocketException}
import java.util.concurrent._

import scala.collection.JavaConverters._
import scala.collection.concurrent.Map
import scala.collection.mutable.ArrayBuffer

/**
  * The server of the chat.
  */
object Server {

  /**
    * The thread manager of the connection of each client.
    * @param socket the socket that connects the client and the server
    */
  class ClientUser(val socket: Socket, val id: Int) extends Runnable {
    private val welcome0 = "Welcome to the chat:\n" +
      "you can now register/login and start exchanging messages with your friends.\n"
    private val welcome1 = "\nCommands:\n" +
      "- OPERATIONS TO THE SERVER:\n" +
      "\t/register username\t\tRegistration\n" +
      "\t/login username\t\t\tLogin\n" +
      "\t/logout [username: optional]\tLogout\n" +
      "\t/join room\t\t\tJoin a room (if the room doesn't exist create it first)\n" +
      "\t/leave room\t\t\tLeave a room (if you already are a member)\n" +
      "\t/members room\t\t\tView the users of a room of which you already are a member\n" +
      "- CHAT:\n" +
      "\t@username\t\t\tSend a private message to another user\n" +
      "\t#room\t\t\t\tSend a message to all the users inside the room\n" +
      "- OTHERS:\n" +
      "\t--help\t\t\t\tShow the commands panel\n" +
      "\t:quit\t\t\t\tExit the chat\n"
    private val out = new PrintWriter(socket.getOutputStream, true)

    var currentUser: Option[User] = None // the user is the state of the thread

    /**
      * Defines the protocol for the interaction between the client and the server.
      * @param m the message received from the client
      */
    def manageMessages(m: String): Unit = {
      val ack = new StringBuilder()

      /**
        * Manages the requests addressed to the server.
        * A request can be of type LOGIN, LOGOUT, JOIN, LEAVE, MEMBERS.
        * @param op the type of request
        * @param s the user name or the room name
        */
      def serverOperations(op: String, s: Option[String]): Unit = {
        op match {
          case "register" => // manage simple registration of the username s
            if(s.isDefined) {
              if(currentUser.isEmpty) {
                val username = s.get.split(" ", 2)(0)
                users.get(username) match {
                  case Some(_) => ack.append("this username is already taken.")
                  case None =>
                    val u = new User(username)
                    u.login(out)
                    users.put(username, u)
                    currentUser = Some(u)
                    ack.append("successful registration and login.")
                    println("User @" + username + " registered.")
                }
              } else {
                ack.append("you are already registered and logged in.")
              }
            } else {
              otherOptions("input error")
            }
          case "login" => // manage simple login of the username s
            if(s.isDefined) {
              if(currentUser.isEmpty) {
                val username = s.get.split(" ", 2)(0)
                users.get(username) match {
                  case Some(u) =>
                    if(u.login(out)) {
                      currentUser = Some(u)
                      ack.append("successful login.")
                      println("User @" + username + " logged in.")
                    } else {
                      ack.append("you are already logged in.")
                    }
                  case None => ack.append("please register first.")
                }
              } else {
                ack.append("you are already logged in.")
              }
            } else {
              otherOptions("input error")
            }
          case "logout" => // manage logout of the username s
            if(s.isEmpty ||
              (s.isDefined && currentUser.isDefined && (s.get.split(" ", 2)(0) equals currentUser.get.name))) {
              if (currentUser.isDefined) {
                users.get(currentUser.get.name) match {
                  case Some(u) =>
                    u.logout()
                    ack.append("successful logout.")
                    println("User @" + u + " logged out")
                  case None => ack.append("ERROR 'user not found'.")
                }
                currentUser = None
              } else {
                ack.append("please login first.")
              }
            } else {
              otherOptions("input error")
            }
          case "join" => // manage subscription to the room s
            if(s.isDefined) {
              if (currentUser.isDefined) {
                val me = currentUser.get.name
                val roomname = s.get.split(" ", 2)(0)
                rooms.get(roomname) match {
                  case Some(r) => // the room already exists
                    if (!r.contains(me)) {
                      r += me
                      ack.append("successfully joined the room.")
                      println("User @" + me + " joined the room #" + roomname)
                      for (u <- r if !u.equals(me)) {
                        users.get(u) match {
                          case Some(u1) => u1.write("[#" + roomname + " @" + me + "] User @" + me + " just joined the room #" + roomname)
                          case None => ack.append("ERROR 'user not found'.")
                        }
                      }
                    } else {
                      ack.append("you already joined this room.")
                    }
                  case None => // the room has to be created
                    var a: ArrayBuffer[String] = new ArrayBuffer[String]()
                    a += me
                    rooms.put(roomname, a)
                    ack.append("successfully created and joined room.")
                    println("User @" + me + " created and joined the room #" + roomname)
                }
              } else {
                ack.append("please login first.")
              }
            } else {
              otherOptions("input error")
            }
          case "leave" => // manage leaving the room s
            if(s.isDefined) {
              if(currentUser.isDefined) {
                val me = currentUser.get.name
                val roomname = s.get.split(" ", 2)(0)
                rooms.get(roomname) match {
                  case Some(r) =>
                    if(r.contains(me)) {
                      r -= me
                      ack.append("successfully leaved room.")
                      println("User @" + me + " leaved the room #" + roomname)
                      for (u <- r if !u.equals(me)) {
                        users.get(u) match {
                          case Some(u1) => u1.write("[#" + roomname + " @" + me + "] User @" + me + " just leaved the room #" + roomname)
                          case None => ack.append("ERROR 'user not found'.")
                        }
                      }
                    } else {
                      ack.append("you are not a member of this room.")
                    }
                  case None => ack.append("ERROR 'room not found'.")
                }
              } else {
                ack.append("please login first.")
              }
            } else {
              otherOptions("input error")
            }
          case "members" => // show the members of the room s (only if the user already joined it)
            if(s.isDefined) {
              if(currentUser.isDefined) {
                val me = currentUser.get.name
                val roomname = s.get.split(" ", 2)(0)
                rooms.get(roomname) match {
                  case Some(r) =>
                    if(r.contains(me)) {
                      val temp = new StringBuilder("[members of #").append(roomname).append("] -> ")
                      r.foreach(m => temp.append(m).append(" "))
                      ack.append(temp.toString())
                    }
                    else {
                      ack.append("you are not a member of this room.")
                    }
                  case None => ack.append("ERROR 'room not found'.")
                }
              } else {
                ack.append("please login first.")
              }
            } else {
              otherOptions("input error")
            }
          case _ => ack.append("please insert a valid command.")
        }
      }

      /**
        * Manages the operation of sending a message to another user.
        * @param dest the user receiver of the message
        * @param message the message to be delivered
        */
      def sendToUser(dest: String, message: Option[String]): Unit = {
        if(message.isDefined) {
          if (currentUser.isDefined) {
            val me = currentUser.get.name
            users.get(dest) match {
              case Some(u) =>
                if (u.isLogged) {
                  u.write("[@" + me + "] " + message.get)
                  //ack.append("successful delivered message.")
                  println("User @" + me + " wrote to user @" + u.name)
                } else {
                  ack.append("the receiver is not logged in.")
                }
              case None => ack.append("ERROR 'user not found'.")
            }
          } else {
            ack.append("please login first.")
          }
        } else {
          ack.append("ERROR 'empty message'.")
        }
      }

      /**
        * Manages the operation of sending a message to a room of users.
        * @param room the room receiver of the message
        * @param message the message to be delivered
        */
      def sendToRoom(room: String, message: Option[String]): Unit = {
        if(message.isDefined) {
          if (currentUser.isDefined) {
            rooms.get(room) match {
              case Some(r) =>
                val me = currentUser.get.name
                if (r.contains(me)) {
                  if (r.length == 1) ack.append("you are the only member of this room.")
                  else {
                    for (u <- r if !u.equals(me)) {
                      users.get(u) match {
                        case Some(u1) => u1.write("[#" + room + " @" + me + "] " + message.get)
                        case None => ack.append("ERROR 'user not found'.")
                      }
                    }
                    println("User @" + me + " wrote to room #" + room)
                  }
                } else {
                  ack.append("please join the room first.")
                }
              case None => ack.append("ERROR 'room not found'.")
            }
          } else {
            ack.append("please login first.")
          }
        } else {
          ack.append("ERROR 'empty message'.")
        }
      }

      /**
        * Manages the special commands HELP and QUIT and the
        * case of an unmatched command.
        * @param str the command inserted by the user
        */
      def otherOptions(str: String): Unit = {
        str match {
          case "--help" => out.println(welcome1)
          case ":quit" =>
            try {
              socket.close()
            } catch {
              case _: SocketException => println("The socket of user @" + currentUser + " has been closed")
              case unknown: Exception => println("Exception captured: "); unknown.printStackTrace()
            } finally {
              if (currentUser.isDefined) {
                users.get(currentUser.get.name) match {
                  case Some(u) =>
                    u.logout()
                    ack.append("successful logout and quit.")
                    println("User @" + u + " logged out and quit")
                  case None => ack.append("ERROR 'user not found'.")
                }
                currentUser = None
              }
            }
          case "+users" => // for debugging purposes
            val temp = new StringBuilder("Users registered to the chat:\n")
            users.foreach { case (n, u) => temp.append("[").append(n).append("] -> ").append(u).append("\n") }
            ack.append(temp.toString())
          case "+rooms" =>  // for debugging purposes
            val temp = new StringBuilder("Rooms created in the chat:\n")
            rooms.foreach {
              case (r, members) =>
                temp.append("[").append(r).append("] -> ")
                members.foreach(m => temp.append(m).append(" "))
                temp.append("\n")
            }
            ack.append(temp.toString())
          case _ => ack.append("please insert a valid command.")
        }
      }

      if(m != null && !m.isEmpty) {
        val parts: Array[String] = m.split(" ", 2)
        val start = parts(0)(0)
        val rest = parts(0).drop(1)
        val body: Option[String] = if(parts.length == 2) Some(parts(1)) else None
        start match {
          case '/' => serverOperations(rest, body) // message to server (body contains the username or the room name)
          case '@' => sendToUser(rest, body) // message to user (body contains the message)
          case '#' => sendToRoom(rest, body) // message to a room (body contains the message)
          case _ => otherOptions(parts(0))
        }
      } else {
        ack.append("please insert a valid command.")
      }
      if(ack.nonEmpty) out.println("[SERVER] " + ack.toString())
    }

    /**
      * The run method of the client manager thread.
      */
    override def run(): Unit = {
      println("[THREAD " + id + "] Starting managing the connection")
      out.println(welcome0 + welcome1)
      val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
      var quit: Boolean = false
      while(!quit) {
        try {
          manageMessages(in.readLine())
        } catch {
          case _: SocketException => quit = true; println("The socket has been closed")
          case unknown: Exception => quit = true; println("Exception captured: "); unknown.printStackTrace()
        }
      }
      println("[THREAD " + id + "] Killed")
    }
  }


  val users: Map[String, User] = new ConcurrentHashMap[String, User]().asScala
  val rooms: Map[String, ArrayBuffer[String]] = new ConcurrentHashMap[String, ArrayBuffer[String]]().asScala

  /**
    * The main of the server.
    * Accepts the connections of the clients and for each one creates and starts
    * a thread that manages the interactions of the client.
    * @param args command line arguments (the port can be specified here)
    */
  def main(args: Array[String]): Unit = {
    val threadPool: ExecutorService = Executors.newFixedThreadPool(100)
    var port: Int = 40137
    if(args.isDefinedAt(0) && args(0).toInt > 1) port = args(0).toInt
    val ss = new ServerSocket(port)
    val address = ss.getInetAddress
    var exit: Boolean = false
    threadPool.execute( () => {
      var id: Int = 0
      while(!exit) {
        val sock = ss.accept()
        println("[ACCEPTOR THREAD] Accepting new connection...")
        try {
          threadPool.execute(new ClientUser(sock, id)) // create a thread for each new connected user
          id = id + 1
        } catch {
          case _: RejectedExecutionException => println("[ACCEPTOR THREAD] Closing..."); sock.close()
        }
      }
    })
    println("Server started! Type 'exit' to quit.")

    val in: BufferedReader = new BufferedReader(new InputStreamReader(System.in))
    while(!exit) {
      val reading = in.readLine()
      if(reading equals "exit") exit = true
    }

    new Socket(address, port).close() // shut down the server (wait for each client thread to terminate)
    threadPool.shutdown()
    threadPool.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS)
    println("[MAIN THREAD] All threads terminated")
  }
}
