package traffic;

import base.graph.CEdge;
import base.graph.CGraph;
import base.graph.CVertex;
import base.network.Link;
import base.network.Node;
import base.Configure;
//import sun.java2d.windows.GDIRenderer;

import java.util.*;

/**
 * 用于管理一条SFC的基本类
 * Created by ly on 2017/3/7.
 */
public class SFC {
    private int sfcId;  //SFC编号
    private int length; //SFC长度（VNF个数）

    //某条SFC的源和目的节点，这两个点不用来表示功能
    private int sourceId;   //SFC的源节点
    private int sinkId; //SFC的目的节点
    private int continueTime;   //本条SFC持续时间，在本次方法中，在线率采用的是几条SFC在线的方式，而不是采用一条SFC的持续系统时间
    private float OnEnergy;
    private float OnDelay;
    private float ForwardonEnergy;
    private float ForwardonDelay;
    private float deployonEnergy;
    private float deployonDelay;
    private float ForwardoffEnergy;
    private float ForwardoffDelay;
    private float DeployoffEnergy;
	private float DeployoffDelay;
	private float EmptyEnergy;
	private float EmptyDelay;
	private float WorkEnergy;
	private float WorkDelay;
    private float OffEnergy;
    private float OffDelay;
    public Map<Integer, Node> nodeMap = new LinkedHashMap<>();  //SFC上的节点需求，因为SFC上节点的顺序性，故采用LinkedHashMap
    public Map<Integer, Link> linkMap = new LinkedHashMap<>();

    public LinkedList<Integer> nodeList = new LinkedList<>();   //为了方便计算，记录SFC节点的顺序
    public LinkedList<Integer> linkList = new LinkedList<>();   //为了方便计算，记录SFC链路的顺序

    private Plan plan;  //SFC部署方案类实例
	
//    
//    /*
//	 * 功能：部署sfc，具体体现在部署节点以及部署链路上资源的扣除，同时对于新开机的服务器，计算开机能耗
//	 */
//	public float deploySFConEnergy(CGraph graph, float onEnergy) {
//		plan.computeForwordNodes(this, graph);
//		if (checkTheLinks() & checkTheNodes()) {
//			onEnergy = deployNodeonEnergy(graph, onEnergy);
//			deployLink(graph);
//			onEnergy = deployForwardNodeonEnergy(graph, onEnergy);
//		}
//		return  onEnergy;
//	}
//	
//	/*
//	 * 功能：部署SFC中的节点，同时计算开机能量消耗
//	 */
//	public float deployNodeonEnergy(CGraph graph, float onEnergy) {
//		for (Integer nodeKey : plan.nodeDeployVertexMap.keySet()) {
//			CVertex vertex = graph.idVertexMap.get(plan.nodeDeployVertexMap.get(nodeKey));
//			vertex.setTotalComputeResource(vertex.getTotalComputeResource() - nodeMap.get(nodeKey).getComputeResourceDemand());
////			vertex.setAvailableComputeResource(vertex.getAvailableComputeResource() - nodeMap.get(nodeKey).getComputeResourceDemand());
////			vertex.historyFunctionList.add(vertex.getCurrentFunction());
//			vertex.setFunctionkey(nodeMap.get(nodeKey).getFunctionDemand());
//			if (!vertex.onoroff()) {
//				onEnergy += Configure.ON_ENERGY;
//
//			}
//			vertex.currentDeployedSFCList.add(sfcId);
//			if (vertex.getRecycleTime() < this.continueTime) {
//				vertex.setRecycleTime(this.continueTime);
//			}
//		}
//		return onEnergy;
//	}
//
//	
//	/*
//	 * 部署中间转发节点，同时计算开机能量开销
//	 */
//	public float deployForwardNodeonEnergy(CGraph graph, float onEnergy) {
//		for (Integer vertexKey : plan.nodeForwardList) {
//			CVertex vertex = graph.idVertexMap.get(vertexKey);
//			if (!vertex.onoroff()) {
//				onEnergy += Configure.ON_ENERGY;
//
//			}
//			if (!vertex.currentForwardSFCList.contains(sfcId)) {
//				vertex.currentForwardSFCList.add(sfcId);
//			}
//			if (vertex.getRecycleTime() < this.continueTime) {
//				vertex.setRecycleTime(this.continueTime);
//			}
//		}
//		return onEnergy;
//	}
//	
//	  /*
//		 * 功能：部署sfc，具体体现在部署节点以及部署链路上资源的扣除，同时对于新开机的服务器，计算开机时延
//		 */
//		public void deploySFC(CGraph graph) {
//			plan.computeForwordNodes(this, graph);
//			if (checkTheLinks() & checkTheNodes()) {
//				deployNode(graph);
//				deployLink(graph);
//				deployForwardNode(graph);
//			}
//
//		}
		
