package core;

import model.Dependency;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.List;

public class MultipleModule extends SingleModule{
    //多模块处理方案 继承单模块处理方案

    private static String projectPath;
    private static List<Dependency> setToBeUpgradedParent = new ArrayList<>();
    //升级父模块的第三方库得到的结果集
    private static List<List<Dependency>> setUpgradedParent = new ArrayList<>();

    MultipleModule(){

    }

    MultipleModule(String path){
        projectPath = path;
    }


    /**
     * 多模块项目的升级方案
     */
    public void multipleModuleUpgrade() {
        parseParentPom();
        getHigherVersions();
        getResults();
    }

    private void parseParentPom() {
        // 对于根目录下的pom，也就是父模块的项目，dependencyManagement
        System.out.print("解析父模块的pom文件");
        SAXReader sr = new SAXReader();
        try {
            //pom.xml文件
            Document document = sr.read(projectPath + "/pom.xml");
            Element root = document.getRootElement();
            Element dependencies = root.element("dependencyManagement").element("dependencies"); //获取到dependencies的节点
            List<Element> list = dependencies.elements(); //dependencies下的子元素

            //解析父pom中的依赖
            for(Element dependency: list) {
                Element e = dependency.element("scope");
                if (e != null) {
                    String scope = dependency.element("scope").getText();
                    if (scope.equals("test") || scope.equals("runtime"))
                        System.out.println("排除范围为" + scope + "的包");
                } else {
                    String groupId = dependency.element("groupId").getText();
//                System.out.println("groupId为：" + groupId);
                    String artifactId = dependency.element("artifactId").getText();
//                System.out.println("artifactId为："+artifactId);
                    // TODO: 4/2/2023 关于${version}的解析
                    String version = dependency.element("version").getText();
//                System.out.println("版本号为：" + version);
                    if(version.contains("${project.version}")) {
                        System.out.println("为本地模块，不考虑");
                    }
                    else{
                        //加入待升级集合。
                        //新建一个Dependency
                        Dependency d = new Dependency(groupId, artifactId, version);
                        setToBeUpgradedParent.add(d);
                    }
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }


    /**
     * 重写升级方法？
     */
    @Override
    public void getHigherVersions() {

    }





}
