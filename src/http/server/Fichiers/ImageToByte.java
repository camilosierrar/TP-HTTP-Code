package http.server.Fichiers;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;

public class ImageToByte {
   public static void main(String args[]) throws Exception{
      BufferedImage bImage = ImageIO.read(new File("Apple.png"));
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ImageIO.write(bImage, "png", bos );
      byte [] data = bos.toByteArray();
      System.out.println(Arrays.toString(data) + " length : " +data.length);
   }
}
