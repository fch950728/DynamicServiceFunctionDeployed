package base.graph;

import base.Configure;
import base.Pair;
import base.network.Link;
import base.network.Network;
import base.network.Node;
//import com.sun.xml.internal.ws.addressing.WsaTubeHelperImpl;
import traffic.SFCManager;

import java.io.*;
import java.util.*;

/**
 * 虚拟网络图
 * Created by ly on 2017/2/21.
 */
public class CGraph {

    private Network network;
    private int vertexNum;  //图中顶点数目
    private int edgeNum;    //图中边数目（无向边数目是有向边的两倍）


    public List<Integer> vertexList = new LinkedList<>();  //***顶点编号(编号从0开始)
    public List<Integer> edgeList = new LinkedList<>();   //虚拟边集合
    public Map<Integer, CEdge> idEdgeMap = new HashMap<>(); //边编号和边的对应Map集合
    public Map<Integer, CVertex> idVertexMap = new HashMap<>(); //节点编号与节点的对应Map集合

    //源、目的、边
    public Map<Integer, Map<Integer, Integer>> sourceSinkEdgeMap = new HashMap<>();	//源节点-宿节点对应边的集合，方便用于通过源目节点查询边



    public CGraph() {

    }

    /**
     * 初始化，建立物理网络与虚拟网络之间的映射（点与点映射，边与边映射）
     * @param network   物理网络的实例
     */
    public void initiate(Network network) {
        this.network = network;
        int tempSourceId = 0, tempSinkId = 0;
        int tempEdgeId = 0;
        float tempWeight = 0;

        vertexNum = network.getNodeNum();
        edgeNum = 2*network.getLinkNum();	//因为是无向链路

        for(int i = 0; i < vertexNum; i++) {
            //根据物理节点定义虚拟顶点，此时顶点编号等于节点编号，且顶点在顶点集合中的位置和编号一致
            CVertex vertex = new CVertex(i);	//同时初始化了network中关联节点的ID为i
//            vertex.setVertexWeight(0);  //设置顶点权重为0(先给个初值)
            vertexList.add(vertex.getVeretexId()); //顶点入顶点集合
            idVertexMap.put(i, vertex);
            Node node = network.nodeVector.get(i);
            vertex.setTotalComputeResource(node.getTotalNodeResource());
            vertex.setRemainComputeResource(node.getRemainNodeResource());
            vertex.setReliability(node.getReliability());
//            vertex.setFunctionkey(node.getFunctionkey());
        }
        for(int i = 0; i < network.getLinkNum(); i++) {
            Link link = network.linkVector.get(i);
            tempSourceId = link.getSourceId();
            tempSinkId = link.getSinkId();
            tempWeight=1;//边的初始权重1
            //tempWeight = network.linkVector.get(i).linkPrice;
            CEdge tempEdge1 = new CEdge(tempSourceId, tempSinkId, tempEdgeId, tempWeight);
            tempEdge1.setAssociateLinkId(i);	//对应于Network中的链路编号
            tempEdge1.setTotalBandwithResource(link.getBandwidth());
            tempEdge1.setRemainBandWidthResource(link.getBandwidth());
            tempEdge1.setDelay(link.getDelay());
            tempEdge1.setReliability(link.getReliability());

            idVertexMap.get(tempSourceId).getOutsideEdgeList().add(tempEdgeId);  //添加节点出度边集合
            idVertexMap.get(tempSinkId).getEntryEdgeList().add(tempEdgeId);  //添加节点入度边集合
            edgeList.add(tempEdgeId);
            idEdgeMap.put(tempEdgeId, tempEdge1);

            createSourceSink(tempEdge1);
            tempEdgeId++;

            CEdge tempEdge2 = new CEdge(tempSinkId, tempSourceId, tempEdgeId, tempWeight);
            tempEdge2.setAssociateLinkId(i);	//对应于Network中的链路编号
            tempEdge2.setTotalBandwithResource(link.getBandwidth());
            tempEdge2.setRemainBandWidthResource(link.getBandwidth());
            tempEdge2.setDelay(link.getDelay());
            tempEdge2.setReliability(link.getReliability());

            idVertexMap.get(tempSinkId).getOutsideEdgeList().add(tempEdgeId);
            idVertexMap.get(tempSourceId).getEntryEdgeList().add(tempEdgeId);
            edgeList.add(tempEdgeId);
            idEdgeMap.put(tempEdgeId, tempEdge2);

            createSourceSink(tempEdge2);
            tempEdgeId++;

            //添加各个点的邻接点集合
            idVertexMap.get(tempSourceId).adjVertexList.add(tempSinkId);
            idVertexMap.get(tempSinkId).adjVertexList.add(tempSourceId);
        }

        for (int edgeKey : idEdgeMap.keySet()) {
            CEdge edge = idEdgeMap.get(edgeKey);
            //为各个点添加邻接边集合
            idVertexMap.get(edge.getSourceId()).adjEdgeList.add(edge);
        }
    }

