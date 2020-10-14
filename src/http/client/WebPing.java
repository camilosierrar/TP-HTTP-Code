package http.client;

import java.io.*;
import java.net.*;

public class WebPing {
  public static void main(String[] args) throws IOException {
  
    if (args.length != 2) {
      System.err.println("Usage java WebPing <server host name> <server port number>");
      return;
    }	
  
    String httpServerHost = args[0];
    int httpServerPort = Integer.parseInt(args[1]);
    httpServerHost = args[0];
    httpServerPort = Integer.parseInt(args[1]);

    PrintStream socOut = null;
    BufferedReader stdIn = null;
    BufferedReader socIn = null;
    Socket sock = null;

    try {
      InetAddress addr;      
      sock = new Socket(httpServerHost, httpServerPort);
      addr = sock.getInetAddress();
      System.out.println("Connected to " + addr);

      socIn = new BufferedReader(
        new InputStreamReader(sock.getInputStream()));    
      socOut= new PrintStream(sock.getOutputStream());
      stdIn = new BufferedReader(new InputStreamReader(System.in));

      //sock.close();
    } catch (java.io.IOException e) {
      System.out.println("Can't connect to " + httpServerHost + ":" + httpServerPort);
      System.out.println(e);
    }

    

    String line;
    while (true) {
      line=stdIn.readLine();
      if (line.equals(".")) 
        break;
      socOut.println(line);
      //System.out.println("echo: " + socIn.readLine());
    }
    socOut.close();
    socIn.close();
    stdIn.close();
    sock.close();
  }
}