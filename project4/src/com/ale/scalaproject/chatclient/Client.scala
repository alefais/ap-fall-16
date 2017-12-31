/*
    Alessandra Fais - mat. 481017
    AP - Advanced Programming
    Computer Science and Networking 2016/17
    Homework 4
 */

package com.ale.scalaproject.chatclient

import java.io._
import java.net.{Socket, SocketException, UnknownHostException}
import java.util.concurrent.{ExecutorService, Executors, RejectedExecutionException, TimeUnit}

/**
  * The client of the chat.
  */
object Client {

  /**
    * The main of the client.
    * Creates and starts two threads: one for reading from the stdin the requests
    * of the user (and send them to the server writing on the socket); the other
    * for reading from the socket the messages of the server (and print them to the
    * user on the stdout).
    * @param args command line arguments (the port and the address can be specified here)
    */
  def main(args: Array[String]): Unit = {
    val threadPool: ExecutorService = Executors.newFixedThreadPool(2)
    var port: Int = 40137
    var address: String = "localhost"
    if(args.isDefinedAt(0) && args(0).toInt > 1) port = args(0).toInt
    if(args.isDefinedAt(1)) address = args(1)
    var sock: Socket = null
    var in: BufferedReader = null
    var out: PrintWriter = null
    var stdin: BufferedReader = null
    var exit: Boolean = false

    try {
      sock = new Socket(address, port)
      in = new BufferedReader(new InputStreamReader(sock.getInputStream))
      out = new PrintWriter(sock.getOutputStream, true)
      stdin = new BufferedReader(new InputStreamReader(System.in))

      threadPool.execute( () => {
        try {
          var message: String = ""
          while (message != null ) {
            if (!(message equals "")) println(message)
            message = in.readLine()
          }
          in.close()
          exit = true
          stdin.close()
        } catch {
          case e: IOException => println("Error: "); e.printStackTrace()
        }
      })

      threadPool.execute( () => {
        try {
          while (!exit) {
            if (stdin.ready()) {
              val message = stdin.readLine()
              if (message != null && !(message equals "")) {
                out.println(message)
              }
            }
          }
        } catch {
          case e: IOException => println("Error: "); e.printStackTrace()
        }
      })

    } catch {
      case _: UnknownHostException => println("Error: 'invalid address'")
      case _: RejectedExecutionException => println("Error: 'closing threads'"); sock.close()
      case _: SocketException => println("Error: 'server disconnected'")
      case _: IOException => println("Closing chat");
      case e: Exception => e.printStackTrace()
    }

    threadPool.shutdown()
    threadPool.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS)
    println("[CLIENT] Input and Output threads terminated")
  }
}
