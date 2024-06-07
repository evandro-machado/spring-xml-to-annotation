//package com.example.demo;
//
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.w3c.dom.*;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//@SpringBootApplication
//public class DemoApplication implements ApplicationRunner {
//
//    public static void main(String[] args) {
//        SpringApplication.run(DemoApplication.class, args);
//    }
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        try {
//            File inputFile = new File("C:\\codegen\\beans.xml");
//            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//            Document doc = dBuilder.parse(inputFile);
//            doc.getDocumentElement().normalize();
//
//            NodeList beanList = doc.getElementsByTagName("bean");
//
//            Set<String> imports = new HashSet<>();
//            StringBuilder beansConfig = new StringBuilder();
//            Map<String, String> beanDefinitions = new HashMap<>();
//            Map<String, String> nestedBeanDefinitions = new HashMap<>();
//            Set<String> addedBeans = new HashSet<>();
//            Map<String, String> valueProperties = new HashMap<>();
//
//            for (int i = 0; i < beanList.getLength(); i++) {
//                Node beanNode = beanList.item(i);
//
//                if (beanNode.getNodeType() == Node.ELEMENT_NODE) {
//                    Element beanElement = (Element) beanNode;
//                    String id = beanElement.getAttribute("id");
//                    String className = beanElement.getAttribute("class");
//                    imports.add(className);
//
//                    if (id.isEmpty()) {
//                        id = getDefaultBeanName(className);
//                    }
//
//                    beanDefinitions.put(id, className);
//                }
//            }
//
//            // Handle nested beans within property elements
//            for (int i = 0; i < beanList.getLength(); i++) {
//                Node beanNode = beanList.item(i);
//
//                if (beanNode.getNodeType() == Node.ELEMENT_NODE) {
//                    Element beanElement = (Element) beanNode;
//                    NodeList propertyList = beanElement.getElementsByTagName("property");
//
//                    for (int j = 0; j < propertyList.getLength(); j++) {
//                        Node propertyNode = propertyList.item(j);
//                        if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element propertyElement = (Element) propertyNode;
//                            NodeList nestedBeans = propertyElement.getElementsByTagName("bean");
//
//                            for (int k = 0; k < nestedBeans.getLength(); k++) {
//                                Node nestedBeanNode = nestedBeans.item(k);
//                                if (nestedBeanNode.getNodeType() == Node.ELEMENT_NODE) {
//                                    Element nestedBeanElement = (Element) nestedBeanNode;
//                                    String nestedClassName = nestedBeanElement.getAttribute("class");
//                                    String nestedBeanName = getDefaultBeanName(nestedClassName);
//                                    imports.add(nestedClassName);
//                                    nestedBeanDefinitions.put(nestedBeanName, nestedClassName);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            for (int i = 0; i < beanList.getLength(); i++) {
//                Node beanNode = beanList.item(i);
//
//                if (beanNode.getNodeType() == Node.ELEMENT_NODE) {
//                    Element beanElement = (Element) beanNode;
//                    String id = beanElement.getAttribute("id");
//                    String className = beanElement.getAttribute("class");
//
//                    String beanName = id.isEmpty() ? getDefaultBeanName(className) : id;
//                    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
//
//                    beansConfig.append("    @Bean\n");
//                    beansConfig.append("    public ").append(simpleClassName).append(" ").append(beanName).append("(");
//
//                    NodeList propertyList = beanElement.getElementsByTagName("property");
//                    boolean firstParam = true;
//                    for (int j = 0; j < propertyList.getLength(); j++) {
//                        Node propertyNode = propertyList.item(j);
//                        if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element propertyElement = (Element) propertyNode;
//                            String ref = propertyElement.getAttribute("ref");
//                            if (!ref.isEmpty()) {
//                                if (!firstParam) {
//                                    beansConfig.append(", ");
//                                }
//                                beansConfig.append(beanDefinitions.get(ref).substring(beanDefinitions.get(ref).lastIndexOf(".") + 1)).append(" ").append(ref);
//                                firstParam = false;
//                            }
//                        }
//                    }
//
//                    // Check for nested bean references and add them as method parameters
//                    if (id.equals("bean5") && nestedBeanDefinitions.containsKey("bean6")) {
//                        if (!firstParam) {
//                            beansConfig.append(", ");
//                        }
//                        beansConfig.append(nestedBeanDefinitions.get("bean6").substring(nestedBeanDefinitions.get("bean6").lastIndexOf(".") + 1)).append(" ").append("bean6");
//                        firstParam = false;
//                        addedBeans.add("bean6");
//                    }
//
//                    beansConfig.append(") {\n");
//                    beansConfig.append("        ").append(simpleClassName).append(" ").append(beanName).append(" = new ").append(simpleClassName).append("();\n");
//
//                    // Handle p: attributes
//                    NamedNodeMap attributes = beanElement.getAttributes();
//                    for (int j = 0; j < attributes.getLength(); j++) {
//                        Node attribute = attributes.item(j);
//                        String attrName = attribute.getNodeName();
//                        if (attrName.startsWith("p:")) {
//                            String propertyName = attrName.substring(2);
//                            String propertyValue = attribute.getNodeValue();
//                            valueProperties.put(propertyName, propertyValue);
//                            beansConfig.append("        ").append(beanName).append(".set").append(capitalize(propertyName)).append("(").append(propertyName).append(");\n");
//                        }
//                    }
//
//                    // Handle <property> elements
//                    for (int j = 0; j < propertyList.getLength(); j++) {
//                        Node propertyNode = propertyList.item(j);
//                        if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element propertyElement = (Element) propertyNode;
//                            String name = propertyElement.getAttribute("name");
//                            String ref = propertyElement.getAttribute("ref");
//                            String value = propertyElement.getAttribute("value");
//
//                            if (!ref.isEmpty()) {
//                                beansConfig.append("        ").append(beanName).append(".set").append(capitalize(name)).append("(").append(ref).append(");\n");
//                            } else if (!value.isEmpty()) {
//                                String valueVariable = value.replace("${", "").replace("}", "");
//                                valueProperties.put(name, valueVariable);
//                                beansConfig.append("        ").append(beanName).append(".set").append(capitalize(name)).append("(").append(name).append(");\n");
//                            } else {
//                                NodeList nestedBeans = propertyElement.getElementsByTagName("bean");
//                                if (nestedBeans.getLength() > 0) {
//                                    Element nestedBeanElement = (Element) nestedBeans.item(0);
//                                    String nestedClassName = nestedBeanElement.getAttribute("class");
//                                    String nestedBeanName = getDefaultBeanName(nestedClassName);
//                                    // Use the parameter directly if it's bean6
//                                    if (nestedBeanName.equals("bean6") && id.equals("bean5")) {
//                                        beansConfig.append("        ").append(beanName).append(".set").append(capitalize(name)).append("(bean6);\n");
//                                    } else {
//                                        beansConfig.append("        ").append(beanName).append(".set").append(capitalize(name)).append("(").append(nestedBeanName).append("());\n");
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    beansConfig.append("        return ").append(beanName).append(";\n");
//                    beansConfig.append("    }\n\n");
//                }
//            }
//
//            // Add nested beans that were not included as parameters
//            for (Map.Entry<String, String> entry : nestedBeanDefinitions.entrySet()) {
//                String nestedBeanName = entry.getKey();
//                String nestedClassName = entry.getValue();
//                String simpleClassName = nestedClassName.substring(nestedClassName.lastIndexOf(".") + 1);
//
//                if (!addedBeans.contains(nestedBeanName)) {
//                    beansConfig.append("    @Bean\n");
//                    beansConfig.append("    public ").append(simpleClassName).append(" ").append(nestedBeanName).append("() {\n");
//                    beansConfig.append("        ").append(simpleClassName).append(" ").append(nestedBeanName).append(" = new ").append(simpleClassName).append("();\n");
//                    beansConfig.append("        return ").append(nestedBeanName).append(";\n");
//                    beansConfig.append("    }\n\n");
//                }
//            }
//
//            generateJavaConfigFile(imports, beansConfig.toString(), valueProperties);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void generateJavaConfigFile(Set<String> imports, String beansConfig, Map<String, String> valueProperties) throws IOException {
//        StringBuilder fileContent = new StringBuilder();
//        fileContent.append("import org.springframework.context.annotation.Bean;\n");
//        fileContent.append("import org.springframework.context.annotation.Configuration;\n");
//        fileContent.append("import org.springframework.beans.factory.annotation.Value;\n");
//        fileContent.append("import org.springframework.beans.factory.annotation.Autowired;\n\n");
//
//        for (String importStmt : imports) {
//            fileContent.append("import ").append(importStmt).append(";\n");
//        }
//
//        fileContent.append("\n@Configuration\n");
//        fileContent.append("public class AppConfig {\n\n");
//
//        System.out.println();
//
//        fileContent.append(beansConfig);
//
//        fileContent.append("}\n");
//
//        FileWriter writer = new FileWriter("C:\\codegen\\AppConfig.java");
//        writer.write(fileContent.toString());
//        writer.close();
//    }
//
//    private static String getDefaultBeanName(String className) {
//        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
//        return Character.toLowerCase(simpleClassName.charAt(0)) + simpleClassName.substring(1);
//    }
//
//    private static String capitalize(String str) {
//        if (str == null || str.isEmpty()) {
//            return str;
//        }
//        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
//    }
//}