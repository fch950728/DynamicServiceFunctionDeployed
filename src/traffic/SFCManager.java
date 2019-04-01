package traffic;

import base.Configure;
import base.graph.CEdge;
import base.graph.CGraph;
import base.graph.CPath;
import base.graph.CVertex;
import base.network.Link;
import base.network.Node;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import traffic.DynamicServiceDeployed;
import javax.sound.sampled.Port.Info;

import BFS.*;

/**
 * Created by ly on 2017/3/7.
 */
@SuppressWarnings("ALL")
public class SFCManager {
    public CGraph graph;
    public DynamicServiceDeployed dynamicservicedeployed;
//    private int openednum=0;//初始化当前开启的物理节点
//    public HashMap<Integer, ArrayList<Integer>> getFunctionCandidateList(CGraph graph, int function);
//    public LinkedHashMap<Integer, LinkedList<Integer>> sfcFunctionMap = new LinkedHashMap<>(Node node ,functionkey);
    public Map<Integer, CVertex> openVertexMap = new HashMap<>(); //节点编号与开启节点的对应Map集合
    public Map<Integer, CVertex> NoOpenVertexMap = new HashMap<>(); //节点编号与未开启节点的对应Map集合
    public Map<Integer, CVertex> openandFunctionMatchVertexMap = new HashMap<>(); //节点编号与开启，功能性匹配节点的对应Map集合
    public Map<Integer, CVertex> openFunctionMatchandResourceVertexMap = new HashMap<>(); //节点编号与开启，功能性匹配，资源满足节点的对应Map集合
    
    
    public List<Integer> openVertexList = new ArrayList<>();   //用于存放开启的物理节点集合   
    public List<Integer> openFunctionandResourceVertexList = new ArrayList<>(); //开启的，功能性和资源都满足要求的节点集合
    public List<Integer> NoOpenVertexList = new ArrayList<>(); //底层网络中，未开启的节点集合
    
    public Map<Integer, SFC> idSFCMap = new LinkedHashMap<>();  //SFC编号与SFC对应map
    //下面两个全局list分别用于存储动态部署时的工作SFC和回收的SFC
    public ArrayList<Integer> workSfcList = new ArrayList<>();
    public ArrayList<Integer> recycleList = new ArrayList<>();


    public SFCManager() {

    }

    public SFCManager(CGraph graph) {
        this.graph = graph;
    }
    
    public class Information{
    	public int vertexnum;
    	public int edgenum;
    	int[] edgestart;
    	int[] edgeend;
    	
    	double[] RemainITresource;
    	double[] Remainbandwidth;
    	
    	int SFCnum;// usernum
    	int[] SFCkey;// 该条有效SFC在总SFC中的key
    	int[] SFCsource;
    	int[] SFCsink;
        int[] SFCbandwidth;
		int[] VNFnum;
    	int[][] VNFtypeinSFC;
    	int[][] VNFdeployedon;
    	int[] SFCusinglinknum;
    	int[][] SFCusinglink;
    	
    	int VNFtype;
    	double[] VNFITresource;
    }
    /*
     * 
     * 功能：找出当前开启的物理节点
     * 每部署完一条sfc，测试一遍
     */
    public void CurrentSituationOfSubstrateNetwork() {
    	System.out.println("测试当前时刻的底层网络开机情况");
    	 openVertexList.clear();
		 openVertexMap.clear();	//若是开机，加入开机序列中
		 NoOpenVertexList.clear();
 		 NoOpenVertexMap.clear();	//若是未开机，加入未开机序列中
    	for (Integer vertexKey : graph.idVertexMap.keySet()) {    		
    		CVertex vertex =graph.idVertexMap.get(vertexKey);
//    		System.out.println("节点 " + vertexKey + " 是否开机： " + vertex.onoroff());
    		if(!vertex.onoroff() &&  vertex.getVMdelayTime() == 0){//
    			NoOpenVertexList.add( vertex.getVeretexId());
        		NoOpenVertexMap.put(vertexKey, vertex);	//若是未开机，加入未开机序列中		
    		}else{
    			 openVertexList.add( vertex.getVeretexId());
        		 openVertexMap.put(vertexKey, vertex);	//若是开机，加入开机序列中	
    		}
    	}    	
    	System.out.println("当前时刻的底层网络开机数："+openVertexList.size());
    	System.out.println("当前时刻的底层网络未开机数："+NoOpenVertexList.size());
    }
 
    public boolean CompareBandwith(Plan plan,SFC sfc) {
    	for (Integer linkKey : plan.linkDeployEdgeMap.keySet()) {   //SFC链路编号
            Link link = sfc.linkMap.get(linkKey);   //获取SFC的该条链路
            System.out.println("SFC链路：" + link.getLinkId() + "  部署的路径长度为：" + plan.linkDeployEdgeMap.get(linkKey).size());
            for (Integer edgeKey : plan.linkDeployEdgeMap.get(linkKey)) {   //遍历虚拟部署路径中的各中间边
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                if (edge == null) {
                    System.out.println("SFC源：" + sfc.getSourceId() + "  SFC目的：" + sfc.getSinkId());
                    System.out.println("SFC链路：" + link.getLinkId() + "  部署到边：" + edgeKey +" 上时出现问题！");
                    plan.printPlan(graph);
                }
                if(edge.getRemainBandWidthResource() < link.getBandWidthResourceDemand()) {
                    System.out.println("带宽 不够，不能部署！！！");
                	return false;
                }
                //虚拟边总的带宽资源 - SFC使用到的带宽资源
//                edge.setRemainBandWidthResource(edge.getRemainBandWidthResource() - link.getBandWidthResourceDemand());
            }
//            System.out.println();
        }
    	return true;
    }
    /*最短路算法：
     * 给以个开始id，返回一个下一跳权重最短的节点id，而且这个id还在选出的m个节点列表内
     * 算法运行在openVertexList，而不是vertexListM
     * */
    
    public Integer ShortestPath(Integer id,ArrayList<Integer> vertexListM ) {
    	Map<Integer, Integer> mincompare=new HashMap<>();
    	for(Integer ver:vertexListM) {
    		PathAndDistence pathcur=Bfs.bfsFind(graph,id,ver);
    		if(pathcur==null) {
    			return null;
    		}
    		mincompare.put(pathcur.distence,ver);
    	}
    	Set<Map.Entry<Integer, Integer>> setEntry =mincompare.entrySet();
    	Integer mindisten=Integer.MAX_VALUE;
    	for(Map.Entry<Integer, Integer> entry:setEntry) {
    		if(entry.getKey()<mindisten) {
    			mindisten=entry.getKey();
    		}
    	}
    	return mincompare.get(mindisten);
    } 
    
    /*-------------------------------------------------------------------------
     *                               对比算法
     *  -----------------------------------------------------------------------
     * */
    /*后面再把以下功能加上：
     * 1、带宽扣除（之前只有节点资源扣除）
     * 2、节点资源消耗计算
     * 3、带宽资源消耗        
     * 第2和3点  要统计好打印出来，在部署完成一条后（成功） 再统计
     * 其中要用到SFC类里面有这两个函数：
     * 
     * plan外改的信息：
     * //虚拟边总的带宽资源 - SFC使用到的带宽资源
                edge.setTotalBandwithResource(edge.getTotalBandwithResource() - link.getBandWidthResourceDemand());
     * 
     * */
    public Plan vnfFunctionDeployListContrast(SFC sfc) {
    	int source=sfc.getSourceId();
    	int sink=sfc.getSinkId();
//    	System.out.println("linkMap="+sfc.linkMap.keySet());
    	System.out.println("nodeMap="+sfc.nodeMap.keySet());
//    	System.out.println("nodeList="+sfc.nodeList);
//    	
//    	System.out.println("graph.getVertexNum()="+graph.getVertexNum());
//    	System.out.println("graph.idVertexMap="+graph.idVertexMap);
//    	System.out.println("graph.getEdgeNum()="+graph.getEdgeNum());
//    	System.out.println("graph.idEdgeMap="+graph.idEdgeMap);
    	
    	//Bfs.printgragh(graph);
    	//打印所有物理节点的剩余资源
//    	ArrayList<Float> reso=new ArrayList<>();
//    	for(Integer vert:graph.vertexList) {
//    		reso.add(graph.idVertexMap.get(vert).getRemainComputeResource());
//    	}
//    	System.out.println("所有物理节点的剩余资源"+reso);
    	
    	System.out.println("source="+source+"; sink="+sink);
    	
    	Map<Integer, Float> vnfre=new HashMap<>();
    	for(Integer sfcre:sfc.nodeList) {
    		vnfre.put(sfcre, sfc.nodeMap.get(sfcre).getComputeResourceDemand());
    	}
    	System.out.println("sfc所需资源="+vnfre);
    	
    	//打印一下每个物理节点能提供的资源
		Map<Integer, Float> verRe=new HashMap<>();
		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
			float vertexResource=graph.idVertexMap.get(vertex).getRemainComputeResource();//每个物理节点的剩余资源
			verRe.put(vertex, vertexResource);
		}
		System.out.println("每个物理节点能提供的资源="+verRe);
    	
		//打印一下每个vnf能提供的带宽资源
		Map<Integer, Integer> vnfbw=new HashMap<>();
    	for(Integer sfcre:sfc.nodeList) {
    		vnfbw.put(sfcre, sfc.linkMap.get(sfcre).getBandWidthResourceDemand());
    	}
    	System.out.println("sfc所需带宽="+vnfbw);
		
    	//打印一下每个物理节点能提供的带宽资源
		Map<Integer, Float> vertotalbw=new HashMap<>();
		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
			float vertexbw=graph.idEdgeMap.get(vertex).getTotalBandwithResource();//每个物理节点的剩余资源
			vertotalbw.put(vertex, vertexbw);
		}
		System.out.println("每个物理节点能提供的总总带宽="+vertotalbw);
    	
    	//打印一下每个物理节点能提供的带宽资源
		Map<Integer, Float> verbw=new HashMap<>();
		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
			float vertexbw=graph.idEdgeMap.get(vertex).getRemainBandWidthResource();//每个物理节点的剩余资源
			verbw.put(vertex, vertexbw);
		}
		System.out.println("每个物理节点能提供的带宽="+verbw);
    	
    	Plan plan = new Plan(); 
    	int random_M=0;//随机找m的次数
    	ArrayList<Integer> vertexListM = new ArrayList<>();  //用于存储网络中随机选出来的物理节点
    	
    	/*
    	 * 随机选出m个物理节点
    	 * 生成vertexListM
    	 * */
    	//打印一下graph.vertexList
//		System.out.println("graph.vertexList="+graph.vertexList);
		
		
		//打印一下sfc.nodeList
//		System.out.println("sfc.nodeList="+sfc.nodeList);
		LinkedList<Integer> AllMidVertex=new LinkedList<>();
		AllMidVertex.addAll(graph.vertexList);
		AllMidVertex.remove((Integer)sfc.getSourceId());
		AllMidVertex.remove((Integer)sfc.getSinkId());
//		System.out.println("allmidvertex="+AllMidVertex);
    	for(int k=0;k<800;k++) {
    		vertexListM.clear();
    		//怀疑openVertexList，不应该用这个
			Random random=new Random();
			int m=random.nextInt(AllMidVertex.size());
			if(m<sfc.nodeList.size()) {
				random_M++;
				continue;
			}
			
			HashSet<Integer> indexofvertexListM =new HashSet();//新建一个hashset用于存放随机选择M的下标
			while(indexofvertexListM.size()<m) {//随机生成m个下标
				int ran=random.nextInt(AllMidVertex.size());
				indexofvertexListM.add((Integer)ran);
			}//等于m，即生成了m个，后退出
//			System.out.println("m="+m+",indexofvertexListM="+indexofvertexListM);
			for(Integer index:indexofvertexListM) {
				vertexListM.add(AllMidVertex.get((int)index));
			}
			//打印一下vertexListM
//			System.out.println("vertexListM="+vertexListM);
			
			//算法二：
			//构建P(n)   preferenceVnf<vnf,PnList<node>>
			ArrayList<Integer> PnList = new ArrayList<>();//P(n)
			HashMap<Integer,ArrayList<Integer>> preferenceVnf =new HashMap<>();//每个vnf映射的P(n)列表，构成map
			ArrayList<Integer> tempvertexListM= new ArrayList<>(vertexListM);
			Integer nextHop=ShortestPath(sfc.getSourceId(),tempvertexListM);
			if(nextHop==null) {
				random_M++;
				continue;
			}
			tempvertexListM.remove(nextHop);
			PnList.add(nextHop);//PnList加入有源找到的最短下一跳
			for(int i=0;tempvertexListM.size()>0;i++) {//只要tempvertexListM.size()>0，说明没找完；
				nextHop=ShortestPath(PnList.get(i),tempvertexListM);
				if(nextHop==null) {
					random_M++;
					continue;
				}
				PnList.add(nextHop);
				tempvertexListM.remove(nextHop);
			}
			//打印一下p(n)
//			System.out.println("P(n)="+PnList);
			
			for(int vnfKey:sfc.nodeList) {
				ArrayList<Integer> pnList2 =new ArrayList<>();
				pnList2.addAll(PnList);
				preferenceVnf.put(vnfKey,pnList2);
			}
			
			/*算法三：
			 * 构建node的优先列表
			 * */
			//sfc.nodeList---->tempNodelist  //去掉sfc的源和目的节点
			ArrayList<Integer> tempNodelist=new ArrayList<>(sfc.nodeList);
//			System.out.println("需要部署的sfc="+tempNodelist);
			HashMap<Integer,Float> temphash =new HashMap<>();
			//tempNodelist---->temphash   //建一个资源和id的键值对map
			for(int vnfKey : tempNodelist) {
				temphash.put(vnfKey,sfc.nodeMap.get(vnfKey).getComputeResourceDemand());
			}
			//temphash--->entrylist 
			List<Map.Entry<Integer,Float>> entrylist =new ArrayList<>(temphash.entrySet());
			//sort(entrylist)
			Collections.sort(entrylist, new Comparator<Map.Entry<Integer, Float>>() {
				public int compare(Entry<Integer, Float> o1,Entry<Integer, Float> o2) {
					return o2.getValue().compareTo(o1.getValue());//递减排列
				}
			});
//			System.out.println("sfc所需资源递减排序：entrylist="+entrylist);
			//preferencenode:<物理ID,vertexprefer<sfc_ID>>
			HashMap<Integer,ArrayList<Integer>> preferencenode =new HashMap<>();
			//记录vertexListM里的每个物理节点能提供的资源
			Map<Integer, Float> verResource=new HashMap<>();
			for(Integer vertex:vertexListM) {//对于每一个物理节点
				ArrayList<Integer> vertexprefer =new ArrayList<>();//每个物理节点的优先队列
				float vertexResource=graph.idVertexMap.get(vertex).getRemainComputeResource();//每个物理节点的剩余资源
				verResource.put(vertex, vertexResource);
				for(Map.Entry<Integer, Float> maping:entrylist) {//对于每一个排好序的sfc节点（已经去掉了源和目的节点）
					//System.out.println("vertexResource:maping.getValue()="+vertexResource+":"+maping.getValue());
					if(vertexResource<maping.getValue()) {//如果不足以提供资源，则不放入优先列表里
						continue;
					}else {
						vertexprefer.add(maping.getKey());//因为之前应对vnf递减排好序,所以直接装入就行
						//System.out.println("vertexResource>maping.getValue()-->vertexprefer.add="+vertexprefer);
					}
				}
				preferencenode.put(vertex, vertexprefer);
			}
//			System.out.println("vertexListM里的每个物理节点能提供的资源"+verResource);
//			System.out.println("node的优先权="+preferencenode);
			/*算法四：
			 * 开始部署
			 * 
			 * */
			boolean falture = false;
			ArrayList<Integer> unssigned =new ArrayList<>(tempNodelist);//tempNodelist：去掉sfc的源和目的节点的数组
			ArrayList<Integer> usedvertex =new ArrayList<>();//用了的物理节点
			HashMap<Integer, Integer> node2vnf=new HashMap<>();//存储物理节点和vnf的键值对，用于通过部署的物理节点找到vnf
			//System.out.println("初始plan.nodeDeployVertexMap.size()="+plan.nodeDeployVertexMap.size());
			plan.nodeDeployVertexMap.clear();
			int unssignedSize=unssigned.size();
			while(unssignedSize>0) {
				if(falture==true) {
					break;
				}
				for(Integer vnfkeycurrent:unssigned) {
//					System.out.println("unssigned[i]="+unssigned);
					if(vnfkeycurrent==-1) {
						continue;
					}
					Integer OptimalVertex=(Integer)(-1);
					if(preferenceVnf.get(vnfkeycurrent).size()!=0) {
						OptimalVertex = preferenceVnf.get(vnfkeycurrent).get(0);
//						System.out.println("当前vnf"+vnfkeycurrent+"的优先列表="+preferenceVnf.get(vnfkeycurrent));
//						System.out.println("OptimalVertex="+OptimalVertex);
					}else {
						falture=true;
						break;
					}
					if(OptimalVertex==-1) {
						System.err.println("OptimalVertex==-1");
						falture=true;
						break;
					}
					if(preferencenode.get(OptimalVertex).size()==0) {
						falture=true;
						break;
					}
					if(!preferencenode.get(OptimalVertex).contains(vnfkeycurrent)) {
						preferenceVnf.get(vnfkeycurrent).remove(OptimalVertex);
//						System.out.println("最优ver的优先列表不包含当前vnf");
						continue;
					}
					float currentVerResource=graph.idVertexMap.get(OptimalVertex).getRemainComputeResource();
					float currentvnfResource=sfc.nodeMap.get(vnfkeycurrent).getComputeResourceDemand();
//					System.out.println("usedvertex="+usedvertex);
					
					if(currentVerResource>=currentvnfResource&&usedvertex.indexOf(OptimalVertex)==-1) {
						//将cnf部署到node
						//为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
//						System.out.println(" 功能 " + vnfkeycurrent + " 部署在点 " + OptimalVertex);
						//设置plan.nodeDeployVertexMap
						plan.nodeDeployVertexMap.put(vnfkeycurrent, OptimalVertex);
						node2vnf.put(OptimalVertex, vnfkeycurrent);
//						System.out.println("node2vnf="+node2vnf);
						
						//资源扣除
						graph.idVertexMap.get(OptimalVertex).setRemainComputeResource(currentVerResource-currentvnfResource);
						//节点移除
						unssigned.set(unssigned.indexOf(vnfkeycurrent),(Integer)(-1));
						unssignedSize--;
						usedvertex.add(OptimalVertex);//将这个物理节点加入到已经使用过的list
						//如果OptimalVertex和vnfkeycurrent在对方的优先列表上都是最优先的，即绝配
						//那就将OptimalVertex和vnfkeycurrent在所有优先列表中去除，这一对的部署不再更改
						if(preferencenode.get(OptimalVertex).indexOf(vnfkeycurrent)==0) {
							List<Map.Entry<Integer,ArrayList<Integer>>> nodeEntry =new ArrayList<>(preferencenode.entrySet());
							for(Map.Entry<Integer, ArrayList<Integer>> nodepre:nodeEntry) {
								nodepre.getValue().remove(vnfkeycurrent);
							}
							List<Map.Entry<Integer,ArrayList<Integer>>> vnfEntry =new ArrayList<>(preferenceVnf.entrySet());
							for(Map.Entry<Integer, ArrayList<Integer>> nvfpre:vnfEntry) {
								nvfpre.getValue().remove(OptimalVertex);
							}
						}
					}else if(currentVerResource<currentvnfResource&&usedvertex.indexOf(OptimalVertex)==-1) {
							preferenceVnf.get(vnfkeycurrent).remove(OptimalVertex);
							preferencenode.get(OptimalVertex).remove(vnfkeycurrent);
//							System.out.println("资源不够，且未部署过");
					}else {
						//如果OptimalVertex已经被使用
						//如果OptimalVertex和其部署对应的vnf，在对方的优先列表上都是最优先的，即绝配
						//那就将OptimalVertex和部署的vnf在所有优先列表中去除，这一对的部署不再更改
						if(preferencenode.get(OptimalVertex).indexOf(node2vnf.get(OptimalVertex))==0) {
							List<Map.Entry<Integer,ArrayList<Integer>>> nodeEntry =new ArrayList<>(preferencenode.entrySet());
							for(Map.Entry<Integer, ArrayList<Integer>> nodepre:nodeEntry) {
								nodepre.getValue().remove(node2vnf.get(OptimalVertex));
							}
							List<Map.Entry<Integer,ArrayList<Integer>>> vnfEntry =new ArrayList<>(preferenceVnf.entrySet());
							for(Map.Entry<Integer, ArrayList<Integer>> nvfpre:vnfEntry) {
								nvfpre.getValue().remove(OptimalVertex);
							}
//							System.out.println("又是绝配");
							continue;
						}
						//找到之前部署在m上的n’，1、然后将m上的n’移除，2、资源还回。
						//3、将n'添加到unssigned上，4、将n’列表的优先列表上将m移除 5、从m上将n'列表移除
						Integer pre_vnf=node2vnf.get((Integer)OptimalVertex);//根据node来找之前映射的vnf
//						System.out.println("node来找之前映射的vnf="+pre_vnf);
						if(pre_vnf!=null) {
							if(preferencenode.get(OptimalVertex).indexOf(vnfkeycurrent)<preferencenode.get(OptimalVertex).indexOf(pre_vnf)) {
//								System.err.println("返还资源");
								if(plan.nodeDeployVertexMap.remove(pre_vnf, OptimalVertex)==true) {
									float pre_vnf_Resource=sfc.nodeMap.get(pre_vnf).getComputeResourceDemand();
									graph.idVertexMap.get(OptimalVertex).setRemainComputeResource(currentVerResource+pre_vnf_Resource);
									usedvertex.remove(OptimalVertex);
									node2vnf.remove(OptimalVertex, pre_vnf);
									//ussigned里重新添加原节点
									for(Integer val:unssigned) {
										if(val==-1) {
											unssigned.set(unssigned.indexOf(val),pre_vnf);
											break;
										}
									}
									unssignedSize++;
								}else {
									System.err.println("plan.nodeDeployVertexMap.remove失败！");
								}
							}
						}else {
							System.err.println("pre_vnf==null");
						}
						
						//找vnfOtherlist
						ArrayList<Integer> vnfOtherlist=new ArrayList<>();
//						System.out.println("当前最优node的优先列表="+preferencenode.get(OptimalVertex));
						boolean hasfind=false;//已经找到vnfOther==vnfkeycurrent
						for(Integer vnfOther:preferencenode.get(OptimalVertex)) {
							if(hasfind==false) {
								if(vnfOther!=vnfkeycurrent) {
									continue;
								}
								if(vnfOther==vnfkeycurrent) {
									hasfind=true;
									continue;
								}
							}else {
								vnfOtherlist.add(vnfOther);
							}
						}
						if(vnfOtherlist.size()==0) {
//							System.out.println("vnfOtherlist.size()==0");
							continue;
						}
//						System.out.println("vnfOtherlist"+vnfOtherlist);
						//互删
						for(Integer vnfother:vnfOtherlist) {
							preferenceVnf.get(vnfother).remove(OptimalVertex);
							preferencenode.get(OptimalVertex).remove(vnfother);
						}
						
					}
				}
			}
			if(falture==true) {
				random_M++;
				for(Integer vertex:graph.vertexList) {//对于每一个物理节点
					graph.idVertexMap.get(vertex).setRemainComputeResource(verRe.get(vertex));
				}
				continue;
			}
			System.out.println("plan.nodeDeployVertexMap="+plan.nodeDeployVertexMap);
			//设置Plan.linkDeployEdgeMap
			Set<Map.Entry<Integer, Integer>> planEntry=plan.nodeDeployVertexMap.entrySet();
			Integer last=source;
			Integer linknode=0;
			ArrayList<Integer> usedver =new ArrayList<>();
			usedver.addAll(plan.nodeDeployVertexMap.values());
			usedver.add(source);
			System.out.println("usedver="+usedver);
			for(Map.Entry<Integer, Integer> entry:planEntry) {
				int bwDemend=sfc.linkMap.get(linknode).getBandWidthResourceDemand();
				usedver.remove(entry.getValue());
				PathAndDistence pathAndDistence=Bfs.bfsFind2(graph, last,entry.getValue(),bwDemend,usedver);
				if(pathAndDistence==null) {
					falture=true;
					break;
				}
				LinkedList<Integer> pathcur=pathAndDistence.path;
				plan.linkDeployEdgeMap.put(linknode++, pathcur);
				for(Integer lin:pathcur) {
					if(!usedver.contains(lin)) {
						usedver.add(lin);
					}
				}
				last=entry.getValue();
			}
			if(falture==true) {
				random_M++;
				continue;
			}
			//找最后一个节点到sink的最短路径
			int bwDemend=sfc.linkMap.get(linknode).getBandWidthResourceDemand();
			PathAndDistence pathAndDistence=Bfs.bfsFind2(graph, last,sink,bwDemend,usedver);
			if(pathAndDistence!=null) {
				LinkedList<Integer> pathcur=pathAndDistence.path;
				plan.linkDeployEdgeMap.put(linknode, pathcur);
				System.out.println("plan.linkDeployEdgeMap="+plan.linkDeployEdgeMap);
			}else {
				falture=true;
			}
			if(falture==true) {
				random_M++;
				continue;
			}
			//判断带宽资源够不够
			if(!CompareBandwith(plan,sfc)) {
				random_M++;
				continue;
			}
			
			break;	
    	}//for(int k=0;k<800;k++)
    	System.out.println("随机次数="+random_M);
    	System.out.println("plan.nodeDeployVertexMap="+plan.nodeDeployVertexMap);
    	System.out.println("plan.linkDeployEdgeMap="+plan.linkDeployEdgeMap);
    	return plan;//返回整条sfc的部署方案 	
    }
    
    
    /* ***************主要部署方案算法*********************/
