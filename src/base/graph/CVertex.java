package base.graph;

import base.Configure;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 虚拟网络的顶点
 * Created by ly on 2017/2/28.
 */
public class CVertex {
    private int veretexId;  //节点编号
    private int openveretexId;  //开启的节点编号
    private int recycleTime;			//该服务器资源回收时间
    private int VMdelayTime;//VM延迟关闭时间
    public boolean off;//ture为关机，false为开机
    private float totalComputeResource; //节点总的计算资源
    private float remainComputeResource;    //节点部署后剩余计算资源
    private float delay;    //节点处理延时
    private int associateNodeId;    //关联节点ID

    private float totalBWCost;
	private float totalBWCostToSink;
    private int previousVertexId;   //用于部署最短路径时，找出到指定源点的最短路径的上一跳
    private float totalDelay;   //用于部署最短路径时，该点到指定源点的总时延
    private int previousVertexIDToSink;	//用于在各个域中部署路径是，找出到指定源点的最短路的上一跳
    private float totalDelayToSink;	////用于在各个域中部署路径时，该点到目的点的总时延
    public ArrayList<Integer> currentDeployedSFCList = new ArrayList<>();	//表示当前节点部署有那些SFC
	public ArrayList<Integer> currentForwardSFCList = new ArrayList<>();	//表示当前节点作为转发节点，转发了那些SFC
    
    
    /*
	 * 能量参数设置
	 */
	private float serverOnEnergyConsumer;		//开机消耗
	private float serverOffEnergyConsumer;		//关机消耗
	/*
	 * 因此基本的能量消耗为：
	 *   serverBaseEnergyConsumer + （serverMaxEnergyConsumer - serverBaseEnergyConsumer）× 负载率
	 */
	private int functionkey;  //节点的功能号
    private float reliability;  //可靠性
    private float reliabilityToSource;  //到源点的总可靠性
    private float reliabilityToSink;    //到目的点的总可靠性
    private int rb_previousVertexIdToSource;    //到源点最大可靠性的前一点
    private int rb_previousVertexIdToSink;  //到目的点的最大可靠性的前一点

    public List<CVertex> openVertexList = new ArrayList<CVertex>();   //用于存放开启的物理节点集合
    public List<CVertex> openandFunctionVertexList = new ArrayList<>(); //开启的功能性匹配的节点集合
    public List<CVertex> openFunctionandResourceVertexList = new ArrayList<>(); //开启的，功能性和资源都满足要求的节点集合
    
    
    public List<Integer> adjVertexList = new ArrayList<>();    //该节点所有邻接节点集合
    public List<CEdge> adjEdgeList = new ArrayList<>();   //该节点所有邻接边集合

    public List<Integer> entryEdgeList = new ArrayList<>();    //该节点所有入度边编号
    public List<Integer> outsideEdgeList = new ArrayList<>();  //该节点所有出度边编号

    //记录已经访问过的该顶点的邻接顶点集合
    private List<Integer> hasVisitedAdjVertexList = new ArrayList<>();

    public List<Integer> sfcList = new LinkedList<>();  //保存该节点上部署的SFC ID
    private float f; //决策因子  f = alpha*(∑B + C) + beta*(1/S)

    /*
	 * 说明：loadBalance = 1/A + ∑1/B + C
	 * 其中：A表示节点V的剩余计算资源
	 * B表示节点V的所有出度边上的剩余带宽资源
	 * C表示，节点V到 `源点` 的带宽代价
	 */
    private float loadBalanceToSource;	//用于在各个域中部署子链时，采用负载均衡的思路，来逼近最短路
    private float loadBalanceToSink;	//用于在各个域中部署子链时，采用负载均衡的思路，来逼近最短路
    private int lb_PreviousVertexIDToSource;
    private int lb_PreviousVertexIDToSink;
    private int currentFunction;	//当前节点上部署的功能种类，0表示没有放功能(用0表示没放是可以的，因为我们的SFC中功能编号是从1 开始的)
    public CVertex() {

    }

