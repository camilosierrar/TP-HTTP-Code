package http.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * WebServer is a very simple web-server. It handles HTTP request of type
 * GET, POST, PUT, HEAD, and DELETE and return a response to the client if
 * necessary
 * 
 * @author Versmée Erwan, Sierra Camilo
 * @version 1.0
 */
public class WebServer {

  /**
   * Reading the first line of the request headers :
   * this line contains the http method to handle, and a path.
   * 
   * @param in - BufferedInputStream to read the request line from
   * @return String containing the request line
   * @throws IOException
   */
  protected String readRequestLine(BufferedInputStream in) throws IOException {
    String requestLine = "";
    while (in.available() > 0) {
      int cur = in.read();
      requestLine += (char) cur;
      if (requestLine.contains("\r\n")) {
        break;
      }
    }
    return requestLine;
  }

  /**
   * Reading the request headers
   * 
   * @param in - BufferedInputStream to read the headers from
   * @return Map of key - header name and value - header value
   * @throws IOException
   */
  protected Map<String, String> readHeaders(BufferedInputStream in) throws IOException {
    Map<String, String> headerFields = new HashMap<>();
    String header = "";
    
    while (in.available() > 0) {
      int cur = in.read();
      header += (char) cur;
      //A blank line signals the end of the HTTP headers
      if(header.equals("\r\n")) {
        break;
      }
      //The end of the current header
      else if (header.contains("\r\n")) {
        int indexFin = header.indexOf(":");
        if (indexFin != -1)
        headerFields.put(header.substring(0, indexFin).trim(), header.substring(indexFin + 1).trim());
        header="";
      }
    }
    return headerFields;
  }

  /**
   * Reading the request body in case there is one
   * 
   * @param in - BufferedInputStream to read the body from
   * @param headers - the headers of the request
   * @return String containing the body of the request
   * @throws IOException
   */
  protected byte[] readBody(BufferedInputStream in, Map<String, String> headers) throws IOException {
    byte[] body = null;
    if (headers.containsKey("Content-Length")) {
      int nbrCharac = Integer.parseInt(headers.get("Content-Length"));
      body = new byte[nbrCharac];
      while(in.available()>0)
        in.read(body);
    }
    return body;
  }

