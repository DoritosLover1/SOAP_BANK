package com.banka.accountservice.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.context.ApplicationContext;

@EnableWs
@Configuration
public class WebServiceConfig {

    //BURADA EĞER BİZ GİDİP APPLICATIONCONTEXT VERMEZSEK SERVLET KENDISI BİR
    //ARAMA YAPMAYA BAŞLIYOR CONFİGURASYON İÇİN BİZİM XSD KULLANMAMDAN
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "accounts")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema accountsSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("AccountServicePort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://banka.com/accountservice");
        wsdl11Definition.setSchema(accountsSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema accountsSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/accounts.xsd"));
    }
}