/* 1、是否开机
 * 2、资源是否足够
 * 3、是否满足带宽平衡参数
 * 4、是，则在开启的物理节点中部署VNF   
 * 5、否则，新开机
 */
    public Plan vnfFunctionDeployList(SFC sfc) {//一条sfc的部署方案
    	Integer source = sfc.getSourceId();
        Integer sink = sfc.getSinkId();
        System.out.println("source="+source);
        System.out.println("sink="+sink);
    	System.out.println("nodeMap="+sfc.nodeMap.keySet());
    	
    	Map<Integer, Float> vnfre=new HashMap<>();
    	for(Integer sfcre:sfc.nodeList) {
    		vnfre.put(sfcre, sfc.nodeMap.get(sfcre).getComputeResourceDemand());
    	}
    	System.out.println("sfc所需资源="+vnfre);
    	
    	//打印一下每个物理节点能提供的资源
		Map<Integer, Float> verRe=new HashMap<>();
		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
			float vertexResource=graph.idVertexMap.get(vertex).getRemainComputeResource();//每个物理节点的剩余资源
			verRe.put(vertex, vertexResource);
		}
		System.out.println("每个物理节点能提供的资源="+verRe);
    	
		//打印一下每个vnf能提供的带宽资源
		Map<Integer, Integer> vnfbw=new HashMap<>();
    	for(Integer sfcre:sfc.nodeList) {
    		vnfbw.put(sfcre, sfc.linkMap.get(sfcre).getBandWidthResourceDemand());
    	}
    	System.out.println("sfc所需带宽="+vnfbw);
		
    	//打印一下每个物理节点能提供的带宽资源
//		Map<Integer, Float> vertotalbw=new HashMap<>();
//		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
//			float vertexbw=graph.idEdgeMap.get(vertex).getTotalBandwithResource();//每个物理节点的剩余资源
//			vertotalbw.put(vertex, vertexbw);
//		}
//		System.out.println("每个物理节点能提供的总总带宽="+vertotalbw);
		//打印一下每个物理节点能提供的带宽资源
		Map<Integer, Float> verbw=new HashMap<>();
		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
			float vertexbw=graph.idEdgeMap.get(vertex).getRemainBandWidthResource();//每个物理节点的剩余资源
			verbw.put(vertex, vertexbw);
		}
		System.out.println("每个物理节点能提供的带宽="+verbw);
		
    	Plan plan = new Plan(); 
    	
    	ArrayList<Integer> allvertexList = new ArrayList<>();
    	allvertexList.addAll(graph.vertexList);
        allvertexList.remove(source);
        allvertexList.remove(sink);
        System.out.println("allvertexList"+allvertexList);
        
     	CurrentSituationOfSubstrateNetwork();     
      	System.out.println("openVertexList="+openVertexList);
//      	System.out.println("openVertexMap="+openVertexMap);
      	System.out.println("NoOpenVertexList="+NoOpenVertexList);
//      	System.out.println("NoOpenVertexMap="+NoOpenVertexMap);
      	Map<Integer, List<Integer>> linkEdgeMap = new HashMap<>();
  		ArrayList<Integer> openver=new ArrayList<>();
  		openver.addAll(openVertexList);
  		openver.remove(source);
  		openver.remove(sink);
  		ArrayList<Integer> unopenver=new ArrayList<>();
  		unopenver.addAll(NoOpenVertexList);
  		unopenver.remove(source);
  		unopenver.remove(sink);
  		plan.nodeDeployVertexMap.clear();
  		plan.linkDeployEdgeMap.clear();
  		ArrayList<Integer> usedver=new ArrayList<>();
  		usedver.add(source);
  		Integer lastver=source;
		Integer mindisten=Integer.MAX_VALUE;
		Integer minver=-1;
		boolean falture=false;
      	for (int vnfKey : sfc.nodeList) {
      		if(vnfKey != sfc.nodeList.getLast()) {
      			ArrayList<Integer> openAndEnoughResource=new ArrayList<>();
      			boolean hopfind=true;
      			if(openver.size()!=0) {
      				//将资源足够的物理节点加入到openAndEnoughResource
          			for(Integer ver:openver) {
          				if(!usedver.contains(ver)&&graph.idVertexMap.get(ver).getRemainComputeResource()>sfc.nodeMap.get(vnfKey).getComputeResourceDemand()) {
          					openAndEnoughResource.add(ver);
          				}
          			}
//          			System.out.println("openAndEnoughResource="+openAndEnoughResource);
          			
          			//步骤2
          			if(openAndEnoughResource.size()!=0) {
          				//找跳数最小
          				//TODO
          				//Bfs.bfsFind(graph,lastver,ver)要重新写，将带宽判断加入
//      					Integer minVertex=ShortestPath(lastver,openAndEnoughResource);
      					//
          				LinkedList<Integer> linkpath=new LinkedList<>();
          				LinkedList<Integer> edgepath=new LinkedList<>();
      					mindisten=Integer.MAX_VALUE;
      					minver=-1;
      			    	for(Integer ver:openAndEnoughResource) {
      			    		PathAndDistence pathcur=Bfs.bfsFind2(graph,lastver,ver,sfc.linkMap.get(vnfKey-1).getBandWidthResourceDemand(),usedver);
      			    		if(pathcur==null) {
      			    			continue;
      			    		}
      			    		if(pathcur.distence<mindisten&&pathcur.distence<Configure.DISTEN) {
      			    			mindisten=pathcur.distence;
      			    			linkpath=pathcur.path;
      			    			edgepath=pathcur.edgepath;
      			    			minver=ver;
      			    		}
      			    	}
      			    	if(minver!=-1) {
      			    		graph.idVertexMap.get(minver).setRemainComputeResource(graph.idVertexMap.get(minver).getRemainComputeResource()-sfc.nodeMap.get(vnfKey).getComputeResourceDemand());
      						plan.nodeDeployVertexMap.put(vnfKey, minver);
      						plan.linkDeployEdgeMap.put(vnfKey-1, edgepath);
      						linkEdgeMap.put(vnfKey-1, linkpath);
//      						System.out.println("vnf "+vnfKey+" 部署在 "+minver);
//      						System.out.println("第 "+(vnfKey-1)+" 段链路为"+linkpath);
      						lastver=minver;
      						//将该点添加进用过的点里
      						usedver.add(minver);
      						//将转发节点加入到用过的点里
      						for(Integer lin:linkpath) {
      							if(!usedver.contains(lin)) {
      								usedver.add(lin);
      							}
      						}
      						continue;//找到了就继续下一个vnf
      					}else {
      						hopfind=false;
						}
          			}
      			}
      			
      			//步骤3
      			if(openver.size()==0||openAndEnoughResource.size()==0||!hopfind){//openAndEnoughResource.size()==0
      			//将资源足够的物理节点加入到unopenAndEnoughResource
      				ArrayList<Integer> unopenAndEnoughResource=new ArrayList<>();
          			for(Integer ver:unopenver) {
          				if(!usedver.contains(ver)&&graph.idVertexMap.get(ver).getRemainComputeResource()>sfc.nodeMap.get(vnfKey).getComputeResourceDemand()) {
          					unopenAndEnoughResource.add(ver);
          				}
          			}
//          			System.out.println("unopenAndEnoughResource="+unopenAndEnoughResource);
          			if(unopenAndEnoughResource.size()!=0) {
          				//找跳数最小
          				LinkedList<Integer> linkpath=new LinkedList<>();
          				LinkedList<Integer> edgepath=new LinkedList<>();
      					mindisten=Integer.MAX_VALUE;
      					minver=-1;
      			    	for(Integer unver:unopenAndEnoughResource) {
      			    		PathAndDistence pathcur=Bfs.bfsFind2(graph,lastver,unver,sfc.linkMap.get(vnfKey-1).getBandWidthResourceDemand(),usedver);
      			    		if(pathcur==null) {
      			    			continue;
      			    		}
      			    		if(pathcur.distence<mindisten) {
      			    			mindisten=pathcur.distence;
      			    			linkpath=pathcur.path;
      			    			edgepath=pathcur.edgepath;
      			    			minver=unver;
      			    		}
      			    	}
      			    	if(minver!=-1) {
      			    		graph.idVertexMap.get(minver).setRemainComputeResource(graph.idVertexMap.get(minver).getRemainComputeResource()-sfc.nodeMap.get(vnfKey).getComputeResourceDemand());
      						plan.nodeDeployVertexMap.put(vnfKey, minver);
      						plan.linkDeployEdgeMap.put(vnfKey-1, edgepath);
      						linkEdgeMap.put(vnfKey-1, linkpath);
//      						System.out.println("vnf "+vnfKey+" 部署在 "+minver);
//      						System.out.println("第 "+(vnfKey-1)+" 段链路为"+linkpath);
      						lastver=minver;
      						//将该点添加进用过的点里
      						usedver.add(minver);
      						//将转发节点加入到用过的点里
      						for(Integer lin:linkpath) {
      							if(!usedver.contains(lin)) {
      								usedver.add(lin);
      							}
      						}
      						continue;//找到了就继续下一个vnf
      					}else {
      						System.err.println("部署失败！！");
							falture=true;
							break;
						}
          			}else {
          				System.err.println("部署失败！！");
          				falture=true;
						break;
					}
				}
          		
      		}else {//-----------vnfKey == sfc.nodeList.getLast()----------------------------------------
      			ArrayList<Integer> openAndEnoughResource=new ArrayList<>();
      			boolean hopfind=true;
      			if(openver.size()!=0) {
      				//将资源足够的物理节点加入到openAndEnoughResource
          			for(Integer ver:openver) {
          				if(!usedver.contains(ver)&&graph.idVertexMap.get(ver).getRemainComputeResource()>sfc.nodeMap.get(vnfKey).getComputeResourceDemand()) {
          					openAndEnoughResource.add(ver);
          				}
          			}
//          			System.out.println("openAndEnoughResource="+openAndEnoughResource);
          			
          			//步骤2
          			if(openAndEnoughResource.size()!=0) {
          				//找跳数最小
          				//TODO
          				//Bfs.bfsFind(graph,lastver,ver)要重新写，将带宽判断加入
//      					Integer minVertex=ShortestPath(lastver,openAndEnoughResource);
      					//
          				LinkedList<Integer> linkpath=new LinkedList<>();
          				LinkedList<Integer> linkpath2=new LinkedList<>();
          				LinkedList<Integer> edgepath=new LinkedList<>();
          				LinkedList<Integer> edgepath2=new LinkedList<>();
      					mindisten=Integer.MAX_VALUE;
      					minver=-1;
      			    	for(Integer ver:openAndEnoughResource) {
      			    		PathAndDistence pathcur=Bfs.bfsFind2(graph,lastver,ver,sfc.linkMap.get(vnfKey-1).getBandWidthResourceDemand(),usedver);
      			    		if(pathcur==null) {
      			    			continue;
      			    		}
      			    		for(Integer lin:pathcur.path) {
      							if(!usedver.contains(lin)) {
      								usedver.add(lin);
      							}
      						}
      			    		PathAndDistence pathcur2=Bfs.bfsFind2(graph,ver,sink,sfc.linkMap.get(vnfKey).getBandWidthResourceDemand(),usedver);
      			    		if(pathcur2==null) {
      			    			continue;
      			    		}
      			    		if(pathcur.distence+pathcur2.distence<mindisten&&pathcur.distence<Configure.DISTEN&&pathcur2.distence<Configure.DISTEN) {
      			    			mindisten=pathcur.distence+pathcur2.distence;
      			    			linkpath=pathcur.path;
      			    			linkpath2=pathcur2.path;
      			    			edgepath=pathcur.edgepath;
      			    			edgepath2=pathcur2.edgepath;
      			    			minver=ver;
      			    		}
      			    	}
      			    	if(minver!=-1) {
      			    		graph.idVertexMap.get(minver).setRemainComputeResource(graph.idVertexMap.get(minver).getRemainComputeResource()-sfc.nodeMap.get(vnfKey).getComputeResourceDemand());
      						plan.nodeDeployVertexMap.put(vnfKey, minver);
      						plan.linkDeployEdgeMap.put(vnfKey-1, edgepath);
      						plan.linkDeployEdgeMap.put(vnfKey, edgepath2);
      						linkEdgeMap.put(vnfKey-1, linkpath);
      						linkEdgeMap.put(vnfKey, linkpath2);
//      						System.out.println("vnf "+vnfKey+" 部署在 "+minver);
//      						System.out.println("第 "+(vnfKey-1)+" 段链路为"+linkpath);
//      						System.out.println("第 "+vnfKey+" 段链路为"+linkpath2);
      						break;
      					}else {
      						hopfind=false;
						}
          			}
      			}
      			
      			//步骤3
      			if(openver.size()==0||openAndEnoughResource.size()==0||!hopfind){//openAndEnoughResource.size()==0
      			//将资源足够的物理节点加入到unopenAndEnoughResource
      				ArrayList<Integer> unopenAndEnoughResource=new ArrayList<>();
          			for(Integer ver:unopenver) {
          				if(!usedver.contains(ver)&&graph.idVertexMap.get(ver).getRemainComputeResource()>sfc.nodeMap.get(vnfKey).getComputeResourceDemand()) {
          					unopenAndEnoughResource.add(ver);
          				}
          			}
//          			System.out.println("unopenAndEnoughResource="+unopenAndEnoughResource);
          			if(unopenAndEnoughResource.size()!=0) {
          				//找跳数最小
          				LinkedList<Integer> linkpath=new LinkedList<>();
          				LinkedList<Integer> linkpath2=new LinkedList<>();
          				LinkedList<Integer> edgepath=new LinkedList<>();
          				LinkedList<Integer> edgepath2=new LinkedList<>();
      					mindisten=Integer.MAX_VALUE;
      					minver=-1;
      			    	for(Integer unver:unopenAndEnoughResource) {
      			    		PathAndDistence pathcur=Bfs.bfsFind2(graph,lastver,unver,sfc.linkMap.get(vnfKey-1).getBandWidthResourceDemand(),usedver);
      			    		if(pathcur==null) {
      			    			continue;
      			    		}
      			    		for(Integer lin:pathcur.path) {
      							if(!usedver.contains(lin)) {
      								usedver.add(lin);
      							}
      						}
      			    		PathAndDistence pathcur2=Bfs.bfsFind2(graph,unver,sink,sfc.linkMap.get(vnfKey).getBandWidthResourceDemand(),usedver);
      			    		if(pathcur2==null) {
      			    			continue;
      			    		}
      			    		if(pathcur.distence+pathcur2.distence<mindisten&&pathcur.distence<Configure.DISTEN&&pathcur2.distence<Configure.DISTEN) {
      			    			mindisten=pathcur.distence+pathcur2.distence;
      			    			linkpath=pathcur.path;
      			    			linkpath2=pathcur2.path;
      			    			edgepath=pathcur.edgepath;
      			    			edgepath2=pathcur2.edgepath;
      			    			minver=unver;
      			    		}
      			    	}
      			    	if(minver!=-1) {
      			    		graph.idVertexMap.get(minver).setRemainComputeResource(graph.idVertexMap.get(minver).getRemainComputeResource()-sfc.nodeMap.get(vnfKey).getComputeResourceDemand());
      						plan.nodeDeployVertexMap.put(vnfKey, minver);
      						plan.linkDeployEdgeMap.put(vnfKey-1, edgepath);
      						plan.linkDeployEdgeMap.put(vnfKey, edgepath2);
      						linkEdgeMap.put(vnfKey-1, linkpath);
      						linkEdgeMap.put(vnfKey, linkpath2);
//      						System.out.println("vnf "+vnfKey+" 部署在 "+minver);
//      						System.out.println("第 "+(vnfKey-1)+" 段链路为"+linkpath);
//      						System.out.println("------第 "+vnfKey+" 段链路为"+linkpath2);
      						break;
      					}else {//minver==-1
      						System.err.println("部署失败！！");
      						falture=true;
							break;
						}
          			}else {//unopenAndEnoughResource.size()==0
          				System.err.println("部署失败！！");
          				falture=true;
						break;
					}
				}else {//openver.size()!=0||openAndEnoughResource.size()!=0||hopfind
					System.err.println("部署失败！！");
					falture=true;
					break;
				}
			}
      		
      	}
      	if(falture==true){
      		for(Integer vertex:graph.vertexList) {//对于每一个物理节点
				graph.idVertexMap.get(vertex).setRemainComputeResource(verRe.get(vertex));
			}
      		return null;
      	}
      	System.out.println("plan.nodeDeployVertexMap="+plan.nodeDeployVertexMap);
      	System.out.println("plan.linkDeployEdgeMap="+plan.linkDeployEdgeMap);
      	System.out.println("linkEdgeMap="+linkEdgeMap);
     	return plan;//返回整条sfc的部署方案 	
       }     
      	
    
    
    public Plan vnfFunctionDeployListold2(SFC sfc/*, int source, int sink*/) {//一条sfc的部署方案
    	
   	 Plan plan = new Plan(); 
//   	 ArrayList<Integer> allvertexList = new ArrayList<>();
//        allvertexList.addAll(graph.vertexList);
        int currentVertexId = sfc.getSourceId();
        int source = sfc.getSourceId();
          int sink = sfc.getSinkId();
//        allvertexList.remove(allvertexList.indexOf(source));
//        allvertexList.remove(allvertexList.indexOf(sink));
        
//        graph.idVertexMap.remove(source);
//        graph.idVertexMap.remove(sink);
        
      	 CurrentSituationOfSubstrateNetwork();     
//   	 System.out.println("测试第"+sfc+"一条sfc部署方案");
   	 
   	 for (int vnfKey : sfc.nodeList) {
   		 Node node = sfc.nodeMap.get(vnfKey);
   		 ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点    
   		 ArrayList<Integer> openandFunctionVertexList = new ArrayList<>(); //开启并功能性匹配的节点集合，每个vnf对应一个满足功能性的底层节点集合
//   		 每部署完一个vnf都要将与上一个VNF功能性相匹配的集合清空
   		 vertexList.clear();
   		 openandFunctionVertexList.clear();
		     openandFunctionMatchVertexMap.clear();
		  if (openVertexList.size() !=0)   		
   	   {
//   	   for (Integer openvertexKey : openVertexMap.keySet()){
//   		CVertex openvertex =openVertexMap.get(openvertexKey); //遍历开启的所有物理节点
//
//   		if (node.getFunctionDemand()== openvertex.getFunctionkey() && openvertex.getRemainComputeResource() >= node.getComputeResourceDemand()) //若vnf的功能需求与开启的物理节点功能性匹配    			    			
//   		 {
//   			System.out.println("测试当前sfc中"+vnfKey+"的VNF功能性与底层物理节点"+openvertexKey+"的功能性是否匹配 ");
//   			openandFunctionVertexList.add(openvertex.getOpenVeretexId());
//   		    openandFunctionMatchVertexMap.put(openvertexKey, openvertex);	//若是开机节点中功能匹配，添加到功能匹配集合中	
//  		        System.out.println("匹配 且资源足够     节点 " + openvertexKey + " 加入到功能匹配集合中 ");   
//     	     }
//   		}
//   	  
//   	   if (openandFunctionVertexList.size() != 0 && openVertexList.size() !=0)   		
//   	   {
  		        /*
  		         *   在功能匹配的集合中找出资源足够且满足最短路要求的物理节点
  		         * 
  		         */   		
//  		  vertexList.addAll(openandFunctionVertexList);
  		vertexList.addAll(openVertexList);
//  		  int currentVertexId = source;
//  		  vertexList.remove(vertexList.indexOf(source));
//  	      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
//  	          vertexList.remove(vertexList.indexOf(sink));
//  	      }
  	          System.out.println("-------------- 在功能性匹配的集合中部署功能：" + vnfKey + "--------------");
  	          if (vnfKey != sfc.nodeList.getLast()) {
//  	        	CPath path = getBestDeployOfLastVNFByBWCost(vertexList, sfc, vnfKey, currentVertexId, sink);
  	              CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
  	              if (path == null) {
  	                  //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
  					System.out.println("部署中间功能时走不通");
  	                  return null;
  	              } else {
//  						System.out.println("path ！= null");
//  						为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
//  						System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
  	                  plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
  	                  if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
  	                      plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
  	                  } else {
  	                      LinkedList<Integer> list = new LinkedList<>();
  	                      list.addAll(path.edgeList);
  	                      plan.linkDeployEdgeMap.put(vnfKey - 1, list);
  	                  }
  	                  vertexList.removeAll(path.vertexList);    //移除已经使用的点
  	                  currentVertexId = path.getVnfDeployPosition();
  	              }
  	          }
  	          else {
//  	        	CPath path = getBestDeployOfLastVNFByBWCost(vertexList, sfc, vnfKey, currentVertexId, sink);
  	              CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
  	              if (path == null) {
  						System.out.println("部署最后一个功能时走不通");
  	                  return null;
  	              } else {
  	                  plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
  	                  //处理vnfKey前的虚拟路径
  	                  if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
  	                      plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
  	                  } else {
  	                      LinkedList<Integer> list = new LinkedList<Integer>();
  	                      list.addAll(path.firstEdgeList);
  	                      plan.linkDeployEdgeMap.put(vnfKey - 1, list);
  	                  }
  	                  //处理vnfKey后的虚拟路径
  	                  if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
  	                      plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
  	                  } else {
  	                      LinkedList<Integer> list = new LinkedList<Integer>();
  	                      list.addAll(path.secondEdgeList);
  	                      plan.linkDeployEdgeMap.put(vnfKey, list);
  	                  }
  	              }
  	          }
   	   }
   	       	   
   	   /*         开启的物理节点功能性或资源不满足
 		  *    		   在新开启的物理节点中找最短路部署此VNF
 		  *   	      
 		  */    		   		        
 	   else
 	   {       	    
		   vertexList.addAll(NoOpenVertexList);

        System.out.println("-------------- 新开机开始部署功能：" + vnfKey + "--------------");
//		   vertexList.remove(vertexList.indexOf(source));
//		      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
//		          vertexList.remove(vertexList.indexOf(sink));
//		      }
        if (vnfKey != sfc.nodeList.getLast()) {
            CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
            if (path == null) {
                //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
//					System.out.println("部署最后中间功能时走不通");
                return null;
            } else {
//					System.out.println("path ！= null");
//					为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
//					System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
                plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                    plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
                } else {
                    LinkedList<Integer> list = new LinkedList<>();
                    list.addAll(path.edgeList);
                    plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                }
                vertexList.removeAll(path.vertexList);    //移除已经使用的点                                       
//                openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中                    
                currentVertexId = path.getVnfDeployPosition();
                
                CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
                vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
//                
//                onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
//                onDelay =onDelay + Configure.ON_DELAY;//开机时延
            }
        } else {
            CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
            if (path == null) {
					System.out.println("新开启VM部署最后一个功能时走不通");
                return null;
            } else {
                plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                
//                openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中
                
                //处理vnfKey前的虚拟路径
                if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                    plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
                } else {
                    LinkedList<Integer> list = new LinkedList<Integer>();
                    list.addAll(path.firstEdgeList);
                    plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                }
                //处理vnfKey后的虚拟路径
                if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
                    plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
                } else {
                    LinkedList<Integer> list = new LinkedList<Integer>();
                    list.addAll(path.secondEdgeList);
                    plan.linkDeployEdgeMap.put(vnfKey, list);
                }
                
                
                CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
                vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
                
               
//                onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
//                onDelay =onDelay + Configure.ON_DELAY;//开机时延
            }
        }  
 	   }
     }
