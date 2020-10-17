///A Simple Web Server (WebServer.java)

package http.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * 
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 * 
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

  protected void lireEnTete() {
      
  }

  /**
   * WebServer constructor.
   */
  protected void start() {
    ServerSocket s;

    System.out.println("Webserver starting up on port 80");
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(3000);
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return;
    }

    System.out.println("Waiting for connection");
    for (;;) {
      try {
        // wait for a connection
        Socket remote = s.accept();
        // remote is now the connected socket
        System.out.println("Connection, sending data.");
        BufferedReader in = new BufferedReader(new InputStreamReader(
            remote.getInputStream()));
        PrintWriter out = new PrintWriter(remote.getOutputStream());

        // read the data sent. We basically ignore it,
        // stop reading once a blank line is hit. This
        // blank line signals the end of the client HTTP
        // headers.
        String str = ".";
        String requestLine = "";
        Map<String,String> headerFields = new HashMap<>();
        String body = "";

        while (str != null && !str.equals("")) {
          str = in.readLine();
          if(requestLine.equals(""))
            requestLine = str;
          else {
            int indexFin = str.indexOf(":");
            if(indexFin != -1)
              headerFields.put(str.substring(0, indexFin).trim(), str.substring(indexFin+1).trim());
          }
        }

        if(headerFields.containsKey("Content-Length")) {
          int nbrCharac = Integer.parseInt(headerFields.get("Content-Length"));
          int count = 0;
          int read;
          while( (read = in.read()) != -1 && count < nbrCharac) {
            body += (char) read;
          }
        }

        //System.out.println("Ligne d'en-tête : " + requestLine);

        /*System.out.println();
        System.out.println("En-têtes : \n" + headerFields);

        System.out.println();
        System.out.println("Fin de l'en-tête");
        System.out.println();

        System.out.println("Corps : " + body);*/

        if(requestLine.contains("GET")) {
          //System.out.println("On a un GET");
          String[] params = requestLine.split(" ");
          //System.out.println("Param : " + Arrays.toString(params));
          int indexStart = params[1].indexOf("/");
          String path = params[1].substring(indexStart+1).trim();
          if (!path.isEmpty()) {
            File file = new File(path);
            if(!file.exists())
              out.write("HTTP ERROR 404 : The requested file was not found on the server\n");
            else {
              FileReader fr = new FileReader(file);
              BufferedReader bfr = new BufferedReader(fr);
              String line;
              while ((line = bfr.readLine()) != null) {
                  out.write(line);
              }
              bfr.close();
            }
          }
        }

        // Send the response
        // Send the headers
        //out.println("HTTP/1.0 200 OK");
        //out.println("Content-Type: text/html");
        //out.println("Server: Bot");
        // this blank line signals the end of the headers
        out.println("");
        // Send the HTML page
        out.println("<H1>Welcome to the Ultra Mini-WebServer</H1>");
        out.flush();
        remote.close();

      } catch (Exception e) {
        System.out.println("Error: " + e);
      }
    }
  }

  /**
   * Start the application.
   * 
   * @param args
   *            Command line parameters are not used.
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start();
  }
}
