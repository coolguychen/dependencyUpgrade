package core;

import com.beust.ah.A;
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
    private enum Type {single, multiple}

    //项目的模块类型
    private Type type;

    //pom文件路径的集合
    private static List<String> fileList = new ArrayList<>();

    //解析出来的项目的依赖的集合
    private List<Dependency> dependencySet = new ArrayList<>();

    //所有依赖对应的更高版本的集合
    private List<List<Dependency>> higherSet = new ArrayList<>();

    //得到的依赖升级版本的结果集合
    private List<List<Dependency>> resultSet = new ArrayList<>();

    //无冲突的结果集
    private List<List<Dependency>> resWithoutConflict = new ArrayList<>();

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

    public void checkType() {
        int cnt = recursive(projectPath);
        if (cnt > 1) type = Type.multiple;
        else type = Type.single;
    }

    private int recursive(String path) {
        int num = 0;
        File file = new File(path);
        String childPath = path + "/pom.xml";
        File pom_file = new File(childPath);
        //若pom文件不存在
        if (!pom_file.exists()) {
//            System.out.println("该路径下无pom.xml");
            return 0;
        }
        //若存在
        else {
            //将文件路径保存下来
            fileList.add(childPath);
            //列出目录下的文件
            File[] fs = file.listFiles();
            for (File f : fs) {
                if (f.isDirectory() && !f.getName().contains("target"))    //若是目录且不是target目录，则递归查看是否存在pom文件
                    num = 1 + recursive(f.getPath());
            }
            return num;
        }
    }

    /**
     * 项目升级和依赖调解程序
     */
    public void upgradeProject(){
        if(type == Type.single) {
            SingleModule single = new SingleModule(projectPath);
            //调用单模块的解决方案
            single.singleModuleUpgrade();
            single.conflictDetect();
        }
        else {
            MultipleModule multi = new MultipleModule(projectPath, fileList);
            //调用多模块的解决方案
            multi.multipleModuleUpgrade();
            multi.conflictDetect();
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
                Dependency latestDep = conflictDepList.get(conflictDepList.size() - 1);
                System.out.print("最后获得最新版本的依赖为：");
                latestDep.printDependency();
                List<Dependency> resList = tree.getResList();
                for (Dependency dependency : resList) {
                    if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                        System.out.println("与实际加载的依赖版本进行比较");
                        //如果实际加载的版本更新
                        if (dependency.getVersion().compareTo(latestDep.getVersion()) > 0) {
                            System.out.println("保留原来加载的版本");
                        }
                        //否则需要exclude实际加载的依赖
                        else {
                            System.out.print("exclude实际加载的依赖：");
                            dependency.printDependency();
                            if (conflictDepList.size() == 1) {
                                //如果map里面只有一个特殊处理？
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
