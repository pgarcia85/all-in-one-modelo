{
	"idFactura": <#if idFactura?exists>"${idFactura}"<#else>""</#if>,
	"tipo": <#if tipo?exists>"${tipo}"<#else>""</#if>,
	"importe": <#if importe?exists>"${importe}"<#else>""</#if>,
	"idNodo": <#if idNodo?exists>"${idNodo}"<#else>""</#if>,
	"exception" :<#if exception?exists>"${exception}"<#else>""</#if>,
}