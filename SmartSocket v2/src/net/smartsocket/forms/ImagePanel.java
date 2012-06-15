package net.smartsocket.forms;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {

    private BufferedImage image;

    public ImagePanel() {
       try {                
          image = ImageIO.read(new File("image name and path"));
       } catch (IOException ex) {
            // handle exception...
       }
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null); // see javadoc for more info on the parameters

    }
    
    public void setImageFromBytes( byte[] bytes)
    {
    	 try {
            image = ImageIO.read( new ByteArrayInputStream(bytes) );
            
            repaint();
         } catch (IOException e) {
             e.printStackTrace();
         }
    }

}