		/*
		 * 功能：部署SFC中的节点，同时计算开机时延
		 */
		public void deployNode(CGraph graph) {

			for (Integer nodeKey : plan.nodeDeployVertexMap.keySet()) {
				CVertex vertex = graph.idVertexMap.get(plan.nodeDeployVertexMap.get(nodeKey));
//				vertex.setTotalComputeResource(vertex.getTotalComputeResource() - nodeMap.get(nodeKey).getComputeResourceDemand());
//				vertex.setAvailableComputeResource(vertex.getAvailableComputeResource() - nodeMap.get(nodeKey).getComputeResourceDemand());
//				vertex.historyFunctionList.add(vertex.getCurrentFunction());
//				vertex.setFunctionkey(nodeMap.get(nodeKey).getFunctionDemand());
				if (vertex.off) {//处于关机状态&&!vertex.onoroff() && vertex.getVMdelayTime() == 0
					deployonEnergy += Configure.ON_ENERGY;
					deployonDelay+=Configure.ON_DELAY;
					vertex.setVMdelayTime(Configure.VM_timeoffDELAY);//新开机，设置延迟关闭的时间
					vertex.off=false;//将该点设为开机
				}
				vertex.currentDeployedSFCList.add(sfcId);
//				if (vertex.getRecycleTime() < this.continueTime) {
//					vertex.setRecycleTime(this.continueTime);
//				}
			}
			System.out.println("deployonDelay="+deployonDelay);

		}

		
		/*
		 * 部署中间转发节点，同时计算开机时延
		 */
		public void deployForwardNode(CGraph graph) {

			for (Integer vertexKey : plan.nodeForwardList) {
				CVertex vertex = graph.idVertexMap.get(vertexKey);
				if (vertex.off) {//处于关机状态&&!vertex.onoroff() && vertex.getVMdelayTime() == 0
					
					ForwardonEnergy+= Configure.ON_ENERGY;
					ForwardonDelay+=Configure.ON_DELAY;
					vertex.setVMdelayTime(Configure.VM_timeoffDELAY);//新开机，设置延迟关闭的时间
					vertex.off=false;
				}
				if (!vertex.currentForwardSFCList.contains(sfcId)) {
					vertex.currentForwardSFCList.add(sfcId);
				}
//				if (vertex.getRecycleTime() < this.continueTime) {
//					vertex.setRecycleTime(this.continueTime);
//				}
			}
			
		}

