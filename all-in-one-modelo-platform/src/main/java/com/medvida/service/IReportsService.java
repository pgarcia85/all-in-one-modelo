package com.medvida.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;



/**
 * IReportsService interface.
 * 
 */
public interface IReportsService {

	/**
	 * @param folder
	 * @param nodePlantilla
	 * @return
	 * @throws XDocReportException 
	 * @throws IOException 
	 * @throws ReportsException
	 */
	InputStream generateReport(final NodeRef nodePlantilla, Map<String, Object> dataMap, boolean pdf) throws Exception;

}
