package Test;

import base.graph.CGraph;
import base.network.Network;
import traffic.SFCManager;

import java.io.IOException;

/**
 * Created by ly on 2017/3/16.
 */
public class TestFunction {
    public static void main(String[] args) throws IOException {
        CGraph graph = new CGraph();
        graph.createTopology(50, 0.7, 20);//生成拓扑图的节点数，两点间边的连接概率图的直径
//        Network network = new Network("./topology/linktest.txt", "./topology/nodetest.txt");
//        System.out.println("链路数目：" + network.getLinkNum());
//        System.out.println("节点数目：" +network.getNodeNum());
//
//        graph.initiate(network);

//        SFCManager sfcManager = new SFCManager(graph);
        /*SFC sfc = sfcManager.generateSFC(4);
        Plan plan = sfcManager.deploySFCByReliability(sfc, 20, 40);
        plan.printPlan(graph);
        sfc.setPlan(plan);
        float reliability = sfc.getDeployedReliability(graph);
        System.out.println("reliability: " + reliability);
        int resource = sfc.getDeployedResource(graph);
        System.out.println("resource: " + resource);*/

//        graph.recycleGraphResource();
//        sfcManager.testTheDeployOfSFC(graph);
//        sfcManager.staticDeployOfSFC(graph);
//        sfcManager.testReliabilityAndLoadBalance(graph);
//        sfcManager.testFixedSFC(graph);
//        sfcManager.testPath(graph);
//        sfcManager.testAdjustMethod(graph);
    }
}
