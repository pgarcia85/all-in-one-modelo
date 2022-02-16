package com.medvida.model;

import org.alfresco.service.namespace.QName;

public class MedvidaModel {

	final public static String MED_MODEL_PREFIX = "http://www.medvida.org/model/content/1.0";

	final public static QName TYPE_FACTURA = QName.createQName(MED_MODEL_PREFIX, "factura");

	final public static QName PROP_FACTURA_TIPO = QName.createQName(MED_MODEL_PREFIX, "tipo");
	final public static QName PROP_FACTURA_IMPORTE = QName.createQName(MED_MODEL_PREFIX, "importe");
	final public static QName PROP_FACTURA_ID = QName.createQName(MED_MODEL_PREFIX, "idfactura");

}
