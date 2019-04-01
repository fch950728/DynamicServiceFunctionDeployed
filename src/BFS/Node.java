package BFS;

import java.util.ArrayList;
/*
 * 为每个物理节点编号新建一个Node，添加以下属性：
 * hasvisited：是否被访问过；
 * distence：距离source的跳数
 * parent：上一条的ID
 * nodeID：当前节点的ID
 * 方便根据广度优先搜索来找到两点之间最少跳的路径。
 * */
public class Node {
	public boolean hasvisited;
	public int distence;
	public Integer parent;
	public Integer nodeID; 
	
	public Node(Integer id) {
		this.hasvisited=false;
		this.distence=0;
		this.parent=-1;
		this.nodeID=id;
	}
	public String toString() {
		return ""+this.nodeID;
		//return ""+"hasvisited="+this.hasvisited+",distence="+this.distence+",parent="+this.parent;
	}
}
