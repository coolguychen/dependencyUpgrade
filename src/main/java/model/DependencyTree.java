package model;

import java.util.ArrayList;
import java.util.List;

//多叉树
public class DependencyTree {
    private Dependency root; //根节点

    private List<DependencyTree> childList; //子树集合

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

    public List<DependencyTree> getChildList() {
        return childList;
    }

    public void setChildList(List<DependencyTree> childList) {
        this.childList = childList;
    }

    public void appendTree() {

    }

    /**
     * 遍历结点 并打印. 同时按每个结点所在深度 在结点前打印不同长度的空格
     *
     * @param changeNode 根结点
     * @param depth      结点深度：1
     */
    public void queryAll(Dependency changeNode, int depth) {
        List<Dependency> sonList = changeNode.getSubDependency();
        String space = generateSpace(depth-1);    //根据深度depth,返回(depth*3)长度的空格

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


    // TODO: 21/11/2022 查看树中节点是否冲突（是否存在两个版本不同的第三包）

}
