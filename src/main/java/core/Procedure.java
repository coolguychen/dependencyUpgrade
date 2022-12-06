package core;

import model.Dependency;
import model.DependencyTree;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 解析的过程体
 */
public class Procedure {

    //解析出来的项目的依赖的集合
    private List<Dependency> dependencySet = new ArrayList<>();

    //所有依赖对应的更高版本的集合
    private List<List<Dependency>> higherSet = new ArrayList<>();

    //得到的依赖升级版本的结果集合
    private List<List<Dependency>> resultSet = new ArrayList<>();

    //项目路径
    private String projectPath;

    private static String filePath = "D:\\Graduation Project";

    //项目的依赖树
    private DependencyTree dependencyTree;

    /**
     * @param init
     */
    public Procedure(Init init) {
        projectPath = init.getFilePath();
    }

    /**
     * 通过项目的pom文件得到依赖。
     */
    public void parsePom() {
//        dependencySet = new DependencySet();
        System.out.print("解析结果中...");
        SAXReader sr = new SAXReader();
        try {
            //pom.xml文件
            Document document = sr.read(projectPath + "/pom.xml");
            Element root = document.getRootElement();
            Element dependencies = root.element("dependencies"); //获取到dependencies的字段
            List<Element> list = dependencies.elements(); //dependencies下的子元素
            for (Element dependency : list) { //循环输出全部dependency的相关信息
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
                    String version = dependency.element("version").getText();
//                System.out.println("版本号为：" + version);
                    //新建一个Dependency
                    Dependency d = new Dependency(groupId, artifactId, version);
                    //添加到项目依赖列表里面
                    dependencySet.add(d);
                }

            }
            System.out.println("获取到如下依赖：");
            for (Dependency d : dependencySet) {
                System.out.println(d.getArtifactId() + ":" + d.getVersion());
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对于dependencySet中的每个dependency，获取它更高的版本
     */
    public void getHigherVersions() {
        // 多线程并行 获取更高的版本
        for (Dependency d : dependencySet) {
            //获取到dependency更高版本的集合
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
            //依赖集合大小不为0
            higherSet.add(higherDependencySet); //加入集合中
        }
        System.out.println("获取更高版本完毕。");
    }

    /**
     * Discription: 笛卡尔乘积算法
     * 把一个List{[1,2],[A,B],[a,b]} 转化成
     * List{[1,A,a],[1,A,b],[1,B,a],[1,B,b],[2,A,a],[2,A,b],[2,B,a],[2,B,b]} 数组输出
     *
     * @param dimensionValue 原List
     * @param result         通过乘积转化后的数组
     * @param layer          中间参数
     * @param currentList    中间参数
     */
    public void descartes(List<List<Dependency>> dimensionValue, List<List<Dependency>> result, int layer, List<Dependency> currentList) {
        //中间参数小于列表
        if (layer < dimensionValue.size() - 1) {
            if (dimensionValue.get(layer).size() == 0) {
                //递归
                descartes(dimensionValue, result, layer + 1, currentList);
            } else {
                for (int i = 0; i < dimensionValue.get(layer).size(); i++) {
                    List<Dependency> list = new ArrayList<Dependency>(currentList);
                    list.add(dimensionValue.get(layer).get(i));
                    //递归 层数+1
                    descartes(dimensionValue, result, layer + 1, list);
                }
            }
        } else if (layer == dimensionValue.size() - 1) {
            if (dimensionValue.get(layer).size() == 0) {
                result.add(currentList);
            } else {
                for (int i = 0; i < dimensionValue.get(layer).size(); i++) {
                    List<Dependency> list = new ArrayList<Dependency>(currentList);
                    list.add(dimensionValue.get(layer).get(i));
                    result.add(list);
                }
            }
        }
    }

    /**
     * 获得最终结果集
     * 多列表笛卡尔积
     */
    public void getResults() {
        List<List<Dependency>> dimensionValue = higherSet;    // 原来的List
        List<List<Dependency>> res = new ArrayList<>(); //返回集合
        descartes(dimensionValue, res, 0, new ArrayList<>());
        //打印结果集信息
        for (List<Dependency> dp : res) {
            List<Dependency> list = new ArrayList<>();
//            System.out.println(dp.size()); //dp.size()为依赖数目
            for (Dependency d : dp) {
//                System.out.print(d.getGroupId() + ":" + d.getArtifactId() + ":"+ d.getVersion() + " ");
                list.add(d);
            }
            //加入结果集
            resultSet.add(list);
        }
    }

    /**
     * 打印可升级的依赖的结果集
     */
    public void printRes() {
        System.out.println("共有" + resultSet.size() + "个结果集。结果如下：");
        for (List<Dependency> list : resultSet) {
            Dependency d = null;
            for (int i = 0; i < list.size() - 1; i++) {
                d = list.get(i);
                System.out.print(d.getArtifactId() + ":" + d.getVersion() + " & ");
            }
            d = list.get(list.size() - 1);
            System.out.println(d.getArtifactId() + ":" + d.getVersion());
        }
        System.out.println("共有" + resultSet.size() + "个结果集。");
    }


    /**
     * 对result结果集中的结果进行冲突检测
     */
    public void conflictDetect() {
        //对每一个结果集 首先构建依赖树
        for (List<Dependency> dependencyList : resultSet) {
            // TODO: 4/12/2022 关于依赖冲突的判断，在一个新的pom.xml执行mvn dependency:tree 查看是否存在冲突，就不用手动解析依赖树
            IOUtil ioUtil = new IOUtil();
            ioUtil.writeXmlByDom4J(dependencyList);
            //根据生成的pom文件，执行mvn命令行 解析出依赖树
            ioUtil.constructTree();
        }
    }

//    public void constructTree(List<Dependency> dependencyList) {
//
//        for (Dependency d : dependencyList) {
//            //以依赖d为根节点构建出一棵依赖树
//            DependencyTree tree = new DependencyTree(d);
//            //查看/获取其传递依赖
//            d.getTransitiveDeps(tree);
//            d.printDependency();
//            System.out.println("的依赖树如下");
//            //打印该依赖树 初使深度为1
//            tree.queryAll(d, 1);
//        }
//    }
    

    public void defaultTest() {
        Dependency d11 = new Dependency("org.apache.httpcomponents", "httpclient", "4.5.12");
        Dependency d12 = new Dependency("org.apache.httpcomponents", "httpclient", "4.5.13");
        Dependency d21 = new Dependency("org.apache.poi", "poi-ooxml", "5.1.0");
        Dependency d22 = new Dependency("org.apache.poi", "poi-ooxml", "5.2.0");
        List<Dependency> list1 = Arrays.asList(d11, d21);
        List<Dependency> list2 = Arrays.asList(d11, d22);
        List<Dependency> list3 = Arrays.asList(d12, d21);
        List<Dependency> list4 = Arrays.asList(d12, d22);
        resultSet.addAll(Arrays.asList(list1, list2, list3, list4));
    }


}
