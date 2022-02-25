package com.medvida.webscript;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.pdfbox.examples.util.PDFMergerExample;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.medvida.service.IReportsService;

import fr.opensagres.xdocreport.core.EncodingConstants;
import fr.opensagres.xdocreport.document.images.ByteArrayImageProvider;
import fr.opensagres.xdocreport.document.images.IImageProvider;

public class GenerarCaratulaWebScript extends DeclarativeWebScript{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalizacionZIPWebScript.class);

	private ServiceRegistry serviceRegistry;
	
	private MimetypeService mimetypeService;
	
	private transient IReportsService reportsService;
	
	
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		long identificador=System.currentTimeMillis();
		//hay unejemplo de crear plantilla en artica GenerateDocumentFromTemplate
		Map<String, Object> model = new HashMap<String, Object>();
		
		String idContrato = req.getParameter("id_contrato");
		//obtener el json que se envia con los datos de la caratula//
		JSONObject map = (JSONObject) req.parseContent();	
		String idPlantilla=map.getString("id_plantilla");
		
		String uuid= null;
		
		//buscar el documento al que se añade la caratura por su id de contrato
		StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");		
		ResultSet rs = serviceRegistry.getSearchService().query(storeRef, SearchService.LANGUAGE_FTS_ALFRESCO,
				"TYPE:\"mvd:factura\" AND =mvd:idfactura:\""+idContrato+"\"");
		rs.length();
		NodeRef nodoContratoAniadirCaratula = null;

		try {
			if (rs.length() == 0) {
				throw new AlfrescoRuntimeException("No encuentra el nodo por ese identificador");
			}
			//Obtener el NodeRef del documento que debe llevar la caratula
			nodoContratoAniadirCaratula = rs.getNodeRef(0);
			
			//Generar el noderef de la plantilla el idenfificador de la plantilla 
			NodeRef nodoPlantilla = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, idPlantilla);
			Map<String, Object> templateProperties = generarMapaPlantilla(map);
			//Con reportService obtemos un stream con el contenido del documento creado a partir de la plantilla
			// como queremos que nos genere pdf ponemos true
			InputStream contenidoDocumento = reportsService.generateReport(nodoPlantilla, templateProperties, true);

			//obtener el nodo carpeta donde se va a guardar la plantilla
			StoreRef storeRef2 = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
			ResultSet rs2 = serviceRegistry.getSearchService().query(storeRef2, SearchService.LANGUAGE_LUCENE,
					"@cm\\:name:\"" + "Documentos con caratula" + "\"");
			NodeRef nodoCarpeta = rs2.getNodeRef(0);
			
			//Guardar el documento en el repo. Le pasamos la carpeta donde se va a guardar el documento, su contenido y si es o no un pdf
			NodeRef caratulaDocumento = generarDocumentoDePlantilla(contenidoDocumento, nodoCarpeta, true, identificador);
			uuid=caratulaDocumento.getId();
			
			//Ahora se trata de unir el archivoGeneradoConPlantilla y nodoContratoAniadirCaratula
			ContentReader readerArchivoGeneradoConPlantilla = serviceRegistry.getFileFolderService().getReader(caratulaDocumento);			
			InputStream archivoGeneradoConPlantillaStream= readerArchivoGeneradoConPlantilla.getContentInputStream();
			
			ContentReader readernodoContratoAniadirCaratula = serviceRegistry.getFileFolderService().getReader(nodoContratoAniadirCaratula);			
			InputStream nodoContratoAniadirCaratulaStream= readernodoContratoAniadirCaratula.getContentInputStream();
			
			List<InputStream> pdfs = new ArrayList<>();
			pdfs.add(archivoGeneradoConPlantillaStream);
			pdfs.add(nodoContratoAniadirCaratulaStream);
			PDFMergerExample pdfMergerExample = new PDFMergerExample();
		    InputStream pdfMergeado = pdfMergerExample.merge(pdfs);
		    //nodo para el nuevo archivo 
		    FileInfo mergeado = serviceRegistry.getFileFolderService().create(nodoCarpeta, "merge-"+identificador+".pdf",
					ContentModel.TYPE_CONTENT);

		    final ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(mergeado.getNodeRef());
		    writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
		    writer.setEncoding(EncodingConstants.UTF_8.displayName());
		    writer.putContent(pdfMergeado);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			rs.close();
		}
		
		model.put("uuid", uuid);
		return model;
		
	}

	private Map<String, Object> generarMapaPlantilla(JSONObject map)
			throws JSONException, Exception {
		//Generar un map con los valores de la plantilla
		Map<String, Object> templateProperties = new HashMap<String, Object>();
		templateProperties.put("usuario", map.get("usuario"));
		templateProperties.put("oficina", map.get("oficina"));
		templateProperties.put("fechaOperacion", map.get("fechaOperacion"));
		JSONObject cliente = (JSONObject) map.get("cliente");
		templateProperties.put("dni", cliente.get("dni"));
		templateProperties.put("nombre", cliente.get("nombre"));
		templateProperties.put("apellido1", cliente.get("apellido1"));
		templateProperties.put("apellido2", cliente.get("apellido2"));
		templateProperties.put("tipoDocumento", map.get("tipoDocumento"));
		templateProperties.put("descripcion", map.get("descripcion"));
		templateProperties.put("dSalud", map.get("declaracionSalud"));
		templateProperties.put("comPermanencia", map.get("compromisoPermanencia"));
		templateProperties.put("condEspeciales", map.get("condicionesEspeciales"));
		templateProperties.put("condGenerales", map.get("condicionesGenerales"));
		templateProperties.put("mandato", map.get("mandato"));
		templateProperties.put("codigoBdi", map.get("codigoBdi"));
		
		
		//crear el codigo qr que insertaremos en la plantilla
		BufferedImage qr= crearQR(map.get("codigoBdi").toString(), 200, 200);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(qr, "png", os);
		IImageProvider selloQR = new ByteArrayImageProvider(os.toByteArray(), true);
		templateProperties.put("sello", selloQR);
		
		//Esto era para insertar el codigo bdi lateral como imagen pero al final lo insertamos como texto en la plantilla
		/*
		 * BufferedImage imageBdi= rotateImage(createImage(map.get("codigoBdi").toString()));
		ByteArrayOutputStream os2 = new ByteArrayOutputStream();
		ImageIO.write(imageBdi, "png", os2);
		IImageProvider bdi = new ByteArrayImageProvider(os2.toByteArray(), true);
		templateProperties.put("bdi", bdi);*/
		
		return templateProperties;
	}
	

	private NodeRef generarDocumentoDePlantilla(InputStream contenidoDocumento, NodeRef nodoCarpeta, boolean pdf, long identificador) {
		FileInfo archivoGeneradoConPlantilla=null;
		String nombreArchivo = "pruebas_plantilla-"+identificador;
		if(pdf) {
			nombreArchivo=nombreArchivo+".pdf";
			// Crea un archivo bajo la carpeta indicada arriba y esta vacio al inicio			
			archivoGeneradoConPlantilla = serviceRegistry.getFileFolderService().create(nodoCarpeta, nombreArchivo,
								ContentModel.TYPE_CONTENT);
			
			
			final ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(archivoGeneradoConPlantilla.getNodeRef());
			writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
			writer.setEncoding(EncodingConstants.UTF_8.displayName());
			writer.putContent(contenidoDocumento);
		}else {
			nombreArchivo=nombreArchivo+".docx";
			// Crea un archivo bajo la carpeta indicada arriba y esta vacio al inicio			
			archivoGeneradoConPlantilla = serviceRegistry.getFileFolderService().create(nodoCarpeta, nombreArchivo,
											ContentModel.TYPE_CONTENT);
			final ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(archivoGeneradoConPlantilla.getNodeRef());
			writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING);
			writer.setEncoding(EncodingConstants.UTF_8.displayName());
			writer.putContent(contenidoDocumento);
			serviceRegistry.getNodeService().addAspect(archivoGeneradoConPlantilla.getNodeRef(), ContentModel.ASPECT_VERSIONABLE,
					new HashMap<>());
			
		}

		return archivoGeneradoConPlantilla.getNodeRef();
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public IReportsService getReportsService() {
		return reportsService;
	}

	public void setReportsService(IReportsService reportsService) {
		this.reportsService = reportsService;
	}
	
	
	
	public MimetypeService getMimetypeService() {
		return mimetypeService;
	}

	public void setMimetypeService(MimetypeService mimetypeService) {
		this.mimetypeService = mimetypeService;
	}

	public BufferedImage crearQR(String datos, int ancho, int altura) throws WriterException {
	    BitMatrix matrix;
	    Writer escritor = new QRCodeWriter();
	    matrix = escritor.encode(datos, BarcodeFormat.QR_CODE, ancho, altura);
	         
	    BufferedImage imagen = new BufferedImage(ancho, altura, BufferedImage.TYPE_INT_RGB);
	         
	    for(int y = 0; y < altura; y++) {
	        for(int x = 0; x < ancho; x++) {
	            int grayValue = (matrix.get(x, y) ? 0 : 1) & 0xff;
	            imagen.setRGB(x, y, (grayValue == 0 ? 0 : 0xFFFFFF));
	        }
	    }
	         
	    return imagen;        
	}
	
}
