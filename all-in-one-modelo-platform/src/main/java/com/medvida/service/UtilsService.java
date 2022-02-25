package com.medvida.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

public interface UtilsService {
	
	NodeRef crearCarpetas(NodeRef parentNodeRef, List<String> pathElements);

}
