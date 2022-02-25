package com.medvida.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;

import com.medvida.service.IReportsService;

import fr.opensagres.xdocreport.converter.ConverterRegistry;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.ConverterTypeVia;
import fr.opensagres.xdocreport.converter.IConverter;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.document.DocumentKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;

public class IReportsServiceImpl implements IReportsService{
	
	private transient ServiceRegistry serviceRegistry;
	private ContentService contentService;


	@Override
	public InputStream generateReport(NodeRef nodePlantilla, Map<String, Object> dataMap, boolean pdf)  {
		
		
		ByteArrayInputStream input = null;

		try {
			
		contentService = serviceRegistry.getContentService();
		// get template binary
		final ContentReader templateContentReader = contentService.getReader(nodePlantilla, ContentModel.PROP_CONTENT);
			
		InputStream in = templateContentReader.getContentInputStream();
		
		IXDocReport report;
		report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Velocity);
		
		FieldsMetadata metadata = new FieldsMetadata();
		metadata.addFieldAsImage("sello");
		/*
		 * Esto era para insertar el codigo bdi como imagen, pero al final lo insertamos como texto
		metadata.addFieldAsImage("bdi");
		*/
		report.setFieldsMetadata(metadata);
		
		IContext context = report.createContext();
		context.putMap(dataMap);

		ByteArrayOutputStream out = new ByteArrayOutputStream();	
		
		
		if(pdf) {
			//para pdf
			Options options = Options.getTo(ConverterTypeTo.PDF).via(ConverterTypeVia.XWPF);
			//Options options = Options.getFrom(DocumentKind.DOCX).to(ConverterTypeTo.PDF).via(ConverterTypeVia.XWPF);
			
			report.convert(context, options, out);
		}else {
			//para docx
			report.process(context, out);
		}

		input = new ByteArrayInputStream(out.toByteArray());

		out.flush();
		out.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XDocReportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return input;
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

}
