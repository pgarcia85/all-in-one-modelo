package com.medvida.webscript;

import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
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
import org.springframework.extensions.webscripts.servlet.FormData;

import com.medvida.model.MedvidaModel;

public class SubirDocumentoWebScript extends DeclarativeWebScript {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubirDocumentoWebScript.class);

	private ServiceRegistry serviceRegistry;
	private SearchService searchService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> propiedades = new HashMap<String, Object>();
		String mimetype = null;
		InputStream content = null;
		String documentName = null;
		FormData form = (FormData) req.parseContent();
		try {
			for (FormData.FormField field : form.getFields()) {
				if (!field.getIsFile()) {
					propiedades.put(field.getName(), field.getValue());
					LOGGER.info("CreateDocument-Field: " + field.getName() + " " + field.getValue());
				} else {
					mimetype = field.getMimetype();
					content = field.getInputStream();
					documentName = field.getFilename();
					LOGGER.info("CreateDocument-File: Mimetype: " + mimetype + " Nombre: " + documentName);
				}
			}

			NodeRef carpetaPadre = getPadre((String) propiedades.get("tipo"));
			NodeRef carpetaFinal = crearCarpetas(carpetaPadre, generarRuta((String) propiedades.get("idFactura")));

			// Crear el documento y su contenido
			// Crea un archivo bajo la carpeta indicada en folderInfo y esta vacio al inicio
			String nombreArchivo="Factura-"+(String) propiedades.get("idFactura")+ "-" + LocalDate.now().toString()+"."+documentName.substring(documentName.lastIndexOf(".") + 1, documentName.length());
			FileInfo fileInfo = serviceRegistry.getFileFolderService().create(carpetaFinal,
					nombreArchivo, MedvidaModel.TYPE_FACTURA);
			// Obtiene el nodeRef del nuevo archivo
			NodeRef archivoNuevo = fileInfo.getNodeRef();
			// añadimos properties
			serviceRegistry.getNodeService().addProperties(archivoNuevo, mapaPopiedades(propiedades));

			// Añade contenido al archivo

			ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(archivoNuevo);
			writer.setMimetype(mimetype);
			writer.setEncoding("UTF-8");
			writer.putContent(content);
			propiedades.put("idNodo", archivoNuevo.getId());
		} catch (Exception e) {
			propiedades.put("exception", "Se ha produccido un error al enviar el documento: "+ e.getMessage());
			return propiedades;
		}
		return propiedades;
	}

	private Map<QName, Serializable> mapaPopiedades(Map<String, Object> propiedades) {
		Map<QName, Serializable> mapaPropiedades = new HashMap<QName, Serializable>();

		mapaPropiedades.put(MedvidaModel.PROP_FACTURA_ID, (Serializable) propiedades.get("idFactura"));
		mapaPropiedades.put(MedvidaModel.PROP_FACTURA_TIPO, (Serializable) propiedades.get("tipo"));
		mapaPropiedades.put(MedvidaModel.PROP_FACTURA_IMPORTE, (Serializable) propiedades.get("importe"));

		return mapaPropiedades;
	}

	private List<String> generarRuta(String idFactura) {
		List<String> lista = new ArrayList<String>();
		lista.add(String.valueOf(idFactura.charAt(0)));
		lista.add(String.valueOf(idFactura.charAt(1)));
		lista.add(String.valueOf(idFactura.charAt(2)));
		lista.add(String.valueOf(idFactura.charAt(3)));
		lista.add(idFactura);
		return lista;

	}

	private NodeRef getPadre(String tipo) {

		StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
		String query = "TYPE:\"cm:folder\" AND =cm:name:\"" + tipo + "\"";
		ResultSet rs = serviceRegistry.getSearchService().query(storeRef, SearchService.LANGUAGE_FTS_ALFRESCO, query);
		NodeRef padre = rs.getNodeRef(0);
		return padre;

	}

	public NodeRef crearCarpetas(NodeRef nodoPadre, List<String> pathElements) {
		// Nodo padre actual, en que se va situando
		NodeRef currentParentRef = nodoPadre;
		// recorrer lista donde cada elemento es un directorio
		for (final String element : pathElements) {
			// busca el nodo
			NodeRef nodeRef = serviceRegistry.getNodeService().getChildByName(currentParentRef,
					ContentModel.ASSOC_CONTAINS, element);

			if (nodeRef == null) {
				FileInfo createdFileInfo = serviceRegistry.getFileFolderService().create(currentParentRef, element,
						ContentModel.TYPE_FOLDER);
				currentParentRef = createdFileInfo.getNodeRef();
			} else {
				// it exists
				currentParentRef = nodeRef;
			}

		}
		return currentParentRef;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

}
