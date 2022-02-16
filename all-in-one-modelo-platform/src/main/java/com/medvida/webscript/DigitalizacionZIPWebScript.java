package com.medvida.webscript;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.medvida.model.MedvidaModel;

public class DigitalizacionZIPWebScript extends DeclarativeWebScript {

	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalizacionZIPWebScript.class);


	private ServiceRegistry serviceRegistry;
	
	private MimetypeService mimetypeService;
	
	private ContentService contentService;


	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		LOGGER.info("INICIO - DigitalizacionZIPWebScript -->" );
		Map<String, Object> model = new HashMap<String, Object>();

		// creamos un lista donde iran los nombres de los documentos
		List<String> lista = new ArrayList<String>();
		// Creamos la carpeta para guardar los documentos. La crea dentro de Entrada Documentos Zip y el 
		// nombre esta formado por la palabra Zip_fecha_identificador
		FileInfo carpetaInfo = createFolder("Zip_" + LocalDate.now().toString() + "_" + System.currentTimeMillis());
		try {
			LOGGER.info("Recibimos un archivo " + req.getContentType() + ", codificado en "
					+ req.getContent().getEncoding());
			InputStream inputStream = req.getContent().getInputStream();
			req.getContent().getEncoding();
			req.getContentType();
			// esto es el zip
			ZipInputStream zipInputStream = new ZipInputStream(inputStream);

			// esto es una entrada del zip
			ZipEntry entry = null;
			
			Map<String, Map<QName, Serializable>> mapaPropiedades=new HashMap<String, Map<QName,Serializable>>();
			
			// al hacer nextEntry, el inputStream se situa en la entrada siguiente
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					String entryName = entry.getName();
					LOGGER.info("obtener los documentos --> nombre: " + entryName);				
					//el primer documento siempre es el de los metadatos
					if(entryName.endsWith(".xml")) {
						//creo el documento de los metadatos y el metodo me devuelve el NodeRef
						NodeRef metadatos = crearDocumento(carpetaInfo, entryName, zipInputStream, null);
						LOGGER.info("extraer los datos del xml");
						
						//Crear el Builder
						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						//esto lee el nodo
						ContentReader contentReader = contentService.getReader(metadatos, ContentModel.PROP_CONTENT);
					
						if (contentReader != null) {
							// se obtiene la lista de etiquetas del XML esto si guardo el xml en alfresco
							Document doc = dBuilder.parse(contentReader.getContentInputStream());
							doc.getDocumentElement().normalize();
							
							 NodeList tagsList = doc.getDocumentElement().getChildNodes();
							
							//lista nodos Factura							
							if (tagsList != null && tagsList.getLength() > 0) {															
								for (int i = 0; i <tagsList.getLength(); i++) {
									Node node = tagsList.item(i);
									if (node.getNodeType() == Node.ELEMENT_NODE) {
										 Element elem = (Element) node;
							                String nombreDocu = elem.getElementsByTagName("nombre")
							                        .item(0).getChildNodes().item(0).getNodeValue();
							                String id_factura = elem.getElementsByTagName("idFactura").item(0)
							                        .getChildNodes().item(0).getNodeValue();
							                String tipo = elem.getElementsByTagName("tipo")
							                        .item(0).getChildNodes().item(0).getNodeValue();
							                Double importe = Double.parseDouble(elem.getElementsByTagName("importe")
							                        .item(0).getChildNodes().item(0).getNodeValue());
							                Map<QName, Serializable> mapa = new HashMap<QName, Serializable>();
							                mapa.put(MedvidaModel.PROP_FACTURA_ID, id_factura.trim());
							                mapa.put(MedvidaModel.PROP_FACTURA_TIPO, tipo.trim());
							                mapa.put(MedvidaModel.PROP_FACTURA_IMPORTE, importe.toString());
							                mapaPropiedades.put(nombreDocu, mapa);
							                
									}
								}
								
							}
						}
							
					}else {
						crearDocumento(carpetaInfo, entryName, zipInputStream, mapaPropiedades);

					}
				
					// añadir nombre documento a la lista
					lista.add(entryName);				
					zipInputStream.closeEntry();

				}

			}

			zipInputStream.close();
			model.put("nombreDocus", lista.toString());
			model.put("numeroDocus", lista.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		LOGGER.info("FIN - DigitalizacionZIPWebScript -->" );
		return model;

	}

	/**
	 * Crea una carpeta para guardar los documentos
	 *
	 * @param folderName the name of the folder
	 * @return a FileInfo object with data about the new folder, such as NodeRef
	 */
	private FileInfo createFolder(String folderName) throws FileExistsException {

		NodeRef parentFolderNodeRef = getNodo("Entrada Documentos Zip");
		
		//NodeRef parentFolderNodeRef = getNodo("Zip_2022-02-11_1644576984950");
		// Create the folder under /Company Home
		FileInfo folderInfo = serviceRegistry.getFileFolderService().create(parentFolderNodeRef, folderName,
				ContentModel.TYPE_FOLDER);

		return folderInfo;
	}

	/**
	 * Metodo que busca un nodo por su nombre
	 * 
	 * @param nombreCarpeta
	 * @return
	 */
	private NodeRef getNodo(String nombreCarpeta) {
		StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
		ResultSet rs = serviceRegistry.getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE,
				"@cm\\:name:\"" + nombreCarpeta + "\"");
		NodeRef nodoCarpeta = null;
		List<ChildAssociationRef> lista = new ArrayList<ChildAssociationRef>();
		try {
			if (rs.length() == 0) {
				throw new AlfrescoRuntimeException("No encuentra el nodo por ese nombre");
			}

			nodoCarpeta = rs.getNodeRef(0);
			//no es necesario para el WS
			//Para practicar los hijos del nodo y obtener sus propiedades
			//asi obtengo las asociaciones del nodo que paso como parametro
			lista=serviceRegistry.getNodeService().getChildAssocs(nodoCarpeta);
			for (ChildAssociationRef childAssociationRef : lista) {
				NodeRef nodoPadre = childAssociationRef.getParentRef();
				NodeRef nodoHijo = childAssociationRef.getChildRef();
				Map<QName, Serializable> propiedades = serviceRegistry.getNodeService().getProperties(nodoHijo);			
				//System.out.println(propiedades.toString());
			}
	
		} finally {
			rs.close();
		}
		return nodoCarpeta;
	}

	/**
	 * Create a file under the passed in folder.
	 *
	 * @param folderInfo the folder that the file should be created in
	 * @param filename   the name of the file
	 * @param fileTxt    the content of the file
	 * @return a FileInfo object with data about the new file, such as NodeRef
	 */
	private NodeRef crearDocumento(FileInfo folderInfo, String nombreArchivo, InputStream contenido, Map<String, Map<QName, Serializable>> mapaPropiedades)
			throws FileExistsException {
		NodeRef archivoNuevo = null;
		try {
			// Creo un flujo nuevo para pasarle a crear documentos porque me cierra el flujo
			// tras escribir el fichero en el write.content
			InputStream ie = new ByteArrayInputStream(contenido.readAllBytes());

			// Crea un archivo bajo la carpeta indicada en folderInfo y esta vacio al inicio
			FileInfo fileInfo = serviceRegistry.getFileFolderService().create(folderInfo.getNodeRef(), nombreArchivo,
					MedvidaModel.TYPE_FACTURA);
			// Obtiene el nodeRef del nuevo archivo
			 archivoNuevo = fileInfo.getNodeRef();
			 //añadimos properties
			 if(mapaPropiedades!=null) {
					serviceRegistry.getNodeService().addProperties(archivoNuevo, mapaPropiedades.get(nombreArchivo));		
					//se añade el aspecto versionable
					serviceRegistry.getNodeService().addAspect(archivoNuevo, ContentModel.ASPECT_VERSIONABLE,
							new HashMap<>());
			 }
			 
			 
			

			// Añade contenido al archivo
			String tipodocumento =mimetypeService
					.getMimetype(nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1, nombreArchivo.length()));
			ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(archivoNuevo);
			writer.setMimetype(tipodocumento);
			writer.setEncoding("UTF-8");
			writer.putContent(ie);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return archivoNuevo;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}


	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public MimetypeService getMimetypeService() {
		return mimetypeService;
	}

	public void setMimetypeService(MimetypeService mimetypeService) {
		this.mimetypeService = mimetypeService;
	}




}
