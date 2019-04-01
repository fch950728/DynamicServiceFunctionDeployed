package base.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ly on 2017/2/28.
 */
public class Node implements Serializable{
    private int nodeId; //节点编号
    private int totalNodeResource;  //总的节点计算资源
    private int unitCost;   //启动每个节点的成本开销
    private float computeResourceDemand;	// 节点计算资源需求
    private float reliability;  //节点可靠性
    
    private int functionDemand;	//vnf的功能需求
    private int functionkey;	//底层物理节点的功能
    private boolean onoroff;//节点是否开机

    private int remainNodeResource;     //节点剩余计算资源

    public List<Integer> adjNodeList = new LinkedList<>();  //该点的所有邻接点集合
    public List<Integer> adjLinkList = new LinkedList<>();  //该点的所有邻接边集合
	

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getTotalNodeResource() {
        return totalNodeResource;
    }

    public void setTotalNodeResource(int totalNodeResource) {
        this.totalNodeResource = totalNodeResource;
    }

    public int getRemainNodeResource() {
        return remainNodeResource;
    }

    public void setRemainNodeResource(int remainNodeResource) {
        this.remainNodeResource = remainNodeResource;
    }

    public int getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(int unitCost) {
        this.unitCost = unitCost;
    }

    public float getComputeResourceDemand() {
        return computeResourceDemand;
    }

    public void setComputeResourceDemand(float computeResourceDemand) {
        this.computeResourceDemand = computeResourceDemand;
    }

    public float getReliability() {
        return reliability;
    }

    

	public void setReliability(float reliability) {
        this.reliability = reliability;
    }

    public List<Integer> getAdjNodeList() {
        return adjNodeList;
    }

    public List<Integer> getAdjLinkList() {
        return adjLinkList;
    }

	public void setOnoroff(boolean onoroff) {
		this.onoroff=onoroff;
		
	}
	public boolean getOnoroff() {
		return onoroff;
		
	}
	public  int getFunctionDemand() {
		return functionDemand;
	}

	public void setFunctionDemand(int functionDemand) {
		this.functionDemand = functionDemand;
	}
	public  int getFunctionkey() {
		return functionkey;
	}

	public void setFunctionkey( int functionkey) {
		this.functionkey = functionkey;
	}
	
	
		
	}

		
	

