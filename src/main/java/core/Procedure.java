package core;

import model.Dependency;
import model.DependencySet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.List;

/**
 * 解析的过程体
 */
public class Procedure {
    //依赖的集合
    private DependencySet dependencySet;

    private String filePath;

    public Procedure() {
        this.dependencySet =  new DependencySet();
    }

    /**
     * @param init
     */
    public Procedure(Init init) {
        dependencySet = new DependencySet();
        filePath = init.getFilePath();
    }

    /**
     * 通过项目的pom文件得到依赖。
     */
    public void parsePom() {
//        dependencySet = new DependencySet();
        System.out.println("解析结果中...");
        SAXReader sr = new SAXReader();
        try {
            //pom.xml文件
            Document document = sr.read(filePath + "/pom.xml");
            Element root = document.getRootElement();
            Element dependencies = root.element("dependencies"); //获取到dependencies的字段
            List<Element> list = dependencies.elements(); //dependencies下的子元素
            for (Element dependency : list) { //循环输出全部dependency的相关信息
                String groupId = dependency.element("groupId").getText();
//                System.out.println("groupId为：" + groupId);
                String artifactId = dependency.element("artifactId").getText();
//                System.out.println("artifactId为："+artifactId);
                String version = dependency.element("version").getText();
//                System.out.println("版本号为：" + version);
                //新建一个Dependency
                Dependency d = new Dependency(groupId, artifactId, version);
                //添加到项目依赖列表里面
                dependencySet.addToList(d);
            }
            for (Dependency d: dependencySet.getDependencySet()) {
                System.out.println(d.getGroupId() + " : " + d.getArtifactId() + " : " + d.getVersion());
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void upgradeVersion(){
        try {
            dependencySet.getUpVersions();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得最终结果集
     */
    public void getResult(){
        dependencySet.listToDescartes();
    }

}