//   		 /*         无开启的物理节点
//   		  *    		   在新开启的物理节点中找最短路部署此VNF
//   		  *   	      
//   		  */    		   		        
//   	   else
//   	   {    		   
//   		   vertexList.addAll(NoOpenVertexList);
//   
//            System.out.println("-------------- 新开机开始部署功能：" + vnfKey + "--------------");
////   		   vertexList.remove(vertexList.indexOf(source));
////   		      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
////   		          vertexList.remove(vertexList.indexOf(sink));
////   		      }
//            if (vnfKey != sfc.nodeList.getLast()) {
//                CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
//                if (path == null) {
//                    //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
////  					System.out.println("部署最后中间功能时走不通");
//                    return null;
//                } else {
////  					System.out.println("path ！= null");
////  					为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
////  					System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
//                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
//                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
//                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
//                    } else {
//                        LinkedList<Integer> list = new LinkedList<>();
//                        list.addAll(path.edgeList);
//                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
//                    }
//                    vertexList.removeAll(path.vertexList);    //移除已经使用的点                                       
////                    openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中                    
//                    currentVertexId = path.getVnfDeployPosition();
//                    
//                    CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
//                    vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
////                    
////                    onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
////                    onDelay =onDelay + Configure.ON_DELAY;//开机时延
//                }
//            } else {
//                CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
//                if (path == null) {
//  					System.out.println("新开启VM部署最后一个功能时走不通");
//                    return null;
//                } else {
//                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
//                    
////                    openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中
//                    
//                    //处理vnfKey前的虚拟路径
//                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
//                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
//                    } else {
//                        LinkedList<Integer> list = new LinkedList<Integer>();
//                        list.addAll(path.firstEdgeList);
//                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
//                    }
//                    //处理vnfKey后的虚拟路径
//                    if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
//                        plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
//                    } else {
//                        LinkedList<Integer> list = new LinkedList<Integer>();
//                        list.addAll(path.secondEdgeList);
//                        plan.linkDeployEdgeMap.put(vnfKey, list);
//                    }
//                    
//                    
//                    CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
//                    vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
//                    
//                   
////                    onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
////                    onDelay =onDelay + Configure.ON_DELAY;//开机时延
//                }
//            }  
//   	   }
//       }   
   	  sfc.setPlan(plan);
   	  return plan;//返回整条sfc的部署方案 	
   	 
//   	  System.out.println(" 部署一条vnf的开机能量为： " + onEnergy);
//   	  System.out.println(" 部署一条vnf的开机时延为： " + onDelay);
 }     
    

/* ***************主要部署方案算法*********************
 * 测试当前vnf的功能性与已开启的openVectorList中物理节点的功能性是否匹配
 * 匹配集合不为空，在可匹配的集合中找最短路
 * 匹配集合为空，在未开启的集合中找最短路
 */
    
    public Plan vnfFunctionDeployListold1(SFC sfc) {//一条sfc的部署方案
    	int source=sfc.getSourceId();
    	int sink=sfc.getSinkId();
    	 Plan plan = new Plan(); 
    	//打印所有物理节点的剩余资源
     	ArrayList<Float> reso=new ArrayList<>();
     	for(Integer vert:graph.vertexList) {
     		reso.add(graph.idVertexMap.get(vert).getRemainComputeResource());
     	}
     	System.out.println("所有物理节点的剩余资源"+reso);
//         allvertexList.addAll(graph.vertexList);
         int currentVertexId = source;
//         
//         allvertexList.remove(allvertexList.indexOf(source));
//         allvertexList.remove(allvertexList.indexOf(sink));
         
//         graph.idVertexMap.remove(source);
//         graph.idVertexMap.remove(sink);
         
       	 CurrentSituationOfSubstrateNetwork();     
//    	 System.out.println("测试第"+sfc+"一条sfc部署方案");
    	 
    	 for (int vnfKey : sfc.nodeList) {
    		 Node node = sfc.nodeMap.get(vnfKey);
    		 ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点    
    		 ArrayList<Integer> openandFunctionVertexList = new ArrayList<>(); //开启并功能性匹配的节点集合，每个vnf对应一个满足功能性的底层节点集合
//    		 每部署完一个vnf都要将与上一个VNF功能性相匹配的集合清空
    		 vertexList.clear();
    		 openandFunctionVertexList.clear();
 		     openandFunctionMatchVertexMap.clear();
 		     if (openVertexList.size() !=0)  //用于存放开启的物理节点集合  		
 		     {
//    	   for (Integer openvertexKey : openVertexMap.keySet()){
//    		CVertex openvertex =openVertexMap.get(openvertexKey); //遍历开启的所有物理节点
//
//    		if (node.getFunctionDemand()== openvertex.getFunctionkey() && openvertex.getRemainComputeResource() >= node.getComputeResourceDemand()) //若vnf的功能需求与开启的物理节点功能性匹配    			    			
//    		 {
//    			System.out.println("测试当前sfc中"+vnfKey+"的VNF功能性与底层物理节点"+openvertexKey+"的功能性是否匹配 ");
//    			openandFunctionVertexList.add(openvertex.getOpenVeretexId());
//    		    openandFunctionMatchVertexMap.put(openvertexKey, openvertex);	//若是开机节点中功能匹配，添加到功能匹配集合中	
//   		        System.out.println("匹配 且资源足够     节点 " + openvertexKey + " 加入到功能匹配集合中 ");   
//      	     }
//    		}
//    	  
//    	   if (openandFunctionVertexList.size() != 0 && openVertexList.size() !=0)   		
//    	   {
   		        /*
   		         *   在功能匹配的集合中找出资源足够且满足最短路要求的物理节点
   		         * 
   		         */   		
//   		  vertexList.addAll(openandFunctionVertexList);
   		vertexList.addAll(openVertexList);
//   		  int currentVertexId = source;
//   		  vertexList.remove(vertexList.indexOf(source));
//   	      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
//   	          vertexList.remove(vertexList.indexOf(sink));
//   	      }
   	          System.out.println("-------------- 在功能性匹配的集合中部署功能：" + vnfKey + "--------------");
   	          if (vnfKey != sfc.nodeList.getLast()) {
//   	        	CPath path = getBestDeployOfLastVNFByBWCost(vertexList, sfc, vnfKey, currentVertexId, sink);
   	              CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
   	              if (path == null) {
   	                  //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
   					System.out.println("部署中间功能时走不通");
   	                  return null;
   	              } else {
//   						System.out.println("path ！= null");
//   						为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
//   						System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
   	                  plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
   	                  if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
   	                      plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
   	                  } else {
   	                      LinkedList<Integer> list = new LinkedList<>();
   	                      list.addAll(path.edgeList);
   	                      plan.linkDeployEdgeMap.put(vnfKey - 1, list);
   	                  }
   	                  vertexList.removeAll(path.vertexList);    //移除已经使用的点
   	                  currentVertexId = path.getVnfDeployPosition();
   	              }
   	          }
   	          else {
//   	        	CPath path = getBestDeployOfLastVNFByBWCost(vertexList, sfc, vnfKey, currentVertexId, sink);
   	              CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
   	              if (path == null) {
   						System.out.println("部署最后一个功能时走不通");
   	                  return null;
   	              } else {
   	                  plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
   	                  //处理vnfKey前的虚拟路径
   	                  if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
   	                      plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
   	                  } else {
   	                      LinkedList<Integer> list = new LinkedList<Integer>();
   	                      list.addAll(path.firstEdgeList);
   	                      plan.linkDeployEdgeMap.put(vnfKey - 1, list);
   	                  }
   	                  //处理vnfKey后的虚拟路径
   	                  if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
   	                      plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
   	                  } else {
   	                      LinkedList<Integer> list = new LinkedList<Integer>();
   	                      list.addAll(path.secondEdgeList);
   	                      plan.linkDeployEdgeMap.put(vnfKey, list);
   	                  }
   	              }
   	          }
    	   }
    	       	   
    	   /*         开启的物理节点功能性或资源不满足
  		  *    		   在新开启的物理节点中找最短路部署此VNF
  		  *   	      
  		  */    		   		        
  	   else//openVertexList.size() ==0)  //用于存放开启的物理节点集合==0
  	   {       	    
		   vertexList.addAll(NoOpenVertexList);

         System.out.println("-------------- 新开机开始部署功能：" + vnfKey + "--------------");
//		   vertexList.remove(vertexList.indexOf(source));
//		      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
//		          vertexList.remove(vertexList.indexOf(sink));
//		      }
         if (vnfKey != sfc.nodeList.getLast()) {
             CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
             if (path == null) {
                 //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
//					System.out.println("部署最后中间功能时走不通");
                 return null;
             } else {
//					System.out.println("path ！= null");
//					为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
//					System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
                 plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                 if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                     plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
                 } else {
                     LinkedList<Integer> list = new LinkedList<>();
                     list.addAll(path.edgeList);
                     plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                 }
                 vertexList.removeAll(path.vertexList);    //移除已经使用的点                                       
//                 openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中                    
                 currentVertexId = path.getVnfDeployPosition();
                 
                 CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
                 vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
//                 
//                 onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
//                 onDelay =onDelay + Configure.ON_DELAY;//开机时延
             }
         } else {
             CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
             if (path == null) {
					System.out.println("新开启VM部署最后一个功能时走不通");
                 return null;
             } else {
                 plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                 
//                 openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中
                 
                 //处理vnfKey前的虚拟路径
                 if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                     plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
                 } else {
                     LinkedList<Integer> list = new LinkedList<Integer>();
                     list.addAll(path.firstEdgeList);
                     plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                 }
                 //处理vnfKey后的虚拟路径
                 if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
                     plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
                 } else {
                     LinkedList<Integer> list = new LinkedList<Integer>();
                     list.addAll(path.secondEdgeList);
                     plan.linkDeployEdgeMap.put(vnfKey, list);
                 }
                 
                 
                 CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
                 vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
                 
                
//                
             }
         }  
  	   }
      }
//    		 /*         无开启的物理节点
//    		  *    		   在新开启的物理节点中找最短路部署此VNF
//    		  *   	      
//    		  */    		   		        
//    	   else
//    	   {    		   
//    		   vertexList.addAll(NoOpenVertexList);
//    
//             System.out.println("-------------- 新开机开始部署功能：" + vnfKey + "--------------");
////    		   vertexList.remove(vertexList.indexOf(source));
////    		      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
////    		          vertexList.remove(vertexList.indexOf(sink));
////    		      }
//             if (vnfKey != sfc.nodeList.getLast()) {
//                 CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
//                 if (path == null) {
//                     //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
////   					System.out.println("部署最后中间功能时走不通");
//                     return null;
//                 } else {
////   					System.out.println("path ！= null");
////   					为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
////   					System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
//                     plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
//                     if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
//                         plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
//                     } else {
//                         LinkedList<Integer> list = new LinkedList<>();
//                         list.addAll(path.edgeList);
//                         plan.linkDeployEdgeMap.put(vnfKey - 1, list);
//                     }
//                     vertexList.removeAll(path.vertexList);    //移除已经使用的点                                       
////                     openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中                    
//                     currentVertexId = path.getVnfDeployPosition();
//                     
//                     CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
//                     vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
////                     
////                     onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
////                     onDelay =onDelay + Configure.ON_DELAY;//开机时延
//                 }
//             } else {
//                 CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
//                 if (path == null) {
//   					System.out.println("新开启VM部署最后一个功能时走不通");
//                     return null;
//                 } else {
//                     plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
//                     
////                     openVertexList.add(path.getVnfDeployPosition());//新开机的节点加入到开机序列中
//                     
//                     //处理vnfKey前的虚拟路径
//                     if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
//                         plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
//                     } else {
//                         LinkedList<Integer> list = new LinkedList<Integer>();
//                         list.addAll(path.firstEdgeList);
//                         plan.linkDeployEdgeMap.put(vnfKey - 1, list);
//                     }
//                     //处理vnfKey后的虚拟路径
//                     if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
//                         plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
//                     } else {
//                         LinkedList<Integer> list = new LinkedList<Integer>();
//                         list.addAll(path.secondEdgeList);
//                         plan.linkDeployEdgeMap.put(vnfKey, list);
//                     }
//                     
//                     
//                     CVertex vertex = NoOpenVertexMap.get(plan.nodeDeployVertexMap.get(vnfKey));
//                     vertex.setFunctionkey(sfc.nodeMap.get(vnfKey).getFunctionDemand()); //新开机的节点的功能性为部署在上面的第一个vnf的功能性
//                     
//                    
////                     onEnergy = onEnergy + Configure.ON_ENERGY ;//开机能耗
////                     onDelay =onDelay + Configure.ON_DELAY;//开机时延
//                 }
//             }  
//    	   }
//        }   
    	  sfc.setPlan(plan);
    	  return plan;//返回整条sfc的部署方案 	
    	 
