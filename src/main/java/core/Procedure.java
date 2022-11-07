package core;

import model.Dependency;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析的过程体
 */
public class Procedure {
    //解析出来的项目的依赖的集合
    private List<Dependency> dependencySet = new ArrayList<>();

    //依赖对应的更高版本的集合
    private List<List<Dependency>> higherSet = new ArrayList<>();

    //得到的依赖升级版本的结果集合
    private List<List<Dependency>> result = new ArrayList<>();

    //项目路径
    private String filePath;

    public Procedure() {
    }

    /**
     * @param init
     */
    public Procedure(Init init) {
        filePath = init.getFilePath();
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
                dependencySet.add(d);
            }
            System.out.println("获取到如下依赖：");
            for (Dependency d: dependencySet) {
                System.out.println(d.getArtifactId() + ":" + d.getVersion());
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对于dependencySet中的每个dependency，获取它更高的版本
     */
    public void getHigherVersions(){
        // 多线程并行 获取更高的版本
        for (Dependency d: dependencySet) {
            //获取到dependency更高版本的集合
            List<Dependency> higherDependencySet = new ArrayList<>();
            try {
                higherDependencySet = d.getHigherDependencyList();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // TODO: 7/11/2022  如果获取到的依赖集合大小为0，说明页面响应失败，重试

            while(higherDependencySet.size() == 0) {
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
     * @param dimensionValue
     *              原List
     * @param result
     *              通过乘积转化后的数组
     * @param layer
     *              中间参数
     * @param currentList
     *               中间参数
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
    public void getResults(){
        List<List<Dependency>> dimensionValue = higherSet;	// 原来的List
        List<List<Dependency>> res = new ArrayList<>(); //返回集合
        descartes(dimensionValue, res, 0, new ArrayList<>());
        //打印结果集信息
        for (List<Dependency> dp: res) {
            List<Dependency> list = new ArrayList<>();
//            System.out.println(dp.size()); //dp.size()为依赖数目
            for (Dependency d: dp) {
//                System.out.print(d.getGroupId() + ":" + d.getArtifactId() + ":"+ d.getVersion() + " ");
                list.add(d);
            }
            //加入结果集
            result.add(list);
        }
    }

    public void printRes(){
        System.out.println("共有" + result.size() + "个结果集。结果如下：");
        for(List<Dependency> list: result) {
            Dependency d = null;
            for (int i = 0; i < list.size()-1; i++) {
                d = list.get(i);
                System.out.print(d.getArtifactId() + ":"+ d.getVersion() + " & ");
            }
            d = list.get(list.size() - 1);
            System.out.println(d.getArtifactId() + ":"+ d.getVersion());
        }
    }
}