    //创建源、目、边ID对
    public void createSourceSink(CEdge edge) {
        if(sourceSinkEdgeMap.containsKey(edge.getSourceId())) {
            sourceSinkEdgeMap.get(edge.getSourceId()).put(edge.getSinkId(), edge.getEdgeId());
        } else {
            Map<Integer, Integer> sinkEdgeIdMap = new HashMap<>();
            sinkEdgeIdMap.put(edge.getSinkId(), edge.getEdgeId());
            sourceSinkEdgeMap.put(edge.getSourceId(), sinkEdgeIdMap);
        }
    }

    //回收图中所有资源
    public void recycleGraphResource() {
        recycleNodeResource();
        recycleLinkResource();
    }

    //回收整个图的节点计算资源
    public void recycleNodeResource() {
        for ( int vertexKey : idVertexMap.keySet()) {
            CVertex vertex = idVertexMap.get(vertexKey);
            vertex.setRemainComputeResource(vertex.getTotalComputeResource());
            vertex.sfcList.clear();     //清空图中每个顶点上部署的SFC集合
        }
    }

    public void recycleLinkResource() {
        for( int edgeKey : idEdgeMap.keySet()) {
            CEdge edge = idEdgeMap.get(edgeKey);
            edge.setRemainBandWidthResource(edge.getTotalBandwithResource());
        }
    }

    //计算图中平均每个点上部署的功能数
    public int avgVNFperVertex() {
        int sum = 0;
        for (int vertexKey : idVertexMap.keySet()) {
            CVertex vertex = idVertexMap.get(vertexKey);
            sum += vertex.sfcList.size();
        }
        return (int) (sum/vertexNum);
    }

    /**
    * 计算图中source到sink的可靠性最大的路径
    * */
    public CPath dijkstra(int source, int sink, Link link) {
        List<Integer> tempList = new ArrayList<>();
        tempList.addAll(vertexList);

        //初始化所有点到source的可靠性
        for(int vertexKey : tempList) {
            idVertexMap.get(vertexKey).setReliabilityToSource(Configure.UNINF);
            idVertexMap.get(vertexKey).setRb_previousVertexIdToSource(Configure.IMPOSSIBLENODE);
        }

        idVertexMap.get(source).setReliabilityToSource(idVertexMap.get(source).getReliability());   //初始化源点到源点的可靠性为其本身
        idVertexMap.get(source).setRb_previousVertexIdToSource(Configure.TERMINALNODE);

        int it = source;    //初始化迭代器表示当前迭代的顶点
        float maxReliability;
        int maxReliabilityId;

        while (!tempList.isEmpty()) {
            CVertex vertex = idVertexMap.get(it);
            for(int edgeKey : vertex.outsideEdgeList) {
                CEdge edge = idEdgeMap.get(edgeKey);
                //带宽资源以及可靠性满足的时候才更新
                if(edge.getRemainBandWidthResource() >= link.getBandWidthResourceDemand()) {
                    CVertex sinkVertex = idVertexMap.get(edge.getSinkId());
                    if(vertex.getReliabilityToSource()*edge.getReliability()*sinkVertex.getReliability() > sinkVertex.getReliabilityToSource()) {
                        sinkVertex.setReliabilityToSource(vertex.getReliabilityToSource()*edge.getReliability()*sinkVertex.getReliability());
                        sinkVertex.setRb_previousVertexIdToSource(it);
                    }
                }

            }

            //从剩余节点中找出可靠性最大的节点
            maxReliability = Configure.UNINF;
            maxReliabilityId = Configure.IMPOSSIBLENODE;
            for (int vertexKey : tempList) {
                if(maxReliability < idVertexMap.get(vertexKey).getReliabilityToSource()) {
                    maxReliability = idVertexMap.get(vertexKey).getReliabilityToSource();
                    maxReliabilityId = idVertexMap.get(vertexKey).getVeretexId();
                }
            }
            if(maxReliabilityId == Configure.IMPOSSIBLENODE) {
                return null;
            }
            it = maxReliabilityId;
            try {
                tempList.remove(tempList.indexOf(maxReliabilityId));
            } catch (Exception e) {
                System.out.println("error:" + maxReliabilityId);
                e.printStackTrace();
            }
        }

        LinkedList<Integer> pathList = new LinkedList<>();
        CVertex vertex = idVertexMap.get(sink); //从目的节点出发一直往回回溯
        while (vertex.getRb_previousVertexIdToSource() != Configure.TERMINALNODE) {
            pathList.addFirst(vertex.getVeretexId());
            vertex = idVertexMap.get(vertex.getRb_previousVertexIdToSource());
        }
        pathList.addFirst(vertex.getVeretexId());   //将最后一个结点加入集合中


        CPath path = new CPath(pathList, this);

        return path;
    }


