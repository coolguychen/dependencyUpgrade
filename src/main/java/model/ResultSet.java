package model;

import java.util.ArrayList;
import java.util.List;

public class ResultSet {

    //基于该依赖集得到的向上版本的结果集
    private List<DependencySet> result;

    ResultSet(){
        result = new ArrayList<>();
    }

    public void addToList(DependencySet dpSet){
        result.add(dpSet);
    }

    public List<DependencySet> getResult() {
        return result;
    }

}
