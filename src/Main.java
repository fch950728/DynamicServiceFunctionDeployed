//import base.ReliableRealize;
import base.graph.CEdge;
import base.graph.CGraph;
import base.network.Network;
import traffic.Plan;
import traffic.SFC;
import traffic.SFCManager;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        CGraph graph = new CGraph();

//        graph.createTopology(20, 0.7, 10);//20个节点，0.7的连接率，10的直径，底层网络的生成，生成一次之后，即可调用读入，如：下一行
        Network network = new Network("./topology/linkInfo.txt", "./topology/nodeInfo.txt");
//        System.out.println("链路数目：" + network.getLinkNum());
//        System.out.println("节点数目：" +network.getNodeNum());

        graph.initiate(network);
//        System.out.println("虚拟链路数目：" + graph.getEdgeNum());
//        System.out.println("虚拟节点数目：" + graph.getVertexNum());
//
        
        SFCManager sfcManager = new SFCManager(graph);   
//        sfcManager.generateSFCs(4, 3);//长度为8，10条
//       
////
//        for (Integer sfcKey : sfcManager.idSFCMap.keySet()) {
//            SFC sfc = sfcManager.idSFCMap.get(sfcKey);
//            sfc.printSFC();
//            System.out.println("------------------------");
//        }


//	    SFC sfc = sfcManager.generateSFC(8);//生成长度为4的一条sfc
//        System.out.println("SFC源点：" + sfc.getSourceId() + "  SFC目的点：" + sfc.getSinkId());
//        graph.recycleGraphResource();

        sfcManager.testDynamicDeploySFCofFunctionMaching1();
//        sfcManager.testDynamicDeploySFCofFunctionMaching2(graph);
//       Plan plan = sfcManager.vnfFunctionDeployList(sfc, 2, 7);  
       
//        plan.printPlan(graph);
//        sfc.deploySFC(graph);


        graph.recycleGraphResource();
        
//        sfcManager.testTheDeployOfSFC();
//        sfcManager.staticDeployOfSFC(graph);

//        ReliableRealize r = new ReliableRealize();
        /*List<Integer> criticalList = r.computeCriticalList1(network);
        System.out.println("关键节点集为：");
        for (int i : criticalList) {
            System.out.print(i + " -- ");
        }
        System.out.println();*/
//        r.testCriticalListWithF(graph);

//        System.out.println();
//        System.out.println("节点 100  的F因子为：" + graph.idVertexMap.get(100).getF());
//        System.out.println("节点 200  的F因子为：" + graph.idVertexMap.get(200).getF());
//        System.out.println("节点 300  的F因子为：" + graph.idVertexMap.get(300).getF());
//        System.out.println("节点 400  的F因子为：" + graph.idVertexMap.get(400).getF());
//        System.out.println();
//        System.out.println("节点326的剩余节点计算资源为：" + graph.idVertexMap.get(326).getRemainComputeResource()
//                    + "     总的计算资源为：" + graph.idVertexMap.get(326).getTotalComputeResource());
//        float sum = 0;
//        for(CEdge edge : graph.idVertexMap.get(326).adjEdgeList) {
//            sum += edge.getRemainBandWidthResource();
//        }
//        System.out.println("节点326剩余邻接带宽资源为：" + sum);
    }

}
