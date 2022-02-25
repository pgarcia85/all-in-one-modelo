package com.medvida.service.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import com.medvida.service.ImagenService;

public class ImagenServiceImpl implements ImagenService{

	@Override
	public BufferedImage createImage(String text) {
		  // Obtener el rectángulo completo del estilo de fuente aplicado a str
    	Font font = new Font (" ", Font.PLAIN, 10);
        int[] arr = getWidthAndHeight(text, font);
        int width = arr[0];
        int height = arr[1];
        // Crea una imagen
     // Crear un lienzo de imagen
        BufferedImage image = new BufferedImage(width, height,BufferedImage.TYPE_INT_BGR); 
        ByteArrayOutputStream outFile = new ByteArrayOutputStream();
        Graphics g =  image.getGraphics();
                g.setColor (Color.WHITE); // Rellena toda la imagen con blanco primero, que es el fondo
                g.fillRect (0, 0, width, height); // Dibuja un área rectangular para escribir texto en el área rectangular
                g.setColor (Color.black); // Reemplazar con negro para facilitar la escritura de texto
                g.setFont (font); // Establecer la fuente del pincel
                g.drawString (text, 0, font.getSize ()); // Dibuja una línea de cadena
  
       return image;
	}
	
	
	 private static int[] getWidthAndHeight(String text, Font font) {
	        Rectangle2D r = font.getStringBounds(text, new FontRenderContext(AffineTransform.getScaleInstance(1, 1), false, false));
	        
	        int unitHeight = (int) Math.floor(r.getHeight());// 
	        // Obtenga el ancho de toda la cadena usando el estilo de fuente. Use +1 después del redondeo para asegurarse de que el ancho pueda acomodar definitivamente esta cadena como el ancho de la imagen
	        int width = (int) Math.round(r.getWidth()) + 1;
	        // La altura de un solo carácter +3 para garantizar que la altura pueda acomodar absolutamente la cadena como la altura de la imagen
	        int height = unitHeight + 3;
	        System.out.println("width:" + width + ", height:" + height);
	        return new int[]{width, height};
	    }


	@Override
	public BufferedImage rotateImage(BufferedImage image) {

        BufferedImage output = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());

        AffineTransform affineTransform = rotateImageCounterClockwise(image);
        AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
        affineTransformOp.filter(image, output);

       return output;
	}
	
	 private  AffineTransform rotateImageCounterClockwise(BufferedImage image) {

	    	int imageWidth = image.getWidth();
	        int imageHeight = image.getHeight();

	        AffineTransform affineTransform = new AffineTransform();
	        affineTransform.rotate(-Math.PI / 2, imageWidth / 2, imageHeight / 2);

	        double offset = (imageWidth - imageHeight) / 2;
	        affineTransform.translate(-offset, -offset);
	        
	        return affineTransform;
	    }

}
