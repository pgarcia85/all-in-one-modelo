package com.medvida.model;

import java.io.Serializable;

public class Plantilla implements Serializable{
	
	private String usuario;
	private String oficina;
	private String fecha;
	private Cliente cliente;
	private String tipoDocumento;
	private String descripcion;
	private String declaracionSalud;
	private String compromisoPermanencia;
	private String condicionesEspeciales;
	private String condicionesGenerales;
	private String mandato;
	private String codigoBdi;
	private String plantilla;
	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getOficina() {
		return oficina;
	}
	public void setOficina(String oficina) {
		this.oficina = oficina;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	public String getTipoDocumento() {
		return tipoDocumento;
	}
	public void setTipoDocumento(String tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public String getDeclaracionSalud() {
		return declaracionSalud;
	}
	public void setDeclaracionSalud(String declaracionSalud) {
		this.declaracionSalud = declaracionSalud;
	}
	public String getCompromisoPermanencia() {
		return compromisoPermanencia;
	}
	public void setCompromisoPermanencia(String compromisoPermanencia) {
		this.compromisoPermanencia = compromisoPermanencia;
	}
	public String getCondicionesEspeciales() {
		return condicionesEspeciales;
	}
	public void setCondicionesEspeciales(String condicionesEspeciales) {
		this.condicionesEspeciales = condicionesEspeciales;
	}
	public String getCondicionesGenerales() {
		return condicionesGenerales;
	}
	public void setCondicionesGenerales(String condicionesGenerales) {
		this.condicionesGenerales = condicionesGenerales;
	}
	public String getMandato() {
		return mandato;
	}
	public void setMandato(String mandato) {
		this.mandato = mandato;
	}
	public String getCodigoBdi() {
		return codigoBdi;
	}
	public void setCodigoBdi(String codigoBdi) {
		this.codigoBdi = codigoBdi;
	}
	public String getPlantilla() {
		return plantilla;
	}
	public void setPlantilla(String plantilla) {
		this.plantilla = plantilla;
	}
	
	
	

}
