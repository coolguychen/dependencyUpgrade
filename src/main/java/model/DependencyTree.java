package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//多叉树
public class DependencyTree {
    private static BufferedReader br; //读入流
    private Dependency root; //根节点
    private static boolean isConflict;
    private static List<Dependency> resList = new ArrayList<>();
    //冲突的依赖的集合
    //Key为groupId&artifactId, Value是对应的冲突的依赖的List
    private static HashMap<String[], List<Dependency>> conflictMap = new HashMap<>();

    private static List<DependencyTree> childList; //子树集合

    public DependencyTree() {
        isConflict = false;
    }

    /**
     * 构造函数
     *
     * @param root
     */
    public DependencyTree(Dependency root) {
        this.root = root;
        this.childList = new ArrayList<>();
    }

    /**
     * 构造函数
     *
     * @param _root
     * @param _childList
     */
    public DependencyTree(Dependency _root, List<DependencyTree> _childList) {
        this.root = _root;
        this.childList = _childList;
    }

    public Dependency getRoot() {
        return root;
    }

    public void setRoot(Dependency root) {
        this.root = root;
    }

    public static void setIsConflict(boolean isConflict) {
        DependencyTree.isConflict = isConflict;
    }

    public List<DependencyTree> getChildList() {
        return childList;
    }

    public void setChildList(List<DependencyTree> childList) {
        this.childList = childList;
    }

    public static List<Dependency> getResList() {
        return resList;
    }

    public HashMap<String[], List<Dependency>> getConflictMap() {
        return conflictMap;
    }

    public boolean isConflict() {
        return isConflict;
    }

    /**
     * 遍历结点 并打印. 同时按每个结点所在深度 在结点前打印不同长度的空格
     *
     * @param changeNode 根结点
     * @param depth      结点深度：1
     */
    public void queryAll(Dependency changeNode, int depth) {
        List<Dependency> sonList = changeNode.getSubDependency();
        String space = generateSpace(depth - 1);    //根据深度depth,返回(depth*3)长度的空格

        if (sonList == null || sonList.isEmpty()) {
            return;
        }

        for (int i = 0; i < sonList.size(); i++) {
            System.out.println(space + "--"      //打印空格 和结点id，name
                    + "<" + sonList.get(i).getGroupId() + ">"
                    + "<" + sonList.get(i).getArtifactId() + ">"
                    + "<" + sonList.get(i).getVersion() + ">"
            );

            if (i == 0) {
                depth = depth + 1;  //结点深度+1，每个for循环仅执行一次。作为子结点sonList.get(i)的深度
            }
            queryAll(sonList.get(i), depth);
        }

    }

    //打印空格
    public static String generateSpace(int count) {
        count = count * 2;
        char[] chs = new char[count];
        for (int i = 0; i < count; i++) {
            chs[i] = ' ';
        }
        return new String(chs);
    }

    /**
     * 根据pom文件执行命令行：mvn dependency:tree -Dverbose生成tree.txt
     *
     * @param rootPath 根目录路径
     */
    public static void constructTree(String rootPath) {
        try {
            Runtime runtime = Runtime.getRuntime();
            System.out.println("正在构造依赖关系......");
            Process process = runtime.exec(new String[]{"cmd", "/c", "mvn dependency:tree -Dverbose > tree.txt"}, null, new File(rootPath));
            //等待线程运行结束
            process.waitFor();
            System.out.println("构造完毕，输出tree.txt");
            printTree(rootPath + "/tree.txt");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印tree.txt中的信息
     * @param treePath tree文件路径
     */
    public static void printTree(String treePath) {
        try {
            //读入流 读取tree.txt 并打印
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(treePath)); // 建立一个输入流对象reader
            BufferedReader br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
            String line = "";
            while (line != null) {
                line = br.readLine(); // 一次读入一行数据
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 输入tree文件路径，解析依赖树
     * @param treePath
     */
    public static void parseTree(String treePath) {
        int cnt = 0;
        try {
            //读入流 读取tree.txt 并打印
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(treePath)); // 建立一个输入流对象reader
            br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
            String currentLine = "";
            //父依赖（直接依赖）
            Dependency parentDependency = null;
            while ((currentLine = br.readLine()) != null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int tem = currentLine.indexOf('+'); // '+'为依赖的开头
                int index = tem == -1 ? currentLine.indexOf('\\') : tem;
                int depth = (index - 7) / 3; //depth为父节点深度
                //说明定位到了依赖所在的行
                if (index != -1) {
                    //depth小于0 说明是最后一行 退出
                    if (currentLine.contains("Finished")) {
                        System.out.println("last line:" + currentLine);
                        break;
                    }
                    String[] info = currentLine.split(":"); //以“:“作为切割
                    String groupId;
                    String artifactId = info[1];
                    //中间是jar
                    String version = info[3];
                    boolean conflict = false;
                    boolean duplicate = false; //冲突和重复都先设为false
                    //info[2]都是jar 因此跳过
                    //如果包含括号，说明有相应的报错
                    //eg. [INFO]    |  +- (commons-codec:commons-codec:jar:1.15:compile - omitted for conflict with 1.11)
                    if (currentLine.contains("(")) {
                        //groupId & scope特殊处理一下
                        groupId = info[0].substring(index + 4);
                        if (currentLine.contains("conflict")) {
                            //出现冲突
                            conflict = true;
                            //打印冲突信息
                            System.out.println("存在依赖冲突！");
                            System.out.println("冲突位置在：" + currentLine);
                            setIsConflict(true); //标记这颗树是存在冲突的
                        } else if (currentLine.contains("duplicate")) {
//                            System.out.println("重复依赖！");
                            duplicate = true;
                        }
                    }
                    //不含括号，正常解析
                    else {
                        groupId = info[0].substring(index + 3);
                    }
                    String[] key = new String[]{groupId, artifactId};

                    //如果depth为0 设置为父节点
                    if (depth == 0) {
                        parentDependency = new Dependency(groupId, artifactId, version);
                    }
                    if (conflict == true) {
                        Dependency conflictDependency = new Dependency(cnt++, groupId, artifactId, version, depth, parentDependency);
                        //判断该依赖是否已经存在于map中
                        if (conflictMap.containsKey(key)) {
                            conflictMap.get(key).add(conflictDependency);
                        }
                        //如果不存在 新加进去
                        else {
                            List<Dependency> list = new ArrayList<>();
                            list.add(conflictDependency);
                            conflictMap.put(key, list);
                        }
                    } else { //如果不含conflict 正常加入resList
                        //依赖结果加入resList
                        Dependency dependency = new Dependency(cnt++, groupId, artifactId, version, depth, parentDependency);
                        resList.add(dependency);
                    }
                }
//                System.out.println(currentLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String testPath = "D:\\1javawork\\multiModelDemo\\B";
        constructTree(testPath);
        parseTree(testPath + "\\tree.txt");
    }
}
