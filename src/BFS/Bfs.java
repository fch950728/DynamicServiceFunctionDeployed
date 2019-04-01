package BFS;
import java.util.ArrayList;
/*
 * 根据广度优先搜来查找两点之间最少跳数的路径：
 * 
 * */
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import base.graph.*;

import BFS.Node;
public class Bfs {

	
	//递归地将Map<Integer, Node> gNode里记录的从source到sink的路径点添加到path
	public static LinkedList<Integer> getpath(Map<Integer, Node> gNode,LinkedList<Integer> path,Integer source,Integer cur){
		if(gNode.get(cur).parent==source) path.add(source);
		else if (gNode.get(cur).parent==-1) {
			System.out.println("no path from source to sink exists");
			return null;
		}
		else {
			getpath(gNode, path, source, gNode.get(cur).parent);
			
		}
		path.add(cur);
		return path;
	}
	public static PathAndDistence bfsFind(CGraph graph,Integer source,Integer sink) {
		Map<Integer, Node> gNode=new HashMap<>();//用于建立节点编号和Node之间的关系
		for(Integer ver:graph.vertexList) {//为每一个节点新建一个Node，并建立节点编号与Node的关系
			Node node =new Node(ver);
			gNode.put(ver, node);
		}
		//System.out.println("gNode="+gNode);
		gNode.get(source).hasvisited=true;//从源节点开始
		LinkedList<Node> queue =new LinkedList<>();//新建队列
		queue.add(gNode.get(source));//将源节点的Node压进队列
		while(queue.size()!=0) {
			Node knode=queue.pollFirst();
			CVertex vertex=graph.idVertexMap.get(knode.nodeID);//得到该节点编号对应的物理节点
			for(Integer edgekey:vertex.outsideEdgeList) {//遍历该物理节点的出度
				CEdge edge = graph.idEdgeMap.get(edgekey);//得到出度的边
				Integer otherside=edge.getSinkId();//该边的另一个节点编号

				if(!gNode.get(otherside).hasvisited) {
					gNode.get(otherside).hasvisited=true;
					gNode.get(otherside).distence++;
					gNode.get(otherside).parent=knode.nodeID;
					queue.add(gNode.get(otherside));
				}
//				if(otherside==sink) {//如果找到了sink，则不需要找了，把队列清零
//					System.out.println("otherside==sink");
//					queue.clear();
//					break;
//				}
			}
		}
		
		LinkedList<Integer> path=new LinkedList<>();
		path=getpath(gNode,path,source,sink);
		if(path==null) {
			return null;
		}
		Integer disten=gNode.get(sink).distence;
		PathAndDistence pathAndDistence=new PathAndDistence(path,null,disten);
		return pathAndDistence;
	}
	
	public static PathAndDistence bfsFind2(CGraph graph,Integer source,Integer sink,int vnfbw,ArrayList<Integer> usedver) {
		Map<Integer, Node> gNode=new HashMap<>();//用于建立节点编号和Node之间的关系
		for(Integer ver:graph.vertexList) {//为每一个节点新建一个Node，并建立节点编号与Node的关系
			Node node =new Node(ver);
			gNode.put(ver, node);
		}
//		System.out.println("gNode="+gNode);
//		System.out.println("source="+source);
		gNode.get(source).hasvisited=true;//从源节点开始
		LinkedList<Node> queue =new LinkedList<>();//新建队列
		queue.add(gNode.get(source));//将源节点的Node压进队列
		while(queue.size()!=0) {
			Node knode=queue.pollFirst();
			CVertex vertex=graph.idVertexMap.get(knode.nodeID);//得到该节点编号对应的物理节点
			for(Integer edgekey:vertex.outsideEdgeList) {//遍历该物理节点的出度
				CEdge edge = graph.idEdgeMap.get(edgekey);//得到出度的边
				if(edge.getRemainBandWidthResource()<vnfbw) {
					continue;
				}
				Integer otherside=edge.getSinkId();//该边的另一个节点编号
				if(usedver.contains(otherside)) {
					continue;
				}
				if(!gNode.get(otherside).hasvisited) {
					gNode.get(otherside).hasvisited=true;
					gNode.get(otherside).distence++;
					gNode.get(otherside).parent=knode.nodeID;
					queue.add(gNode.get(otherside));
				}
				if(otherside==sink) {//如果找到了sink，则不需要找了，把队列清零
//					System.out.println("otherside==sink");
					queue.clear();
					break;
				}
			}
		}
		
		LinkedList<Integer> path=new LinkedList<>();
		path=getpath(gNode,path,source,sink);
		if(path==null) {
			return null;
		}
		LinkedList<Integer> edgepath=new LinkedList<>();
		for(int i=1;i<path.size();i++) {
			for(Integer edgekey:graph.idVertexMap.get(path.get(i-1)).outsideEdgeList) {
				CEdge edge = graph.idEdgeMap.get(edgekey);
				if(edge.getSinkId()==path.get(i)) {
					edgepath.add((Integer)edge.getEdgeId());
				}
	    	}
		}
		Integer disten=gNode.get(sink).distence;
		PathAndDistence pathAndDistence=new PathAndDistence(path,edgepath, disten);
		return pathAndDistence;
	}
	
	
	public static void printgragh(CGraph graph) {
		for(Integer ver:graph.vertexList) {
			System.out.print("ver"+ver+":  ");
			CVertex vertex=graph.idVertexMap.get(ver);
			for(Integer edgekey:vertex.outsideEdgeList) {//遍历该物理节点的出度
				CEdge edge = graph.idEdgeMap.get(edgekey);//得到出度的边
				System.out.print(edge.getSinkId()+"  ");
			}
			System.out.println();
		}
	}

}
