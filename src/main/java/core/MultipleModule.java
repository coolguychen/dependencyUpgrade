package core;

import model.Dependency;
import model.DependencyTree;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import util.IOUtil;

import java.util.*;

public class MultipleModule extends SingleModule {
    //多模块处理方案 继承单模块处理方案

    private static String projectPath;

    private static List<String> fileList = new ArrayList<>();

    //pom文件路径 以及对应的依赖集合
    private static HashMap<String, List<Dependency>> filePath_dpList = new HashMap<>();

    //特殊处理：父模块对应的集合和升级方案--最后处理
    // TODO: 16/2/2023
    private static List<Dependency> parentDependencyManagement = new ArrayList<>();

    //pom文件路径 以及对应的升级的依赖的集合(笛卡尔积)
    private static HashMap<String, List<List<Dependency>>> filePath_resSet = new HashMap<>();

    //无冲突的结果集
    private static HashMap<String, List<List<Dependency>>> filePath_resWithoutConflict = new HashMap<>();

    //需要调解/升级的结果集
    private static HashMap<String,List<DependencyTree>> filePath_resToMediate = new HashMap<>();

    MultipleModule(String path, List<String> list) {
        projectPath = path;
        fileList = list; //传递pom文件列表
    }

    /**
     * 多模块项目的升级方案
     */
    public void multipleModuleUpgrade() {
        try {
            parsePom();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getHigherVersions();
    }

    /**
     *
     */
    @Override
    public void parsePom() throws InterruptedException {
        SAXReader sr = new SAXReader();
        for (int j = 0; j < fileList.size(); j++) {
            //获取pom文件路径
            String pomPath = fileList.get(j);
            System.out.println("解析" + pomPath + "的结果中...");
            //解析该pom文件使用的依赖列表
            List<Dependency> dependencyList = new ArrayList<>();
            try {
                Document document = sr.read(pomPath);
                Element root = document.getRootElement();
                String groupId, artifactId, version;
                Element dependencies;
                List<Element> list;
                if (j == 0) { //父模块的pom特殊处理
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
                        if (version_ele != null) {
                            version = dependency.element("version").getText();
                            if (version.contains("${project.version}")) {
//                                System.out.println("为本地模块，不考虑");
                            } else {
                                //加入待升级集合。
                                //新建一个Dependency
                                Dependency d = new Dependency(groupId, artifactId, version);
                                dependencyList.add(d);
                            }
                        }
                        //版本号为空 默认latest / 父模块管理
                        else{
                            // TODO: 16/2/2023
                        }
                    }
                }
                if(j == 0) {
                    parentDependencyManagement = dependencyList;
                }
                else {
                    //将文件路径及其对应的依赖列表放入hash表中
                    filePath_dpList.put(pomPath, dependencyList);
                }
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void getHigherVersions() {
        //对于子模块 分别找到可升级的结果集
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
            System.out.println("-----------获取更高版本完毕。------------");
            System.out.println("--生成结果集--");
            //根据upgradedSet,生成笛卡尔乘积——>结果集，加入hashmap中，进行下一步的依赖调解
            List<List<Dependency>> resSet = new ArrayList<>();
            descartes(upgradedSet, resSet, 0, new ArrayList<>());
            filePath_resSet.put(pomPath, resSet);
        }
    }

    @Override
    public void conflictDetect() {
        //对每一个结果集 首先构建依赖树
        for (Map.Entry<String, List<List<Dependency>>> entry : filePath_resSet.entrySet()) {
            String filePath = entry.getKey(); //pom文件路径
            List<List<Dependency>> set = entry.getValue(); //对应的升级方案
            String pomPath = filePath;
            //pom文件上一层目录地址 比如 A/pom.xml -> A
            String parentPath = filePath.substring(0, filePath.lastIndexOf("/"));
            String backUpPath = parentPath + "/backUpPom.xml";
            // TODO: 8/2/2023 先备份一下原来的pom文件。
            IOUtil ioUtil = new IOUtil();
            //备份文件
            ioUtil.copyFile(pomPath, backUpPath);
            //对于集合中的每一个dependencyList（升级方案），构建项目并定位依赖冲突位置
            for (List<Dependency> dependencyList : set) {
                DependencyTree dependencyTree = new DependencyTree();
                //修改原来的pom文件，输入pom文件路径和dependencyList，根据dependencyList修改pom文件
                ioUtil.modifyDependenciesXml(pomPath, dependencyList);
                // TODO: 8/2/2023 多模块项目先进行mvn install 内部模块依赖关系
//                dependencyTree.mvnInstall(projectPath);
                // TODO: 15/2/2023 对每个pom文件构建依赖树
                dependencyTree.constructTree(parentPath);
                dependencyTree.parseTree(parentPath + "/tree.txt");
                //如果树存在conflict 加入待调解列表
                if (dependencyTree.isConflict()) {
                    System.out.println("加入待调解列表！");
                    //如果存在key 添加进列表
                    if(filePath_resToMediate.containsKey(parentPath)) {
                        filePath_resToMediate.get(parentPath).add(dependencyTree);
                    }
                    //否则新建
                    else{
                        List<DependencyTree> list = new ArrayList<>();
                        list.add(dependencyTree);
                        filePath_resToMediate.put(parentPath, list);
                    }
                } else {
                    //否则加入无冲突结果集
                    if(filePath_resWithoutConflict.containsKey(parentPath)) {
                        filePath_resWithoutConflict.get(parentPath).add(dependencyList);
                    }
                    //否则新建
                    else{
                        List<List<Dependency>> list = new ArrayList<>();
                        list.add(dependencyList);
                        filePath_resWithoutConflict.put(parentPath, list);
                    }
                    System.out.println("对于模块" + parentPath + "无冲突，继续");
                }
            }
            // TODO: 15/2/2023 然后对其进行依赖调解
            //如果无冲突的结果集不存在 进入冲突调解程序
            if (filePath_resWithoutConflict.get(parentPath).size() == 0) {
                System.out.println("对于模块" + parentPath + ":");
                conflictMediation(filePath_resToMediate.get(parentPath));
            }
            //重新恢复pom文件为backUpPom
            ioUtil.copyFile(backUpPath, pomPath);

            System.out.println("======分割线======下一个模块=======");
        }
    }

    public void conflictMediation(List<DependencyTree> resToMediate) {
        //待冲突调解的结果集合
        //遍历filePath_resToMediate

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

    //打印无冲突的结果
    public void printRes() {
        for(Map.Entry<String, List<List<Dependency>>> entry: filePath_resWithoutConflict.entrySet()) {
            String pomPath = entry.getKey();
            System.out.println("对于模块" + pomPath + ", 有以下升级且无冲突的结果集。");
            List<List<Dependency>> resWithoutConflict = entry.getValue();
            int i = 0;
            for(List<Dependency> dependencyList : resWithoutConflict) {
                System.out.println("结果集" + i + ":");
                i++;
                for(Dependency d: dependencyList) {
                    d.printDependency();
                }
                System.out.println("=================");
            }
        }
    }

    // TODO: 17/2/2023 提取结果集的公共依赖，在父模块进行统一管理


}