    public int getVertexNum() {
        return vertexNum;
    }

    public void setVertexNum(int vertexNum) {
        this.vertexNum = vertexNum;
    }

    public int getEdgeNum() {
        return edgeNum;
    }

    public void setEdgeNum(int edgeNum) {
        this.edgeNum = edgeNum;
    }

    /**
     *
     * 生成拓扑图
     *http://blog.csdn.net/caad3/article/details/5399755
     * @param nodeNum	拓扑总的节点数目
     * @param linkPro	两点间连边的概率
     * @param graphDia	图的直径
     * @throws IOException
     */
    public void createTopology(int nodeNum, double linkPro, int graphDia) throws IOException {
        int totalNodeNum = nodeNum;
        double linkIndex = linkPro;
        int N = graphDia;
        int linkNum = 0;	//拓扑图中的链路数
        int nodeLinkNum = Configure.NODE_LINK_NUM; //每个节点的连接数，并不是最终连接数
        List<Integer> tmpList = new ArrayList<>();	//存放有链路的节点
        Map<Integer, Integer> tmpNodeMap = new HashMap<>();	//存放有链路的节点对

        File file1 = new File("./topology/linkInfo.txt");
//        File file1 = new File("./topology/linkInfo_big.txt");
        FileWriter fw1 = new FileWriter(file1);
        PrintWriter out1 = new PrintWriter(new BufferedWriter(fw1));

//        out1.println("linkNum	" + totalNodeNum*linkIndex);
//        out1.println("linkName	source	sink	bandwith");
//        out1.println("linkName	source	sink	cost	bandwdith	delay	reliability");
        out1.println("linkName	source	sink	cost	bandwdith	delay");
       
        File file2 = new File("./topology/nodeInfo.txt");
//        File file2 = new File("./topology/nodeInfo_big.txt");
        FileWriter fw2 = new FileWriter(file2);
        PrintWriter out2 = new PrintWriter(new BufferedWriter(fw2));

//        out2.println("nodeNum	" + totalNodeNum);
//        out2.println("nodeName	nodeCapacity");
//        out2.println("nodeName	isFacility	cost	nodeCapacity	location	reliability");
        out2.println("nodeName	isFacility	cost	nodeCapacity	location	on/off   functionkey");
        Random rand = new Random(47);

        //随机产生节点容量信息
        for(int j = 0; j < totalNodeNum; j++) {
            int nodeCapacity = rand.nextInt(10) + 5;	  //节点容量20~100
            
            
//            float reliability = (float) (Configure.random.nextFloat()*Configure.INTERVAL + Configure.MAXRB);
//            out2.println(j + "	" + 1 + "	" + 1 + "	" + nodeCapacity + "	" + 1 + "	" + reliability);
//            int functionkey = (int) (Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);
//          boolean onoroff=Configure.random.nextBoolean();
//          if (!onoroff)
//          	functionkey =0;
          out2.println(j + "	" + 1 + "	" + 1 + "	" + nodeCapacity + "	" + 1 + "	" );
        }
        out2.close();

        //生成拓扑图的链路
        for(int k = 0; k < totalNodeNum-1; k++) {
            int x = 0;	//与节点k相连的边数
            for(int m = k+1; m < totalNodeNum; m++) {
                if(!tmpNodeMap.isEmpty()) {      //两点之间有连线时将不再连线
                    if((tmpNodeMap.containsKey(k) && tmpNodeMap.get(k) == m) ||
                            (tmpNodeMap.containsKey(m) && tmpNodeMap.get(m) == k)) {    //解决空指针异常
                        continue;
                    }
                }
                // Waxman 1模型	//N：图的直径 http://blog.csdn.net/caad3/article/details/5399755
                int randNum = (int)(linkIndex*Math.pow(Math.E, -(m-k)/N)*1000);

                if(rand.nextInt(1000)%1000 < randNum) {
                    int tmpBandwidth = rand.nextInt(20)+ Configure.BANDWIDTH;	//带宽容量20~50
                    int delay = rand.nextInt(20)%10+1;     //延迟1~10

//                    float reliability = (float) (Configure.random.nextFloat()*Configure.INTERVAL + Configure.MAXRB);
                    out1.println(linkNum + "	" + k + "	" + m + "	" + 1 + "	" + tmpBandwidth +"	"+ delay);
//                  out1.println(linkNum + "	" + k + "	" + m + "	" + 1 + "	" + tmpBandwidth +"	"+ delay + "	"+ reliability);
                    tmpNodeMap.put(k, m);	//无向拓扑图
                    tmpNodeMap.put(m, k);
                    linkNum++;
                    x++;
                    if(!tmpList.contains(k)) {
                        tmpList.add(k);
                    }
                    if(!tmpList.contains(m)) {
                        tmpList.add(m);
                    }
                    if(x == nodeLinkNum) {
                        break;	//与节点k相连的节点有4（NodeLinkNum）个时就不再产生链路
                    }
                }
            }
        }

        //如果还有节点不与其它任何节点连接
        for(int p = 0; p < totalNodeNum; p++) {
            if(!tmpList.contains(p)) {
                int x = 0;
                int y = p-1;
                int z = p+1;
                for(int s = 0; s < 1000; s++,y--,z++) {	//节点p向前向后搜索可以与之连接的节点
                    //两点之间有连线时将不再连线
                    if((tmpNodeMap.containsKey(p) && tmpNodeMap.get(p) == y) ||
                            (tmpNodeMap.containsKey(y) && tmpNodeMap.get(y) == p)) {    //解决空指针异常
                        continue;
                    }
                    int randNum1 = (int)(linkIndex*Math.pow(Math.E, -(p-y)/N)*1000);

                    if(rand.nextInt(1000)%1000 < randNum1) {
                        int tmpBandwidth = rand.nextInt(20)%20 + Configure.BANDWIDTH;	//带宽容量20~50
                        int delay = rand.nextInt(20)%10+1;    //延迟1~10

                       
//                         float reliability = (float) (Configure.random.nextFloat()*Configure.INTERVAL + Configure.MAXRB);
//                         out1.println(linkNum + "	" + p + "	" + y + "	" + 1 + "	" + tmpBandwidth +"	"+ delay + "	"+ reliability);
//                        int functionkey = (int) (Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);
//                        boolean onoroff=Configure.random.nextBoolean();
//                        if (!onoroff)
//                        	functionkey =0;
                        out1.println(linkNum + "	" + p + "	" + y + "	" + 1 + "	" + tmpBandwidth +"	"+ delay);
                        tmpNodeMap.put(p, y);	//无向拓扑图
                        tmpNodeMap.put(y, p);
                        linkNum++;
                        x++;
                        if(!tmpList.contains(p)) {
                            tmpList.add(p);
                        }
                        if(!tmpList.contains(y)) {
                            tmpList.add(y);
                        }
                    }
                    if(x == nodeLinkNum) {
                        break;	//与节点k相连的节点有4（NodeLinkNum）个时就不再产生链路
                    }


                    //两点之间有连线时将不再连线
                    if((tmpNodeMap.containsKey(z) && tmpNodeMap.get(z) == p) ||
                            (tmpNodeMap.containsKey(p) && tmpNodeMap.get(p) == z)) {    //解决空指针异常
                        continue;
                    }
                    int randNum2 = (int)(linkIndex*Math.pow(Math.E, -(z-p)/N)*1000);

                    if(rand.nextInt(1000)%1000 < randNum2) {
                        int tmpBandwidth = rand.nextInt(20)%20 + Configure.BANDWIDTH;	//带宽容量20~50
                        int delay = rand.nextInt(20)%10+1;     //延迟1~10

//                        float reliability = (float) (Configure.random.nextFloat()*Configure.INTERVAL + Configure.MAXRB);
//                        out1.println(linkNum + "	" + p + "	" + z + "	" + 1 + "	" + tmpBandwidth +"	"+ delay + "	"+ reliability);
//                        int functionkey = (int) (Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);
//                        boolean onoroff = Configure.random.nextBoolean();
//                        if (!onoroff)
//                        	functionkey =0;
                        out1.println(linkNum + "	" + p + "	" + z + "	" + 1 + "	" + tmpBandwidth +"	"+ delay);
                        tmpNodeMap.put(p, z);	//无向拓扑图
                        tmpNodeMap.put(z, p);
                        linkNum++;
                        x++;
                        if(!tmpList.contains(p)) {
                            tmpList.add(p);
                        }
                        if(!tmpList.contains(z)) {
                            tmpList.add(z);
                        }
                    }
                    if(x == nodeLinkNum) {
                        break;	//与节点k相连的节点有4（NodeLinkNum）个时就不再产生链路
                    }
                }
            }
        }

        //给最后的M个节点增加冗余度
        for(int lastNum = totalNodeNum-Configure.REDUNDANCY_NODE; lastNum < totalNodeNum; lastNum++) {	//最后Configure.REDUNDANCY_NODE个节点
            for(int t = lastNum-1; t >= 0; t--) {	//遍历这Configure.REDUNDANCY_NODE个节点之前的所有节点

                //两点之间有连线时将不再连线
                if((tmpNodeMap.containsKey(lastNum) && tmpNodeMap.get(lastNum) == t) ||
                        (tmpNodeMap.containsKey(t) && tmpNodeMap.get(t) == lastNum)) {    //解决空指针异常
                    continue;
                }
                int randNum = (int)(linkIndex*Math.pow(Math.E, -(lastNum-t)/N)*1000);

                if(rand.nextInt(1000)%1000 < randNum) {
                    int tmpBandwidth = rand.nextInt(20)%20 + Configure.BANDWIDTH;	//带宽容量20~50
                    int delay = rand.nextInt(20)%10+1;     //延迟1~10

//                    float reliability = (float) (Configure.random.nextFloat()*Configure.INTERVAL + Configure.MAXRB);
//                    out1.println(linkNum + "	" + lastNum + "	" + t + "	" + 1 + "	" + tmpBandwidth +"	"+ delay + "	"+ reliability);
//                    int functionkey = (int) (Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC);
//                    boolean onoroff=Configure.random.nextBoolean();
//                    if (!onoroff)
//                    	functionkey =0;
                    out1.println(linkNum + "	" + lastNum + "	" + t + "	" + 1 + "	" + tmpBandwidth +"	"+ delay);
                    tmpNodeMap.put(lastNum, t);	//无向拓扑图
                    tmpNodeMap.put(t, lastNum);
                    linkNum++;
                    if(!tmpList.contains(lastNum)) {
                        tmpList.add(lastNum);
                    }
                    if(!tmpList.contains(t)) {
                        tmpList.add(t);
                    }
                    break;	//相当于只产生一条冗余连接
                }
            }
        }
        out1.close();
    }




}