	/*
	 * 功能： 部署SFC，具体体现在，部署节点以及部署链路上资源的扣除
	 */
	public boolean deploySFC(CGraph graph) {
		if (plan == null) {		//当plan==null时，表示该条SFC部署失败
			return false;
		}
		plan.nodeForwardList.clear();
		plan.computeForwordNodes(this, graph);
		
//		System.out.println("plan.nodeDeployVertexMap="+plan.nodeDeployVertexMap);
//		System.out.println("plan.linkDeployEdgeMap="+plan.linkDeployEdgeMap);
		System.out.println("plan.nodeForwardList="+plan.nodeForwardList);
		
		if (checkTheLinks() && checkTheNodes()) {
			deployNode(graph);
			deployLink(graph);
			deployForwardNode(graph);
			OnEnergy=deployonEnergy+ForwardonEnergy;
			OnDelay=deployonDelay+ForwardonDelay;
			System.out.println("部署点的开机能量："+deployonEnergy);
	        System.out.println("部署点的开机时延："+deployonDelay);
	        System.out.println("转发点的开机能量："+ForwardonEnergy);
	        System.out.println("转发点的开机时延："+ForwardonDelay);
			System.out.println("部署当前sfc的开机机能量："+OnEnergy);
            System.out.println("部署当前sfc的开机时延："+OnDelay);
			return true;
		}
		return false;
	}
	 public float getOnEnergy(CGraph graph){
//	    	OnEnergy=OnEnergy;
	    	return OnEnergy;
	    }
	    public float getOnDelay(CGraph graph){
//	    	OnDelay=OnDelay;
	    	return OnDelay;
	    }
//
//    /*
//     * 功能：部署SFC中的节点（扣除节点计算资源）
//     */
//    public boolean deployNode(CGraph graph) {
//        for (Integer nodeKey : plan.nodeDeployVertexMap.keySet()) { //SFC功能节点集
//            CVertex vertex = graph.idVertexMap.get(plan.nodeDeployVertexMap.get(nodeKey));
//            vertex.setRemainComputeResource(vertex.getRemainComputeResource() - nodeMap.get(nodeKey).getComputeResourceDemand());
//            vertex.setFunctionkey(nodeMap.get(nodeKey).getFunctionDemand());
//			vertex.currentDeployedSFCList.add(sfcId);
////			if (vertex.getRecycleTime() < this.continueTime) {
////				vertex.setRecycleTime(this.continueTime);
////			}
//        }
//        return true;
//    }
//	/*
//	 * 部署中间转发节点
//	 */
//	public void deployForwardNode(CGraph graph) {
//		for (Integer vertexKey : plan.nodeForwardList) {
//			CVertex vertex = graph.idVertexMap.get(vertexKey);
//			if (!vertex.currentForwardSFCList.contains(sfcId)) {
//				vertex.currentForwardSFCList.add(sfcId);
//			}
////			if (vertex.getRecycleTime() < this.continueTime) {
////				vertex.setRecycleTime(this.continueTime);
////			}
//		}
//	}
	
    /*
     * 功能： 部署SFC中的链路（扣除链路带宽资源）
     * 注意： SFC的第0条链路和最后一条链路不消耗带宽资源
     */
    public boolean deployLink(CGraph graph) {
        for (Integer linkKey : plan.linkDeployEdgeMap.keySet()) {   //SFC链路编号
            Link link = linkMap.get(linkKey);   //获取SFC的该条链路
//            System.out.println("SFC链路：" + link.getLinkId() + "  部署的路径长度为：" + plan.linkDeployEdgeMap.get(linkKey).size());
            for (Integer edgeKey : plan.linkDeployEdgeMap.get(linkKey)) {   //遍历虚拟部署路径中的各中间边
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                if (edge == null) {
                    System.out.println("SFC源：" + getSourceId() + "  SFC目的：" + getSinkId());
                    System.out.println("SFC链路：" + link.getLinkId() + "  部署到边：" + edgeKey +" 上时出现问题！");
                    plan.printPlan(graph);
                }
                if(edge.getRemainBandWidthResource() < link.getBandWidthResourceDemand()) {
                    return false;
                }
                //虚拟边总的带宽资源 - SFC使用到的带宽资源
                edge.setRemainBandWidthResource(edge.getRemainBandWidthResource() - link.getBandWidthResourceDemand());
//                edge.setTotalBandwithResource(edge.getTotalBandwithResource() - link.getBandWidthResourceDemand());
            }
        }
        return true;
    }
    
