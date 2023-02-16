package core;

import model.Dependency;
import model.DependencyTree;

import java.io.File;
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
    public void upgradeProject() {
        if (type == Type.single) {
            SingleModule single = new SingleModule(projectPath);
            //调用单模块的解决方案
            single.singleModuleUpgrade();
            single.conflictDetect();
        } else {
            MultipleModule multi = new MultipleModule(projectPath, fileList);
            //调用多模块的解决方案
            multi.multipleModuleUpgrade();
            multi.conflictDetect();
            multi.printRes();
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
