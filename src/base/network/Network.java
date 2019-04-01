package base.network;

import base.Configure;

import java.io.*;
import java.util.*;

/**
 * 物理网络   底层物理
 * Created by ly on 2017/2/28.
 */
public class Network implements Serializable, Cloneable{

    private int nodeNum;    //网络中节点数目
    private int linkNum;    //网络中链路数目

    private String linkInfoFile;    //链路基本信息文件
    private String nodeInfoFile;    //节点基本信息文件

    public List<Link> linkVector = new ArrayList<>();    //物理网络中的链路集合
    public List<Node> nodeVector = new ArrayList<>();   //物理网络中的节点集合

    public List<Integer> nodeList = new LinkedList<>(); //在计算失败节点集时使用

    public Map<Integer, Link> idLinkMap = new HashMap<>();  //链路ID--->链路
    public Map<Integer, Node> idNodeMap = new HashMap<>();  //节点ID--->节点

    public Network() {

    }

    private class TempLink
    {
    	int last;
    	int next;
    	int linkName;
    	int source;
    	int sink;
    	int cost;
    	int bandwdith;
    	int delay;
    }
//    public Network(Network network) {
//        nodeNum = network.nodeNum;
//        linkNum = network.linkNum;
          // ....
//    }

    public Network(String linkInfoFile, String nodeInfoFile) {
        this.linkInfoFile = linkInfoFile;
        this.nodeInfoFile = nodeInfoFile;
        //初始化整个网络的节点以及链路信息
        initiateNodeInfo();
        initiateLinkInfo();
        FillAdjList();
    }

