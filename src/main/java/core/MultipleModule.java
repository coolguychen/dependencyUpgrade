package core;

import model.Dependency;
import model.DependencyTree;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import util.IOUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleModule extends SingleModule {
    //多模块处理方案 继承单模块处理方案

    private static String projectPath;

    private static List<String> fileList = new ArrayList<>();

    //pom文件路径 以及对应的依赖集合
    private static HashMap<String, List<Dependency>> filePath_dpList = new HashMap<>();

//    //pom文件路径 以及对应的可以升级的依赖集合
//    private static HashMap<String, List<Dependency>> filePath_dpList = new HashMap<>();

    //得到的依赖升级版本的结果集合
    private List<List<Dependency>> resultSet = new ArrayList<>();

    //pom文件路径 以及对应的升级的依赖的集合(笛卡尔积)
    private static HashMap<String, List<List<Dependency>>> filePath_resSet = new HashMap<>();

    //    private static List<Dependency> setToUpgradeParent = new ArrayList<>();
    //升级父模块的第三方库得到的结果集
    private static List<List<Dependency>> setUpgradedParent = new ArrayList<>();


    MultipleModule() {

    }

    MultipleModule(String path, List<String> list) {
        projectPath = path;
        fileList = list; //传递pom文件列表
    }


    /**
     * 多模块项目的升级方案
     */
    public void multipleModuleUpgrade() {
        parsePom();
        getHigherVersions();
    }

    /**
     *
     */
    @Override
    public void parsePom() {
        SAXReader sr = new SAXReader();
        for (int i = 0; i < fileList.size(); i++) {
            //获取pom文件路径
            String pomPath = fileList.get(i);
            System.out.println("解析"+pomPath+"的结果中...");
            //解析该pom文件使用的依赖列表
            List<Dependency> dependencyList = new ArrayList<>();
            try {
                Document document = sr.read(pomPath);
                Element root = document.getRootElement();
                String groupId, artifactId, version;
                Element dependencies = null;
                List<Element> list = new ArrayList<>();
                if (i == 0) { //父模块的pom特殊处理
                    dependencies = root.element("dependencyManagement").element("dependencies"); //获取到dependencies的节点
                } else { //子模块的pom
                    dependencies = root.element("dependencies"); //获取到dependencies的字段
                }
                list = dependencies.elements(); //dependencies下的子元素
                for (Element dependency : list) { //遍历全部dependency的相关信息
                    Element e = dependency.element("scope");
                    if (e != null) {
                        String scope = dependency.element("scope").getText();
                        if (scope.equals("test") || scope.equals("runtime"))
                            System.out.println("排除范围为" + scope + "的包");
                    } else {
                        groupId = dependency.element("groupId").getText();
//                System.out.println("groupId为：" + groupId);
                        artifactId = dependency.element("artifactId").getText();
//                System.out.println("artifactId为："+artifactId);
                        // TODO: 4/2/2023 关于${version}的解析
                        Element version_ele = dependency.element("version");
                        //如果版本号为空，说明已在父模块进行统一版本管理，跳过
                        //版本号不为空：
                        if(version_ele != null){
                            version = dependency.element("version").getText();
                            if (version.contains("${project.version}")) {
                                System.out.println("为本地模块，不考虑");
                            } else {
                                //加入待升级集合。
                                //新建一个Dependency
                                Dependency d = new Dependency(groupId, artifactId, version);
                                dependencyList.add(d);
                            }
                        }
                    }
                }
                //将文件路径及其对应的依赖列表放入hash表中
                filePath_dpList.put(pomPath, dependencyList);

            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void getHigherVersions() {
        for (Map.Entry<String, List<Dependency>> entry : filePath_dpList.entrySet()) {
            //对于map中的每一个pom文件，获取文件路径 & 使用的依赖，对每一个使用的依赖获取其更高版本：upgradedSet
            String pomPath = entry.getKey();
            List<List<Dependency>> upgradedSet = new ArrayList<>();
            List<Dependency> dependencyList = entry.getValue();
            for (Dependency d : dependencyList) {
                //获取到比dependency更高版本的集合
                List<Dependency> higherDependencySet = new ArrayList<>();
                try {
                    higherDependencySet = d.getHigherDependencyList();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (higherDependencySet.size() == 0) {
                    try {
                        higherDependencySet = d.getHigherDependencyList();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //获取dependency更高的依赖后，加入upgradedSet
                upgradedSet.add(higherDependencySet); //加入集合中
            }
            System.out.println("获取更高版本完毕。");
            System.out.println("-------------");
            System.out.println("--生成结果集--");
            //根据upgradedSet,生成笛卡尔乘积——>结果集，加入hashmap中，进行下一步的依赖调解
            List<List<Dependency>> resSet = new ArrayList<>();
            descartes(upgradedSet, resSet, 0, new ArrayList<>());
            filePath_resSet.put(pomPath, resSet);
        }
    }


    // TODO: 8/2/2023 多模块升级后的依赖冲突解决

    @Override
    public void conflictDetect() {
        //对每一个结果集 首先构建依赖树
        for (Map.Entry<String, List<List<Dependency>>> entry : filePath_resSet.entrySet()) {
            String filePath = entry.getKey();
            List<List<Dependency>> set = entry.getValue();
            String pomPath = filePath;
            String backUpPath = filePath.substring(0,filePath.lastIndexOf("/")) + "/backUpPom.xml";
            // TODO: 8/2/2023 先备份一下原来的pom文件。
            IOUtil ioUtil = new IOUtil();
            ioUtil.copyFile(pomPath, backUpPath);
            //对于集合中的每一个dependencyList，构建项目并定位依赖冲突位置
            for (List<Dependency> dependencyList : set) {
                DependencyTree dependencyTree = new DependencyTree();
                //修改原来的pom文件，输入pom文件路径和dependencyList
                ioUtil.modifyXmlByDom4J(pomPath, dependencyList);
                // TODO: 8/2/2023 多模块项目先进行mvn install 内部模块依赖关系
                dependencyTree.mvnInstall(projectPath);
                dependencyTree.constructTree(projectPath);
                dependencyTree.parseTreeMulti();
                //如果树存在conflict 加入待调解列表
                if (dependencyTree.isConflict()) {
//                resToMediate.add(dependencyTree);
                    System.out.println("加入待调解列表！");
                } else {
                    //否则加入无冲突结果集
//                resWithoutConflict.add(dependencyList);
                    System.out.println("无冲突，继续");
                }
            }
        }
    }

    @Override
    public void conflictMediation() {

    }
}
