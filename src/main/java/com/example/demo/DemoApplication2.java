package com.example.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class DemoApplication2 implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication2.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Document doc = loadXmlFile(args.getSourceArgs()[0]);

        NodeList beanList = doc.getElementsByTagName("bean");
        List<Bean> rawBeans = processBeanElements(beanList);

        Map<String, Bean> mapStringBean = rawBeans.stream()
                .map(this::enrichBeanInfo)
                .collect(Collectors.toMap(Bean::getId, Function.identity(), (first, second) -> first, TreeMap::new));

        generateJavaConfigFile(mapStringBean);
    }

    private void generateJavaConfigFile(Map<String, Bean> mapStringBean) {
        StringBuilder sb = new StringBuilder();

        sb.append("import org.springframework.context.annotation.Bean;\n");
        sb.append("import org.springframework.context.annotation.Configuration;\n");
        sb.append("import org.springframework.beans.factory.annotation.Value;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n\n");

        mapStringBean.forEach((beanName, bean) -> {
            sb.append("import ").append(bean.getClazz()).append(";\n");
        });
        sb.append("\n");

        sb.append("@Configuration\n");
        sb.append("public class AppConfig {\n\n");

        List<String> externalProperties = new ArrayList<>();
        mapStringBean.forEach((beanName, bean) -> {
            bean.getProperties().entrySet().forEach(entry -> {
                if (entry.getValue().startsWith("${") && entry.getValue().endsWith("}")) {
                    externalProperties.add(entry.getValue());
                    entry.setValue(getValueFromExternalProperty(entry.getValue()));
                }
            });
        });

        externalProperties.forEach(externalProperty -> {
            sb.append("@Value(\"").append(externalProperty).append("\")\n");
            sb.append("private String ").append(getValueFromExternalProperty(externalProperty)).append(";\n\n");
        });


        mapStringBean.forEach((beanName, bean) -> {
            sb.append("@Bean\n");
            List<String> parameterList = new ArrayList<>();
            bean.getProperties().forEach((attrName, value) -> {
                Bean referencingBean = mapStringBean.get(value);
                if (referencingBean != null) {
                    parameterList.add(getClassDeclaration(referencingBean));
                }
            });
            String parameters = String.join(", ", parameterList);
            sb.append("public ").append(getClassDeclaration(bean)).append("(").append(parameters).append(") {\n");
            sb.append("    ").append(getClassDeclaration(bean)).append(" = new ").append(getLastElement(bean.getClazz())).append("();\n");
            String variableName = getLastElementUncapitalized(bean.getClazz());
            bean.getProperties().forEach((attrName, value) -> {
                sb.append("    ").append(variableName).append(".set").append(StringUtils.capitalize(attrName)).append("(").append(value).append(");\n");
            });

            sb.append("    ").append("return ").append(variableName).append(";\n");
            sb.append("}");

            sb.append("\n\n");
        });

        sb.append("}");
        System.out.println(sb);
    }

    private String getValueFromExternalProperty(String externalProperty) {
        String value = externalProperty.substring(externalProperty.indexOf('{') + 1, externalProperty.indexOf('}'));
        return StringUtils.uncapitalize(getLastElement(value));
    }

    private Bean enrichBeanInfo(Bean bean) {
        //Class should always exist in properties map
        String clazz = bean.getProperties().get("class");
        bean.setClazz(clazz);
        String id = bean.getProperties().get("id");
        if (id == null) {
            bean.setId(getLastElementUncapitalized(clazz));
        } else {
            bean.setId(id);
        }
        bean.getProperties().remove("id");
        bean.getProperties().remove("class");
        return bean;
    }

    private List<Bean> processBeanElements(NodeList beanList) {
        List<Bean> rawBeans = new ArrayList<>();
        for (int i = 0; i < beanList.getLength(); i++) {
            Element item = (Element) beanList.item(i);
            Bean bean = new Bean();
            rawBeans.add(bean);
            bean.setProperties(getPropertiesFromBeanAttributes(item));

            NodeList propertyList = item.getElementsByTagName("property");
            processPropertyElements(bean, propertyList);
        }
        return rawBeans;
    }

    private static void processPropertyElements(Bean bean, NodeList propertyList) {
        for (int j = 0; j < propertyList.getLength(); j++) {
            Element property = (Element) propertyList.item(j);
            if (propertyList.getLength() > 0) {
                final Map<String, String> propertiesMap = getAttributesFromPropertyElement(bean, property);
                if (!propertiesMap.isEmpty()) {
                    setPropertiesWithoutInnerBean(bean, propertiesMap);
                }
            }
        }
    }

    private static Map<String, String> getAttributesFromPropertyElement(Bean bean, Element property) {
        final Map<String, String> propertiesMap = new HashMap<>();
        for (int i = 0; i < property.getAttributes().getLength(); i++) {
            Node propertyAttribute = property.getAttributes().item(i);
            NodeList innerBeans = property.getElementsByTagName("bean");
            if (innerBeans.getLength() == 0) {
                propertiesMap.put(propertyAttribute.getNodeName(), propertyAttribute.getNodeValue());
            } else {
                Element item1 = (Element) innerBeans.item(0);
                String classValue = item1.getAttribute("class");
                classValue = getLastElementUncapitalized(classValue);
                bean.getProperties().put(propertyAttribute.getNodeValue(), classValue);
            }
        }
        return propertiesMap;
    }

    private static String getLastElementUncapitalized(String classValue) {
        return StringUtils.uncapitalize(getLastElement(classValue));
    }


    private static String getLastElement(String classValue) {
        return classValue.substring(classValue.lastIndexOf(".") + 1);
    }

    private static void setPropertiesWithoutInnerBean(Bean bean, Map<String, String> propertiesMap) {
        if (propertiesMap.get("ref") != null) {
            bean.getProperties().put(propertiesMap.get("name"), propertiesMap.get("ref"));
        } else {
            bean.getProperties().put(propertiesMap.get("name"), propertiesMap.get("value"));
        }
    }

    private static Document loadXmlFile(String pathString) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(pathString);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private Map<String, String> getPropertiesFromBeanAttributes(Element item) {
        Map<String, String> properties = new TreeMap<>();
        for (int i = 0; i < item.getAttributes().getLength(); i++) {
            Node item1 = item.getAttributes().item(i);
            String nodeName = item1.getNodeName();
            if (nodeName.startsWith("p:")) {
                nodeName = nodeName.substring("p:".length());
            }
            properties.put(nodeName, item1.getNodeValue());
        }

        return properties;
    }

    private String getClassDeclaration(Bean bean) {
        return getLastElement(bean.getClazz()) + " " + getLastElementUncapitalized(bean.getClazz());
    }
}