//    	  System.out.println(" 部署一条vnf的开机能量为： " + onEnergy);
//    	  System.out.println(" 部署一条vnf的开机时延为： " + onDelay);
  } 

    /**
     * 按SFC不同长度分别部署一定数量的SFC
     */
    public void testDynamicDeploySFCofFunctionMaching2(CGraph graph) {
        int success = 0;
        int fail = 0;
        float successRate = 0;
        float onEnergy = 0;
        float offEnergy = 0;
        float onDelay = 0;
        float offDelay = 0;

        Random random = new Random();
        for (int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
            success = 0;
            fail = 0;
            idSFCMap.clear();
            generateSFCs(i, Configure.SFCNUM);    //生成200条SFC

            long start = System.currentTimeMillis();
            float reliability = 0;
            int resource = 0;
            for (Integer sfcKey : idSFCMap.keySet()) {
                int source = random.nextInt(graph.getVertexNum());
                int sink = random.nextInt(graph.getVertexNum());
                while (source == sink) {
                    sink = random.nextInt(graph.getVertexNum());
                }
                SFC sfc = idSFCMap.get(sfcKey);
                
                
//            Plan plan = deploySFCByDelay(sfc, source, sink);
//                Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
                Plan plan = vnfFunctionDeployList(sfc);
                
                
                sfc.setPlan(plan);
                if (sfc.deploySFC(graph)) {
                	workSfcList.add(sfc.getSfcId());    //部署成功，加入工作队列
                    onEnergy= sfc.getOnEnergy(graph);
                    onDelay=sfc.getOnDelay(graph);
//                    reliability += sfc.getDeployedReliability(graph);
                    resource += sfc.getDeployedResource(graph);
                    success++;
                } else {
                    fail++;
                }
            }
//            reliability = reliability / success;  //所有SFC部署后总的平均可靠性

            long end = System.currentTimeMillis();
            float time = (float) (end - start) / 1000;
            successRate = (float) success / idSFCMap.size();

            try {
                File file = new File("./result/2.txt");
                FileWriter fw = new FileWriter(file, true);     //以追加的形式向文件写入数据
                PrintWriter out = new PrintWriter(new BufferedWriter(fw));
                out.println("节点总数：" + graph.getVertexNum() + "    SFC长度为 " + i + " 时， 总共 " + idSFCMap.size() + " 条 SFC	 " + " 成功 " + success +
                        " 条     失败 " + fail + " 条    成功率 " + successRate + "     耗时 " + time + "s    开机能量为 "
                        + onEnergy +"  关机能量为 "  + offEnergy + "    资源开销 " + resource + "     平均部署功能数 " + graph.avgVNFperVertex());
                out.flush();
                out.close();
            } catch (FileNotFoundException cnfe) {
                System.err.println("找不到文件");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("节点总数：" + graph.getVertexNum() + "    SFC长度为 " + i + " 时， 总共 " + idSFCMap.size() + " 条 SFC	 " + " 成功 " + success +
                    " 条     失败 " + fail + " 条    成功率 " + successRate + "     耗时 " + time + "s    开机能量为 "
                    + onEnergy +"  关机能量为 "  + offEnergy + "    资源开销 " + resource + "     平均部署功能数 " + graph.avgVNFperVertex());
            graph.recycleGraphResource();
        }
    }
    

    /*
     * 测试主算法
     * 功能性匹配的动态部署,打印出资源消耗，开机能耗，开机时延，关机能耗，关机时延，（VM运行能耗）
     * 
     */
    public void testDynamicDeploySFCofFunctionMaching1() {
        int success = 0;
        int fail = 0;
        float allEnergy = 0;
        float blockRate = 0;
        float wasteRate = 0;
        float onEnergy = 0;
        float offEnergy = 0;
        float workEnergy = 0;
        float emptyEnergy = 0;
        float onDelay = 0;
        float offDelay = 0;
//        float reliability = 0;
        float resource = 0;
        float noderesource = 0;
        float linkresource = 0;
        float successRate=0;
        long startTime = 0;
        long endTime = 0;
        int firstBlock = -1;
        double time = 0;

        Information info = new Information();// 传递给子程序的数据
        
        File file = null;
        FileWriter fw = null;
        PrintWriter out = null;
        try {
            file = new File("./result/2.txt"); //20个顶点83条边的拓扑，15条SFC的在线率，10000条SFC
            fw = new FileWriter(file,false);     //以追加的形式向文件写入数据
            out = new PrintWriter(new BufferedWriter(fw));
//            out.println("SFC长度\t时间（ms）\t\t\t\t成功率\t\t\t阻塞率\t\t第一次阻塞\t\t\t总资源\t\t\t节点资源\t\t\t链路资源\t\t\t工作能耗\t\t\t空载能耗\t\t\t开机能耗\t\t开机时延\t\t关机能耗\t\t关机时延");
            out.println("SFC数量\t时间（ms）\t\t\t\t成功率\t\t\t阻塞率\t\t第一次阻塞\t\t\t总资源\t\t\t节点资源\t\t\t链路资源\t\t\t总能耗\t\t\t空载能耗\t\t\t开机能耗\t\t开机时延\t\t关机能耗\t\t关机时延\t\t能源浪费占比");
            out.flush();
        }catch (FileNotFoundException e) {
            System.err.println("文件不存在！");
        }catch (IOException e) {
            e.printStackTrace();
        }

        //生成不同长度的SFC
        for(int i = 1; i <=1; i=i+1) {
//        	int i=10;
            graph.recycleGraphResource();//清空图中的所有资源，开始一组SFC的部署          
            
            info.vertexnum = graph.getVertexNum();
            int tempEdge = graph.getEdgeNum();
            info.edgenum = 0;
            info.edgestart = new int[tempEdge + 1];
            info.edgeend = new int[tempEdge +1];
            for (int j = 1; j <= tempEdge; j++)
            {
            	if ((graph.idEdgeMap.get(j-1).getSourceId() + 1) < (graph.idEdgeMap.get(j-1).getSinkId() + 1))
            	{
            		info.edgenum++;
            		info.edgestart[info.edgenum] = graph.idEdgeMap.get(j-1).getSourceId() + 1;
            		info.edgeend[info.edgenum] = graph.idEdgeMap.get(j-1).getSinkId() + 1;
            	}
            }
            
            info.SFCnum = 0;
            info.VNFtype = 10;
            
            idSFCMap.clear();
            CurrentSituationOfSubstrateNetwork();
            for (Integer vertexKey : graph.idVertexMap.keySet()) {    		
        		CVertex vertex =graph.idVertexMap.get(vertexKey);
            } 
            
            generateSFCs(3, 10);//生成指定长度，数量的sfc
            
            info.SFCkey = new int[11];
            info.SFCsource = new int[11];
            info.SFCsink = new int[11];
            info.SFCbandwidth = new int[11];
            info.SFCusinglinknum = new int[11];
            info.SFCusinglink = new int[11][50];
            info.VNFnum = new int[11];
            for (int j = 0; j <= 10 ; j++)
            	info.VNFnum[j] = 3;
            
            info.VNFtypeinSFC = new int[11][4];
            info.VNFdeployedon = new int [11][4];
            info.VNFITresource = new double[11];
            for (int j = 0; j < 11 ; j++)
            {
            	info.VNFITresource[j] = 5;
            }
             success = 0;
             fail = 0;
             allEnergy = 0;
             blockRate = 0;
             wasteRate = 0;
            onEnergy = 0;
             offEnergy = 0;
             workEnergy = 0;
             emptyEnergy = 0;
             onDelay = 0;
             offDelay = 0;
//             reliability = 0;
             resource = 0;
             noderesource = 0;
            linkresource = 0;
            successRate=0;
            startTime = 0;
             endTime = 0;
            firstBlock = -1;
             time = 0;
            //遍历所有随机生成的SFC
            for(int sfcKey : idSFCMap.keySet()) {
                startTime = System.currentTimeMillis();
                SFC sfc = idSFCMap.get(sfcKey); //当前需要部署的SFC
                System.out.println("开始部署第"+sfcKey+"条sfc");
                
         
//              Plan plan = vnfFunctionDeployListContrast(sfc);
                Plan plan = vnfFunctionDeployList(sfc);
                
                sfc.setPlan(plan);
                if(sfc.deploySFC(graph)) {
                    workSfcList.add(sfc.getSfcId());    //部署成功，加入工作队列
//                    
//                    reliability += sfc.getDeployedReliability(graph);
                    resource += sfc.getDeployedResource(graph);
                    linkresource+=sfc.getDeployedLinkResource(graph);
                    noderesource+=sfc.getDeployedNodeResource(graph);
                    success++;
                    endTime = System.currentTimeMillis();
                    time += (endTime - startTime);
                    
                    info.SFCnum++;
                    info.SFCkey[info.SFCnum] = sfcKey;
                    info.SFCsource[info.SFCnum] = sfc.getSourceId() + 1;
                    info.SFCsink[info.SFCnum] = sfc.getSinkId() + 1;
                    info.SFCbandwidth[info.SFCnum] = 5;
                    
                    for (int j = 0 ; j <= info.VNFnum[info.SFCnum]; j++)
                    {
                    	 List<Integer> tmpList = plan.linkDeployEdgeMap.get(j);
                    	 for (int edgeKey : tmpList)
                    	 {
                    		 info.SFCusinglinknum[info.SFCnum] ++;
                    		 info.SFCusinglink[info.SFCnum][info.SFCusinglinknum[info.SFCnum]] = (edgeKey/2) + 1;
                    	 }
                    }
                    
                    for (int j = 1; j <= sfc.getLength(); j++)
                    {
                    	info.VNFtypeinSFC[info.SFCnum][j] = sfc.nodeMap.get(j).getFunctionDemand();
                    	info.VNFdeployedon[info.SFCnum][j] = plan.nodeDeployVertexMap.get(j) + 1;
                    }
             
                    
                }
                else {
                    fail++;
                    if(fail == 1) {
                        firstBlock = sfc.getSfcId();    //第一次阻塞发生在哪一个SFC部署时
                    }
                }
                System.out.println("第"+sfcKey+"条sfc部署完成");               
                System.out.println("-------------------------------------------------------------------");
//                for(int workSFCKey : workSfcList) { //遍历工作SFC集合，定期回收已经离开的SFC资源
//                    SFC workSfc = idSFCMap.get(workSFCKey);
//                   
//                    if(workSfc.getContinueTime() <= sfc.getSfcId()) {//该工作SFC到期，回收资源
//                    	 System.out.println("空载能耗"+emptyEnergy);
//                        if(workSfc.checkTheNodes() && workSfc.checkTheLinks()) {//判断是否找到了部署节点和部署路径
//                        	
//                        	workSfc.recycleResource(graph);                            
//                            
//                            workEnergy+= workSfc.getWorkEnergy(graph);
//                            emptyEnergy+= workSfc.getEmptyEnergy(graph);
//                            offEnergy+= workSfc.getOffEnergy(graph);
//                            offDelay+=workSfc.getOffDelay(graph);  
//                            System.out.println("总的关机机能量："+offEnergy);
//                            System.out.println("总的关机时延："+ offDelay);
//                            System.out.println("总空载能量："+ emptyEnergy);
//                        }
//                        recycleList.add(workSFCKey);
//                    }
//                }
//                for(int recycleId : recycleList) {
//                    workSfcList.remove(workSfcList.indexOf(recycleId));
//                }
//                workSfcList.removeAll(recycleList);
//                recycleList.clear();
            }           
            
//            // 随机生成一个要改变的SFC
//            int SFCchange = Configure.random.nextInt(info.SFCnum - 1) +1;// 随机改变的SFC标识
//            int changeSFCsource = Configure.random.nextInt(info.vertexnum - 1) + 1; // 随机改变的SFC起点
//            int changeSFCsink = 0;
//            do
//            {
//            	changeSFCsink = Configure.random.nextInt(info.vertexnum - 1) + 1;//随机改变的SFC终点
//            }
//            while (changeSFCsink != changeSFCsource);

            
            info.RemainITresource = new double[info.vertexnum + 1];
            for (int j = 1; j <= info.vertexnum ; j++)
            {
            	info.RemainITresource[j] = graph.idVertexMap.get(j-1).getRemainComputeResource();
            }
            info.Remainbandwidth = new double[info.edgenum + 1];
            int tempcount = 0;
            for (int j = 1; j <= tempEdge; j++)
            {
            	if ((graph.idEdgeMap.get(j-1).getSourceId() + 1) < (graph.idEdgeMap.get(j-1).getSinkId() + 1))
            	{
            		tempcount++;
                	info.Remainbandwidth[tempcount] = graph.idEdgeMap.get(j-1).getRemainBandWidthResource();
            	}

            }
            
            
////            // check the info
            System.out.println("------------ check Info ---------------");
            System.out.println(info.vertexnum);
            for (int j = 1; j <= info.vertexnum; j++)
            {
            	System.out.println("Vertex " + j + " :");
            	System.out.println(info.RemainITresource[j]);
            }
            System.out.println(info.edgenum);
            for (int j = 1; j <= info.edgenum; j++)
            {
            	System.out.println("Edge "+j+" :");
            	System.out.println(info.edgestart[j]);
            	System.out.println(info.edgeend[j]);
            	System.out.println(info.Remainbandwidth[j]);
            }
            System.out.println("VNFtype: " + info.VNFtype);
            for (int j = 1; j <= info.VNFtype; j++)
            {
            	System.out.println("VNF " + j + " IT resource: " + info.VNFITresource[j]);
            }
            
            System.out.println("SFCnum: " + info.SFCnum);
            for (int j = 1; j <= info.SFCnum; j++)
            {
            	System.out.println("Sfc " + j + " :");
            	System.out.println("Source: " + info.SFCsource[j]);
            	System.out.println("Sink: " + info.SFCsink[j]);
            	System.out.println("Bandewidth: " + info.SFCbandwidth[j]);
            	System.out.println("VNFnum: " + info.VNFnum[j]);
            	for (int k = 1; k <= info.VNFnum[j]; k++)
            	{
            		System.out.println("VNFtype: " + info.VNFtypeinSFC[j][k]);
            		System.out.println("VNFdeployed: " + info.VNFdeployedon[j][k]);
            	}
            	
            	System.out.println("Sfc "+ j + " using " + info.SFCusinglinknum[j] + " links:");
            	for (int k = 1; k <= info.SFCusinglinknum[j] ; k++)
            	{
            		System.out.println("using link " + info.SFCusinglink[j][k]);
            	}
            	System.out.println("");
            }
            
            
            dynamicservicedeployed = new DynamicServiceDeployed(info);
            try {
				DynamicServiceDeployed.Mainplan();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//            workSfcList.clear();
            
            allEnergy=onEnergy+workEnergy+emptyEnergy+offEnergy;
            wasteRate=1-workEnergy/allEnergy;
            successRate = (float) success/(float) idSFCMap.size();
            blockRate = (float) fail/(float) idSFCMap.size();
//            reliability = reliability/success;
            time = (double)(time/(double)success);
            resource = resource/success;
            linkresource=linkresource/success;
            noderesource=noderesource/success;
//            System.out.println("开机数 " + onDelay);
//            out.println(i + "\t\t" + (endTime-startTime) + "ms\t\t" + blockRate + "\t\t"+ firstBlock + "\t\t" + resource + "\t\t" + reliability);
//            out.printf("%d\t\t%6f\t\t%10.4f\t\t%10.4f\t\t%5d\t\t%10.4f\t\t%10.4f\t\t%10.4f\t\t%10.5f\t\t%10.5f\t\t%10.5f  %10.3f   %10.5f  %10.3f   %10.3f", i, time,successRate,blockRate, firstBlock, resource,noderesource,linkresource, allEnergy, emptyEnergy,onEnergy, onDelay, offEnergy, offDelay, wasteRate);
          
//            out.printf("%d\t\t%6f\t\t%10.4f\t\t%10.4f\t\t%5d\t\t%10.4f\t\t%10.4f\t\t%10.4f\t\t%10.5f\t\t%10.5f\t\t%10.5f  %10.3f   %10.5f  %10.3f", i, time,successRate,blockRate, firstBlock, resource,noderesource,linkresource, workEnergy, emptyEnergy,onEnergy, onDelay, offEnergy, offDelay);
            out.println();
            out.flush();
//            System.out.println("SFC总数目 " + idSFCMap.size() + "      成功部署 " + success + " 条     失败 " + fail + " 条    成功率 " + successRate + " 条    阻塞率 " + blockRate + "     耗时 " + time + " s");
            CurrentSituationOfSubstrateNetwork();
            for (Integer vertexKey : graph.idVertexMap.keySet()) {    		
        		CVertex vertex =graph.idVertexMap.get(vertexKey);
        		vertex.off=true;//每次一定数量的SFC部署完成之后，底层物理节点全部为关机状态：1、空载
                vertex.setVMdelayTime(0);//每次一定数量的SFC部署完成之后，2、物理节点延迟关机时间都要恢复为0
            }
        }         //SFC的生成循环 
        if(out != null) {
            out.close();
        }
        System.out.println("结束部署，回收所有资源，关闭所有VM"); 
        
    }
//    /*
//     * 动态部署，回收资源
//     * 
//     */
//    public void RecyleResourceOfNetwork() {
//    	long startTime = 0;
//    	long endTime = 0;
//    startTime = System.currentTimeMillis();
//	for (Integer sfcKey : idSFCMap.keySet()) {
////		if (sfcKey.intValue() < Configure.INIT_SFC_AMOUCT) {
////			continue;
////		}
//		SFC sfc = idSFCMap.get(sfcKey);
//		//遍历已经部署的SFC集合，回收已经到期的SFC所占用的资源
//		if (Configure.IS_DEPLOYED) {
//			//回收前先进行能量测试，以SFC条数为单位，不以时间为单位。
//			for (Integer vertexKey : cGraphManager.graph.vertexMap.keySet()) {
//				CVertex vertex = cGraphManager.graph.vertexMap.get(vertexKey);
//				if (vertex.isON()) {
//					serverEnergy = serverEnergy + vertex.getCurrentEnergyConsumerOfServer();
//					linkEnergy = linkEnergy + vertex.getSwitchEnergyCost();
//				}
//			}
//			for (Integer workSFCKey : workSFCList) {
//				SFC workSFC = idSFCMap.get(workSFCKey);
//				if (workSFC.getContinueTime() <= sfc.getSFCID()) {	
//					//到期，回收资源
//					if (workSFC.checkTheLinks() && workSFC.checkTheNodes()) {
//						offEnergy = workSFC.recycleResource(cGraphManager.graph, offEnergy);
//					}
//					recycleList.add(workSFCKey);
//					removeHistoryGraph(workSFC); 			//回收一条SFC后，需要更新历史信息图
//				}
//			}
//			for (Integer recyleSFCKey: recycleList) {
//				workSFCList.remove(workSFCList.indexOf(recyleSFCKey));
//			}
//			recycleList.clear();
//		}
//		deployOneSFCByEnergyEfficient(sfc);
//		if (sfc.checkTheNodes() && sfc.checkTheLinks()) {
////			sfc.printSFC(cGraphManager.graph);
//			if (Configure.IS_DEPLOYED) {
//				onEnergy = sfc.deploySFC(cGraphManager.graph, onEnergy);
//			}
//			workSFCList.add(sfc.getSFCID());	//部署成功，加入工作SFC序列
//			updateHistoryGraphBySFC(sfc); 		//部署一条SFC，更新历史信息功能图资源
//			costSum += sfc.getLinkCostOfSFC();
//			delaySum += sfc.getDeployDelayOfSFC(cGraphManager.graph);
//			success++;
//		} else {
//			fail++;
//		}
//	}
//	if (Configure.IS_DEPLOYED) {
//		int SFCCount = idSFCMap.size();
//		while (workSFCList.size() != 0) {
//			//回收前先进行能量测试，以SFC条数为单位，不以时间为单位。
//			for (Integer vertexKey : cGraphManager.graph.vertexMap.keySet()) {
//				CVertex vertex = cGraphManager.graph.vertexMap.get(vertexKey);
//				if (vertex.isON()) {
//					serverEnergy += vertex.getCurrentEnergyConsumerOfServer();
//					linkEnergy = linkEnergy + vertex.getAccepterEnergyConsumer() + vertex.getSenderEnergyConsumer();
//				}
//			}
//				
//			for (Integer workSFCKey : workSFCList) {
//				SFC workSFC = idSFCMap.get(workSFCKey);
//				if (workSFC.getContinueTime() <= SFCCount) {	
//					//到期，回收资源
//					if (workSFC.checkTheLinks() && workSFC.checkTheNodes()) {
//						offEnergy = workSFC.recycleResource(cGraphManager.graph, offEnergy);
//					}
//					recycleList.add(workSFCKey);
//				}
//			}
//				
//			for (Integer recyleSFCKey: recycleList) {
//				workSFCList.remove(workSFCList.indexOf(recyleSFCKey));
//			}
//			recycleList.clear();
//			SFCCount++;
//		}
//	}
//   }		   
    
//   	
//    /*
//     * 以时延为标准开启新的VM部署当前VNF
//     * 
//     */
//    public Plan newVMdeployVNFByDelay(SFC sfc, int source, int sink) {
////      System.out.println("进入deploySFCByDelay方法");
//      Plan plan = new Plan();
//      ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点
//      vertexList.addAll(NoOpenVertexList);
//      int currentVertexId = source;
//
//      /*if(source == sfc.getSourceId()) {
//          vertexList.remove(vertexList.indexOf(source));
//      }*/
//      vertexList.remove(vertexList.indexOf(source));
//      if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
//          vertexList.remove(vertexList.indexOf(sink));
//      }
////      System.out.println("开始执行sfc功能节点的部署");
//      for (int vnfKey : sfc.nodeList) {
////          System.out.println("-------------- 开始部署功能：" + vnfKey + "--------------");
//          if (vnfKey != sfc.nodeList.getLast()) {
//              CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
//              if (path == null) {
//                  //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
////					System.out.println("部署最后中间功能时走不通");
//                  return null;
//              } else {
////					System.out.println("path ！= null");
////					为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
////					System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
//                  plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
//                  if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
//                      plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
//                  } else {
//                      LinkedList<Integer> list = new LinkedList<>();
//                      list.addAll(path.edgeList);
//                      plan.linkDeployEdgeMap.put(vnfKey - 1, list);
//                  }
//                  vertexList.removeAll(path.vertexList);    //移除已经使用的点
//                  currentVertexId = path.getVnfDeployPosition();
//              }
//          } else {
//              CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
//              if (path == null) {
//					System.out.println("部署最后一个功能时走不通");
//                  return null;
//              } else {
//                  plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
//                  //处理vnfKey前的虚拟路径
//                  if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
//                      plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
//                  } else {
//                      LinkedList<Integer> list = new LinkedList<Integer>();
//                      list.addAll(path.firstEdgeList);
//                      plan.linkDeployEdgeMap.put(vnfKey - 1, list);
//                  }
//                  //处理vnfKey后的虚拟路径
//                  if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
//                      plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
//                  } else {
//                      LinkedList<Integer> list = new LinkedList<Integer>();
//                      list.addAll(path.secondEdgeList);
//                      plan.linkDeployEdgeMap.put(vnfKey, list);
//                  }
//              }
//          }
//
//      }
////      System.out.println("deploySFCByDelay方法执行完成");
//      return plan;//返回整条sfc的部署方案
//  }
    
	/**
     * 随机生成SFC的长度，静态部署一定数量的SFC到网络中
     */
    public void staticDeployOfSFC(CGraph graph) {
        int success = 0;
        int fail = 0;
        float successRate = 0;
        idSFCMap.clear();

        for (int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
            int num = (int) (Configure.random.nextFloat() * Configure.SFCNUM);
            generateSFCs(i, num);
        }
//        System.out.println("SFC总数目 " + idSFCMap.size());

        long start = System.currentTimeMillis();
        success = computeSuccessNum(success, fail);//基本数据类型是值传递，这里形参的改变不会影响实参的值
        long end = System.currentTimeMillis();
        float time = (float) (end - start) / 1000;

        fail = idSFCMap.size() - success;
        successRate = (float) success / idSFCMap.size();

        try {
            File file = new File("./result/staticDeployment.txt");
            FileWriter fw = new FileWriter(file, true);     //以追加的形式向文件写入数据
            PrintWriter out = new PrintWriter(new BufferedWriter(fw));
            out.println("节点总数：" + graph.getVertexNum() + "  SFC总数目 " + idSFCMap.size() + "      成功部署 " + success + " 条     失败 " + fail + " 条    成功率 " + successRate + "     耗时 " + time + " s");
            out.flush();
            out.close();
        } catch (FileNotFoundException cnfe) {
            System.err.println("找不到文件");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("SFC总数目 " + idSFCMap.size() + "      成功部署 " + success + " 条     失败 " + fail + " 条    成功率 " + successRate + "     耗时 " + time + " s");
    }

    /**
     * 计算当前网络图中所有SFC部署的成功数和失败数
     *
     * @param success
     * @param fail
     */
    public int computeSuccessNum(int success, int fail) {
        float reliability = 0;
        Random random = new Random(47);
        for (Integer sfcKey : idSFCMap.keySet()) {
            int source = random.nextInt(graph.getVertexNum());
            int sink = random.nextInt(graph.getVertexNum());
            while (source == sink) {
                sink = random.nextInt(graph.getVertexNum());
            }
            SFC sfc = idSFCMap.get(sfcKey);
//            Plan plan = deploySFCByDelay(sfc, source, sink);
//            Plan plan = deploySFCByLoadBalance(sfc, source, sink);
            Plan plan = deploySFCByReliability(sfc, source, sink);
            sfc.setPlan(plan);
            if (sfc.deploySFC(graph)) {
                reliability += sfc.getDeployedReliability(graph);
                success++;
            } else {
                fail++;
            }
        }
        reliability = reliability / idSFCMap.size();  //所有SFC部署后总的可靠性
        return success;
    }

    /**
     * 按SFC不同长度分别部署一定数量的SFC
     */
    public void testTheDeployOfSFC(CGraph graph) {
        int success = 0;
        int fail = 0;
        float successRate = 0;

        Random random = new Random();
        for (int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
            success = 0;
            fail = 0;
            idSFCMap.clear();
            generateSFCs(i, Configure.SFCNUM);    //生成200条SFC

            long start = System.currentTimeMillis();
            float reliability = 0;
            int resource = 0;
            for (Integer sfcKey : idSFCMap.keySet()) {
                int source = random.nextInt(graph.getVertexNum());
                int sink = random.nextInt(graph.getVertexNum());
                while (source == sink) {
                    sink = random.nextInt(graph.getVertexNum());
                }
                SFC sfc = idSFCMap.get(sfcKey);
//            Plan plan = deploySFCByDelay(sfc, source, sink);
//            Plan plan = deploySFCByLoadBalance(sfc, source, sink);
//                Plan plan = deploySFCByReliability(sfc, source, sink);
                Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
                sfc.setPlan(plan);
                if (sfc.deploySFC(graph)) {
                    reliability += sfc.getDeployedReliability(graph);
                    resource += sfc.getDeployedResource(graph);
                    success++;
                } else {
                    fail++;
                }
            }
            reliability = reliability / success;  //所有SFC部署后总的平均可靠性

            long end = System.currentTimeMillis();
            float time = (float) (end - start) / 1000;
            successRate = (float) success / idSFCMap.size();

            try {
                File file = new File("./result/DeploymentResult.txt");
                FileWriter fw = new FileWriter(file, true);     //以追加的形式向文件写入数据
                PrintWriter out = new PrintWriter(new BufferedWriter(fw));
                out.println("节点总数：" + graph.getVertexNum() + "    SFC长度为 " + i + " 时， 总共 " + idSFCMap.size() + " 条 SFC	 " + " 成功 " + success +
                        " 条     失败 " + fail + " 条    成功率 " + successRate + "     耗时 " + time + "s    所有SFC部署的可靠性为 "
                        + reliability + "    资源开销 " + resource + "     平均部署功能数 " + graph.avgVNFperVertex());
                out.flush();
                out.close();
            } catch (FileNotFoundException cnfe) {
                System.err.println("找不到文件");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("SFC长度为 " + i + " 时， 总共 " + idSFCMap.size() + " 条 SFC	 " + " 成功 " + success + " 条    失败 " + fail +
                    " 条		成功率 " + successRate + "     耗时 " + time + "s    所有SFC部署的可靠性为 " + reliability + "    资源开销 " + resource
                    + "     平均部署功能数 " + graph.avgVNFperVertex());
            graph.recycleGraphResource();
        }
    }

    public void testFixedSFC(CGraph graph) {
        int success1 = 0;
        int fail1 = 0;
        float successRate1 = 0;

        int success2 = 0;
        int fail2 = 0;
        float successRate2 = 0;

        idSFCMap.clear();
        generateSFCs(2, Configure.SFCNUM);

        float reliability1 = 0;
        float resource1 = 0;
        float reliability2 = 0;
        float resource2 = 0;

        long start = System.currentTimeMillis();
        for (int sfcKey : idSFCMap.keySet()) {
            int source = Configure.random.nextInt(graph.getVertexNum());
            int sink = Configure.random.nextInt(graph.getVertexNum());
            while (source == sink) {
                sink = Configure.random.nextInt(graph.getVertexNum());
            }
            SFC sfc = idSFCMap.get(sfcKey);
            Plan plan = deploySFCByReliability(sfc, source, sink);
            sfc.setPlan(plan);
            if (sfc.deploySFC(graph)) {
                reliability1 += sfc.getDeployedReliability(graph);
                resource1 += sfc.getDeployedResource(graph);
                success1++;
            } else
                fail1++;
        }
        long end = System.currentTimeMillis();
        float time = (float) (end - start) / 1000;   //部署Configure.SFCNUM所需要的时间
        reliability1 = reliability1 / success1;
        successRate1 = (float) success1 / idSFCMap.size();

        graph.recycleGraphResource();   //恢复图中的资源

        long start2 = System.currentTimeMillis();
        for (int sfcKey : idSFCMap.keySet()) {
            int source = Configure.random.nextInt(graph.getVertexNum());
            int sink = Configure.random.nextInt(graph.getVertexNum());
            while (source == sink) {
                sink = Configure.random.nextInt(graph.getVertexNum());
            }
            SFC sfc = idSFCMap.get(sfcKey);
            Plan plan = deploySFCByLoadBalance(sfc, source, sink);
            sfc.setPlan(plan);
            if (sfc.deploySFC(graph)) {
                reliability2 += sfc.getDeployedReliability(graph);
                resource2 += sfc.getDeployedResource(graph);
                success2++;
            } else
                fail2++;
        }
        long end2 = System.currentTimeMillis();
        float time2 = (float) (end2 - start2) / 1000;   //部署Configure.SFCNUM所需要的时间
        reliability2 = reliability2 / success2;
        successRate2 = (float) success2 / idSFCMap.size();

        graph.recycleGraphResource();   //恢复图中的资源

        float reliability3 = 0;
        float resource3 = 0;
        int success3 = 0;
        int fail3 = 0;
        float successRate3 = 0;

        long start3 = System.currentTimeMillis();
        for (int sfcKey : idSFCMap.keySet()) {
            int source = Configure.random.nextInt(graph.getVertexNum());
            int sink = Configure.random.nextInt(graph.getVertexNum());
            while (source == sink) {
                sink = Configure.random.nextInt(graph.getVertexNum());
            }
            SFC sfc = idSFCMap.get(sfcKey);
            Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
            sfc.setPlan(plan);
            if (sfc.deploySFC(graph)) {
                reliability3 += sfc.getDeployedReliability(graph);
                resource3 += sfc.getDeployedResource(graph);
                success3++;
            } else
                fail3++;
        }
        long end3 = System.currentTimeMillis();
        float time3 = (float) (end3 - start3) / 1000;   //部署Configure.SFCNUM所需要的时间
        reliability3 = reliability3 / success3;
        successRate3 = (float) success3 / idSFCMap.size();

        try {
            File file = new File("./result/compareResource.txt");
            FileWriter fw = new FileWriter(file, true);     //以追加的形式向文件写入数据
            PrintWriter out = new PrintWriter(new BufferedWriter(fw));
            out.println("只考虑可靠性情况");
            out.println("节点总数：" + graph.getVertexNum() + "  SFC总数：" + idSFCMap.size() + "   成功：" + success1 + "  失败：" + fail1
                    + "   成功率：" + successRate1 + "   资源开销：" + resource1 + "   平均可靠性：" + reliability1 + "   耗时：" + time + "s");
            out.println();
            out.println("只考虑负载均衡情况");
            out.println("节点总数：" + graph.getVertexNum() + "  SFC总数：" + idSFCMap.size() + "   成功：" + success2 + "  失败：" + fail2
                    + "   成功率：" + successRate2 + "   资源开销：" + resource2 + "   平均可靠性：" + reliability2 + "   耗时：" + time2 + "s");
            out.println();
            out.println("考虑可靠性和负载均衡两种情况");
            out.println("节点总数：" + graph.getVertexNum() + "  SFC总数：" + idSFCMap.size() + "   成功：" + success3 + "  失败：" + fail3
                    + "   成功率：" + successRate3 + "   资源开销：" + resource3 + "   平均可靠性：" + reliability3 + "   耗时：" + time3 + "s");
            out.println();

            out.flush();
            out.close();
        } catch (FileNotFoundException cnfe) {
            System.err.println("找不到文件");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testReliabilityAndLoadBalance(CGraph graph) {
        System.out.println("部署一条SFC的情况：");
        SFC sfc = generateSFC(5);   //生成一个功能数是5的SFC
        int source = Configure.random.nextInt(graph.getVertexNum());
        int sink = Configure.random.nextInt(graph.getVertexNum());
        while (source == sink) {
            sink = Configure.random.nextInt(graph.getVertexNum());
        }
        Plan plan1 = deploySFCByReliability(sfc, source, sink);
        Plan plan2 = deploySFCByLoadBalance(sfc, source, sink);
        Plan plan3 = deploySFCByLoadBalanceAndReliability(sfc, source, sink);

        float reliability1 = 0, reliability2 = 0, reliability3 = 0;
        float resource1 = 0, resource2 = 0, resource3 = 0;
        sfc.setPlan(plan1);
        if (sfc.deploySFC(graph)) {
            reliability1 = sfc.getDeployedReliability(graph);
            resource1 = sfc.getDeployedResource(graph);
        }
        System.out.println("只考虑可靠性：" + reliability1 + "     资源开销：" + resource1);

        sfc.setPlan(plan2);
        if (sfc.deploySFC(graph)) {
//            reliability1 = sfc.getDeployedReliability(graph);
            resource2 = sfc.getDeployedResource(graph);
        }
        System.out.println("只考虑负载均衡：" + reliability2 + "     资源开销：" + resource2);

        sfc.setPlan(plan3);
        if (sfc.deploySFC(graph)) {
            reliability3 = sfc.getDeployedReliability(graph);
            resource3 = sfc.getDeployedResource(graph);
        }
        System.out.println("考虑可靠性与负载均衡：" + reliability3 + "     资源开销：" + resource3);
    }

    public void testAdjustMethod(CGraph graph) {
        int success = 0;
        int fail = 0;
        double blockRate = 0;

        float reliability = 0;
        float resource = 0;

        idSFCMap.clear();
        generateSFCs(5, Configure.SFCNUM);

        long start = System.currentTimeMillis();
        for (int sfcKey : idSFCMap.keySet()) {
            int source = Configure.random.nextInt(graph.getVertexNum());
            int sink = Configure.random.nextInt(graph.getVertexNum());
            while (source == sink) {
                sink = Configure.random.nextInt(graph.getVertexNum());
            }
            SFC sfc = idSFCMap.get(sfcKey);
            Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
            sfc.setPlan(plan);
            if (sfc.deploySFC(graph)) {
                reliability += sfc.getDeployedReliability(graph);
                resource += sfc.getDeployedResource(graph);
                success++;
            } else
                fail++;
        }
        long end = System.currentTimeMillis();
        double time = (double) (end - start) / 1000;
        blockRate = (double) fail / idSFCMap.size();
        reliability = reliability / success;
        System.out.println("考虑可靠性与负载情况，不做后续处理：");
        System.out.println("节点总数：" + graph.getVertexNum() + "  SFC总数：" + idSFCMap.size() + "   成功：" + success + "  失败：" + fail
                + "   阻塞率：" + blockRate + "   资源开销：" + resource + "   平均可靠性：" + reliability + "   耗时：" + time + "s");
        graph.recycleGraphResource();

        reliability = 0;
        resource = 0;
        success = 0;
        fail = 0;

        start = System.currentTimeMillis();
        for (int sfcKey : idSFCMap.keySet()) {
            int source = Configure.random.nextInt(graph.getVertexNum());
            int sink = Configure.random.nextInt(graph.getVertexNum());
            while (source == sink) {
                sink = Configure.random.nextInt(graph.getVertexNum());
            }
            SFC sfc = idSFCMap.get(sfcKey);
            Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
            if (plan == null) {
                fail++;
                continue;
            }
            plan = plan.adjustPlan(graph, sfc); //调整带宽
            sfc.setPlan(plan);
            if (sfc.deploySFC(graph)) {
                reliability += sfc.getDeployedReliability(graph);
                resource += sfc.getDeployedResource(graph);
                success++;
            } else
                fail++;
        }
        end = System.currentTimeMillis();
        time = (double) (end - start) / 1000;
        blockRate = (double) fail / idSFCMap.size();
        reliability = reliability / success;
        System.out.println("考虑可靠性与负载情况，后面调整带宽：");
        System.out.println("节点总数：" + graph.getVertexNum() + "  SFC总数：" + idSFCMap.size() + "   成功：" + success + "  失败：" + fail
                + "   阻塞率：" + blockRate + "   资源开销：" + resource + "   平均可靠性：" + reliability + "   耗时：" + time + "s");
        graph.recycleGraphResource();
    }

    public void testPath(CGraph graph) {
        SFC sfc = generateSFC(5);
        Plan plan1 = deploySFCByReliability(sfc, 10, 400);
        sfc.setPlan(plan1);
        System.out.println("-----------------------");
        System.out.println("资源开销：" + sfc.getDeployedResource(graph));
        plan1.printPlan(graph);
        graph.recycleGraphResource();

        Plan plan2 = deploySFCByLoadBalance(sfc, 10, 400);
        sfc.setPlan(plan2);
        System.out.println("-----------------------");
        System.out.println("资源开销：" + sfc.getDeployedResource(graph));
        plan2.printPlan(graph);
        graph.recycleGraphResource();

        Plan plan3 = deploySFCByLoadBalanceAndReliability(sfc, 10, 400).adjustPlan(graph, sfc);
        sfc.setPlan(plan3);
        System.out.println("-----------------------");
        System.out.println("资源开销：" + sfc.getDeployedResource(graph));
        plan3.printPlan(graph);
    }

//    public void testDeploySFCByLMFAlg() {
//        int success = 0;
//        int fail = 0;
//        float blockRate = 0;
//        float reliability = 0;
//        float resource = 0;
//        long startTime = 0;
//        long endTime = 0;
//        int firstBlock = -1;
//        double time = 0;
//
//        File file = null;
//        FileWriter fw = null;
//        PrintWriter out = null;
//        try {
//            file = new File("./result/3_12length100_670UNINF/LMF100_670_8_10000.txt"); //20个顶点83条边的拓扑，15条SFC的在线率，10000条SFC
//            fw = new FileWriter(file, true);     //以追加的形式向文件写入数据
//            out = new PrintWriter(new BufferedWriter(fw));
//            out.println("SFC长度      时间（ms）      阻塞率      第一次阻塞      资源      可靠性");
//            out.flush();
//        }catch (FileNotFoundException e) {
//            System.err.println("文件不存在！");
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        //生成不同长度的SFC
//        for(int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
//            graph.recycleGraphResource();
//            idSFCMap.clear();
//            success = 0;
//            fail = 0;
//            reliability = 0;
//            resource = 0;
//            time = 0;
//            generateSFCs(i, Configure.SFCNUM);
//
//            //遍历所有随机生成的SFC
//            for(int sfcKey : idSFCMap.keySet()) {
//                startTime = System.currentTimeMillis();
//                int source = Configure.random.nextInt(graph.getVertexNum());
//                int sink = Configure.random.nextInt(graph.getVertexNum());
//                while (source == sink) {
//                    sink = Configure.random.nextInt(graph.getVertexNum());
//                }
//                SFC sfc = idSFCMap.get(sfcKey); //当前需要部署的SFC
//                for(int workSFCKey : workSfcList) { //遍历工作SFC集合，定期回收已经离开的SFC资源
//                    SFC workSfc = idSFCMap.get(workSFCKey);
//                    if(workSfc.getContinueTime() <= sfc.getSfcId()) {
//                        //该工作SFC到期，回收资源
//                        if(workSfc.checkTheNodes() && workSfc.checkTheLinks()) {//判断是否找到了不部署和部署路径
//                            workSfc.recycleResourceoffEnergy(graph);
//                        }
//                        recycleList.add(workSFCKey);
//                    }
//                }
//                for(int recycleId : recycleList) {
//                    workSfcList.remove(workSfcList.indexOf(recycleId));
//                }
//                recycleList.clear();
//                Plan plan = deploySFCByLMFalg(sfc, source, sink);
//                sfc.setPlan(plan);
//                if(sfc.deploySFC(graph)) {
//                    workSfcList.add(sfc.getSfcId());    //部署成功，加入工作队列
//                    reliability += sfc.getDeployedReliability(graph);
//                    resource += sfc.getDeployedResource(graph);
//                    success++;
//                    endTime = System.currentTimeMillis();
//                    time += (endTime - startTime);
//                }
//                else {
//                    fail++;
//                    if(fail == 1) {
//                        firstBlock = sfc.getSfcId();    //第一次阻塞发生在哪一个SFC部署时
//                    }
//                }
//            }
//            //如果仍然有工作SFC还没有回收资源，则回收所有的资源
//            for(int workKey : workSfcList) {
//                SFC workSFC = idSFCMap.get(workKey);
//                if(workSFC.checkTheLinks() && workSFC.checkTheNodes()) {
//                    workSFC.recycleResourceoffEnergy(graph);
//                }
//            }
//            workSfcList.clear();
////            endTime = System.currentTimeMillis();
//            blockRate = (float) fail/(float) idSFCMap.size();
//            reliability = reliability/success;
//            time = (double)(time/(double)success);
//            resource = resource/success;
////            out.println(i + "\t\t" + (endTime-startTime) + "ms\t\t" + blockRate + "\t\t"+ firstBlock + "\t\t" + resource + "\t\t" + reliability);
//            out.printf("%d   %6f  %10.4f   %5d   %10.4f   %10.9f", i, time, blockRate, firstBlock, resource, reliability);
//            out.println();
//            out.flush();
//        }
//
//        if(out != null) {
//            out.close();
//        }
//        graph.recycleGraphResource();
//    }

//    public void testReliability() {
//        int success = 0;
//        int fail = 0;
//        float blockRate = 0;
//        float reliability = 0;
//        float resource = 0;
//        long startTime = 0;
//        long endTime = 0;
//        double time = 0;
//        int firstBlock = -1;
//
//        File file = null;
//        FileWriter fw = null;
//        PrintWriter out = null;
//        try {
//            file = new File("./result/3_12length100_670UNINF/RB100_670_8_10000.txt");
//            fw = new FileWriter(file, true);
//            out = new PrintWriter(new BufferedWriter(fw));
//            out.println("SFC长度    时间(ms)    阻塞率    第一次阻塞    资源    可靠性");
//
//            out.flush();
//
//        }catch (FileNotFoundException e) {
//            System.out.println("文件不存在！");
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        for(int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
//            graph.recycleGraphResource();
//            idSFCMap.clear();
//            success = 0;
//            fail = 0;
//            reliability = 0;
//            resource = 0;
//            time = 0;
//
//            generateSFCs(i, Configure.SFCNUM);
//            for (int sfcKey : idSFCMap.keySet()) {
//                startTime = System.currentTimeMillis();
//                SFC sfc = idSFCMap.get(sfcKey);
//                int source = Configure.random.nextInt(graph.getVertexNum());
//                int sink = Configure.random.nextInt(graph.getVertexNum());
//                while (source == sink) {
//                    sink = Configure.random.nextInt(graph.getVertexNum());
//                }
//
//                for(int workSFC : workSfcList) {
//                    SFC workSfc = idSFCMap.get(workSFC);
//                    if(workSfc.getContinueTime() < sfc.getSfcId()) {
//                        if (workSfc.checkTheNodes() && workSfc.checkTheNodes()) {
//                            workSfc.recycleResourceoffEnergy(graph);
//                        }
//                        recycleList.add(workSFC);
//                    }
//                }
//                for(int recycleId : recycleList) {
//                    workSfcList.remove(workSfcList.indexOf(recycleId));
//                }
//                recycleList.clear();
//                Plan plan = deploySFCByReliability(sfc, source, sink);
//                sfc.setPlan(plan);
//                if(sfc.deploySFC(graph)) {
//                    success++;
//                    reliability += sfc.getDeployedReliability(graph);
//                    resource += sfc.getDeployedResource(graph);
//                    workSfcList.add(sfc.getSfcId());
//                    endTime = System.currentTimeMillis();
//                    time += (endTime - startTime);
//                }
//                else {
//                    fail++;
//                    if(fail == 1) {
//                        firstBlock = sfc.getSfcId();
//                    }
//                }
//            }
//            for(int work : workSfcList) {
//                SFC workSfc = idSFCMap.get(work);
//                if(workSfc.checkTheNodes() && workSfc.checkTheLinks()) {
//                    workSfc.recycleResourceoffEnergy(graph);
//                }
//            }
//            workSfcList.clear();
////            endTime = System.currentTimeMillis();
//            blockRate = (float) fail/(float) idSFCMap.size();
//            reliability = reliability / success;
//            time = (double) (time/(double) success);
//            resource = resource/success;
//            out.printf("%d   %6f  %10.4f   %5d   %10.4f   %10.9f", i, time, blockRate, firstBlock, resource, reliability);
//            out.println();
//        }
//        if(out != null) {
//            out.close();
//        }
//        graph.recycleGraphResource();
//    }
//
//    public void testDeploySFCByLoadBalanceAndReliability() {
//        int success = 0;
//        int fail = 0;
//        float blockRate = 0;
//        int firstBlock = 0;
//        float reliability = 0;
//        float resource = 0;
//        long startTime = 0;
//        long endTime = 0;
//        double time = 0;
//
//        File file = null;
//        FileWriter fw = null;
//        PrintWriter out = null;
//        try {
//            file = new File("./result/3_12length100_670UNINF/LB_RB100_670_8_10000.txt");
//            fw = new FileWriter(file, true);
//            out = new PrintWriter(new BufferedWriter(fw));
//            out.println("SFC长度    时间(ms)    阻塞率    第一次阻塞    资源    可靠性");
//            out.flush();
//
//        }catch (FileNotFoundException e) {
//            System.err.println("文件不存在");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        for(int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
//            graph.recycleGraphResource();
//            idSFCMap.clear();
//            success = 0;
//            fail = 0;
//            reliability = 0;
//            resource = 0;
//            time = 0;
//
//            generateSFCs(i, Configure.SFCNUM);
//            for(int sfcKey : idSFCMap.keySet()) {
//                startTime = System.currentTimeMillis();
//                SFC sfc = idSFCMap.get(sfcKey);
//                int source = Configure.random.nextInt(graph.getVertexNum());
//                int sink = Configure.random.nextInt(graph.getVertexNum());
//                while (source == sink) {
//                    sink = Configure.random.nextInt(graph.getVertexNum());
//                }
//
//                for(int workKey : workSfcList) {
//                    SFC workSFC = idSFCMap.get(workKey);
//                    if(workSFC.getContinueTime() <= sfc.getSfcId()) {
//                        //到期
//                        if(workSFC.checkTheNodes() && workSFC.checkTheLinks()) {
//                            workSFC.recycleResourceoffEnergy(graph);
//                        }
//                        recycleList.add(workKey);
//                    }
//                }
//
//                for(int recycleIt : recycleList) {
//                    workSfcList.remove(workSfcList.indexOf(recycleIt));
//                }
//                recycleList.clear();
//                Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
//                sfc.setPlan(plan);
//                if(sfc.deploySFC(graph)) {
//                    success++;
//                    workSfcList.add(sfc.getSfcId());
//                    reliability += sfc.getDeployedReliability(graph);
//                    resource += sfc.getDeployedResource(graph);
//                    endTime = System.currentTimeMillis();
//                    time += (endTime - startTime);
//                }
//                else {
//                    fail++;
//                    if(fail == 1) {
//                        firstBlock = sfc.getSfcId();
//                    }
//                }
//
//            }
//            for(int workKey : workSfcList) {
//                SFC workSfc = idSFCMap.get(workKey);
//                if(workSfc.checkTheNodes() && workSfc.checkTheLinks()) {
//                    workSfc.recycleResourceoffEnergy(graph);
//                }
//            }
//            workSfcList.clear();
////            endTime = System.currentTimeMillis();
//            blockRate = (float) fail/(float)idSFCMap.size();
//            reliability = reliability/success;
//            time = (double) (time/(double) success);
//            resource = resource/success;
//            out.printf("%d   %6f  %10.4f   %5d   %10.4f   %10.9f", i, time, blockRate, firstBlock, resource, reliability);
////            out.println(i + "\t\t" + (endTime-startTime) + "ms\t\t" + blockRate + "\t\t"+ firstBlock + "\t\t" + resource + "\t\t" + reliability);
//            out.println();
//            out.flush();
//        }
//        if(out != null) {
//            out.close();
//        }
//        graph.recycleGraphResource();
//
//    }
//
//    public void testDeploySFCByLoadBalance() {
//        int success = 0;
//        int fail = 0;
//        float blockRate = 0;
//        int firstBlock = 0;
//        float reliability = 0;
//        float resource = 0;
//        long startTime = 0;
//        long endTime = 0;
//        double time = 0;
//
//        File file = null;
//        FileWriter fw = null;
//        PrintWriter out = null;
//        try {
//            file = new File("./result/3_12length100_670UNINF/LB100_670_8_10000.txt");
//            fw = new FileWriter(file, true);
//            out = new PrintWriter(new BufferedWriter(fw));
//            out.println("SFC长度    时间(ms)    阻塞率    第一次阻塞    资源    可靠性");
//            out.flush();
//
//        }catch (FileNotFoundException e) {
//            System.err.println("文件不存在");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        for(int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
//            graph.recycleGraphResource();
//            idSFCMap.clear();
//            success = 0;
//            fail = 0;
//            reliability = 0;
//            resource = 0;
//
//            generateSFCs(i, Configure.SFCNUM);
//            for(int sfcKey : idSFCMap.keySet()) {
//                startTime = System.currentTimeMillis();
//                SFC sfc = idSFCMap.get(sfcKey);
//                int source = Configure.random.nextInt(graph.getVertexNum());
//                int sink = Configure.random.nextInt(graph.getVertexNum());
//                while (source == sink) {
//                    sink = Configure.random.nextInt(graph.getVertexNum());
//                }
//
//                for(int workKey : workSfcList) {
//                    SFC workSFC = idSFCMap.get(workKey);
//                    if(workSFC.getContinueTime() <= sfc.getSfcId()) {
//                        //到期
//                        if(workSFC.checkTheNodes() && workSFC.checkTheLinks()) {
//                            workSFC.recycleResourceoffEnergy(graph);
//                        }
//                        recycleList.add(workKey);
//                    }
//                }
//
//                for(int recycleIt : recycleList) {
//                    workSfcList.remove(workSfcList.indexOf(recycleIt));
//                }
//                recycleList.clear();
//                Plan plan = deploySFCByLoadBalance(sfc, source, sink);
//                sfc.setPlan(plan);
//                if(sfc.deploySFC(graph)) {
//                    success++;
//                    workSfcList.add(sfc.getSfcId());
//                    reliability += sfc.getDeployedReliability(graph);
//                    resource += sfc.getDeployedResource(graph);
//                    endTime = System.currentTimeMillis();
//                    time += (endTime - startTime);
//                }
//                else {
//                    fail++;
//                    if(fail == 1) {
//                        firstBlock = sfc.getSfcId();
//                    }
//                }
//
//            }
//            for(int workKey : workSfcList) {
//                SFC workSfc = idSFCMap.get(workKey);
//                if(workSfc.checkTheNodes() && workSfc.checkTheLinks()) {
//                    workSfc.recycleResourceoffEnergy(graph);
//                }
//            }
//            workSfcList.clear();
//            blockRate = (float) fail/(float)idSFCMap.size();
//            reliability = reliability/success;
//            time = time/(double)success;
//            resource = resource/success;
//            out.printf("%d   %6f  %10.4f   %5d   %10.4f   %10.9f", i, time, blockRate, firstBlock, resource, reliability);
////            out.println(i + "\t\t" + (endTime-startTime) + "ms\t\t" + blockRate + "\t\t"+ firstBlock + "\t\t" + resource + "\t\t" + reliability);
//            out.println();
//            out.flush();
//        }
//        if(out != null) {
//            out.close();
//        }
//        graph.recycleGraphResource();
//
//    }
//
//    //根据可靠性和负载均衡部署后再调整
//    public void testDeploySFCByLoadBalanceAndReliabilityByAdjust() {
//        int success = 0;
//        int fail = 0;
//        float blockRate = 0;
//        int firstBlock = 0;
//        float reliability = 0;
//        float resource = 0;
//        long startTime = 0;
//        long endTime = 0;
//        double time = 0;
//
//        File file = null;
//        FileWriter fw = null;
//        PrintWriter out = null;
//        try {
//            file = new File("./result/3_12length100_670UNINF/LB_RB_Adjust100_670_8_10000.txt");
//            fw = new FileWriter(file, true);
//            out = new PrintWriter(new BufferedWriter(fw));
//            out.println("SFC长度    时间(ms)    阻塞率    第一次阻塞    资源    可靠性");
//            out.flush();
//
//        }catch (FileNotFoundException e) {
//            System.err.println("文件不存在");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        for(int i = Configure.SFC_LENGTH_MIN; i <= Configure.SFC_LENGTH_MAX; i++) {
//            graph.recycleGraphResource();
//            idSFCMap.clear();
//            success = 0;
//            fail = 0;
//            reliability = 0;
//            resource = 0;
//            time = 0;
//
//            generateSFCs(i, Configure.SFCNUM);
//            for(int sfcKey : idSFCMap.keySet()) {
//                startTime = System.currentTimeMillis();
//                SFC sfc = idSFCMap.get(sfcKey);
//                int source = Configure.random.nextInt(graph.getVertexNum());
//                int sink = Configure.random.nextInt(graph.getVertexNum());
//                while (source == sink) {
//                    sink = Configure.random.nextInt(graph.getVertexNum());
//                }
//
//                for(int workKey : workSfcList) {
//                    SFC workSFC = idSFCMap.get(workKey);
//                    if(workSFC.getContinueTime() <= sfc.getSfcId()) {
//                        //到期
//                        if(workSFC.checkTheNodes() && workSFC.checkTheLinks()) {
//                            workSFC.recycleResourceoffEnergy(graph);
//                        }
//                        recycleList.add(workKey);
//                    }
//                }
//
//                for(int recycleIt : recycleList) {
//                    workSfcList.remove(workSfcList.indexOf(recycleIt));
//                }
//                recycleList.clear();
//                Plan plan = deploySFCByLoadBalanceAndReliability(sfc, source, sink);
//                if(plan == null) {
//                    fail++;
//                    if(fail == 1) {
//                        firstBlock = sfc.getSfcId();
//                    }
//                    continue;
//                }
//                sfc.setPlan(plan.adjustPlan(graph, sfc));
//                if(sfc.deploySFC(graph)) {
//                    success++;
//                    workSfcList.add(sfc.getSfcId());
//                    reliability += sfc.getDeployedReliability(graph);
//                    resource += sfc.getDeployedResource(graph);
//                    endTime = System.currentTimeMillis();
//                    time += (endTime - startTime);
//                }
//                else {
//                    fail++;
//                    if(fail == 1) {
//                        firstBlock = sfc.getSfcId();
//                    }
//                }
//
//            }
//            for(int workKey : workSfcList) {
//                SFC workSfc = idSFCMap.get(workKey);
//                if(workSfc.checkTheNodes() && workSfc.checkTheLinks()) {
//                    workSfc.recycleResourceoffEnergy(graph);
//                }
//            }
//            workSfcList.clear();
////            endTime = System.currentTimeMillis();
//            blockRate = (float) fail/(float)idSFCMap.size();
//            reliability = reliability/success;
//            time = (double) (time/(double) success);
//            resource = resource/success;
//            out.printf("%d   %6f  %10.4f   %5d   %10.4f   %10.9f", i, time, blockRate, firstBlock, resource, reliability);
////            out.println(i + "\t\t" + (endTime-startTime) + "ms\t\t" + blockRate + "\t\t"+ firstBlock + "\t\t" + resource + "\t\t" + reliability);
//            out.println();
//            out.flush();
//        }
//        if(out != null) {
//            out.close();
//        }
//        graph.recycleGraphResource();
//    }

    /**
     * 根据用户所需的可靠性部署SFC
     *
     * @param sfc
     * @param source
     * @param sink
     * @return
     */
    public Plan deploySFCByReliability(SFC sfc, int source, int sink) {
        Plan plan = new Plan();
        ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点
        vertexList.addAll(graph.vertexList);
        int currentVertexId = source;
        vertexList.remove(vertexList.indexOf(source));
        /*if (sink == sfc.getSinkId()) {
            vertexList.remove(vertexList.indexOf(sink));
        }*/
        vertexList.remove(vertexList.indexOf(sink));

        for (int vnfKey : sfc.nodeList) {
            if (vnfKey != sfc.nodeList.getLast()) {
                CPath path = getBestDeployOfMiddleVNFByReliability(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<>();
                        list.addAll(path.edgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    vertexList.removeAll(path.vertexList);    //移除已经使用的点
                    currentVertexId = path.getVnfDeployPosition();
                }
            } else {
                CPath path = getBestDeployOfLastVNFByReliability(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    //处理vnfKey前的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.firstEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    //处理vnfKey后的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
                        plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.secondEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey, list);
                    }
                }
            }
        }
        sfc.setPlan(plan);
        float reliability = sfc.getDeployedReliability(graph);
        if (reliability >= Configure.REQUEST_RELIABILITY) {
            return plan;
        } else
            return null;
    }

    /**
     * 根据可靠性部署SFC的中间功能节点
     *
     * @param vertexList
     * @param sfc
     * @param vnf
     * @param source
     * @param sink
     * @return
     */
    public CPath getBestDeployOfMiddleVNFByReliability(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tmpList = new ArrayList<>();
        tmpList.addAll(vertexList);
        initReliability(tmpList);
        CVertex vertex1 = graph.idVertexMap.get(source);
        vertex1.setReliabilityToSource(vertex1.getReliability()); //设当前节点source到源节点的可靠性为该节点本身
        graph.idVertexMap.get(source).setRb_previousVertexIdToSource(Configure.TERMINALNODE);    //设前一节点为不可达

        //SFC功能节点编号从1开始，链路编号从0开始
        Node node = sfc.nodeMap.get(vnf);   //需要部署的功能节点
        Link link = sfc.linkMap.get(vnf - 1);

        //第一步
        updateAllReliability(tmpList, source, link);

        //第2步，从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay延时最小`
        float maxReliability = Configure.UNINF;
        int maxReliabilityVertexID = Configure.IMPOSSIBLENODE;
        for (int key : vertexList) {
            CVertex vertex = graph.idVertexMap.get(key);
            if (vertex.getVeretexId() != sink && vertex.getRemainComputeResource() >= node.getComputeResourceDemand()
                    && maxReliability < vertex.getReliabilityToSource()) {
                maxReliability = vertex.getReliabilityToSource();
                maxReliabilityVertexID = vertex.getVeretexId();
            }
        }

        //第3步，如果不存在满足：`满足node的资源需求，且负载最均衡` 的节点，说明不能部署这一节点，返回null
        if (maxReliability == Configure.UNINF) {
//			System.out.println("在搜寻满足node需求的过程过程中，找不到时延最小的节点，用return null 退出");
            return null;
        }

        //第4步，找出满足：`满足node的资源需求，且负载最均衡` 的节点，构建这一条路径，返回路径
        LinkedList<Integer> pathList = new LinkedList<>();
        CVertex vertex = graph.idVertexMap.get(maxReliabilityVertexID);
        while (vertex.getRb_previousVertexIdToSource() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            int tmp = vertex.getRb_previousVertexIdToSource();
            vertex = graph.idVertexMap.get(vertex.getRb_previousVertexIdToSource());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径

        CPath path = new CPath(pathList, graph, sfc);
        path.setVnfDeployPosition(maxReliabilityVertexID);
        return path;
    }

    /**
     * 根据可靠性部署SFC的最后一个功能节点
     *
     * @param vertexList
     * @param sfc
     * @param vnf
     * @param source
     * @param sink
     * @return
     */
    public CPath getBestDeployOfLastVNFByReliability(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.addAll(vertexList);
        initReliability(tempList);

        CVertex vertex1 = graph.idVertexMap.get(source);
        vertex1.setReliabilityToSource(vertex1.getReliability()); //设当前节点source到源节点的可靠性为该节点本身
        vertex1.setRb_previousVertexIdToSource(Configure.TERMINALNODE);    //设前一节点为不可达

        if (tempList.contains(source)) {
            tempList.remove(tempList.indexOf(source));
        }

        Node node = sfc.nodeMap.get(vnf);
        Link firstLink = sfc.linkMap.get(vnf - 1);  //该功能前的虚拟链路
        Link secondLink = sfc.linkMap.get(vnf);     //该功能后的虚拟链路

        //先处理 某点到source点的可靠性最大的可部署路径,第一步
        updateAllReliability(tempList, source, firstLink);

        //再处理 某点到sink最大可靠性可部署路径
        tempList.addAll(vertexList);
        CVertex vertex2 = graph.idVertexMap.get(sink);
        vertex2.setReliabilityToSink(vertex2.getReliability());
        vertex2.setRb_previousVertexIdToSink(Configure.TERMINALNODE);
        if (tempList.contains(sink)) {
            tempList.remove(tempList.indexOf(sink));
        }
        updateAllReliabilityToSink(tempList, sink, secondLink);


        float maxReliability;
        int maxReliabilityVertexId;

        //第2步：从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小的点`
        maxReliability = Configure.UNINF;
        maxReliabilityVertexId = Configure.IMPOSSIBLENODE;

        for (Integer vertexkey : vertexList) {
            CVertex vertex = graph.idVertexMap.get(vertexkey);
            //因为在toSource和toSink这两个里面都乘了一遍当前点的可靠性，所以还要除以一遍当前点可靠性
            if (vertex.getRemainComputeResource() >= node.getComputeResourceDemand() &&
                    maxReliability < vertex.getReliabilityToSink() * vertex.getReliabilityToSource() / vertex.getReliability()) {
                maxReliability = vertex.getReliabilityToSink() * vertex.getReliabilityToSource() / vertex.getReliability();
                maxReliabilityVertexId = vertexkey;
            }
        }

        //第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，说明不能部署这一节点，返回null
        if (maxReliability == Configure.UNINF || maxReliabilityVertexId == Configure.IMPOSSIBLENODE) {
            return null;
        }

        //第4步：存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，能够部署，返回部署路径
        LinkedList<Integer> pathList = new LinkedList<Integer>();
        CVertex vertex = graph.idVertexMap.get(maxReliabilityVertexId);
        while (vertex.getRb_previousVertexIdToSource() != Configure.TERMINALNODE) {
            //如果当前节点没有前一节点，直接返回空
            if(vertex.getRb_previousVertexIdToSource() == Configure.IMPOSSIBLENODE) {
                return null;
            }
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getRb_previousVertexIdToSource());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径

        vertex = graph.idVertexMap.get(maxReliabilityVertexId);
        while (vertex.getRb_previousVertexIdToSink() != Configure.TERMINALNODE) {
            //如果当前节点没有前一节点，直接返回空
            if(vertex.getRb_previousVertexIdToSink() == Configure.IMPOSSIBLENODE) {
                return null;
            }
            if (pathList.getLast() != vertex.getVeretexId()) {
                pathList.addLast(vertex.getVeretexId());
            }
            vertex = graph.idVertexMap.get(vertex.getRb_previousVertexIdToSink());
        }
        if (pathList.getLast() != vertex.getVeretexId()) {
            pathList.addLast(vertex.getVeretexId());
        }
        CPath path = new CPath(pathList, graph, maxReliabilityVertexId, sfc);
        path.setVnfDeployPosition(maxReliabilityVertexId);
        return path;
    }

    public void initReliability(ArrayList<Integer> list) {
        for (Integer vertexKey : list) {
            CVertex vertex = graph.idVertexMap.get(vertexKey);
            vertex.setRb_previousVertexIdToSink(Configure.IMPOSSIBLENODE);
            vertex.setRb_previousVertexIdToSource(Configure.IMPOSSIBLENODE);
            vertex.setReliabilityToSource(Configure.UNINF);
            vertex.setReliabilityToSink(Configure.UNINF);
        }
    }

    public void updateAllReliability(ArrayList<Integer> vertexList, int source, Link link) {
        int it = source;
        float maxReliability;
        int maxReliabilityVertexID;

        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                //这里加入了带宽资源不满足时的判断
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());

                    //链路1-2，节点1到源的可靠性 * 边的可靠性 * 节点2的可靠性 > 节点2到源的可靠性，，，那么选择可靠性较大的更新
                    if (vertex.getReliabilityToSource() * edge.getReliability() * sinkVertex.getReliability() > sinkVertex.getReliabilityToSource()
                            && vertex.getReliabilityToSource() * edge.getReliability() * sinkVertex.getReliability() >= Configure.REQUEST_RELIABILITY) {   //在部署的过程中保证可靠性
                        sinkVertex.setReliabilityToSource(vertex.getReliabilityToSource() * edge.getReliability() * sinkVertex.getReliability());
                        sinkVertex.setRb_previousVertexIdToSource(it);
                    }
                }
            }

            //从剩余的节点中，选出可靠性最大的点
            maxReliability = Configure.UNINF;
            maxReliabilityVertexID = Configure.IMPOSSIBLENODE;
            for (int vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (maxReliability < tempVertex.getReliabilityToSource()) {
                    maxReliability = tempVertex.getReliabilityToSource();
                    maxReliabilityVertexID = vertexKey;
                }
            }
            if (maxReliability == Configure.UNINF) {
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
//				System.out.println("找完所有节点，有几个节点都是没有更新数据，用break退出");
                break;
            }
            it = maxReliabilityVertexID;
            vertexList.remove(vertexList.indexOf(maxReliabilityVertexID));
        }
    }

    public void updateAllReliabilityToSink(ArrayList<Integer> vertexList, int sink, Link link) {
        int it = sink;
        float maxReliability;
        int maxReliabilityVertexID;
        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
                    if (vertex.getReliabilityToSink() * edge.getReliability() * sinkVertex.getReliability() > sinkVertex.getReliabilityToSink()
                            && vertex.getReliabilityToSink() * edge.getReliability() * sinkVertex.getReliability() >= Configure.REQUEST_RELIABILITY) {   //在部署的过程中保证可靠性
                        sinkVertex.setReliabilityToSink(vertex.getReliabilityToSink() * edge.getReliability() * sinkVertex.getReliability());
                        sinkVertex.setRb_previousVertexIdToSink(it);
                    }
                }
            }

            //从剩余的节点中，选出可靠性最大的点
            maxReliability = Configure.UNINF;
            maxReliabilityVertexID = Configure.IMPOSSIBLENODE;

            for (Integer vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (maxReliability < tempVertex.getReliabilityToSink()) {
                    maxReliability = tempVertex.getReliabilityToSink();
                    maxReliabilityVertexID = vertexKey;
                }
            }
            if (maxReliability <= Configure.UNINF) {
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
                break;
            }
            it = maxReliabilityVertexID;
            vertexList.remove(vertexList.indexOf(maxReliabilityVertexID));
        }
    }

    
   
    
    
    
    /**
     * 以时延为标准找出一条SFC的部署方案
     *
     * @param sfc
     * @param source
     * @param sink
     * @return
     */
    public Plan deploySFCByDelay(SFC sfc, int source, int sink) {
//        System.out.println("进入deploySFCByDelay方法");
        Plan plan = new Plan();
        ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点
        vertexList.addAll(graph.vertexList);
        int currentVertexId = source;

        /*if(source == sfc.getSourceId()) {
            vertexList.remove(vertexList.indexOf(source));
        }*/
        vertexList.remove(vertexList.indexOf(source));
        if (sink == sfc.getSinkId()) {//即物理节点的目的点与sfc的目的点要相同
            vertexList.remove(vertexList.indexOf(sink));
        }

//        System.out.println("开始执行sfc功能节点的部署");
        for (int vnfKey : sfc.nodeList) {
//            System.out.println("-------------- 开始部署功能：" + vnfKey + "--------------");
            if (vnfKey != sfc.nodeList.getLast()) {
                CPath path = getBestDeployOfMiddleVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    //返回的path为null，表示对于功能vnfKey或者vnfKey前的虚拟链路没有部署方案，故部署不成功
//					System.out.println("部署最后中间功能时走不通");
                    return null;
                } else {
//					System.out.println("path ！= null");
//					为vnfKey找到部署方案，将返回的部署路径，加入到plan中，并从vertexList中去掉已经使用的底层节点
//					System.out.println(" 功能 " + vnfKey + " 部署在点 " + path.getVnfDeployPosition());
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<>();
                        list.addAll(path.edgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    vertexList.removeAll(path.vertexList);    //移除已经使用的点
                    currentVertexId = path.getVnfDeployPosition();
                }
            } else {
                CPath path = getBestDeployOfLastVNF(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
					System.out.println("部署最后一个功能时走不通");
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    //处理vnfKey前的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.firstEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    //处理vnfKey后的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
                        plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.secondEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey, list);
                    }
                }
            }

        }
