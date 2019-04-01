package base.graph;

import traffic.SFC;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by ly on 2017/3/8.
 */
public class CPath {
    private float pathDelay;
    private int vnfDeployPosition;		//该条路径上，用于放置VNF的点，这个变量用于在每个域中部署子链时
    public LinkedHashMap<Integer, Integer> vertexMap = new LinkedHashMap<Integer, Integer>();
    //	public Map<Integer, Integer> edgeMap = new HashMap<Integer, Integer>();
    public LinkedList<Integer> vertexList = new LinkedList<>();	//路径所经过的点
    public LinkedList<Integer> edgeList = new LinkedList<>();		//路径所经过

    //下面两个链表，主要用于存放，在部署功能时，最后一个功能，需要计算某点到源 + 某点到目的的整个路径
    public LinkedList<Integer> firstEdgeList = new LinkedList<Integer>();	//表示路径的前半部分
    public LinkedList<Integer> secondEdgeList = new LinkedList<Integer>();	//表示路径的后半部分

    public CPath() {

    }

    /**
     * 路径节点集
     * @param list
     * @param graph
     */
    public CPath(LinkedList<Integer> list, CGraph graph) {
        vertexList.addAll(list);
        for (int i = 0; i < vertexList.size()-1; i++) {
            int edgeId = judgeEdgeId(i, list, graph);   //这里可能返回的edgeId可能为空

            edgeList.addLast(edgeId);
        }
    }

    /**
     *
     * @param list  路径节点集
     * @param graph
     */
    public CPath(LinkedList<Integer> list, CGraph graph, SFC sfc) {

        this(list, graph);
        addSFCID(vertexList, graph, sfc);
    }

    public CPath (LinkedList<Integer> list, CGraph graph, int deployPosition, SFC sfc) {
        vertexList.addAll(list);
        int index = list.indexOf(deployPosition);
//		System.out.println("deployPosition == " + index);
        for (int i = 0; i < index; i++) {
            int edgeId = judgeEdgeId(i, list, graph);   //这里可能返回的edgeId可能为空
            edgeList.addLast(edgeId);
            firstEdgeList.addLast(edgeId);
        }

        for (int i = index; i < list.size() - 1; i++) {
            int edgeId = judgeEdgeId(i, list, graph);   //这里可能返回的edgeId可能为空
            edgeList.addLast(edgeId);
            secondEdgeList.addLast(edgeId);
        }

        addSFCID(vertexList, graph, sfc);
    }

    //为部署路径上每个顶点的sfcList添加SFC编号
    public void addSFCID(List<Integer> list, CGraph graph, SFC sfc) {
        for (Integer vertexKey : list) {
            CVertex vertex = graph.idVertexMap.get(vertexKey);
            vertex.sfcList.add(sfc.getSfcId());
        }
    }

    //判断获取边ID
    public int judgeEdgeId(int i, LinkedList<Integer> list, CGraph graph) {
        int source = list.get(i);
        Integer edgeId = null;
        Map<Integer, Integer> sourceSinkMap = graph.sourceSinkEdgeMap.get(source);
        for(int sinkKey : sourceSinkMap.keySet()) {
            if(sinkKey == list.get(i+1)) {
                edgeId = sourceSinkMap.get(sinkKey);
                break;
            }
        }
        return edgeId;
    }

    /**
     * 获取路径的可靠性
     * @param graph
     * @return
     */
    public float getPathReliability(CGraph graph) {
        float reliability = 1;
        for(int vertexKey : vertexList) {
            reliability *= graph.idVertexMap.get(vertexKey).getReliability();
        }
        for(int edgeKey : edgeList) {
            CEdge edge = graph.idEdgeMap.get(edgeKey);
            reliability *= edge.getReliability();
        }
        return reliability;
    }

    public float getPathDelay() {
        return pathDelay;
    }

    public void setPathDelay(float pathDelay) {
        this.pathDelay = pathDelay;
    }

    public int getVnfDeployPosition() {
        return vnfDeployPosition;
    }

    public void setVnfDeployPosition(int vnfDeployPosition) {
        this.vnfDeployPosition = vnfDeployPosition;
    }

    public void printPath() {
        System.out.println("路径经过的顶点集为：");
        for(int id : vertexList) {
            System.out.print(id + " -> ");
        }
        System.out.println();
        System.out.println("路径经过的边集为：");
        for(int id : edgeList) {
            System.out.print(id + " -> ");
        }
    }

}
