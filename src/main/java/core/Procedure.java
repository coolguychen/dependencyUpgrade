package core;

import model.Dependency;
import model.DependencyTree;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import util.IOUtil;

import java.io.*;
import java.util.*;

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

    //无冲突的结果集
    private List<List<Dependency>> resWithoutConflict = new ArrayList<>();

//    //结果集对应的依赖树
//    private DependencyTree dependencyTree = new DependencyTree();


    //需要调解/升级的结果集
    private static List<DependencyTree> resToMediate = new ArrayList<>();

    //项目路径
    private String projectPath;

    private String filePath = "D:\\Graduation Project";

    private String treePath = "D:\\Graduation Project\\tree.txt";


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
            DependencyTree dependencyTree = new DependencyTree();
            IOUtil ioUtil = new IOUtil();
            ioUtil.writeXmlByDom4J(dependencyList);
            //根据生成的pom文件，执行mvn命令行 解析出依赖树
            dependencyTree.constructTree();
            dependencyTree.parseTree();
            //如果树存在conflict 加入待调解列表
            if (dependencyTree.isConflict()) {
                resToMediate.add(dependencyTree);
                System.out.println("加入待调解列表！");
            } else {
                //否则加入无冲突结果集
                resWithoutConflict.add(dependencyList);
                System.out.println("无冲突，继续");

            }
        }
        //如果无冲突的结果集不存在 进入冲突调解程序
        if (resWithoutConflict.size() == 0) {
            conflictMediation();
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

    public void conflictMediation() {
        //待冲突调解的结果集合
        for (DependencyTree tree : resToMediate) {
            //获取冲突依赖的集合
            HashMap<String[], List<Dependency>> conflictMap = tree.getConflictMap();
            //遍历map
            for (Map.Entry<String[], List<Dependency>> entry : conflictMap.entrySet()) {
                List<Dependency> conflictDepList = entry.getValue(); //获取冲突的集合
                String groupId = conflictDepList.get(0).getGroupId();
                String artifactId = conflictDepList.get(0).getArtifactId();
                //编写比较器 对象按照version从小到大
                Collections.sort(conflictDepList, new Comparator<Dependency>() {
                    @Override
                    public int compare(Dependency o1, Dependency o2) {
                        //选择version最大的 --version升序
                        //如果version1 == version2,按depth比较 --depth降序
                        //如果depth1 == depth2,按id比较 --id降序
                        if (o1.getVersion().equals(o2.getVersion())) {
                            if (o1.getDepth() == o2.getDepth()) {
                                return o1.getId() - o2.getDepth();
                            } else {
                                return o1.getDepth() - o2.getDepth();
                            }
                        } else {
                            return o2.getVersion().compareTo(o1.getVersion());
                        }
                    }
                });
                // TODO: 21/12/2022 最后跟实际的加载的依赖进行比较 实际加载的版本如何获取？

                Dependency latestDep = conflictDepList.get(conflictDepList.size() - 1);
                System.out.print("最后获得最新版本的依赖为：");
                latestDep.printDependency();

                List<Dependency> resList = tree.getResList();
                for (Dependency dependency : resList) {
                    if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                        System.out.println("与实际加载的依赖版本进行比较");
                        //如果实际加载的版本更新
                        if(dependency.getVersion().compareTo(latestDep.getVersion()) > 0 ) {
                            System.out.println("保留原来加载的版本");
                        }
                        //否则需要exclude实际加载的依赖
                        else {
                            System.out.print("exclude实际加载的依赖：");
                            dependency.printDependency();
                            if (conflictDepList.size() == 1) {
                                //如果map里面只有一个特殊处理？
                                // TODO: 30/12/2022 conflictMap中只有一个元素的情况
                                System.out.println("加载跳过");
                            } else {
                                for (int i = 0; i < conflictDepList.size() - 1; i++) {
                                    Dependency unLoadDependency = conflictDepList.get(i);
                                    Dependency parent = unLoadDependency.getParentDependency();
                                    System.out.print("建议父依赖：");
                                    parent.printDependency();
                                    System.out.print("需要exclude子依赖：");
                                    unLoadDependency.printDependency();
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }


    /**
     * 对所要加载依赖结果集，他们的javadoc和
     */



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