//        System.out.println("deploySFCByDelay方法执行完成");
        return plan;
    }

    /**
     * 功能：从剩余节点中，找到一点，使得该点满足：
     * ①该点能够部署vnf（计算资源足够）
     * ②该点到源点的路径时延 + 该点到终点的路径时延 之和最小
     *
     * @param vertexList 图中剩余节点集
     * @param sfc        需要部署的SFC
     * @param vnf        某个SFC中需要部署的功能点（这里为一条SFC的最后一个功能点）
     * @param source     源点
     * @param sink
     * @return
     */
    public CPath getBestDeployOfLastVNF(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.addAll(vertexList);
        initTheVertexInList(tempList);

//      System.out.println("源点  == " + source + "   宿点 == " + sink + "  功能 == " + vnf);
		System.out.println("进入最后一个功能的部署模块, 得到的剩余的节点组合为：");
		for (Integer vertexKey : vertexList) {
			System.out.print(vertexKey + "   ");
		}
		System.out.println();

        graph.idVertexMap.get(source).setTotalDelay(0);
        graph.idVertexMap.get(source).setPreviousVertexId(Configure.TERMINALNODE);

        if (tempList.contains(source)) {
            tempList.remove(tempList.indexOf(source));
        }

        Node node = sfc.nodeMap.get(vnf);
        Link firstLink = sfc.linkMap.get(vnf - 1);  //该功能前的虚拟链路
        Link secondLink = sfc.linkMap.get(vnf);     //该功能后的虚拟链路

        //先处理 某点到source点的最短时延可部署路径,第一步
        updateAllDelay(tempList, source, firstLink);

        //再处理 某点到sink最短时延可部署路径
        tempList.addAll(vertexList);
        graph.idVertexMap.get(sink).setTotalDelayToSink(0);
        graph.idVertexMap.get(sink).setPreviousVertexIDToSink(Configure.TERMINALNODE);
        if (tempList.contains(sink)) {
            tempList.remove(tempList.indexOf(sink));
        }
        int it = sink;
        updateAllDelayToSink(tempList, it, secondLink);
        float minTotalDelay;
        int minDelayVertexID;

        //第2步：从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小的点`
        minTotalDelay = Configure.INF;
        minDelayVertexID = Configure.IMPOSSIBLENODE;

        for (Integer vertexkey : vertexList) {
            CVertex vertex = graph.idVertexMap.get(vertexkey);
//			System.out.println("---------------------------------测试数据开始--------------------------------------");
//			System.out.println("Node " + node.getNodeId() + " 资源需求是 " + node.getComputeResourceDemand());
//			System.out.println("Vertex " + vertex.getVeretexId() + "到源延时 " + vertex.getTotalDelay());
//			System.out.println("Vertex " + vertex.getVeretexId() + "到目的延时 " + vertex.getTotalDelayToSink());
//			System.out.println("----------------------------------测试数据结束-------------------------------------");
            if (vertex.getRemainComputeResource() >= node.getComputeResourceDemand() && minTotalDelay > vertex.getTotalDelay() + vertex.getTotalDelayToSink()) {
                minTotalDelay = vertex.getTotalDelay() + vertex.getTotalDelayToSink();
                minDelayVertexID = vertexkey;
            }
        }

        //第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，说明不能部署这一节点，返回null
        if (minTotalDelay >= Configure.INF) {
//			System.out.println("最后一个功能在这里返回null==============");
//			System.out.println("minTotalDelay = " + minTotalDelay);
            return null;
        }

        //第4步：存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，能够部署，返回部署路径
        LinkedList<Integer> pathList = new LinkedList<Integer>();
        CVertex vertex = graph.idVertexMap.get(minDelayVertexID);
        while (vertex.getPreviousVertexId() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getPreviousVertexId());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径

        vertex = graph.idVertexMap.get(minDelayVertexID);
        while (vertex.getPreviousVertexIDToSink() != Configure.TERMINALNODE) {
            if (pathList.getLast() != vertex.getVeretexId()) {
                pathList.addLast(vertex.getVeretexId());
            }
            vertex = graph.idVertexMap.get(vertex.getPreviousVertexIDToSink());
        }
        if (pathList.getLast() != vertex.getVeretexId()) {
            pathList.addLast(vertex.getVeretexId());
        }
		System.out.print("链路" + (vnf - 1) + " 部署在 路径 ");
		for (Integer vertexKey : pathList) {
			System.out.print(vertexKey + " ---> ");
		}
		System.out.println();
		System.out.println("功能" + vnf + " 部署在 节点" + minDelayVertexID + "上");
        CPath path = new CPath(pathList, graph, minDelayVertexID, sfc);
        path.setVnfDeployPosition(minDelayVertexID);
        return path;

    }

    /**
     * 中间模块功能的部署
     * 功能：从源点开始，计算剩余节点vertexList中，能够部署vnf的最短路径
     *
     * @param vertexList 图中剩余节点集
     * @param sfc        需要部署的SFC
     * @param vnf        某个SFC中需要部署的功能点
     * @param source     源点
     * @param sink
     * @return path
     */
    public CPath getBestDeployOfMiddleVNF(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tmpList = new ArrayList<>();
        tmpList.addAll(vertexList);
        initTheVertexInList(tmpList);
        graph.idVertexMap.get(source).setTotalDelay(0);
        graph.idVertexMap.get(source).setPreviousVertexId(Configure.TERMINALNODE);

        //SFC功能节点编号从1开始，链路编号从0开始
        Node node = sfc.nodeMap.get(vnf);   //需要部署的功能节点
        Link link = sfc.linkMap.get(vnf - 1);

        //第一步
        updateAllDelay(tmpList, source, link);

//        System.out.println("第一步部署完成");

        //第2步，从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay延时最小`
        float minTotalDelay = Configure.INF;
        int minDelayVertexID = Configure.IMPOSSIBLENODE;
        for (int key : vertexList) {
            CVertex vertex = graph.idVertexMap.get(key);
//            System.out.println("当前节点: " + vertex.getVeretexId() +" 的总延时：" + vertex.getTotalDelay());
            if (vertex.getVeretexId() != sink && vertex.getRemainComputeResource() >= node.getComputeResourceDemand()
                    && minTotalDelay > vertex.getTotalDelay()) {
                minTotalDelay = vertex.getTotalDelay();
                minDelayVertexID = vertex.getVeretexId();
//                System.out.println("资源是否满足");
            }
        }
//        System.out.println("第二步完成");

        //第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay延时最小` 的节点，说明不能部署这一节点，返回null
        if (minTotalDelay == Configure.INF) {
//			System.out.println("在搜寻满足node需求的过程过程中，找不到时延最小的节点，用return null 退出");
            return null;
        }
//        System.out.println("VNF " + vnf + "  的minDelayVertexID：" + minDelayVertexID);

        //第4步，找出满足：`满足node的资源需求，且节点的totalDelay延时最小` 的节点，构建这一条路径，返回路径
        LinkedList<Integer> pathList = new LinkedList<>();
        CVertex vertex = graph.idVertexMap.get(minDelayVertexID);
        while (vertex.getPreviousVertexId() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getPreviousVertexId());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径


        System.out.println("功能" + vnf + " 部署在 节点" + minDelayVertexID + "上");
        System.out.print("链路" + vnf + " 部署在 路径: ");
		for (int vertexKey : pathList) {
			System.out.print(vertexKey + " ---> ");
		}
		System.out.println();

        CPath path = new CPath(pathList, graph, sfc);
        path.setVnfDeployPosition(minDelayVertexID);
        return path;
    }

    public void updateAllDelayToSink(ArrayList<Integer> vertexList, int sink, Link link) {
        int it = sink;
        float minTotalDelay;
        int minDelayVertexID;
        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
                    if (vertex.getTotalDelayToSink() + edge.getDelay() < sinkVertex.getTotalDelayToSink()) {
                        sinkVertex.setTotalDelayToSink(vertex.getTotalDelayToSink() + edge.getDelay());
                        sinkVertex.setPreviousVertexIDToSink(it);
                    }
                }
            }

            //从剩余的节点中，选出时延最小的点
            minTotalDelay = Configure.INF;
            minDelayVertexID = Configure.IMPOSSIBLENODE;