  /**
   * WebServer constructor.
   * Start the server, accept socket connection and handle different HTTP request
   * Return a Response that contain a status, headers and that may contain a body
   * @param portNumber - the port number to start the server on
   * @throws SocketException
   */
  @SuppressWarnings("unchecked")
  protected void start(int portNumber) throws SocketException{
    ServerSocket s;

    Boolean DEBUG = false;

    System.out.println("Webserver starting up on port " +portNumber);
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(portNumber);
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
        BufferedInputStream in = new BufferedInputStream(remote.getInputStream());
        OutputStream out = remote.getOutputStream();
        
        if(DEBUG)
          System.out.println("Starting...");

        //Informations about the request
        String requestLine = "";
        Map<String,String> headerFields = new HashMap<>();
        byte[] body = null;

        requestLine = readRequestLine(in);
        headerFields = readHeaders(in);
        body = readBody(in, headerFields);

        //For the response
        String http="HTTP/1.0";
        String status = "200";
        Map<String, String> responseHeaders = new HashMap<>();
        String responseBody = "";

        String path ="";
        String httpMethod="";
        String[] params = requestLine.split(" ");
        if(params.length>=2) {
          httpMethod = params[0];
          System.out.println(httpMethod);
          int indexStart = params[1].indexOf("/");
          path = params[1].substring(indexStart+1).trim();
        } else {
          System.out.println("Error 400 : Request sent was not a HTTP request");
          status="400";
        }

        if(DEBUG && !requestLine.isEmpty()) {
          System.out.println("Request Line : " + requestLine);
          System.out.println();
          System.out.println("Headers : \n" + headerFields);
          System.out.println();
          System.out.println("End of headers");
          System.out.println();
          System.out.println("Body : " + ((body!=null)?"is and length : "+body.length:"no body"));
          System.out.println("Body : " +Arrays.toString(body));
          System.out.println();
          System.out.println("Parameters : " + Arrays.toString(params));
          System.out.println("The path : " + path);
        }

        List<Object> infosReponse = new ArrayList<>();
       
        if(requestLine.contains("GET")) {
          if(DEBUG && !requestLine.isEmpty()) System.out.println("Here is a GET");
          infosReponse =handleGet(path, responseHeaders, responseBody);
        }
        else if(requestLine.contains("POST")) {
          if(DEBUG && !requestLine.isEmpty()) System.out.println("Here is a POST");
          infosReponse = handlePost(path, body, responseHeaders, responseBody);
        } 
        else if(requestLine.contains("PUT")) {
          if(DEBUG && !requestLine.isEmpty()) System.out.println("Here is a PUT");
          infosReponse = handlePut(path, body, responseHeaders, responseBody);
        }
        else if(requestLine.contains("HEAD")) {
          if(DEBUG && !requestLine.isEmpty()) System.out.println("Here is a HEAD");
          infosReponse = handleHead(path, responseHeaders, responseBody);
        }
        else if(requestLine.contains("DELETE")) {
          if(DEBUG && !requestLine.isEmpty()) System.out.println("Here is a DELETE");
          infosReponse = handleDelete(path, responseHeaders, responseBody);
        }
        else if(status.equals("200")){
          System.out.println("Error 501 : Not implemented\r\n");
          status="501";
        }

        if(infosReponse.size()==3) {
          responseHeaders = (Map<String,String>) infosReponse.get(0);
          responseBody = (String) infosReponse.get(1);
          status = (String) infosReponse.get(2);
        } else {
          status="500";
        }

        if(DEBUG && !requestLine.isEmpty()) {
          if(!responseBody.isEmpty())
            System.out.println("responseBody : " +responseBody);
          if(!responseHeaders.isEmpty())
            System.out.println("responseHeaders : " +responseHeaders);
        }

        try {
          out.write((http +" "+status+"\r\n").getBytes("UTF-8"));
          for(Map.Entry<String,String> entry: responseHeaders.entrySet()) {
            out.write((entry.getKey()+": "+entry.getValue()+"\r\n").getBytes("UTF-8"));
          }
  
          out.write((""+"\r\n").getBytes("UTF-8"));
          out.write(Base64.getDecoder().decode(responseBody));

          if(DEBUG && !requestLine.isEmpty())
            System.out.println("Sending...");

          out.flush();

          if(DEBUG && !requestLine.isEmpty())
            System.out.println("Sent");
          
        } 
        catch(SocketException ex) {
          // Handle the case where client closed the connection while server was writing to it
          System.out.println("Connection closed while response was sending\r\n");
        }
        catch(Exception ex) {
          ex.printStackTrace();
          try {
            out.write("500 Internal Server Error".getBytes("UTF-8"));
            status="500";
            out.flush();
          } 
          catch (Exception e2) {};
        }
        remote.close();

      } catch (Exception e) {
        System.out.println("Error: " + e);
        e.printStackTrace();
      }
    }
  }

  /**
   * Handle GET HTTP call :
   * 
   * Get the file stored at the path specified in the request line and displays
   * it$ In case the file does not exist, an error 404 message is displayed
   * 
   * @param path - URL of the content to get
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   */
  protected List<Object> handleGet(String path, Map<String,String> responseHeaders, String responseBody) throws IOException {
    String status = "200";
    if (!path.isEmpty()) {
      File file = new File(path);
      if (!file.exists()) {
        responseBody = Base64.getEncoder().encodeToString("HTTP ERROR 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
        status="404";
      }
      else if (file.isDirectory()) {
        responseBody = Base64.getEncoder().encodeToString("HTTP ERROR : The requested file is a directory\r\n".getBytes("UTF-8"));
        status="404";
      }
      else {
        byte[] bytes = Files.readAllBytes(file.toPath());

        responseHeaders.put("Content-Type", Files.probeContentType(file.toPath()));
        responseHeaders.put("Content-Length", Integer.toString(bytes.length));

        responseBody = Base64.getEncoder().encodeToString(bytes);
      }
    } else {
      // Send the HTML page
      responseHeaders.put("Content-Type", "text/html");
      responseHeaders.put("Content-Length", "44");
      responseBody = Base64.getEncoder().encodeToString("<H1>Welcome to the Ultra Mini-WebServer</H1>".getBytes("UTF-8"));
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    resultat.add(status);
    return resultat;
  }

  /**
   * Handle POST HTTP call :
   * 
   * Append content of body to the file stored at the path specified in the
   * request line
   * 
   * @param path - URL of the content to get
   * @param body - the string to append to the file
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   */
  protected List<Object> handlePost(String path, byte[] requestBody, Map<String,String> responseHeaders, String responseBody) throws IOException {
    String status = "200";
    if (!path.isEmpty()) {
      if (path.indexOf("Fichiers/") == 0) {
        File file = new File(path);
        /*FileWriter fr = new FileWriter(file, true);
        BufferedWriter bfr = new BufferedWriter(fr);*/
        /*for (int i = 0; i < requestBody.length; i++) {
          bfr.append(requestBody[i]);
        }*/
        //bfr.close();

        BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(file, file.exists()));
        bof.write(requestBody);

        bof.flush();
        bof.close();
      } else {
        responseBody = Base64.getEncoder().encodeToString("Error 404 : specified path must start with 'Fichiers/'\r\n".getBytes("UTF-8"));
        status="404";
      }
    } else {
      responseBody = Base64.getEncoder().encodeToString("Error 403 : Not authorized to modify welcome page of server\r\n".getBytes("UTF-8"));
      status="403";
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    resultat.add(status);
    return resultat;
  }

  /**
   * Handle PUT HTTP call :
   * 
   * Overwrite content of body to the file stored at the path specified in the
   * request line
   * 
   * @param path - URL of the content to get
   * @param body - the string to write to the file
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   */
  protected List<Object> handlePut(String path, byte[] requestBody, Map<String,String> responseHeaders, String responseBody) throws IOException {
    String status = "204";
    if (!path.isEmpty()) {
      if (path.indexOf("Fichiers/") == 0) {
        File file = new File(path);
        if(!file.exists())
          status="201";
        PrintWriter pw = new PrintWriter(file);
        pw.close();
        OutputStream os = new FileOutputStream(file);
        try {
          os.write(requestBody);
          os.flush();
        } catch(Exception ex) {
          System.out.println("Error 500 : Can't overwrite body to file");
          status="500";
        } finally {
          os.close();
        }

      } else {
        responseBody = Base64.getEncoder().encodeToString("Error 404 : specified path must start with 'Fichiers/'\r\n".getBytes("UTF-8"));
        status="404";
      }
    } else {
      responseBody = Base64.getEncoder().encodeToString("Error 403 : Not authorized to modify welcome page of server\r\n".getBytes("UTF-8"));
      status="403";
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    resultat.add(status);
    return resultat;
  }

  /**
   * Handle Head HTTP call :
   * 
   * Return the Head of the file stored at the path specified in the request line
   * 
   * @param path - URL of the content to get
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   * @throws IOException
   */
  protected List<Object> handleHead(String path, Map<String,String> responseHeaders, String responseBody) throws IOException {
    String status = "200";
    if (path.isEmpty()) {
      responseBody = Base64.getEncoder().encodeToString("HTTP Error 403 : Not authorized to get information on welcome page of server\r\n".getBytes("UTF-8"));
      status="403";
    }
    else {
      File file = new File(path);
      if (!file.exists()) {
        responseBody = Base64.getEncoder().encodeToString("HTTP Error 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
        status="404";
      }
      else {
        byte[] bytes = Files.readAllBytes(file.toPath());
        responseHeaders.put("Content-Type", Files.probeContentType(file.toPath()));
        responseHeaders.put("Content-Length", Integer.toString(bytes.length));
      }
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    resultat.add(status);
    return resultat;
  }

  /**
   * Handle Delete HTTP call :
   * 
   * Return the Head of the file stored at the path specified in the request line
   * 
   * @param path - URL of the content to get
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   * @throws IOException
   */
  protected List<Object> handleDelete(String path, Map<String,String> responseHeaders, String responseBody) throws IOException {
    String status = "200";
    try {
      if (path.isEmpty()) {
        responseBody = Base64.getEncoder().encodeToString("Error 403 : Unhautorized to suppress server's welcome page".getBytes("UTF-8"));
        status="403";
      }
      else {
        File file = new File(path);
        if(!file.getAbsoluteFile().exists()) {
          responseBody = Base64.getEncoder().encodeToString("HTTP Error 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
          status="404";
        }
        else if(!(path.indexOf("Fichiers/")==0)) {
          responseBody = Base64.getEncoder().encodeToString("Error : specified path must start with 'Fichiers/'".getBytes("UTF-8"));
          status="404";
        }
        else {
          if(file.delete())
            responseBody = Base64.getEncoder().encodeToString("File suppressed".getBytes("UTF-8"));
          else {
            responseBody = Base64.getEncoder().encodeToString(("Error 403 : the file couldn't be suppressed").getBytes("UTF-8"));
            status="403";
          }
        } 
      }
    }catch(Exception ex) {
      responseBody = Base64.getEncoder().encodeToString(("Error 500 : Internal Server Error").getBytes("UTF-8"));
      status="500";
    }
    
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    resultat.add(status);
    return resultat;
  }

  /**
   * Start the application.
   * 
   * @param args First command line parameter must the the port number on which the server will start
   * @throws SocketException
   */
  public static void main(String args[]) throws SocketException {
    WebServer ws = new WebServer();
    if(args.length !=1) {
      System.out.println("Usage: java WebServer.java <WebServer port>");
      System.exit(1);
    }
    else {
      int port = Integer.parseInt(args[0]);
      ws.start(port);
    }
  }
}