    /*
	 * 功能：传入一个int，检查这个int，与当前SFC已有的VNF中的功能需求比较，是否有重复的VNF
	 */
    public boolean checkFunction(int f) {
		for (Integer nodeKey : nodeList) {
			if (f == nodeMap.get(nodeKey).getFunctionDemand()) {
				return false;
			}
		}
		return true;
	}
    /*
     * 功能：判断SFC中的Node是否已经全部找到部署节点
     */
    public boolean checkTheNodes() {
        if (plan == null) {		//当plan==null时，表示该条SFC部署失败
            return false;
        }
        for (Integer nodeKey : nodeMap.keySet()) {
            if (!plan.nodeDeployVertexMap.containsKey(nodeKey)) {
                return false;
            }
        }
        return true;
    }

    /*
     * 功能：判断SFC中的Link是否都已经找到部署边或者部署路径（路径就是：有的边可能需要中介节点）
     */
    public boolean checkTheLinks() {
        if (plan == null) {		//当plan==null时，表示该条SFC部署失败
            return false;
        }
        for (Integer linkkey : linkMap.keySet()) {
            if (!plan.linkDeployEdgeMap.containsKey(linkkey)) {
                return false;
            }
        }
        return true;
    }
    
    
    //SFC部署完成后，资源回收并设置延迟机制
    public void recycleResource(CGraph graph) {
    	if (checkTheLinks() && checkTheNodes()) {
        recycleVertexComputeResource(graph);
        recycleEdgeBandwithResource(graph);
        recycleForwardNode(graph);
        for (Integer vertexKey : plan.nodeDeployVertexMap.values()) {
			CVertex vertex = graph.idVertexMap.get(vertexKey);
			if(!vertex.off) {//如果该节点已经开机
				if (!vertex.onoroff()) {//即该点既不作为部署点，
					 if( vertex.getVMdelayTime() == 0){//VM的延迟时间到达，则关闭
						DeployoffEnergy+= Configure.OFF_ENERGY;
						DeployoffDelay += Configure.OFF_DELAY;
						vertex.off=true;//关机
						System.out.println("该VM持续时间到达，节点 "+vertexKey+" 关闭");
					    }
					 else{//空载，但节点延迟时间没有到达
								vertex.setVMdelayTime(Configure.VM_timeoffDELAY-1);
								EmptyEnergy=EmptyEnergy + Configure.EMPTY_ENERGY;
								EmptyDelay=EmptyDelay + Configure.EMPTY_DELAY;
							}
					}			
					else{//节点处于工作状态
						vertex.setVMdelayTime(Configure.VM_timeoffDELAY);
					    WorkEnergy=WorkEnergy + Configure.WORK_ENERGY;
					    WorkDelay=WorkDelay + Configure.WORK_DELAY;
					}
			}
			
        }
//        System.out.println("========plan.linkDeployEdgeMap="+plan.linkDeployEdgeMap);
//        System.out.println("========plan.nodeForwardList="+plan.nodeForwardList);
		for (Integer vertexKey : plan.nodeForwardList) {
			CVertex vertex = graph.idVertexMap.get(vertexKey);
//			if(getVMContinueTime() <= getSfcId()) {//判断是否到达VM的持续时间
			if(!vertex.off) {//如果该节点已经开机
				if (!vertex.onoroff()) {//即该点既不作为部署点，
					 if( vertex.getVMdelayTime() == 0){//VM的延迟时间到达，则关闭
						 ForwardoffEnergy+= Configure.OFF_ENERGY;
						 ForwardoffDelay+= Configure.OFF_DELAY;
						 vertex.off=true;//关机
						 System.out.println("Forward该VM持续时间到达，节点 "+vertexKey+" 关闭");
				    }
					 else{//节点延迟时间没有到达
						 vertex.setVMdelayTime(Configure.VM_timeoffDELAY-1);
							EmptyEnergy += Configure.EMPTY_ENERGY;
							EmptyDelay += Configure.EMPTY_DELAY;
						}
				}			
				else{
					vertex.setVMdelayTime(Configure.VM_timeoffDELAY);
					WorkEnergy += Configure.WORK_ENERGY;
				    WorkDelay += Configure.WORK_DELAY;
				}
			}
			
		}
//		setOffEnergy(getDeployoffEnergy() + getForwardoffEnergy());
		OffEnergy=DeployoffEnergy + ForwardoffEnergy;
		OffDelay=DeployoffDelay + ForwardoffDelay;
		System.out.println("当前部署点的关机能量："+DeployoffEnergy);
        System.out.println("当前部署点的关机时延："+DeployoffDelay);
        System.out.println("当前转发点的关机能量："+ForwardoffEnergy);
        System.out.println("当前转发点的关机时延："+ForwardoffDelay);
		System.out.println("部署当前sfc的关机能量："+OffEnergy);
        System.out.println("部署当前sfc的关机时延："+OffDelay);
    	}
//		return Configure.INF;
    }
    public float getWorkEnergy(CGraph graph){
//    	WorkEnergy=getWorkEnergy();
    	return WorkEnergy;
    }
    public float getWorkDelay(CGraph graph){
//    	WorkDelay=getWorkDelay();
    	return WorkDelay;
    }
    
