package traffic;

import base.Configure;
import base.graph.CEdge;
import base.graph.CGraph;
import base.network.Link;
import base.network.Node;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 部署方案类
 * Created by ly on 2017/2/28.
 */
public class Plan {
    private int sfcId;
    private float delay;

    //功能节点部署在虚拟网络上的节点对，功能节点，虚拟网络节点
    public Map<Integer, Integer> nodeDeployVertexMap = new HashMap<>();
    //SFC链路（只包含两个点的链路）在底层网络上的部署路径（这其中包括作为转发节点的中间节点）
    public Map<Integer, List<Integer>> linkDeployEdgeMap = new HashMap<>();
    public LinkedList<Integer> nodeForwardList = new LinkedList<>();		//部署方案中，作为转发节点的集合
    public Plan() {

    }
    
    /*
	 * 计算该Plan中有哪些转发节点。
	 */
	public void computeForwordNodes(SFC sfc, CGraph graph) {
		for (Integer linkKey : linkDeployEdgeMap.keySet()) {
			for (Integer edgeKey : linkDeployEdgeMap.get(linkKey)) {
				CEdge edge = graph.idEdgeMap.get(edgeKey);
				if (!nodeDeployVertexMap.containsValue(edge.getSourceId()) && !nodeForwardList.contains(edge.getSourceId())) {
					if(edge.getSourceId()!=sfc.getSourceId()) {
						nodeForwardList.add(edge.getSourceId());
					}
				}
				
			}
		}
	}
    /*
    * 调整部署方案    链路带宽需求最小的后面的点依次往后面移动
    *
    * 调整后会导致阻塞率增加，因为破坏了负载均衡，会导致有些点上部署太多功能而有些点上功能数很少，不均衡导致部署失败
    * */
    public Plan adjustPlan(CGraph graph, SFC sfc) {
        float minBandwidth = Configure.INF;
        int minLinkId = Configure.IMPOSSIBLENODE;
        int lastId = sfc.linkList.getLast();
//        System.out.println("SFC的链路带宽需求：");
        //遍历找到最小带宽需求的sfc链路
        for(int linkId : sfc.linkList) {
            Link link = sfc.linkMap.get(linkId);
//            System.out.print(link.getBandWidthResourceDemand() + "    ");
            if(minBandwidth >= link.getBandWidthResourceDemand()) {
                minBandwidth = link.getBandWidthResourceDemand();
                minLinkId = linkId; //获取带宽需求最小的链路ID
            }
            if(minLinkId == lastId) {
                return this;
            }
        }
//        System.out.println();
        int moveNum = lastId-minLinkId; //需要往后移动的功能数

        //将该部署方案经过的所有点集加入集合中
        List<Integer> pathList = new ArrayList<>();
        int tmp = 0;    //只是为了显示是第一个节点时使用
        for(int linkKey : linkDeployEdgeMap.keySet()) {
            List<Integer> tmpList = linkDeployEdgeMap.get(linkKey);
            for(int edgeKey : tmpList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                if(tmp == 0) {
                    pathList.add(edge.getSourceId());
                    pathList.add(edge.getSinkId());
                    tmp++;
                }
                else {
                    pathList.add(edge.getSinkId());
                }
            }
        }
        int total = pathList.size();    //SFC总共经过的点数
        int t = 1;
        for(int i = 0; i < moveNum; i++) {  //从后往前移动功能
            int vertexId = pathList.get(total-1-t); //最后一个节点部署目的节点,保持不动
            int vnf =  sfc.nodeList.getLast()-i;    //当前移动的功能ID
            Node node = sfc.nodeMap.get(vnf);   //功能节点
            Link link = sfc.linkMap.get(vnf-1); //功能VNF的前一条需求链路
            while (nodeDeployVertexMap.get(vnf) != vertexId) {
//                CEdge edge = graph.idEdgeMap.get(graph.sourceSinkEdgeMap.get(preVertexId).get(vertexId));   //通过源、目的节点对获得边
                //如果计算资源满足条件，就将后面的第i个功能移动到目的点前第i个位置
                if(node.getComputeResourceDemand() <= graph.idVertexMap.get(vertexId).getRemainComputeResource()) {
                    //判断带宽资源是否满足
                    if(judgeResource(graph, pathList, nodeDeployVertexMap.get(vnf), vertexId, link)) {
                        nodeDeployVertexMap.put(vnf, vertexId); //将该功能移动
                        break;
                    }
                }
                //如果计算资源或带宽资源不满足
                t++;
                vertexId = pathList.get(total-1-t);
            }
            t++;
        }

        int last = pathList.size();
        //功能节点vnf 4-3-2-1, 链路节点 4-3-2-1-0
        for(int i = sfc.nodeList.size()-1; i >= 0; i--) {
            int vnf = sfc.nodeList.get(i);
            int vertexId = nodeDeployVertexMap.get(vnf);    //功能节点部署位置ID
            int index = pathList.indexOf(vertexId);
            linkDeployEdgeMap.get(vnf).clear();
            List<Integer> tmpList = pathList.subList(index, last);
            int x = 0;
            while(x != tmpList.size()-1) {
                int source = tmpList.get(x);
                int sink = tmpList.get(x+1);
                int edgeKey = graph.sourceSinkEdgeMap.get(source).get(sink);  //获取边ID
                linkDeployEdgeMap.get(vnf).add(edgeKey);
                x++;
            }
            last = index+1;
        }
        return this;
    }


    /**
     *  判断功能移动后与移动之前之间的边是否能满足带宽需求
        f2-->d-->e-->f3-->g-->h-->j-->t,f3移到j时，链路是否满足f2-->f3之间的带宽需求
     * @param graph
     * @param pathList  已经找到的路径集
     * @param ori   功能部署的节点位置（已经部署好的位置）
     * @param curr  当前路径中的节点
     * @param link
     * @return
     */
    public boolean judgeResource(CGraph graph, List<Integer> pathList,int ori, int curr, Link link) {
        int oriIndex = pathList.indexOf(ori);
        int currIndex = pathList.indexOf(curr);
        int pre = pathList.get(currIndex-1);    //已知路径中当前点的前一节点
        while (currIndex > oriIndex) {
            CEdge edge = graph.idEdgeMap.get(graph.sourceSinkEdgeMap.get(pre).get(curr));
            if(edge.getRemainBandWidthResource() < link.getBandWidthResourceDemand()) {
                return false;
            }
            curr = pathList.get(--currIndex);
            pre = pathList.get(currIndex-1);
        }
        return true;
    }

    public void removeFirstAndLast() {
        nodeDeployVertexMap.remove(0);
        nodeDeployVertexMap.remove(nodeDeployVertexMap.size());
    }

    public int getSfcId() {
        return sfcId;
    }

    public void setSfcId(int sfcId) {
        this.sfcId = sfcId;
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float delay) {
        this.delay = delay;
    }
    public void printPlan(CGraph graph) {
        System.out.println("节点部署");
        for (Integer nodeKey : nodeDeployVertexMap.keySet()) {
            System.out.println("VNF " + nodeKey + "部署在" + nodeDeployVertexMap.get(nodeKey));
        }
        System.out.println("链路部署");
        for (Integer linkKey : linkDeployEdgeMap.keySet()) {
            System.out.print("链路" + linkKey + " 部署在 ");
            for (Integer edgeKey : linkDeployEdgeMap.get(linkKey)) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                System.out.print(edgeKey + " (" + edge.getSourceId() + " --- " + edge.getSinkId() + ") ----> ");
            }
            System.out.println();
        }
    }

}