//			System.out.println("测试tempList中是否还有 sink");
//			for (Integer vertexKey : tempList) {
//				System.out.print(vertexKey + " ---> ");
//			}
//			System.out.println();
            for (Integer vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (minTotalDelay > tempVertex.getTotalDelayToSink()) {
                    minTotalDelay = tempVertex.getTotalDelayToSink();
                    minDelayVertexID = tempVertex.getVeretexId();
                }
            }
            if (minTotalDelay >= Configure.INF) {
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
                break;
            }
            it = minDelayVertexID;
//			System.out.println("删除的 最小时延节点是 " + minDelayVertexID);
			CVertex minVertex = graph.idVertexMap.get(minDelayVertexID);
//			System.out.println("最小时延节点的时延是： " + minVertex.getTotalDelayToSink());
            vertexList.remove(vertexList.indexOf(minDelayVertexID));
        }
    }

    /**
     * 更新图剩余节点vertexList中所有节点到source的 `满足链路需求的`、最短时延，以及该最短路径的上一跳
     *
     * @param vertexList
     * @param source
     */
    public void updateAllDelay(ArrayList<Integer> vertexList, int source, Link link) {
        int it = source;
        float minTotalDelay;
        int minDelayVertexID;

        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
//            System.out.print("更新所有节点到source的时延：" + vertex.getVeretexId() + "-->");
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
//                System.out.println("边 " + edge.getEdgeId() + " ( " + edge.getSourceId() + "->" + edge.getSinkId() +
//                        " ) 的剩余带宽容量为：" + edge.getRemainBandWidthResource());
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
                    if (vertex.getTotalDelay() + edge.getDelay() < sinkVertex.getTotalDelay()) {
                        sinkVertex.setTotalDelay(vertex.getTotalDelay() + edge.getDelay());
                        sinkVertex.setPreviousVertexId(it);
//                        System.out.println("当前点：" + sinkVertex.getVeretexId() + " 时延："+sinkVertex.getTotalDelay()
//                            + " 前一节点：" + sinkVertex.getPreviousVertexId());
                    }
                }
            }

            //从剩余的节点中，选出时延最小的点
            minTotalDelay = Configure.INF;
            minDelayVertexID = Configure.IMPOSSIBLENODE;
            for (int vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (minTotalDelay > tempVertex.getTotalDelay()) {
                    minTotalDelay = tempVertex.getTotalDelay();
                    minDelayVertexID = tempVertex.getVeretexId();
                }
            }
            if (minTotalDelay == Configure.INF) {
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
//				System.out.println("找完所有节点，有几个节点都是没有更新数据，用break退出");
                break;
            }
            it = minDelayVertexID;
