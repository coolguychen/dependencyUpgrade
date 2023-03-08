package util;

import model.Dependency;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.print.Doc;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class IOUtil {

    private static String pomPath = "D:\\Graduation Project\\pom.xml";
    private static Document document;
    private static Element root;


    /**
     * 初始化函数
     */
    public IOUtil() {
        // 创建文档并设置文档的根元素节点
        this.document = DocumentHelper.createDocument();
        //根节点 & 命名空间
        this.root = document.addElement("project", "http://maven.apache.org/POM/4.0.0");
        this.root.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        this.root.addAttribute("xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
        //子节点 定义 modelVersion
        Element modelVersion = root.addElement("modelVersion");
        modelVersion.addText("4.0.0");

        //子节点 groupId artifactId version
        Element groupId = root.addElement("groupId");
        groupId.addText("org.example");
        Element artifactId = root.addElement("artifactId");
        artifactId.addText("project");
        Element version = root.addElement("version");
        version.addText("1.0-SNAPSHOT");

        //子节点 properties
        Element properties = root.addElement("properties");
        Element property1 = properties.addElement("maven.compiler.source");
        property1.addText("8");
        Element property2 = properties.addElement("maven.compiler.target");
        property2.addText("8");
    }

    /**
     * 读取pom文件的demo
     */
    private static void readXmlByDom4J() {
        try {
            // 1. 创建org.dom4j.io.SAXReader对象
            SAXReader saxReader = new SAXReader();
            InputStream ins = new FileInputStream(pomPath);
            Document document = saxReader.read(ins);
            Element rootElement = document.getRootElement();
            System.out.println("根节点的名称是：" + rootElement.getName());
            Iterator iterator = rootElement.elementIterator();
            while (iterator.hasNext()) {
                Element element = (Element) iterator.next();
                // 获取所有属性名和属性值
                List<Attribute> attributesList = element.attributes();
                for (Attribute attribute : attributesList) {
                    System.out.println("属性名：" + attribute.getName() + "======属性值" + attribute.getValue());
                }

                // 遍历子节点
                Iterator childIterator = element.elementIterator();
                while (childIterator.hasNext()) {
                    Element child = (Element) childIterator.next();
                    System.out.println("属性名：" + child.getName() + "======属性值" + child.getStringValue());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }


    /**
     * 写入pom文件的demo
     */
    public static void writeXmlDemo() {
        //子节点 dependencies
        Element dependencies = root.addElement("dependencies");
        Element dependency1 = dependencies.addElement("dependency");
        Element element21 = dependency1.addElement("groupId");
        element21.addText("org.apache.httpcomponents");
        Element element22 = dependency1.addElement("artifactId");
        element22.addText("httpclient");
        Element element23 = dependency1.addElement("version");
        element23.addText("4.5.13");

        //子节点 dependency
        Element dependency2 = dependencies.addElement("dependency");
        Element element31 = dependency2.addElement("groupId");
        element31.addText("org.slf4j");
        Element element32 = dependency2.addElement("artifactId");
        element32.addText("slf4j-nop");
        Element element33 = dependency2.addElement("version");
        element33.addText("1.7.25");

        writeXml(document,pomPath);

    }

    /**
     * 将dpcument写入pom文件
     *
     * @param document
     */
    public static void writeXml(Document document, String pomPath) {
//        System.out.println("写入pom.xml文件......");
        try {
            //创建文件
            OutputFormat format = OutputFormat.createPrettyPrint();
            //设定编码
            format.setEncoding("UTF-8");
            //添加writer写入xml文件
            XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(pomPath), format);
            xmlWriter.write(document);
            xmlWriter.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据dependncyList构造一个新的pom文件
     * @param pomPath
     * @param dependencyList
     */
    public static void writeXmlByDom4J(String pomPath, List<Dependency> dependencyList) {
        //子节点 dependencies
        Element dependencies = root.addElement("dependencies");
        for (Dependency dependency : dependencyList) {
            String groupId1 = dependency.getGroupId();
            String artifactId1 = dependency.getArtifactId();
            String version1 = dependency.getVersion();
            Element depElement = dependencies.addElement("dependency");
            Element element1 = depElement.addElement("groupId");
            element1.addText(groupId1);
            Element element2 = depElement.addElement("artifactId");
            element2.addText(artifactId1);
            Element element3 = depElement.addElement("version");
            element3.addText(version1);
        }
        //写入xml文件
        writeXml(document,pomPath);
    }

    /**
     * 根据dependncyList构造一个修改子模块的pom文件
     * @param pomPath
     * @param dependencyList
     */
    public static void modifyDependenciesXml(String pomPath, List<Dependency> dependencyList) {
        CountDownLatch latch = new CountDownLatch(1);

        // 创建SAXReader对象
        SAXReader sr = new SAXReader();
        try{
            // 关联xml
            Document document = sr.read(pomPath);
            // 获取根元素
            Element root = document.getRootElement();
            // 获取dependencies标签
            //子模块获取方式
            Element dependencies = root.element("dependencies");
            if(dependencies == null) { //父模块获取方式
                dependencies = root.element("dependencyManagement").element("dependencies");
            }
            //不为空才能继续
            if(dependencies != null) {
                List<Element> list = dependencies.elements();
                for (Dependency dependency : dependencyList) {
                    String groupId1 = dependency.getGroupId();
                    String artifactId1 = dependency.getArtifactId();
                    String version1 = dependency.getVersion();
                    for(Element e : list) {
                        String groupId = e.element("groupId").getText();
                        String artifactId = e.element("artifactId").getText();
                        if(groupId1.equals(groupId) && artifactId1.equals(artifactId)) {
                            //去除原来节点
                            dependencies.remove(e);
                            Element depElement = dependencies.addElement("dependency");
                            Element element1 = depElement.addElement("groupId");
                            element1.addText(groupId1);
                            Element element2 = depElement.addElement("artifactId");
                            element2.addText(artifactId1);
                            Element element3 = depElement.addElement("version");
                            element3.addText(version1);
                        }
                    }

                }
                // 调用下面的静态方法完成xml的写出
                //写入xml文件
                writeXml(document,pomPath);
                sleep();
            }else{
                System.out.println("依赖项为空！");
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void modifyDependencyManagement(){

    }

    /**
     * 实现对文件的复制
     * @param isFile inputFile
     * @param osFile outputFile
     */
    public static void copyFile(String isFile, String osFile) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(isFile);
            os = new FileOutputStream(osFile);
            byte[] data = new byte[1024];//缓存容器
            int len = -1;//接收长度
            while((len=is.read(data))!=-1) {
                os.write(data, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            //	释放资源 分别关闭 先打开的后关闭
            try {
                if(null!=os) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
            try {
                if(null!=is) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }




}
