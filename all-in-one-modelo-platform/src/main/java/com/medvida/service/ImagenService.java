package com.medvida.service;

import java.awt.image.BufferedImage;

public interface ImagenService {
	
	/**
	 * Este metodo genera una imagen a partir de un texto
	 * 
	 * @param text
	 * @return
	 */
	BufferedImage createImage(String text);
	
	/**
	 * Este metodo rota una imagen en sentido antihorario
	 * 
	 * @param image
	 * @return
	 */
	BufferedImage rotateImage(BufferedImage image);

}