//            System.out.println("==========");
//            System.out.println("minDelayVertexID: "  + minDelayVertexID);
            vertexList.remove(vertexList.indexOf(minDelayVertexID));
        }
    }

    /*
	 * 功能：将指定链表中的节点的previousVertexID设置为Configure.IMPOSSIBLENODE和totalDelay设置为Configure.INF
	 */
    private void initTheVertexInList(ArrayList<Integer> list) {
        for (int key : list) {
            CVertex vertex = graph.idVertexMap.get(key);
            vertex.setPreviousVertexId(Configure.IMPOSSIBLENODE);
            vertex.setTotalDelay(Configure.INF);
            vertex.setPreviousVertexIDToSink(Configure.IMPOSSIBLENODE);
            vertex.setTotalDelayToSink(Configure.INF);
        }
    }


    /**
     * 根据负载均衡部署SFC
     *
     * @param sfc
     * @param source
     * @param sink
     * @return
     */
    public Plan deploySFCByLoadBalance(SFC sfc, int source, int sink) {
        Plan plan = new Plan();
        ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点
        vertexList.addAll(graph.vertexList);
        int currentVertexId = source;

        vertexList.remove(vertexList.indexOf(source));
        vertexList.remove(vertexList.indexOf(sink));

        for (int vnfKey : sfc.nodeList) {
            if (vnfKey != sfc.nodeList.getLast()) {
                CPath path = getBestDeployOfMiddleVNFByLoadBalance(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<>();
                        list.addAll(path.edgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    vertexList.removeAll(path.vertexList);    //移除已经使用的点
                    currentVertexId = path.getVnfDeployPosition();
                }
            } else {
                CPath path = getBestDeployOfLastVNFByLoadBalance(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    //处理vnfKey前的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.firstEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    //处理vnfKey后的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
                        plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.secondEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey, list);
                    }
                }
            }

        }
        return plan;
    }

    /**
     * 根据负载均衡思想部署SFC的前n-1个功能
     *
     * @param vertexList
     * @param sfc
     * @param vnf
     * @param source
     * @param sink
     * @return
     */
    public CPath getBestDeployOfMiddleVNFByLoadBalance(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tmpList = new ArrayList<>();
        tmpList.addAll(vertexList);
        initLoadBalance(tmpList);
        graph.idVertexMap.get(source).setLoadBalanceToSource(0);
        graph.idVertexMap.get(source).setLb_PreviousVertexIDToSource(Configure.TERMINALNODE);

        //SFC功能节点编号从1开始，链路编号从0开始
        Node node = sfc.nodeMap.get(vnf);   //需要部署的功能节点
        Link link = sfc.linkMap.get(vnf - 1);

        //第一步
        updateAllLoadBalance(tmpList, source, link);

        //第2步，从剩余节点中，搜寻节点：`满足node的资源需求，负载最均衡的点`
        float minTotalLoadBalance = Configure.INF;
        int minLoadBalanceVertexID = Configure.IMPOSSIBLENODE;
        for (int key : vertexList) {
            CVertex vertex = graph.idVertexMap.get(key);
            if (vertex.getVeretexId() != sink && vertex.getRemainComputeResource() >= node.getComputeResourceDemand()
                    && minTotalLoadBalance > (float) Configure.BETA_LB * vertex.getLoadBalanceToSource() + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource())) {
                minTotalLoadBalance = (float) Configure.BETA_LB * vertex.getLoadBalanceToSource() + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource());
                minLoadBalanceVertexID = vertex.getVeretexId();
            }
        }

        //第3步，如果不存在满足：`满足node的资源需求，且负载最均衡` 的节点，说明不能部署这一节点，返回null
        if (minTotalLoadBalance == Configure.INF) {
//			System.out.println("在搜寻满足node需求的过程过程中，找不到时延最小的节点，用return null 退出");
            return null;
        }

        //第4步，找出满足：`满足node的资源需求，且负载最均衡` 的节点，构建这一条路径，返回路径
        LinkedList<Integer> pathList = new LinkedList<>();
        CVertex vertex = graph.idVertexMap.get(minLoadBalanceVertexID);
        while (vertex.getLb_PreviousVertexIDToSource() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getLb_PreviousVertexIDToSource());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径


        CPath path = new CPath(pathList, graph, sfc);
        path.setVnfDeployPosition(minLoadBalanceVertexID);
        return path;
    }

    /**
     * 功能：从剩余节点中，找到一点，使得该点满足：
     * ①该点能够部署vnf（计算资源足够）
     * ②该点到源点的路径时延 + 该点到终点的路径时延 之和最小
     *
     * @param vertexList 图中剩余节点集
     * @param sfc        需要部署的SFC
     * @param vnf        某个SFC中需要部署的功能点（这里为一条SFC的最后一个功能点）
     * @param source     源点
     * @param sink
     * @return
     */
    public CPath getBestDeployOfLastVNFByLoadBalance(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.addAll(vertexList);
        initLoadBalance(tempList);

        graph.idVertexMap.get(source).setLoadBalanceToSource(0);
        graph.idVertexMap.get(source).setLb_PreviousVertexIDToSource(Configure.TERMINALNODE);

        if (tempList.contains(source)) {
            tempList.remove(tempList.indexOf(source));
        }

        Node node = sfc.nodeMap.get(vnf);
        Link firstLink = sfc.linkMap.get(vnf - 1);  //该功能前的虚拟链路
        Link secondLink = sfc.linkMap.get(vnf);     //该功能后的虚拟链路

        //先处理 某点到source点的负载最均衡的可部署路径,第一步
        updateAllLoadBalance(tempList, source, firstLink);

        //再处理 某点到sink最短时延可部署路径
        tempList.addAll(vertexList);
        graph.idVertexMap.get(sink).setLoadBalanceToSink(0);
        graph.idVertexMap.get(sink).setLb_PreviousVertexIDToSink(Configure.TERMINALNODE);
        if (tempList.contains(sink)) {
            tempList.remove(tempList.indexOf(sink));
        }
        updateAllLoadBalanceToSink(tempList, sink, secondLink);


        float minTotalLoadBalance;
        int minLoadBalanceVertexId;

        //第2步：从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小的点`
        minTotalLoadBalance = Configure.INF;
        minLoadBalanceVertexId = Configure.IMPOSSIBLENODE;

        for (Integer vertexkey : vertexList) {
            CVertex vertex = graph.idVertexMap.get(vertexkey);
            if (vertex.getRemainComputeResource() >= node.getComputeResourceDemand() &&
                    minTotalLoadBalance > (float) Configure.BETA_LB * (vertex.getLoadBalanceToSink() + vertex.getLoadBalanceToSource()) + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource())) {
                minTotalLoadBalance = (float) Configure.BETA_LB * (vertex.getLoadBalanceToSink() + vertex.getLoadBalanceToSource()) + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource());
                minLoadBalanceVertexId = vertexkey;
            }
        }

        //第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，说明不能部署这一节点，返回null
        if (minTotalLoadBalance >= Configure.INF) {
//			System.out.println("最后一个功能在这里返回null==============");
//			System.out.println("minTotalDelay = " + minTotalDelay);
            return null;
        }

        //第4步：存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，能够部署，返回部署路径
        LinkedList<Integer> pathList = new LinkedList<Integer>();
        CVertex vertex = graph.idVertexMap.get(minLoadBalanceVertexId);
        while (vertex.getLb_PreviousVertexIDToSource() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getLb_PreviousVertexIDToSource());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径

        vertex = graph.idVertexMap.get(minLoadBalanceVertexId);
        while (vertex.getLb_PreviousVertexIDToSink() != Configure.TERMINALNODE) {
            if (pathList.getLast() != vertex.getVeretexId()) {
                pathList.addLast(vertex.getVeretexId());
            }
            vertex = graph.idVertexMap.get(vertex.getLb_PreviousVertexIDToSink());
        }
        if (pathList.getLast() != vertex.getVeretexId()) {
            pathList.addLast(vertex.getVeretexId());
        }
        CPath path = new CPath(pathList, graph, minLoadBalanceVertexId, sfc);
        path.setVnfDeployPosition(minLoadBalanceVertexId);
        return path;
    }

    /**
     * 更新剩余点到目的点的负载最均衡的路径
     *
     * @param vertexList
     * @param sink
     * @param link
     */
    public void updateAllLoadBalanceToSink(ArrayList<Integer> vertexList, int sink, Link link) {
        int it = sink;
        float minTotalLoadBalance;
        int minLoadBalanceVertexID;
        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
                    if (vertex.getLoadBalanceToSink() + link.getBandWidthResourceDemand() < sinkVertex.getLoadBalanceToSink()) {
                        sinkVertex.setLoadBalanceToSink(vertex.getLoadBalanceToSink() + link.getBandWidthResourceDemand());
                        sinkVertex.setLb_PreviousVertexIDToSink(it);
                    }
                }
            }

            //从剩余的节点中，选出负载最均衡的点
            minTotalLoadBalance = Configure.INF;
            minLoadBalanceVertexID = Configure.IMPOSSIBLENODE;

            for (Integer vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (minTotalLoadBalance > tempVertex.getLoadBalanceToSink()) {
                    minTotalLoadBalance = tempVertex.getLoadBalanceToSink();
                    minLoadBalanceVertexID = tempVertex.getVeretexId();
                }
            }
            if (minTotalLoadBalance >= Configure.INF) {
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
                break;
            }
            it = minLoadBalanceVertexID;
            vertexList.remove(vertexList.indexOf(minLoadBalanceVertexID));
        }
    }

    /*
	 * 功能：初始化，给定节点集合中节点的loadBalance
	 */
    private void initLoadBalance(ArrayList<Integer> list) {
        for (Integer vertexKey : list) {
            CVertex vertex = graph.idVertexMap.get(vertexKey);
            vertex.setLb_PreviousVertexIDToSink(Configure.IMPOSSIBLENODE);
            vertex.setLb_PreviousVertexIDToSource(Configure.IMPOSSIBLENODE);
            vertex.setLoadBalanceToSource(Configure.INF);
            vertex.setLoadBalanceToSink(Configure.INF);
        }
    }

    /**
     * 更新图剩余节点vertexList中所有节点到source的 `满足链路需求的`、负载值，以及该最短路径的上一跳
     *
     * @param vertexList
     * @param source
     */
    public void updateAllLoadBalance(ArrayList<Integer> vertexList, int source, Link link) {
        int it = source;
        float minTotalLoadBalance;
        int minLoadBalanceVertexID;

        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                //这里加入了带宽资源不满足时的判断
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
                    if (vertex.getLoadBalanceToSource() + link.getBandWidthResourceDemand() < sinkVertex.getLoadBalanceToSource()) {
                        sinkVertex.setLoadBalanceToSource(vertex.getLoadBalanceToSource() + link.getBandWidthResourceDemand());
                        sinkVertex.setLb_PreviousVertexIDToSource(it);
                    }
                }
            }

            //从剩余的节点中，选出负载最小的点
            minTotalLoadBalance = Configure.INF;
            minLoadBalanceVertexID = Configure.IMPOSSIBLENODE;
            for (int vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (minTotalLoadBalance > tempVertex.getLoadBalanceToSource()) {
                    minTotalLoadBalance = tempVertex.getLoadBalanceToSource();
                    minLoadBalanceVertexID = tempVertex.getVeretexId();
                }
            }
            if (minTotalLoadBalance == Configure.INF) {
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
//				System.out.println("找完所有节点，有几个节点都是没有更新数据，用break退出");
                break;
            }
            it = minLoadBalanceVertexID;
            vertexList.remove(vertexList.indexOf(minLoadBalanceVertexID));
        }
    }


    /**
     * 在满足用户可靠性条件下，根据负载均衡部署SFC
     *
     * @param sfc
     * @param source
     * @param sink
     * @return
     */
    public Plan deploySFCByLoadBalanceAndReliability(SFC sfc, int source, int sink) {
        Plan plan = new Plan();
        ArrayList<Integer> vertexList = new ArrayList<>();  //用于存储网络图中的可用节点
        vertexList.addAll(graph.vertexList);
        int currentVertexId = source;

        vertexList.remove(vertexList.indexOf(source));
        vertexList.remove(vertexList.indexOf(sink));

        for (int vnfKey : sfc.nodeList) {
            if (vnfKey != sfc.nodeList.getLast()) {
                CPath path = getBestDeployOfMiddleVNFByLoadBalanceAndReliability(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.edgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<>();
                        list.addAll(path.edgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    vertexList.removeAll(path.vertexList);    //移除已经使用的点
                    currentVertexId = path.getVnfDeployPosition();
                }
            } else {
                CPath path = getBestDeployOfLastVNFByLoadBalanceAndReliability(vertexList, sfc, vnfKey, currentVertexId, sink);
                if (path == null) {
                    return null;
                } else {
                    plan.nodeDeployVertexMap.put(vnfKey, path.getVnfDeployPosition());
                    //处理vnfKey前的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey - 1)) {
                        plan.linkDeployEdgeMap.get(vnfKey - 1).addAll(path.firstEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.firstEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey - 1, list);
                    }
                    //处理vnfKey后的虚拟路径
                    if (plan.linkDeployEdgeMap.containsKey(vnfKey)) {
                        plan.linkDeployEdgeMap.get(vnfKey).addAll(path.secondEdgeList);
                    } else {
                        LinkedList<Integer> list = new LinkedList<Integer>();
                        list.addAll(path.secondEdgeList);
                        plan.linkDeployEdgeMap.put(vnfKey, list);
                    }
                }
            }

        }
        /*sfc.setPlan(plan);
        float reliability = sfc.getDeployedReliability(graph);
        if (reliability >= Configure.REQUEST_RELIABILITY) {
            return plan;
        } else
            return null;*/
        return plan;
    }

    /**
     * 满足用户可靠性条件下根据负载均衡思想部署SFC的前n-1个功能
     *
     * @param vertexList
     * @param sfc
     * @param vnf
     * @param source
     * @param sink
     * @return
     */
    public CPath getBestDeployOfMiddleVNFByLoadBalanceAndReliability(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tmpList = new ArrayList<>();
        tmpList.addAll(vertexList);
        initLoadBalanceAndReliability(tmpList);

        CVertex vertex1 = graph.idVertexMap.get(source);
        vertex1.setLoadBalanceToSource(0);
        vertex1.setLb_PreviousVertexIDToSource(Configure.TERMINALNODE);
        vertex1.setReliabilityToSource(vertex1.getReliability()); //设当前节点source到源节点的可靠性为该节点本身
        vertex1.setRb_previousVertexIdToSource(Configure.TERMINALNODE);    //设前一节点为不可达


        //SFC功能节点编号从1开始，链路编号从0开始
        Node node = sfc.nodeMap.get(vnf);   //需要部署的功能节点
        Link link = sfc.linkMap.get(vnf - 1);

        //第一步
        updateAllLoadBalanceAndReliability(tmpList, source, link);

        //第2步，从剩余节点中，搜寻节点：`满足node的资源需求，负载最均衡的点`
        float minTotalLoadBalance = Configure.INF;
        float maxReliability = Configure.UNINF;
        int suitableVertexID = Configure.IMPOSSIBLENODE;
        for (int key : vertexList) {
            CVertex vertex = graph.idVertexMap.get(key);
            if(vertex.getReliabilityToSource() >= Configure.REQUEST_RELIABILITY) {  //只考虑可靠性满足的那些点
                if (vertex.getVeretexId() != sink && vertex.getRemainComputeResource() >= node.getComputeResourceDemand()
                        && minTotalLoadBalance > (float) Configure.BETA_LB * vertex.getLoadBalanceToSource() + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource())
                        ) {  //&& maxReliability < vertex.getReliabilityToSource()
                    minTotalLoadBalance = (float) Configure.BETA_LB * vertex.getLoadBalanceToSource() + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource());
                    maxReliability = vertex.getReliabilityToSource();
                    suitableVertexID = vertex.getVeretexId();
                }
            }

        }

        //第3步，如果不存在满足：`满足node的资源需求，且负载最均衡` 的节点，说明不能部署这一节点，返回null
        if (minTotalLoadBalance == Configure.INF) { // || maxReliability == Configure.UNINF
            return null;
        }

        //第4步，找出满足：`满足node的资源需求，且负载最均衡` 的节点，构建这一条路径，返回路径
        LinkedList<Integer> pathList = new LinkedList<>();
        CVertex vertex = graph.idVertexMap.get(suitableVertexID);
        while (vertex.getLb_PreviousVertexIDToSource() != Configure.TERMINALNODE &&
                vertex.getRb_previousVertexIdToSource() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getLb_PreviousVertexIDToSource());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径


        CPath path = new CPath(pathList, graph, sfc);
        path.setVnfDeployPosition(suitableVertexID);
        return path;
    }

    /**
     * 功能：从剩余节点中，找到一点，使得该点满足：
     * ①该点能够部署vnf（计算资源足够）
     * ②该点到源点的路径时延 + 该点到终点的路径时延 之和最小
     *
     * @param vertexList 图中剩余节点集
     * @param sfc        需要部署的SFC
     * @param vnf        某个SFC中需要部署的功能点（这里为一条SFC的最后一个功能点）
     * @param source     源点
     * @param sink
     * @return
     */
    public CPath getBestDeployOfLastVNFByLoadBalanceAndReliability(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.addAll(vertexList);
        initLoadBalanceAndReliability(tempList);

        CVertex vertex1 = graph.idVertexMap.get(source);
        vertex1.setReliabilityToSource(vertex1.getReliability()); //设当前节点source到源节点的可靠性为该节点本身
        vertex1.setRb_previousVertexIdToSource(Configure.TERMINALNODE);    //设前一节点为不可达
        vertex1.setLoadBalanceToSource(0);
        vertex1.setLb_PreviousVertexIDToSource(Configure.TERMINALNODE);

        if (tempList.contains(source)) {
            tempList.remove(tempList.indexOf(source));
        }

        Node node = sfc.nodeMap.get(vnf);
        Link firstLink = sfc.linkMap.get(vnf - 1);  //该功能前的虚拟链路
        Link secondLink = sfc.linkMap.get(vnf);     //该功能后的虚拟链路

        //先处理 某点到source点的负载最均衡的可部署路径,第一步
        updateAllLoadBalanceAndReliability(tempList, source, firstLink);

        //再处理 某点到sink最短时延可部署路径
        tempList.addAll(vertexList);
        CVertex vertex2 = graph.idVertexMap.get(sink);
        vertex2.setReliabilityToSink(vertex2.getReliability());
        vertex2.setRb_previousVertexIdToSink(Configure.TERMINALNODE);
        vertex2.setLoadBalanceToSink(0);
        vertex2.setLb_PreviousVertexIDToSink(Configure.TERMINALNODE);
        if (tempList.contains(sink)) {
            tempList.remove(tempList.indexOf(sink));
        }
        updateAllLoadBalanceAndReliabilityToSink(tempList, sink, secondLink);


        float minTotalLoadBalance;
        float maxReliability;
        int suitableVertexId;

        //第2步：从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小的点`
        minTotalLoadBalance = Configure.INF;
        maxReliability = Configure.UNINF;
        suitableVertexId = Configure.IMPOSSIBLENODE;

        for (Integer vertexkey : vertexList) {
            CVertex vertex = graph.idVertexMap.get(vertexkey);
            if(vertex.getReliabilityToSink() * vertex.getReliabilityToSource() / vertex.getReliability() >= Configure.REQUEST_RELIABILITY) {
                if (vertex.getRemainComputeResource() >= node.getComputeResourceDemand() &&
                        minTotalLoadBalance > (float) Configure.BETA_LB * (vertex.getLoadBalanceToSink() + vertex.getLoadBalanceToSource()) + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource())
                        ) {
                    minTotalLoadBalance = (float) Configure.BETA_LB * (vertex.getLoadBalanceToSink() + vertex.getLoadBalanceToSource()) + vertex.getLoadBalanceOfVertex(graph, vertex.getLb_PreviousVertexIDToSource());
                    maxReliability = vertex.getReliabilityToSink() * vertex.getReliabilityToSource() / vertex.getReliability();
                    suitableVertexId = vertexkey;
                }
            }

        }

        //第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，说明不能部署这一节点，返回null
        if (minTotalLoadBalance == Configure.INF) { // || maxReliability == Configure.UNINF
            return null;
        }

        //第4步：存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，能够部署，返回部署路径
        LinkedList<Integer> pathList = new LinkedList<Integer>();
        CVertex vertex = graph.idVertexMap.get(suitableVertexId);
        while (vertex.getLb_PreviousVertexIDToSource() != Configure.TERMINALNODE &&
                vertex.getRb_previousVertexIdToSource() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = graph.idVertexMap.get(vertex.getLb_PreviousVertexIDToSource());
        }
        pathList.addFirst(vertex.getVeretexId());    //把起始点加入路径

        vertex = graph.idVertexMap.get(suitableVertexId);
        while (vertex.getLb_PreviousVertexIDToSink() != Configure.TERMINALNODE &&
                vertex.getRb_previousVertexIdToSink() != Configure.TERMINALNODE) {
            if (pathList.getLast() != vertex.getVeretexId()) {
                pathList.addLast(vertex.getVeretexId());
            }
            vertex = graph.idVertexMap.get(vertex.getLb_PreviousVertexIDToSink());
        }
        if (pathList.getLast() != vertex.getVeretexId()) {
            pathList.addLast(vertex.getVeretexId());
        }
        CPath path = new CPath(pathList, graph, suitableVertexId, sfc);
        path.setVnfDeployPosition(suitableVertexId);
        return path;
    }

    /**
     * 更新剩余点到目的点的满足可靠性以及负载最均衡的路径
     *
     * @param vertexList
     * @param sink
     * @param link
     */
    public void updateAllLoadBalanceAndReliabilityToSink(ArrayList<Integer> vertexList, int sink, Link link) {
        int it = sink;
        float minTotalLoadBalance;
        float maxReliability;
        int suitableVertexID;
        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                //满足带宽需求时
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());

                    if (vertex.getLoadBalanceToSink() + link.getBandWidthResourceDemand() < sinkVertex.getLoadBalanceToSink() &&
                            vertex.getReliabilityToSink() * edge.getReliability() * sinkVertex.getReliability() > sinkVertex.getReliabilityToSink() &&
                            vertex.getReliabilityToSink() * edge.getReliability() * sinkVertex.getReliability() >= Configure.REQUEST_RELIABILITY) {

                        sinkVertex.setLoadBalanceToSink(vertex.getLoadBalanceToSink() + link.getBandWidthResourceDemand());
                        sinkVertex.setLb_PreviousVertexIDToSink(it);
                        sinkVertex.setReliabilityToSink(vertex.getReliabilityToSink() * edge.getReliability() * sinkVertex.getReliability());
                        sinkVertex.setRb_previousVertexIdToSink(it);
                    }
                }
            }

            //从剩余的节点中，选出负载最均衡的点
            minTotalLoadBalance = Configure.INF;
            maxReliability = Configure.UNINF;
            suitableVertexID = Configure.IMPOSSIBLENODE;

            for (Integer vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (minTotalLoadBalance > tempVertex.getLoadBalanceToSink() && maxReliability < tempVertex.getReliabilityToSink()) {
                    minTotalLoadBalance = tempVertex.getLoadBalanceToSink();
                    maxReliability = tempVertex.getReliabilityToSink();
                    suitableVertexID = tempVertex.getVeretexId();
                }
            }
            if (minTotalLoadBalance == Configure.INF) { // || maxReliability == Configure.UNINF
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
                break;
            }
            it = suitableVertexID;
            vertexList.remove(vertexList.indexOf(suitableVertexID));
        }
    }

    /*
	 * 功能：初始化，给定节点集合中节点的loadBalance以及reliability
	 */
    private void initLoadBalanceAndReliability(ArrayList<Integer> list) {
        for (Integer vertexKey : list) {
            CVertex vertex = graph.idVertexMap.get(vertexKey);
            vertex.setLb_PreviousVertexIDToSink(Configure.IMPOSSIBLENODE);
            vertex.setLb_PreviousVertexIDToSource(Configure.IMPOSSIBLENODE);
            vertex.setLoadBalanceToSource(Configure.INF);
            vertex.setLoadBalanceToSink(Configure.INF);

            vertex.setRb_previousVertexIdToSink(Configure.IMPOSSIBLENODE);
            vertex.setRb_previousVertexIdToSource(Configure.IMPOSSIBLENODE);
            vertex.setReliabilityToSource(Configure.UNINF);
            vertex.setReliabilityToSink(Configure.UNINF);
        }
    }

    /**
     * 更新图剩余节点vertexList中所有节点到source的 `满足链路可靠性需求的`、负载值，以及该最短路径的上一跳
     *
     * @param vertexList
     * @param source
     */
    public void updateAllLoadBalanceAndReliability(ArrayList<Integer> vertexList, int source, Link link) {
        int it = source;
        float minTotalLoadBalance;
        int suitableVertexID;
        float maxReliability;

        while (!vertexList.isEmpty()) {
            CVertex vertex = graph.idVertexMap.get(it);
            for (Integer edgeKey : vertex.outsideEdgeList) {
                CEdge edge = graph.idEdgeMap.get(edgeKey);
                //这里加入了带宽资源不满足时的判断
                if (edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
                    CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
                    /*//更新节点到源的最小负载以及上一跳节点
                    if (vertex.getLoadBalanceToSource() + link.getBandWidthResourceDemand() < sinkVertex.getLoadBalanceToSource()) {
                        sinkVertex.setLoadBalanceToSource(vertex.getLoadBalanceToSource() + link.getBandWidthResourceDemand());
                        sinkVertex.setLb_PreviousVertexIDToSource(it);
                    }
                    //链路1-2，节点1到源的可靠性 * 边的可靠性 * 节点2的可靠性 > 节点2到源的可靠性，，，那么选择可靠性较大的更新
                    if(vertex.getReliabilityToSource()*edge.getReliability()*sinkVertex.getReliability() > sinkVertex.getReliabilityToSource()) {
                        sinkVertex.setReliabilityToSource(vertex.getReliabilityToSource()*edge.getReliability()*sinkVertex.getReliability());
                        sinkVertex.setRb_previousVertexIdToSource(it);
                    }*/
                    //更新节点到源的最小负载，上一跳节点，以及最大可靠性且满足需求的节点
                    if (vertex.getLoadBalanceToSource() + link.getBandWidthResourceDemand() < sinkVertex.getLoadBalanceToSource() &&
                            vertex.getReliabilityToSource() * edge.getReliability() * sinkVertex.getReliability() > sinkVertex.getReliabilityToSource() &&
                            vertex.getReliabilityToSource() * edge.getReliability() * sinkVertex.getReliability() >= Configure.REQUEST_RELIABILITY) {

                        sinkVertex.setLoadBalanceToSource(vertex.getLoadBalanceToSource() + link.getBandWidthResourceDemand());
                        sinkVertex.setLb_PreviousVertexIDToSource(it);
                        sinkVertex.setReliabilityToSource(vertex.getReliabilityToSource() * edge.getReliability() * sinkVertex.getReliability());
                        sinkVertex.setRb_previousVertexIdToSource(it);
                    }
                }
            }

            //从剩余的节点中，选出负载最小的点
            minTotalLoadBalance = Configure.INF;
            suitableVertexID = Configure.IMPOSSIBLENODE;
            maxReliability = Configure.UNINF;
            for (int vertexKey : vertexList) {
                CVertex tempVertex = graph.idVertexMap.get(vertexKey);
                if (minTotalLoadBalance > tempVertex.getLoadBalanceToSource() && maxReliability < tempVertex.getReliabilityToSource()) {
                    minTotalLoadBalance = tempVertex.getLoadBalanceToSource();
                    maxReliability = tempVertex.getReliabilityToSource();
                    suitableVertexID = tempVertex.getVeretexId();
                }
            }

            if (minTotalLoadBalance == Configure.INF) { // || maxReliability == Configure.UNINF
                //有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
//				System.out.println("找完所有节点，有几个节点都是没有更新数据，用break退出");
                break;
            }
            it = suitableVertexID;
            vertexList.remove(vertexList.indexOf(suitableVertexID));
        }
    }

    /**
     * 对比算法根据链路优先映射原则部署SFC
     * @param sfc
     * @param source
     * @param sink
     * @return
     */
    public Plan deploySFCByLMFalg(SFC sfc, int source, int sink) {
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.addAll(graph.vertexList);
        tempList.remove(tempList.indexOf(source));
        tempList.remove(tempList.indexOf(sink));
        int vnfSize = sfc.nodeList.size();
        Plan plan = new Plan();
        Map<Integer, Integer> nodeVertexMap = new HashMap<>();  //临时存储map,SFC功能节点ID,图中部署位置节点ID
        LinkedList<Link> linkList = sfc.sortSFCLink();
//        System.out.println("SFC的链路长度为：" + linkList.size());
        for (Link link : linkList) {    //按SFC的链路带宽从大到小部署链路
            int sourceNodeId = link.getSourceId();
            int sinkNodeId = link.getSinkId();
            float maxReliability = Configure.UNINF;
            CVertex maxReliabilityVertex = null;
            CPath maxReliabilityPath = null;
            CVertex maxReliabilityVertex2 = null;
            Node sinkNode = sfc.nodeMap.get(sinkNodeId);
            Node sourceNode = sfc.nodeMap.get(sourceNodeId);

            //如果当前要部署的SFC链路的源、目的节点之前已经部署过了，则只用部署他的最短路到路径中即可
            if(nodeVertexMap.containsKey(sourceNodeId) && nodeVertexMap.containsKey(sinkNodeId)) {
                CPath path = graph.dijkstra(nodeVertexMap.get(sourceNodeId), nodeVertexMap.get(sinkNodeId), link);
                if(path == null) {
                    return null;
                }
                pathToPlan(path, plan, link.getLinkId());
            }
            //如果当前要部署的SFC链路的目的节点还没有部署过
            else if(nodeVertexMap.containsKey(sourceNodeId) && !nodeVertexMap.containsKey(sinkNodeId)) {
                CVertex vertex = graph.idVertexMap.get(nodeVertexMap.get(sourceNodeId));    //SFC链路源节点部署的位置节点
                if(link.getLinkId() == vnfSize) {   //部署最后一条链路时
                    CPath path = graph.dijkstra(vertex.getVeretexId(), sink, link);
                    if(path == null) {
                        return null;
                    }
                    pathToPlan(path, plan, link.getLinkId());
                    nodeVertexMap.put(sinkNodeId, sink);
                }
                else {
                    for(int vertexKey : tempList) { //在剩余节点集中找到还没有功能部署过得可靠性最大的sinkVertex
                        CVertex sinkVertex = graph.idVertexMap.get(vertexKey);
                        if(sinkVertex.getRemainComputeResource() < sinkNode.getComputeResourceDemand()) {   //计算资源不足时
                            continue;
                        }
                        CPath path = graph.dijkstra(vertex.getVeretexId(), sinkVertex.getVeretexId(), link);  //根据可靠性找路
                        if(path == null) {
                            continue;
                        }
                        if(checkSinkNodeResource(sinkVertex.getVeretexId(), link, sfc) && maxReliability < path.getPathReliability(graph)) {
                            maxReliability = path.getPathReliability(graph);
                            maxReliabilityVertex = sinkVertex;
                            maxReliabilityPath = path;
                        }
                    }

                    if(maxReliabilityPath == null) {
                        return null;
                    }
                    nodeVertexMap.put(sinkNodeId, maxReliabilityVertex.getVeretexId());
                    tempList.remove(tempList.indexOf(maxReliabilityVertex.getVeretexId()));
                    pathToPlan(maxReliabilityPath, plan, link.getLinkId()); //将选出的路径加到部署方案plan中
                }

            }
            //如果当前要部署的SFC链路的源节点还没有部署过
            else if(!nodeVertexMap.containsKey(sourceNodeId) && nodeVertexMap.containsKey(sinkNodeId)) {
                CVertex vertex = graph.idVertexMap.get(nodeVertexMap.get(sinkNodeId));  //SFC链路目的节点部署的位置节点
                if(link.getLinkId() == 0) { //部署第一条链路时
                    CPath path = graph.dijkstra(source, vertex.getVeretexId(), link);
                    if(path == null) {
                        return null;
                    }
                    pathToPlan(path, plan, 0);  //最后一个参数0即为link.getLinkId()
//                    tempList.remove(tempList.indexOf(source));
                    nodeVertexMap.put(sourceNodeId, source);
                }
                else {
                    for(int vertexKey : tempList) {
                        CVertex sourceVertex = graph.idVertexMap.get(vertexKey);
                        if(sourceVertex.getRemainComputeResource() < sourceNode.getComputeResourceDemand()) {
                            continue;
                        }
                        CPath path = graph.dijkstra(sourceVertex.getVeretexId(), vertex.getVeretexId(), link);    //根据可靠性找路
                        if(path == null) {
                            continue;
                        }
                        if(checkSourceNodeResource(sourceVertex.getVeretexId(), link, sfc) && maxReliability < path.getPathReliability(graph)) {
                            maxReliability = path.getPathReliability(graph);
                            maxReliabilityVertex = sourceVertex;
                            maxReliabilityPath = path;
                        }
                    }

                    if(maxReliabilityPath == null) {
                        return null;
                    }
                    nodeVertexMap.put(sourceNodeId, maxReliabilityVertex.getVeretexId());
                    tempList.remove(tempList.indexOf(maxReliabilityVertex.getVeretexId()));
                    pathToPlan(maxReliabilityPath, plan, link.getLinkId());
                }

            }
            //如果当前要部署的SFC链路的源、目的节点都还没有部署过
            else if(!nodeVertexMap.containsKey(sourceNodeId) && !nodeVertexMap.containsKey(sinkNodeId)) {
                if(link.getLinkId() == 0) { //部署第一条链路时,固定源点部署位置
//                    tempList.remove(tempList.indexOf(source));
                    for(int vertexKey : tempList) { //在剩余节点集中找到还没有功能部署过得可靠性最大的sinkVertex
                        CVertex sinkVertex = graph.idVertexMap.get(vertexKey);
                        if(sinkVertex.getRemainComputeResource() < sinkNode.getComputeResourceDemand()) {
                            continue;
                        }
                        CPath path = graph.dijkstra(source, sinkVertex.getVeretexId(), link);  //根据可靠性找路
                        if(path == null) {
                            continue;
                        }
                        else {
                            if(checkSinkNodeResource(sinkVertex.getVeretexId(), link, sfc) && maxReliability < path.getPathReliability(graph)) {
                                maxReliability = path.getPathReliability(graph);
                                maxReliabilityVertex = sinkVertex;
                                maxReliabilityPath = path;
                            }
                        }

                    }
                    if(maxReliabilityPath == null) {
                        return null;
                    }
                    nodeVertexMap.put(sinkNodeId, maxReliabilityVertex.getVeretexId());
                    nodeVertexMap.put(sourceNodeId, source);
                    tempList.remove(tempList.indexOf(maxReliabilityVertex.getVeretexId()));
                    pathToPlan(maxReliabilityPath, plan, link.getLinkId()); //将选出的路径加到部署方案plan中
                }
                else if(link.getLinkId() == vnfSize) {  //部署最后一条链路时
//                    tempList.remove(tempList.indexOf(sink));
                    for(int vertexKey : tempList) {
                        CVertex sourceVertex = graph.idVertexMap.get(vertexKey);
                        if(sourceVertex.getRemainComputeResource() < sourceNode.getComputeResourceDemand()) {
                            continue;
                        }
                        CPath path = graph.dijkstra(sourceVertex.getVeretexId(), sink, link);    //根据可靠性找路
                        if(path == null) {
                            continue;
                        }
                        if(checkSourceNodeResource(sourceVertex.getVeretexId(), link, sfc) && maxReliability < path.getPathReliability(graph)) {
                            maxReliability = path.getPathReliability(graph);
                            maxReliabilityVertex = sourceVertex;
                            maxReliabilityPath = path;
                        }
                    }

                    if(maxReliabilityPath == null) {
                        return null;
                    }
                    nodeVertexMap.put(sourceNodeId, maxReliabilityVertex.getVeretexId());
                    nodeVertexMap.put(sinkNodeId, sink);
                    tempList.remove(tempList.indexOf(maxReliabilityVertex.getVeretexId()));
                    pathToPlan(maxReliabilityPath, plan, link.getLinkId());
                }
                else {  //部署中间部分链路时
                    //时间复杂度O(n^2)
                    for(int sourceKey : tempList) {
                        CVertex sourceVertex = graph.idVertexMap.get(sourceKey);
                        if(sourceVertex.getRemainComputeResource() < sourceNode.getComputeResourceDemand()) {
                            continue;
                        }
                        long startTime = System.currentTimeMillis();
                        for(int sinkKey : tempList) {
                            if(sourceKey == sinkKey) {
                                continue;
                            }
                            CVertex sinkVertex = graph.idVertexMap.get(sinkKey);
                            if(sinkVertex.getRemainComputeResource() < sinkNode.getComputeResourceDemand()) {
                                continue;
                            }
                            CPath path = graph.dijkstra(sourceVertex.getVeretexId(), sinkVertex.getVeretexId(), link);
                            if(path == null) {
                                continue;
                            }
                            if(checkNodeResource(sourceVertex.getVeretexId(), sinkVertex.getVeretexId(), link, sfc) &&
                                    maxReliability < path.getPathReliability(graph)) {
                                maxReliability = path.getPathReliability(graph);
                                maxReliabilityVertex = sourceVertex;
                                maxReliabilityVertex2 = sinkVertex;
                                maxReliabilityPath = path;
                            }
                        }
                    }
                    if(maxReliabilityPath == null) {
                        return null;
                    }
                    nodeVertexMap.put(sourceNodeId, maxReliabilityVertex.getVeretexId());
                    nodeVertexMap.put(sinkNodeId, maxReliabilityVertex2.getVeretexId());
                    tempList.remove(tempList.indexOf(maxReliabilityVertex.getVeretexId()));
                    tempList.remove(tempList.indexOf(maxReliabilityVertex2.getVeretexId()));
                    pathToPlan(maxReliabilityPath, plan, link.getLinkId());
                }
            }
        }

        plan.removeFirstAndLast();
        sfc.setPlan(plan);
        float reliability = sfc.getDeployedReliability(graph);
        if (reliability >= Configure.REQUEST_RELIABILITY) {
            return plan;
        } else
            return null;
    }

    /**
     * 检查SFC链路link部署在source和sink两个源、目的点上的节点资源是否满足条件
     * @param source
     * @param sink
     * @param link
     * @return
     */
    public boolean checkNodeResource(int source, int sink, Link link, SFC sfc) {
        if(checkSourceNodeResource(source, link, sfc) && checkSinkNodeResource(sink, link, sfc)) {
            return true;
        }
        return false;
    }

    public boolean checkSourceNodeResource(int source, Link link, SFC sfc) {
        Node sourceNode = sfc.nodeMap.get(link.getSourceId());
        if(sourceNode.getComputeResourceDemand() <= graph.idVertexMap.get(source).getRemainComputeResource()) {
            return true;
        }
        return false;
    }

    public boolean checkSinkNodeResource(int sink, Link link, SFC sfc) {
        Node sinkNode = sfc.nodeMap.get(link.getSinkId());
        if(sinkNode.getComputeResourceDemand() <= graph.idVertexMap.get(sink).getRemainComputeResource()) {
            return true;
        }
        return false;
    }

    /**
     * 将路径信息加入到plan中
     * @param path
     * @param plan
     * @param linkId    当前部署的sfc链路编号
     */
    public void pathToPlan(CPath path, Plan plan, int linkId) {
        int source = path.vertexList.getFirst();
        int sink = path.vertexList.getLast();
        //如果之前的部署方案plan中已经保存了之前部署的源、目的节点，则直接把链路添加到plan.linkDeployEdgeMap
        if(plan.nodeDeployVertexMap.containsValue(source) && plan.nodeDeployVertexMap.containsValue(sink)) {
            /*int nodeId = -1; //链路编号和功能节点编号
            for(Map.Entry entry : plan.nodeDeployVertexMap.entrySet()) {
                if(entry.getValue() == Integer.valueOf(source)) {
                    nodeId = (int)entry.getKey();
                    break;  //找到就直接跳出循环
                }
            }*/
            if(plan.linkDeployEdgeMap.containsKey(linkId)) {
                plan.linkDeployEdgeMap.get(linkId).addAll(path.edgeList);
            }
            else {
                LinkedList<Integer> list = new LinkedList<>();
                list.addAll(path.edgeList);
                plan.linkDeployEdgeMap.put(linkId, list);
            }
        }
        //如果之前的部署方案plan中保存了之前部署的source，但没有保存sink,则把链路和sink一起加入plan中
        else if(plan.nodeDeployVertexMap.containsValue(source) && (!plan.nodeDeployVertexMap.containsValue(sink))) {
            if(plan.linkDeployEdgeMap.containsKey(linkId)) {
                plan.linkDeployEdgeMap.get(linkId).addAll(path.edgeList);
            }
            else {
                LinkedList<Integer> list = new LinkedList<>();
                list.addAll(path.edgeList);
                plan.linkDeployEdgeMap.put(linkId, list);
            }
            plan.nodeDeployVertexMap.put(linkId+1, sink);   //将sink节点加入部署方案中
        }
        //如果之前的部署方案plan中保存了之前部署的sink，但没有保存source,则把链路和source一起加入plan中
        else if(!plan.nodeDeployVertexMap.containsValue(source) && plan.nodeDeployVertexMap.containsValue(sink)) {
            if(plan.linkDeployEdgeMap.containsKey(linkId)) {
                plan.linkDeployEdgeMap.get(linkId).addAll(path.edgeList);
            }
            else {
                LinkedList<Integer> list = new LinkedList<>();
                list.addAll(path.edgeList);
                plan.linkDeployEdgeMap.put(linkId, list);
            }
            plan.nodeDeployVertexMap.put(linkId, source);
        }
        //当之前的部署方案中都不包含source和sink的时候，将这两个节点以及链路信息都加入plan中
        else if(!plan.nodeDeployVertexMap.containsValue(source) && !plan.nodeDeployVertexMap.containsValue(sink)) {
            plan.nodeDeployVertexMap.put(linkId, source);
            plan.nodeDeployVertexMap.put(linkId+1, sink);
            if(plan.linkDeployEdgeMap.containsKey(linkId)) {
                plan.linkDeployEdgeMap.get(linkId).addAll(path.edgeList);
            }
            else {
                LinkedList<Integer> list = new LinkedList<>();
                list.addAll(path.edgeList);
                plan.linkDeployEdgeMap.put(linkId, list);
            }
        }
    }

    /**
     * 生成指定条数的SFC
     *
     * @param sfcLength
     * @param sfcNum
     */
    public void generateSFCs(int sfcLength, int sfcNum) {
        for (int i = 0; i < sfcNum; i++) {
            SFC sfc = generateSFC(sfcLength);
            idSFCMap.put(sfc.getSfcId(), sfc);
        }
    }

    /**
     * 生成一条SFC
     *
     * @param sfcLength
     * @return
     */
    public SFC generateSFC(int sfcLength) {
        SFC sfc = new SFC();
        sfc.setSfcId(idSFCMap.size());
        sfc.setLength(sfcLength);

        //生成节点
        for (int i = 0; i < sfcLength; i++) {
            Node node = sfc.createNode();
            sfc.nodeMap.put(node.getNodeId(), node);
            sfc.nodeList.add(node.getNodeId());
            /*
			 *为节点附加功能属性。具体要求如下：
			 *1、一条SFC上的没有相同的VNF
			 *2、VNF的范围表示为：[1, Configure.FUNCTION_RANGE*SFCLength+1) 
			 */
//			int function = (int) (Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);
//			while (!sfc.checkFunction(function)) {
//				//当前VNF的种类与之前的节点的VNF种类重复。重新生成
//				function = (int) (Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);
//			}
//			node.setFunctionDemand(function);
        }
        //生成链路需求，每两个节点之间就有一条需求链路，在生成的时候，需要特别注意保持节点的编号顺序。
        for (int i = 0; i < sfcLength + 1; i++) {
            Link link = sfc.createLink(i, i + 1);
            sfc.linkMap.put(link.getLinkId(), link);
            sfc.linkList.add(link.getLinkId());
        }
        //SFC持续时间的处理
        int sfcHold = Configure.createPoission();	//该条服务链持续时间，在采用的方法表示，当SFC执行到本条SFC的持续时间时，就回收该SFC所占用的资源
        sfc.setContinueTime(sfc.getSfcId() + sfcHold);
//        sfc.setVMContinueTime(sfc.getSfcId() + sfcHold);
        //SFC起始和结束的处理，
        int source = Configure.random.nextInt(graph.getVertexNum());
        int sink = Configure.random.nextInt(graph.getVertexNum());
        while (source == sink) {
            sink = Configure.random.nextInt(graph.getVertexNum());
        }

        sfc.setSourceId(source);
        sfc.setSinkId(sink);
        return sfc;
    }
    
    
    
    private CPath getBestDeployOfMiddleVNFByBWCost(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
		ArrayList<Integer> tempList = new ArrayList<Integer>();
		tempList.addAll(vertexList);
		initTheVertexInList(vertexList);
		graph.idVertexMap.get(source).setTotalBWCost(0);
		graph.idVertexMap.get(source).setPreviousVertexId(Configure.TERMINALNODE);
		
		Node node = sfc.nodeMap.get(vnf);	//需要部署的节点
		Link link = sfc.linkMap.get(vnf - 1);	//需要满足的链路需求
		int it = source;
		float minTotalBWCost;
		int minBWCostVertexID;
		
		//第一步：更新vertexList中所有节点到source的 `满足链路需求的`、最短时延，以及该最短路径的上一跳
		while (!tempList.isEmpty()) {
			CVertex vertex = graph.idVertexMap.get(it);
			for (Integer edgeKey : vertex.outsideEdgeList) {
				CEdge edge = graph.idEdgeMap.get(edgeKey);
				CEdge trueEdge = graph.idEdgeMap.get(edge.getEdgeId());
				if (trueEdge.getTotalBandwithResource() >= link.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
					CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
					if (vertex.getTotalBWCost() + edge.getTotalBandwithResource() < sinkVertex.getTotalBWCost()) {
						sinkVertex.setTotalBWCost(vertex.getTotalBWCost() + edge.getTotalBandwithResource());
						sinkVertex.setPreviousVertexId(it);
					}
				}
			}
			
			//从剩余的节点中，选出时延最小的点
			minTotalBWCost = Configure.INF;
			minBWCostVertexID = Configure.IMPOSSIBLENODE;
			for (Integer vertexKey : tempList) {
				CVertex tempVertex = graph.idVertexMap.get(vertexKey);
				if (minTotalBWCost > tempVertex.getTotalBWCost()) {
					minTotalBWCost = tempVertex.getTotalBWCost();
					minBWCostVertexID = tempVertex.getVeretexId();
				}
			}
			if (minTotalBWCost == Configure.INF) {
				//有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
//				System.out.println("找完所有节点，有几个节点都是没有更新数据，用break退出");
				break;
			}
			it = minBWCostVertexID;
			tempList.remove(tempList.indexOf(minBWCostVertexID));
		}
		
		//第2步，从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay延时最小`
		minTotalBWCost = Configure.INF;
		minBWCostVertexID = Configure.IMPOSSIBLENODE;
		for (Integer vertexKey : vertexList) {
			CVertex vertex = graph.idVertexMap.get(vertexKey);
			if (vertex.getVeretexId() != sink && vertex.getTotalComputeResource() >= node.getComputeResourceDemand() && minTotalBWCost > vertex.getTotalBWCost()) {
				minTotalBWCost = vertex.getTotalBWCost();
				minBWCostVertexID = vertexKey;
			}
		}
		
		//第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay延时最小` 的节点，说明不能部署这一节点，返回null
		if (minTotalBWCost == Configure.INF) {
//			System.out.println("在搜寻满足node需求的过程过程中，找不到时延最小的节点，用return null 退出");
			return null;
		}
		
		//第4步，找出满足：`满足node的资源需求，且节点的totalDelay延时最小` 的节点，构建这一条路径，返回路径
		LinkedList<Integer> pathList = new LinkedList<Integer>();
		CVertex vertex = graph.idVertexMap.get(minBWCostVertexID);
		while (vertex.getPreviousVertexId() != Configure.TERMINALNODE) {
			pathList.addFirst(vertex.getVeretexId());
			vertex = graph.idVertexMap.get(vertex.getPreviousVertexId());
		}
		pathList.addFirst(vertex.getVeretexId()); 	//把起始点加入路径
		CPath path = new CPath(pathList, graph,sfc);
		path.setVnfDeployPosition(minBWCostVertexID);
		return path;
	}
	   
    
	private CPath getBestDeployOfLastVNFByBWCost(ArrayList<Integer> vertexList, SFC sfc, int vnf, int source, int sink) {
		ArrayList<Integer> tempList = new ArrayList<Integer>();
		tempList.addAll(vertexList);
		initTheVertexInList(tempList);
		
		graph.idVertexMap.get(source).setTotalBWCost(0);
		graph.idVertexMap.get(source).setPreviousVertexId(Configure.TERMINALNODE);
		if (tempList.contains(source)) {
			tempList.remove(tempList.indexOf(source));
		}
		
		Node node = sfc.nodeMap.get(vnf);	//需要部署的功能
		Link firstLink = sfc.linkMap.get(vnf - 1);		//该功能前的虚拟链路
		Link secondeLink = sfc.linkMap.get(vnf);	//该功能后的虚拟链路
		
		int it = source;
		float minTotalBWCost;
		int minBWCostVertexID;
		//第一步：更新vertexList中所有节点到source的 `满足链路需求的`、最短时延，以及该最短路径的上一跳
		while (!tempList.isEmpty()) {
			CVertex vertex = graph.idVertexMap.get(it);
			for (Integer edgeKey : vertex.outsideEdgeList) {
				CEdge edge = graph.idEdgeMap.get(edgeKey);
				CEdge trueEdge = graph.idEdgeMap.get(edge.getEdgeId());
				if (trueEdge.getTotalBandwithResource() >= firstLink.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
					CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
					if (vertex.getTotalBWCost() + edge.getTotalBandwithResource() < sinkVertex.getTotalBWCost()) {
						sinkVertex.setTotalBWCost(vertex.getTotalBWCost() + edge.getTotalBandwithResource());
						sinkVertex.setPreviousVertexId(it);
					}
				}
			}
					
			//从剩余的节点中，选出时延最小的点
			minTotalBWCost = Configure.INF;
			minBWCostVertexID = Configure.IMPOSSIBLENODE;
			for (Integer vertexKey : tempList) {
				CVertex tempVertex = graph.idVertexMap.get(vertexKey);
				if (minTotalBWCost > tempVertex.getTotalBWCost()) {
					minTotalBWCost = tempVertex.getTotalBWCost();
					minBWCostVertexID = tempVertex.getVeretexId();
				}
			}
			if (minTotalBWCost == Configure.INF) {
				//有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
				break;
			}
			it = minBWCostVertexID;
			tempList.remove(tempList.indexOf(minBWCostVertexID));
		}
				
		//再处理 某点到sink最短时延可部署路径
		it = sink;
		tempList.clear();
		tempList.addAll(vertexList);
		graph.idVertexMap.get(sink).setTotalBWCostToSink(0);
		graph.idVertexMap.get(sink).setPreviousVertexIDToSink(Configure.TERMINALNODE);
		if (tempList.contains(sink)) {
			tempList.remove(tempList.indexOf(sink));
		}
				
		while (!tempList.isEmpty()) {
			CVertex vertex = graph.idVertexMap.get(it);
			for (Integer edgeKey : vertex.outsideEdgeList) {
				CEdge edge = graph.idEdgeMap.get(edgeKey);
				CEdge trueEdge = graph.idEdgeMap.get(edge.getEdgeId());
				if (trueEdge.getTotalBandwithResource() >= secondeLink.getBandWidthResourceDemand() && vertexList.contains(edge.getSinkId())) {
					CVertex sinkVertex = graph.idVertexMap.get(edge.getSinkId());
					if (vertex.getTotalBWCostToSink() + edge.getTotalBandwithResource() < sinkVertex.getTotalBWCostToSink()) {
						sinkVertex.setTotalBWCostToSink(vertex.getTotalBWCostToSink() + edge.getTotalBandwithResource());
						sinkVertex.setPreviousVertexIDToSink(it);
					}
				}
			}
					
			//从剩余的节点中，选出时延最小的点
			minTotalBWCost = Configure.INF;
			minBWCostVertexID = Configure.IMPOSSIBLENODE;
					
			for (Integer vertexKey : tempList) {
				CVertex tempVertex = graph.idVertexMap.get(vertexKey);
				if (minTotalBWCost > tempVertex.getTotalBWCostToSink()) {
					minTotalBWCost = tempVertex.getTotalBWCostToSink();
					minBWCostVertexID = tempVertex.getVeretexId();
				}
			}
			if (minTotalBWCost >= Configure.INF) {
				//有可能就是很多点都不满足资源约束，导致不能更新节点的totalDelay，此时表示已经找完，直接退出while循环即可
				break;
			}
			it = minBWCostVertexID;
			tempList.remove(tempList.indexOf(minBWCostVertexID));
		}
				
		//第2步：从剩余节点中，搜寻节点：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小的点`
		minTotalBWCost = Configure.INF;
		minBWCostVertexID = Configure.IMPOSSIBLENODE;
				
		for (Integer vertexkey : vertexList) {
			CVertex vertex = graph.idVertexMap.get(vertexkey);
			if (vertex.getTotalComputeResource() >= node.getComputeResourceDemand() && minTotalBWCost > vertex.getTotalBWCost() + vertex.getTotalBWCostToSink()) {
				minTotalBWCost = vertex.getTotalBWCost() + vertex.getTotalBWCostToSink();
				minBWCostVertexID = vertexkey;
			}
		}
				
		//第3步，如果不存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，说明不能部署这一节点，返回null
		if (minTotalBWCost >= Configure.INF) {
			return null;
		}
				
		//第4步：存在满足：`满足node的资源需求，且节点的totalDelay + totalDelayToSink的和最小` 的节点，能够部署，返回部署路径
		LinkedList<Integer> pathList = new LinkedList<Integer>();
		CVertex vertex = graph.idVertexMap.get(minBWCostVertexID);
		while (vertex.getPreviousVertexId() != Configure.TERMINALNODE) {
			pathList.addFirst(vertex.getVeretexId());
			vertex = graph.idVertexMap.get(vertex.getPreviousVertexId());
		}
		pathList.addFirst(vertex.getVeretexId()); 	//把起始点加入路径
				
		vertex = graph.idVertexMap.get(minBWCostVertexID);
		while (vertex.getPreviousVertexIDToSink() != Configure.TERMINALNODE) {
			if (pathList.getLast() != vertex.getVeretexId()) {
				pathList.addLast(vertex.getVeretexId());
			}
			vertex = graph.idVertexMap.get(vertex.getPreviousVertexIDToSink());
		}
		if (pathList.getLast() != vertex.getVeretexId()) {
			pathList.addLast(vertex.getVeretexId());
		}
		CPath path = new CPath(pathList, graph,sfc);
		path.setVnfDeployPosition(minBWCostVertexID);
		return path;
	}

	
}
