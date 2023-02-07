package core;

import model.Dependency;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.List;

public class SingleModule {
    //单模块处理方案

    private static String projectPath;

    //构造函数
    SingleModule(){

    }

    SingleModule(String path){
        projectPath = path;
    }

    //解析出来的项目的依赖的集合
    private List<Dependency> dependencySet = new ArrayList<>();

    //所有依赖对应的更高版本的集合
    private List<List<Dependency>> higherSet = new ArrayList<>();

    //得到的依赖升级版本的结果集合
    private List<List<Dependency>> resultSet = new ArrayList<>();

    //无冲突的结果集
    private List<List<Dependency>> resWithoutConflict = new ArrayList<>();



    /**
     * 单模块项目的升级方案
     */
    public void singleModuleUpgrade() {
        //解析pom文件
        parsePom();
        //对于每一个依赖，向上搜索
        getHigherVersions();
        //获取最终的结果集
        getResults();
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
                    // TODO: 4/2/2023 关于${version}的解析
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

}