    /**
     * 初始化节点基本信息
     */
    public void initiateNodeInfo() {
        File file = new File(nodeInfoFile);
        try {
            Scanner scan = new Scanner(file);
            if(scan.hasNextLine()) {
                scan.nextLine();    //省略文件中的第一行（表头信息行）
            }

            while (scan.hasNextLine()) {
                Node node = new Node();
                node.setNodeId(scan.nextInt());
                scan.next();    //跳过文件中的IsFacility列
                node.setUnitCost(scan.nextInt());
                int resource = scan.nextInt();  //节点总的计算资源
                node.setTotalNodeResource(resource);
                node.setRemainNodeResource(resource);
//                node.setTotalNodeResource((int)Configure.INF);  //对比无限资源下的部署开销
//                node.setRemainNodeResource((int)Configure.INF);
                scan.next();    //跳过文件中的location列
                /*scan.nextFloat();   //对比无限资源情况下结合考虑负载均衡的结果
                node.setReliability(1);*/
//                node.setReliability(scan.nextFloat());
//                node.setOnoroff(scan.nextBoolean());
//                node.setFunctionkey(scan.nextInt());
                scan.nextLine();
                nodeVector.add(node);
                idNodeMap.put(node.getNodeId(), node);
                nodeList.add(node.getNodeId());
            }
            setNodeNum(nodeVector.size());
            if(scan != null) {
                scan.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println("节点基本信息文件未找到！");
            e.printStackTrace();
        }
    }

    public void initiateLinkInfo() {
        File file = new File(linkInfoFile);
//        int linknum = -1;
//        int startlink = 0;
//        TempLink templink[] = new TempLink[400];
        
        try {
            Scanner scan = new Scanner(file);
            if(scan.hasNextLine()) {
                scan.nextLine();    //跳过表头第一行信息
            }
            
            while(scan.hasNextLine()) {
                Link link = new Link();
                link.setLinkId(scan.nextInt());
                link.setSourceId(scan.nextInt());
                link.setSinkId(scan.nextInt());
                if (link.getSourceId() > link.getSinkId())
                {
                	scan.nextLine();
                	continue;
                }
                link.setUnitCost(scan.nextInt());
                link.setBandwidth(scan.nextInt());
//                scan.nextInt(); //对比无限资源的情况
//                link.setBandwidth((int)Configure.INF);
                link.setDelay(scan.nextInt());
                /*scan.nextFloat();   //对比无限资源情况下结合考虑负载均衡的结果
                link.setReliability(1);*/
//                link.setReliability(scan.nextFloat());
                scan.nextLine();
                
//            	linknum ++;
//            	templink[linknum] = new TempLink();
//            	templink[linknum].last = -1;
//            	templink[linknum].next = -1;
//            	templink[linknum].linkName = scan.nextInt();
//            	templink[linknum].source = scan.nextInt();
//            	templink[linknum].sink = scan.nextInt();
//            	templink[linknum].cost = scan.nextInt();
//            	templink[linknum].bandwdith = scan.nextInt();
//            	templink[linknum].delay = scan.nextInt();
//            	
//            	if (linknum == 0)
//            		continue;
//            	
//            	int templast = 0;
//            	int tempnext = startlink;
//            	while(templink[tempnext].source < templink[linknum].source)
//            	{
//            		templast = tempnext;
//            		tempnext = templink[templast].next;
//            		if (tempnext == -1)
//            			break;
//            	}
//            	
//            	if (tempnext != -1)
//            	{
//            		while(templink[tempnext].sink < templink[linknum].sink)
//            		{
//            			templast = tempnext;
//            			tempnext = templink[templast].next;
//            			if (tempnext == -1)
//            				break;
//            		}
//            		
//            		if (tempnext == -1)
//            		{
//            			templink[templast].next = linknum;
//            			templink[linknum].last = templast;
//            		}
//            		else
//            		{
//            			if (tempnext != startlink)
//            			{	
//            				templink[templast].next = linknum;
//               				templink[linknum].last = templast;
//            				templink[tempnext].last = linknum;
//            				templink[linknum].next = tempnext;
//            			}
//            			else
//            			{
//            				templink[tempnext].last = linknum;
//            				templink[linknum].next = tempnext;
//            				startlink = linknum;
//            			}
//            		}
//            	}
//            	else
//            	{
//            		templink[templast].next = linknum;
//            		templink[linknum].last = templast;
//            	}
//            	
//            	scan.nextLine();
//            }
//            
//            int nowlink = startlink;
//            for (int i = 0; i <= linknum ; i++)
//            {
//            	Link link = new Link();
//                link.setLinkId(i);
//                link.setSourceId(templink[nowlink].source);
//                link.setSinkId(templink[nowlink].sink);
//                link.setUnitCost(templink[nowlink].cost);
//                link.setBandwidth(templink[nowlink].bandwdith);
////                scan.nextInt(); //对比无限资源的情况
////                link.setBandwidth((int)Configure.INF);
//                link.setDelay(templink[nowlink].delay);
//                nowlink = templink[nowlink].next;
//                /*scan.nextFloat();   //对比无限资源情况下结合考虑负载均衡的结果
//                link.setReliability(1);*/
//                link.setReliability(scan.nextFloat());
                
                linkVector.add(link);
                idLinkMap.put(link.getLinkId(), link);
            }
            
            setLinkNum(linkVector.size());
            if(scan != null) {
                scan.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println("链路基本信息文件未找到！");
            e.printStackTrace();
        }
    }

    /**
     * 填充物理节点的邻接点和边集合，用于后面计算节点失败后对其他点的影响
     */
    public void FillAdjList() {
        for (int linkKey : idLinkMap.keySet()) {
            Link link = idLinkMap.get(linkKey);
            idNodeMap.get(link.getSourceId()).adjNodeList.add(link.getSinkId());    //设置链路目的点加入源点邻接点集合
            idNodeMap.get(link.getSourceId()).adjLinkList.add(linkKey); //设置链路ID加入链路源点的邻接边集合
            idNodeMap.get(link.getSinkId()).adjNodeList.add(link.getSourceId());
            idNodeMap.get(link.getSinkId()).adjLinkList.add(linkKey);
        }
    }

    public int getNodeNum() {
        return nodeNum;
    }

    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public int getLinkNum() {
        return linkNum;
    }

    public void setLinkNum(int linkNum) {
        this.linkNum = linkNum;
    }

    /**
     * 实现Network深拷贝
     *      计算关键节点集时用到
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Network copy() throws IOException, ClassNotFoundException {
        Network s = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.flush();
        oos.close();

        byte[] arrByte = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(arrByte);
        ObjectInputStream ois = new ObjectInputStream(bais);
        s = (Network) ois.readObject();
        ois.close();
        return s;
    }
}