    public float getEmptyEnergy(CGraph graph){
//    	EmptyEnergy=getEmptyEnergy();
    	return EmptyEnergy;
    }
    public float getEmptyDelay(CGraph graph){
//    	EmptyDelay=getEmptyDelay();
    	return EmptyDelay;
    } 
    public float getOffEnergy(CGraph graph){
//    	OffEnergy=getOffEnergy();
    	return OffEnergy;
    }
    public float getOffDelay(CGraph graph){
//    	OffDelay=getOffDelay();
    	return OffDelay;
    }
  
    //SFC部署完成后，节点计算资源回收
    private void recycleVertexComputeResource(CGraph graph) {
        for (Integer nodeKey : plan.nodeDeployVertexMap.keySet()) {
            CVertex vertex = graph.idVertexMap.get(plan.nodeDeployVertexMap.get(nodeKey));
            vertex.setRemainComputeResource(vertex.getRemainComputeResource() + nodeMap.get(nodeKey).getComputeResourceDemand());
            if (vertex.currentDeployedSFCList.contains(sfcId)) {
				vertex.currentDeployedSFCList.remove(vertex.currentDeployedSFCList.indexOf(sfcId));				
			}           
        }
    }
    //SFC部署完成后，链路带宽资源回收
    private void recycleEdgeBandwithResource(CGraph graph) {
        for (Integer linkKey : plan.linkDeployEdgeMap.keySet()) {
            Link link = linkMap.get(linkKey);
            for (Integer edgeKey : plan.linkDeployEdgeMap.get(linkKey)) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                edge.setRemainBandWidthResource(edge.getRemainBandWidthResource() + link.getBandWidthResourceDemand());             
            }
        }
    }
    //SFC部署完成后，转发节点回收
    private void recycleForwardNode(CGraph graph) {
		for (Integer vertexKey : plan.nodeForwardList) {
			CVertex vertex = graph.idVertexMap.get(vertexKey);
			if (vertex.currentForwardSFCList.contains(sfcId)) {
				vertex.currentForwardSFCList.remove(vertex.currentForwardSFCList.indexOf(sfcId));				
			}			
		}
	}
	
    //SFC类中，提供产生需求Node以及需求Link的函数
    public Node createNode() {
        Node node = new Node();
        node.setNodeId(nodeMap.size() + 1); //功能节点顺序从1开始
//        node.setComputeResourceDemand(Configure.random.nextInt(9)+ 1);	//节点开销为1-3，这个可以继续调整
        node.setComputeResourceDemand(5);
        node.setFunctionDemand(Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);//给每个VNF随机生成与底层物理节点个数相同的功能性
//        node.setFunctionDemand(Configure.random.nextInt(Configure.FUNCTION_SEED) + 1);	//节点所需求的功能
        return node;
    }

    public Link createLink(int source, int sink) {
        Link link = new Link();
        link.setLinkId(linkMap.size());
        link.setSourceId(source);
        link.setSinkId(sink);
//        link.setBandWidthResourceDemand(Configure.random.nextInt(5) + 1);   //链路开销为5-15的随机整数
        link.setBandWidthResourceDemand(5);
        return link;
    }

    //获取部署成功后该条SFC所需要的总的资源
    public int getDeployedResource(CGraph graph) {
        return getDeployedLinkResource(graph) + getDeployedNodeResource(graph);
    }

    //计算部署成功后的节点计算资源开销
    public int getDeployedNodeResource(CGraph graph) {
        int computeResource = 0;
        for(int nodeKey : plan.nodeDeployVertexMap.keySet()) {
            Node node = nodeMap.get(nodeKey);
            if (node != null) {
                computeResource += node.getComputeResourceDemand();
            }
        }
        return computeResource;
    }

    //计算部署成功后的带宽开销
    public int getDeployedLinkResource(CGraph graph) {
        int bandWidthResource = 0;
        for(int linkKey : plan.linkDeployEdgeMap.keySet()) {
            Link link = linkMap.get(linkKey);
            bandWidthResource = bandWidthResource +
                    (plan.linkDeployEdgeMap.get(linkKey).size()*link.getBandWidthResourceDemand());
        }
        return bandWidthResource;
    }

    /**
     * 计算SFC部署成功后的可靠性
     * @param graph
     * @return
     */
    public float getDeployedReliability(CGraph graph) {
        float reliability = (float) 1.0;
        for(int linkKey : plan.linkDeployEdgeMap.keySet()) {
            int count = 1;  //用于判断何时到达SFC部署的最后一个节点
            for (int edgeKey : plan.linkDeployEdgeMap.get(linkKey)) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                CVertex source = graph.idVertexMap.get(edge.getSourceId());
                reliability = reliability * edge.getReliability() * source.getReliability();
                if(linkKey == getLength() && count == plan.linkDeployEdgeMap.size()) {
                    CVertex sink = graph.idVertexMap.get(edge.getSinkId());
                    reliability *= sink.getReliability();
                }
                count++;
            }
        }
        return reliability;
    }

    /**
     * 根据SFC链路的带宽需求大小排序，顺序从大到小排列
     * @return
     */
    public LinkedList<Link> sortSFCLink() {
        LinkedList<Link> linkList = new LinkedList<>();
        int[] linkNum = new int[getLength()+1];
        int index = 0;
        for(int linkKey : linkMap.keySet()) {
            linkNum[index++] = linkKey;
        }

        //由于SFC长度比较小，故采用冒泡排序法根据带宽需求大小进行排序
        for(int i = 0; i < linkNum.length; i++) {   //控制循环次数
            for(int j = i+1; j < linkNum.length; j++) {
                Link link1 = linkMap.get(linkNum[j-1]);
                Link link2 = linkMap.get(linkNum[j]);
                if(link1.getBandWidthResourceDemand() < link2.getBandWidthResourceDemand()) {
                    swap(linkNum, j-1, j);
                }
            }
        }

        //再把排好序的Link装入集合中
        for(int i = 0; i < linkNum.length; i++) {
            linkList.add(linkMap.get(linkNum[i]));
        }

        return linkList;
    }

    public void swap(int[] arr, int left, int right) {
        int tmp = arr[left];
        arr[left] = arr[right];
        arr[right] = tmp;
    }

    public SFC() {
        // TODO Auto-generated constructor stub
    }
   

    public int getSfcId() {
        return sfcId;
    }

    public void setSfcId(int sfcId) {
        this.sfcId = sfcId;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getSinkId() {
        return sinkId;
    }

    public void setSinkId(int sinkId) {
        this.sinkId = sinkId;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public int getContinueTime() {
        return continueTime;
    }

    public void setContinueTime(int continueTime) {
        this.continueTime = continueTime;
    }
    public void printSFC() {
        System.out.println(getSfcId() + " (源点" + getSourceId() + " ---> 目的 " + getSinkId() + ") SFC长度 = " + getLength());
        for (Integer nodeKey : nodeMap.keySet()) {
            Node node = nodeMap.get(nodeKey);
            System.out.print(node.getNodeId() + "(资源需求：" + node.getComputeResourceDemand() + ";  功能需求:"+node.getFunctionDemand()+")" + " ---> ");
        }
        System.out.println();
        System.out.println("sfc链路请求为");
        for (Integer linkKey : linkMap.keySet()) {
            Link link = linkMap.get(linkKey);
            System.out.println("链路 " + link.getLinkId() + " (" + link.getSourceId() + " ----> " + link.getSinkId() + ")" +
                        "   带宽需求为：" + link.getBandWidthResourceDemand());
        }
        /*System.out.println("plan方案");
        plan.printPlan(graph);*/
    }


//	public float getOnEnergy() {
//		return onEnergy;
//	}
//
//
//	public void setOnEnergy(float onEnergy) {
//		this.onEnergy = onEnergy;
//	}
//
//
//	public float getOffEnergy() {
//		return offEnergy;
//	}
//
//
//	public void setOffEnergy(float offEnergy) {
//		this.offEnergy = offEnergy;
//	}
//
//
//	public float getOnDelay() {
//		return onDelay;
//	}
//
//
//	public void setOnDelay(float onDelay) {
//		this.onDelay = onDelay;
//	}
//
//
//	public float getOffDelay() {
//		return offDelay;
//	}
//
//
//	public void setOffDelay(float offDelay) {
//		this.offDelay = offDelay;
//	}
//
//
//	public float getForwardoffEnergy() {
//		return ForwardoffEnergy;
//	}
//
//
//	public void setForwardoffEnergy(float forwardoffEnergy) {
//		ForwardoffEnergy = forwardoffEnergy;
//	}
//
//
//	public float getForwardoffDelay() {
//		return ForwardoffDelay;
//	}
//
//
//	public void setForwardoffDelay(float forwardoffDelay) {
//		ForwardoffDelay = forwardoffDelay;
//	}
//
//
//	public float getDeployoffEnergy() {
//		return deployoffEnergy;
//	}
//
//
//	public void setDeployoffEnergy(float deployoffEnergy) {
//		this.deployoffEnergy = deployoffEnergy;
//	}
//
//
//	public float getDeployoffDelay() {
//		return deployoffDelay;
//	}
//
//
//	public void setDeployoffDelay(float deployoffDelay) {
//		this.deployoffDelay = deployoffDelay;
//	}
//
//
//	public float getEmptyEnergy() {
//		return EmptyEnergy;
//	}
//
//
//	public void setEmptyEnergy(float emptyEnergy) {
//		EmptyEnergy = emptyEnergy;
//	}
//
//
//	public float getEmptyDelay() {
//		return EmptyDelay;
//	}
//
//
//	public void setEmptyDelay(float emptyDelay) {
//		EmptyDelay = emptyDelay;
//	}
//
//
//	public float getWorkEnergy() {
//		return WorkEnergy;
//	}
//
//
//	public void setWorkEnergy(float workEnergy) {
//		WorkEnergy = workEnergy;
//	}
//
//
//	public float getWorkDelay() {
//		return WorkDelay;
//	}
//
//
//	public void setWorkDelay(float workDelay) {
//		WorkDelay = workDelay;
//	}
}


