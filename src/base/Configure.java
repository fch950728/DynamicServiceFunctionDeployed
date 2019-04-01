/*
 * 配置信息类，用于存放仿真中可能用到的参数，方便调试
 */
package base;

/*import base.graph.CPath;*/

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Configure {

	public static float INF = (float) 10000000000.0;	//定义十亿为无限大
	public static int IMPOSSIBLENODE = 1000000000;	//定义一亿为不可能节点
	public static int TERMINALNODE = -1;	//定义-1为终点节点，用于求最短路时，表示到达终点节点 
	public static int INTRA_EDGE = 0;	// 域内链路
	public static int INTER_EDGE = 1;	// 域间链路
	public static long RANDOM_INIT_SEED = 47;	//生成随机数的初始seed
	public static int FUNCTION_SEED = 3;	//随机生成的功能数
	public static Random random = new Random(RANDOM_INIT_SEED);
//	public static int SFC_AMOUNT = 10000;	//产生SFC的条数	
	public static int POISSON_LAMDA = 4;	//泊松分布的lamda参数
	public static int DOMAIN_ONLYONE_VERTEX = 1;	//表示，路径经过的域只有一个边界点在该路径上
	public static int DOMAIN_NOT_ONLYONE_VERTEX = 0;	//表示，路径经过的域不止一个边界点在路径上
	public static int PATH_MAX_NUM = 20;		//在找路时，表示找到条数
	
	public static int SFC_LENGTH_MIN = 5;		//SFC最小长度
	public static int SFC_LENGTH_MAX = 14;		//SFC最大长度
	public static int SFCNUM =10000;	//网络图中产生的SFC数目
	
	public static float ON_ENERGY = (float)5;		//VM开机过程消耗能量
	public static float OFF_ENERGY = (float)3;		//VM关机过程能量消耗
	public static float ON_DELAY = (float)1;		//VM开机时延：120s
	public static float OFF_DELAY = (float)1;		//VM关机时延
	public static float EMPTY_ENERGY = (float)1;	//节点单位时间空载耗能
	public static float WORK_ENERGY = (float)2;		//节点单位时间工作耗能
	public static float EMPTY_DELAY = (float)1;		//
	public static float WORK_DELAY = (float)1;		//
	public static int VM_timeoffDELAY = (int)1; //VM延迟关闭的时间，不能为1
	public static int DISTEN = 2;//能够容忍的最多跳数（链路的条数）
	
	public static String DATA_SAVE_DIRECTORY = "/home/liyayu/code/eclipse_workplace/Multi-domainNRA/data";

	public static int DURING = (int) 9;	//产生节点功能性序号区间
	public static int MINFUNC = (int) 1;	//产生节点功能性最小序号		random（9）+1	区间[1, 10]   ：(Configure.random.nextInt(Configure.DURING) + Configure.MINFUNC)
//	public static float INTERVAL = (float) 0.002;	//产生节点与链路的可靠性区间
//	public static float MAXRB = (float)0.997;	//产生节点与链路最小的可靠性		random*0.004+0.995	区间[0.995, 0.999]
	public static int NODE_LINK_NUM = 7;	//每个节点的连接数，但不是最终的连接数
	public static int REDUNDANCY_NODE = 10;	//给最后该数量的节点增加冗余度
	public static float ALPHA = 1;		//用于表示负载均衡中的 （∑B + C）前面的系数
	public static float BETA = 1;	//用于表示负载均衡中的 C 前的系数
	public static int CRITICAL_NUM = 30;	//关键点集合个数

	public static float ALPHA_LB = 1;	//根据负载均衡部署SFC的系数
	public static float BETA_LB = 1;

	public static int BANDWIDTH = 30;	//设定带宽范围为BANDWIDTH~BANDWIDTH+30

	public static float REQUEST_RELIABILITY = (float) 0.9;	//设置用户需求可靠性
	public static float UNINF = (float) -10000000000.0;		//设置-10亿
	
	
	

	/*
	 * 功能：产生泊松分布的随机数，用于指定SFC业务的到达，参数是上面定义的POISSON_LAMDA
	 * 参考的是：Knuth的算法思路。具体参看维基百科
	 * https://en.wikipedia.org/wiki/Poisson_distribution
	 */
	public static int createPoission() {
		int k = 0;
		double p = 1.0, L = Math.exp(-POISSON_LAMDA);
		double u;
		do {
			k++;
			u = random.nextDouble();
			p = p*u;
		} while(p > L);
		return k - 1;
	}
	
	public static void testPossion() {
		List<Integer> poissionList = new ArrayList<Integer>();
		for (int i = 0; i < 100; i++) {
			poissionList.add(Configure.createPoission());
		}
		int sum = 0;
		for (Integer key : poissionList) {
			System.out.print(key + " -> ");
			sum += key;
		}
		System.out.println("泊松分布的均值为：");
		double average = (double)sum / poissionList.size();
		System.out.println("均值 = " + average);
		double variance = 0.0;
		for (Integer key : poissionList) {
			variance += (key - average) * (key - average);
		}
		System.out.println("方差 = " + (double)variance / poissionList.size());
	}
	
	public static boolean intToBool(int value) {
		if (value == 0) {
			return false;
		}
		return true;
	}
	
	/*public static Comparator<CPath> comparator = new Comparator<CPath>() {

		@Override
		public int compare(CPath o1, CPath o2) {
			// TODO Auto-generated method stub
			if (o2.getPathDelay() - o1.getPathDelay() > 0) {
				return -1;
			} else if (o2.getPathDelay() - o1.getPathDelay() < 0) {
				return 1;
			} else {
				return 0;
			}
		}
	};*/
}
