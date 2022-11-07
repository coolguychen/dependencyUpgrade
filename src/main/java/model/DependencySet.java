package model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.checkerframework.checker.units.qual.C;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class DependencySet {
    //依赖的集合
    private List<Dependency> dependencySet;

    private List<DependencySet> setList = new ArrayList<>();

    private List<List<Dependency>> list = new ArrayList<>();

    private List<List<Dependency>> result = new ArrayList<>();


    /**
     * 构造函数
     */
    public DependencySet(){
        dependencySet = new ArrayList<>();
        setList = new ArrayList<>();
    }

    /**
     * 列表里添加一个依赖
     * @param d
     */
    public void addToList(Dependency d){
        dependencySet.add(d);
    }

    /**
     * 获取依赖列表
     * @return
     */
    public List<Dependency> getDependencySet(){
        return this.dependencySet;
    }

    public int getSetSize(){
        return this.dependencySet.size();
    }

    public Dependency getDependencyByIndex(int i){
        return this.dependencySet.get(i);
    }

    /**
     * 对于dependencySet中的每个dependency，获取它更高的版本
     */
    public void getUpVersions() throws InterruptedException{
        // 多线程并行 获取更高的版本
        for (Dependency d: dependencySet) {
            //获取到dependency更高版本的集合
            DependencySet upDpSet = null;
            try {
                upDpSet = d.getUpDpSet(); //传入计数器
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // TODO: 7/11/2022  如果获取到的依赖集合大小为0，说明页面响应失败，重试

            //依赖集合大小不为0
            setList.add(upDpSet); //加入集合中
            list.add(upDpSet.getDependencySet());
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
     * 将多列表转换为笛卡尔积的形式
     */
    public void listToDescartes(){
        List<List<Dependency>> dimensionValue = list;	// 原来的List
        List<List<Dependency>> res = new ArrayList<>(); //返回集合
        descartes(dimensionValue, res, 0, new ArrayList<>());
        //打印结果集信息
        for (List<Dependency> dp: res) {
//            System.out.println(dp.size()); //dp.size()为依赖数目
            for (Dependency d: dp) {
                System.out.println(d.getGroupId() + " " + d.getVersion());
            }
        }

    }





}