    public CVertex(int vertexId) {
        this.veretexId = vertexId;
        this.associateNodeId = vertexId;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public int getAssociateNodeId() {
        return associateNodeId;
    }

    public void setAssociateNodeId(int associateNodeId) {
        this.associateNodeId = associateNodeId;
    }

    public int getVeretexId() {
        return veretexId;
    }

    public void setVeretexId(int veretexId) {
        this.veretexId = veretexId;
    }
//    

    public int getOpenVeretexId() {
        return openveretexId;
    }

    public void setOpenVeretexId(int openveretexId) {
        this.veretexId = openveretexId;
        
    }
    public float getTotalComputeResource() {
        return totalComputeResource;
    }

    public void setTotalComputeResource(float totalComputeResource) {
        this.totalComputeResource = totalComputeResource;
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float delay) {
        this.delay = delay;
    }

    public int getPreviousVertexId() {
        return previousVertexId;
    }

    public void setPreviousVertexId(int previousVertexId) {
        this.previousVertexId = previousVertexId;
    }

    public float getTotalDelay() {
        return totalDelay;
    }

    public void setTotalDelay(float totalDelay) {
        this.totalDelay = totalDelay;
    }
   
    
    public List<Integer> getOpenVertexList(){
    	return getOpenVertexList();
    }
    public void setOpenVertexList(List<CVertex>openVertexList){ 
    	this.openVertexList=openVertexList;
    }
    
	public List<Integer> getAdjVertexList() {
        return adjVertexList;
    }

    public void setAdjVertexList(List<Integer> adjVertexList) {
        this.adjVertexList = adjVertexList;
    }

    public List<Integer> getEntryEdgeList() {
        return entryEdgeList;
    }

    public void setEntryEdgeList(List<Integer> entryEdgeList) {
        this.entryEdgeList = entryEdgeList;
    }

    public List<Integer> getOutsideEdgeList() {
        return outsideEdgeList;
    }

    public void setOutsideEdgeList(List<Integer> outsideEdgeList) {
        this.outsideEdgeList = outsideEdgeList;
    }

    public List<Integer> getHasVisitedAdjVertexList() {
        return hasVisitedAdjVertexList;
    }

    public void setHasVisitedAdjVertexList(List<Integer> hasVisitedAdjVertexList) {
        this.hasVisitedAdjVertexList = hasVisitedAdjVertexList;
    }

    public int getPreviousVertexIDToSink() {
        return previousVertexIDToSink;
    }

    public void setPreviousVertexIDToSink(int previousVertexIDToSink) {
        this.previousVertexIDToSink = previousVertexIDToSink;
    }

    public float getTotalDelayToSink() {
        return totalDelayToSink;
    }

    public void setTotalDelayToSink(float totalDelayToSink) {
        this.totalDelayToSink = totalDelayToSink;
    }

    public float getRemainComputeResource() {
        return remainComputeResource;
    }

    public void setRemainComputeResource(float remainComputeResource) {
        this.remainComputeResource = remainComputeResource;
    }

    public float getLoadBalanceToSource() {
        return loadBalanceToSource;
    }

    public void setLoadBalanceToSource(float loadBalanceToSource) {
        this.loadBalanceToSource = loadBalanceToSource;
    }

    public float getLoadBalanceToSink() {
        return loadBalanceToSink;
    }

    public void setLoadBalanceToSink(float loadBalanceToSink) {
        this.loadBalanceToSink = loadBalanceToSink;
    }

    public int getLb_PreviousVertexIDToSource() {
        return lb_PreviousVertexIDToSource;
    }

    public void setLb_PreviousVertexIDToSource(int lb_PreviousVertexIDToSource) {
        this.lb_PreviousVertexIDToSource = lb_PreviousVertexIDToSource;
    }

    public int getLb_PreviousVertexIDToSink() {
        return lb_PreviousVertexIDToSink;
    }

    public void setLb_PreviousVertexIDToSink(int lb_PreviousVertexIDToSink) {
        this.lb_PreviousVertexIDToSink = lb_PreviousVertexIDToSink;
    }

    public float getReliability() {
        return reliability;
    }

    public void setReliability(float reliability) {
        this.reliability = reliability;
    }
    public int getFunctionkey() {
        return functionkey;
    }

    public void setFunctionkey(int functionkey) {
        this.functionkey = functionkey;
    }

    public float getServerOnEnergyConsumer() {
		return serverOnEnergyConsumer;
	}

	public void setServerOnEnergyConsumer(float serverOnEnergyConsumer) {
		this.serverOnEnergyConsumer = serverOnEnergyConsumer;
	}

	public float getServerOffEnergyConsumer() {
		return serverOffEnergyConsumer;
	}

	public void setServerOffEnergyConsumer(float serverOffEnergyConsumer) {
		this.serverOffEnergyConsumer = serverOffEnergyConsumer;
	}
	
    public float getReliabilityToSource() {
        return reliabilityToSource;
    }
    

    public void setReliabilityToSource(float reliabilityToSource) {
        this.reliabilityToSource = reliabilityToSource;
    }

    public float getReliabilityToSink() {
        return reliabilityToSink;
    }

    public void setReliabilityToSink(float reliabilityToSink) {
        this.reliabilityToSink = reliabilityToSink;
    }

    public int getRb_previousVertexIdToSource() {
        return rb_previousVertexIdToSource;
    }

    public void setRb_previousVertexIdToSource(int rb_previousVertexIdToSource) {
        this.rb_previousVertexIdToSource = rb_previousVertexIdToSource;
    }

    public int getRb_previousVertexIdToSink() {
        return rb_previousVertexIdToSink;
    }

    public void setRb_previousVertexIdToSink(int rb_previousVertexIdToSink) {
        this.rb_previousVertexIdToSink = rb_previousVertexIdToSink;
    }

    /**
     * 获取该顶点的所有剩余容量  ∑B + C
     *      ∑B = 所有邻接边的剩余带宽
     *      C = 顶点的剩余计算容量
     */
    public float getRemainResource(CGraph graph) {
        float sum = getRemainComputeResource(); // C 剩余计算容量
        CVertex vertex = graph.idVertexMap.get(getVeretexId()); //获取当前节点
        for(CEdge edge : vertex.adjEdgeList) {
            sum += edge.getRemainBandWidthResource();   // ∑B 剩余带宽
        }

        return (float) Configure.ALPHA * sum;
    }

    //获取每个顶点上部署的SFC数目的倒数 1/S
    public float getDeployedSFCNum() {
        if(sfcList.size() == 0) {
            return 0;
        }
        return  Configure.BETA *(float)(1/sfcList.size());
    }

    /*
	 * 功能：返回该节点的 剩余资源倒数 + （该点出度边剩余带宽资源倒数和），即：1/A + ∑1/B
	 * 需要说明的是：对于该点的出度边中，应该去掉到达该点的那一条边
	 */
    public float getLoadBalanceOfVertex(CGraph graph, int lastVertexID) {
        float sum = 0;
        if(remainComputeResource != 0) {
            sum = (float) 1 / remainComputeResource;
        }
        for (Integer edgeKey : outsideEdgeList) {
            CEdge edge = graph.idEdgeMap.get(edgeKey);
            if (edge.getSinkId() != lastVertexID) {
                if(edge.getRemainBandWidthResource() == 0) {
                    continue;
                }
                sum += (float)1 / edge.getRemainBandWidthResource();
            }
        }
//        return (float)Configure.ALPHA_LB * sum;
        return 1;
    }

	public boolean onoroff() {
		if (currentDeployedSFCList.size() + currentForwardSFCList.size() != 0) {
			
			return true;
		} 
		else {
			return false;
		}
	}
	public boolean off() {
		if (!onoroff() &&  VMdelayTime == 0) {
			off=true;
		} 
		else {
			off=false;
		}
		return off;
	}
	//检查传入的功能，是否能够放在当前节点上
public boolean checkVNFFunction(int function) {
			if (currentFunction == 0) {
				return true;
			}
			if (currentFunction == function) {
				return true;
			}
			return false;
		}
		

 public int getRecycleTime() {
	return recycleTime;
}

 public void setRecycleTime(int recycleTime) {
	this.recycleTime = recycleTime;
}
 public int getVMdelayTime() {
	return VMdelayTime;
}
 public void setVMdelayTime(int VMdelayTime) {
	this.VMdelayTime = VMdelayTime;
}
 public float getTotalBWCost() {
		return totalBWCost;
	}
	public void setTotalBWCost(float totalBWCost) {
		this.totalBWCost = totalBWCost;
	}
	public float getTotalBWCostToSink() {
		return totalBWCostToSink;
	}
	public void setTotalBWCostToSink(float totalBWCostToSink) {
		this.totalBWCostToSink = totalBWCostToSink;
	}
public int getFunctionkey(CGraph graph) {
	// TODO Auto-generated method stub
	return 0;
}
}

