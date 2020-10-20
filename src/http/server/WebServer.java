///A Simple Web Server (WebServer.java)

package http.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

  /**
   * Reading the first line of the request headers
   * 
   * @param in
   * @return String containing the request line
   * @throws IOException
   */
  protected String readLigneRequete(BufferedReader in) throws IOException {
    String str = ".";
    String requestLine = "";
    str = in.readLine();
    if (str != null && !str.equals(""))
      requestLine = str;
    return requestLine;
  }

  /**
   * Reading the request headers
   * 
   * @param in
   * @return Map of key - header name and value - header value
   * @throws IOException
   */
  protected Map<String, String> readEnTetes(BufferedReader in) throws IOException {
    String str = ".";
    Map<String, String> headerFields = new HashMap<>();
    while (str != null && !str.equals("")) {
      str = in.readLine();
      if (str != null && !str.equals("")) {
        int indexFin = str.indexOf(":");
        if (indexFin != -1)
          headerFields.put(str.substring(0, indexFin).trim(), str.substring(indexFin + 1).trim());
      }
    }
    return headerFields;
  }

  /**
   * Read the request body in case there is one
   * 
   * @param in
   * @param headers
   * @return String containing the body of the request
   * @throws IOException
   */
  protected String readBody(BufferedReader in, Map<String, String> headers) throws IOException {
    String body = "";
    if (headers.containsKey("Content-Length")) {
      int nbrCharac = Integer.parseInt(headers.get("Content-Length"));
      // System.out.println("Content-Length : "+ nbrCharac);
      int count = 0;
      while (count < nbrCharac) {
        body += (char) in.read();
        count++;
      }
    }
    return body;
  }

  /**
   * WebServer constructor.
   */
  @SuppressWarnings("unchecked")
  protected void start() {
    ServerSocket s;

    Boolean DEBUG = false;

    System.out.println("Webserver starting up on port 3000");
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
        BufferedReader in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
        OutputStream out = remote.getOutputStream();
        
        if(DEBUG)
          System.out.println("On commence");

        // read the data sent. We basically ignore it,
        // stop reading once a blank line is hit. This
        // blank line signals the end of the client HTTP
        // headers.

        //Informations about the request
        String requestLine = "";
        Map<String,String> headerFields = new HashMap<>();
        String body = "";

        requestLine = readLigneRequete(in);
        headerFields = readEnTetes(in);
        body = readBody(in, headerFields);
        
        String path ="";
        String[] params = requestLine.split(" ");
        if(params.length>=2) {
          int indexStart = params[1].indexOf("/");
          path = params[1].substring(indexStart+1).trim();
        } else {
          System.out.println("La requête envoyé n'était pas une requête HTTP");
        }

        if(DEBUG) {
          System.out.println("Ligne d'en-tête : " + requestLine);
          System.out.println();
          System.out.println("En-têtes : \n" + headerFields);
          System.out.println();
          System.out.println("Fin de l'en-tête");
          System.out.println();
          System.out.println("Corps : " + body);
          System.out.println();
          System.out.println("Param : " + Arrays.toString(params));
          System.out.println("Le path : " + path);
        }

        String http="HTTP/1.0";
        String status = "200";
        Map<String, String> responseHeaders = new HashMap<>();
        String responseBody = "";
        List<Object> infosReponse = new ArrayList<>();
       
        if(requestLine.contains("GET")) {
          if(DEBUG) System.out.println("On a un GET");
          infosReponse =handleGet(out, path, responseHeaders, responseBody);
        }
        else if(requestLine.contains("POST")) {
          if(DEBUG) System.out.println("On a un POST");
          infosReponse = handlePost(path, body, out, responseHeaders, responseBody);
        } 
        else if(requestLine.contains("PUT")) {
          if(DEBUG) System.out.println("On a un PUT");
          infosReponse = handlePut(path, body, out, responseHeaders, responseBody);
        }
        else if(requestLine.contains("HEAD")) {
          if(DEBUG) System.out.println("On a un HEAD");
          infosReponse = handleHead(out, path, responseHeaders, responseBody);
        }
        else if(requestLine.contains("DELETE")) {
          if(DEBUG) System.out.println("On a un DELETE");
          infosReponse = handleDelete(out, path, responseHeaders, responseBody);
        }
        else if(requestLine.contains("OPTION")) {
          if(DEBUG) System.out.println("On a un OPTION");
        }

        if(infosReponse.size()==2) {
          responseHeaders = (Map<String,String>) infosReponse.get(0);
          responseBody = (String) infosReponse.get(1);
        }

        if(DEBUG) {
          System.out.println("responseBody : " +responseBody);
          System.out.println("responseHeaders : " +responseHeaders);
        }

        out.write((http +" "+status+"\r\n").getBytes("UTF-8"));
        for(Map.Entry<String,String> entry: responseHeaders.entrySet()) {
          out.write((entry.getKey()+": "+entry.getValue()+"\r\n").getBytes("UTF-8"));
        }

        out.write((""+"\r\n").getBytes("UTF-8"));
        //out.write(responseBody.getBytes("UTF-8"));
        out.write(Base64.getDecoder().decode(responseBody));

        if(DEBUG)
          System.out.println("On envoie tout");

        out.flush();
        remote.close();

        if(DEBUG)
          System.out.println("Envoyé");

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
   * @param out  - OutputStream to write the body
   * @param path - URL of the content to get
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   */
  protected List<Object> handleGet(OutputStream out, String path, Map<String,String> responseHeaders, String responseBody) throws IOException {
    if (!path.isEmpty()) {
      File file = new File(path);
      if (!file.exists()) 
        responseBody = Base64.getEncoder().encodeToString("HTTP ERROR 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
      else if (file.isDirectory()) 
        responseBody = Base64.getEncoder().encodeToString("HTTP ERROR : The requested file is a directory\r\n".getBytes("UTF-8"));
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
   * @param out
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   */
  protected List<Object> handlePost(String path, String body, OutputStream out, Map<String,String> responseHeaders, String responseBody) throws IOException {
    if (!path.isEmpty()) {
      if (path.indexOf("Fichiers/") == 0) {
        File file = new File(path);
        FileWriter fr = new FileWriter(file, true);
        BufferedWriter bfr = new BufferedWriter(fr);
        for (int i = 0; i < body.length(); i++) {
          bfr.append(body.charAt(i));
        }
        bfr.close();
      } else {
        responseBody = Base64.getEncoder().encodeToString("Erreur : le chemin spécifié doit commencer par 'Fichiers/'\r\n".getBytes("UTF-8"));
      }
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
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
   * @param out
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   */
  protected List<Object> handlePut(String path, String body, OutputStream out, Map<String,String> responseHeaders, String responseBody) throws IOException {
    if (!path.isEmpty()) {
      if (path.indexOf("Fichiers/") == 0) {
        File file = new File(path);
        FileWriter fr = new FileWriter(file);
        BufferedWriter bfr = new BufferedWriter(fr);
        for (int i = 0; i < body.length(); i++) {
          bfr.write(body.charAt(i));
        }
        bfr.close();
      } else {
        responseBody = Base64.getEncoder().encodeToString("Erreur : le chemin spécifié doit commencer par 'Fichiers/'\r\n".getBytes("UTF-8"));
      }
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    return resultat;
  }

  /**
   * Handle Head HTTP call :
   * 
   * Return the Head of the file stored at the path specified in the request line
   * 
   * @param out  - OutputStream to write the body
   * @param path - URL of the content to get
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   * @throws IOException
   */
  protected List<Object> handleHead(OutputStream out, String path, Map<String,String> responseHeaders, String responseBody) throws IOException {
    if (path.isEmpty()) 
      responseBody = Base64.getEncoder().encodeToString("HTTP ERROR 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
    else {
      File file = new File(path);
      if (!file.exists())
        responseBody = Base64.getEncoder().encodeToString("HTTP ERROR 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
      else {
        byte[] bytes = Files.readAllBytes(file.toPath());
        responseHeaders.put("Content-Type", Files.probeContentType(file.toPath()));
        responseHeaders.put("Content-Length", Integer.toString(bytes.length));
      }
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    return resultat;
  }

  /**
   * Handle Delete HTTP call :
   * 
   * Return the Head of the file stored at the path specified in the request line
   * 
   * @param out  - OutputStream to write the body
   * @param path - URL of the content to get
   * @param responseHeaders - Map to add response headers to be sent
   * @param responseBody - Body of the response
   * @throws IOException
   */
  protected List<Object> handleDelete(OutputStream out, String path, Map<String,String> responseHeaders, String responseBody) throws IOException {
    if (path.isEmpty()) 
      responseBody = Base64.getEncoder().encodeToString("Vous ne pouvez pas supprimer la page d'accueil du serveur".getBytes("UTF-8"));
    else {
      File file = new File(path);
      if(!file.exists())
        responseBody = Base64.getEncoder().encodeToString("HTTP ERROR 404 : The requested file was not found on the server\r\n".getBytes("UTF-8"));
      else if(!(path.indexOf("Fichiers/")==0))
        responseBody = Base64.getEncoder().encodeToString("Erreur : le chemin spécifié doit commencer par 'Fichiers/'".getBytes("UTF-8"));
      else {
        if(file.delete())
          responseBody = Base64.getEncoder().encodeToString("Fichier supprimé".getBytes("UTF-8"));
        else
          responseBody = Base64.getEncoder().encodeToString(("Erreur : le fichier n'a pas pu être supprimé").getBytes("UTF-8"));
      } 
    }
    List<Object> resultat = new ArrayList<>();
    resultat.add(responseHeaders);
    resultat.add(responseBody);
    return resultat;
  }

  /**
   * Start the application.
   * 
   * @param args
   * Command line parameters are not used.
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start();
  }
}
