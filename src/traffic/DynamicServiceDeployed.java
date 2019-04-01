package traffic;


//import lpsolve.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.sound.midi.MidiDevice.Info;

import java.lang.Math;

import com.mathworks.toolbox.javabuilder.MWArray;  
import com.mathworks.toolbox.javabuilder.MWClassID;  
import com.mathworks.toolbox.javabuilder.MWComplexity;  
import com.mathworks.toolbox.javabuilder.MWNumericArray;  


import java.beans.beancontext.BeanContextSupport;
import java.util.ArrayList;

import optimator.Optimator;
import intoptimator.Intoptimator;
import base.graph.CEdge;
import base.graph.CGraph;
import base.network.Network;
import base.Configure;
import traffic.Plan;
import traffic.SFC;
import traffic.SFCManager;

import java.sql.SQLOutput;
import java.util.List;
import java.io.*;

public class DynamicServiceDeployed {
	
	public static SFCManager.Information info;
	public  DynamicServiceDeployed(SFCManager.Information info) {
        this.info = info;
    }
	
	static private class Check // 用于记录重新部署的SFC信息 
	{
		public int changeSFC;// 改变的SFC序号
		public int changeSource;// 改变后的SFC起点
		public int changeSink;// 改变后的SFC终点
		
		public int changeSFCITresource;// 改变的SFC资源占用量
		public int changeLinkBandwidth;// 改变的link资源占用量
		public int changeVNFnum;// 改变的SFC中包含的VNF个数
		public int changeLinkusingnum;// 改变后的SFC部署使用了多少链路
		
		public int[] changeVNFdeployon;// 改变后的VNF部署情况
		public int[] changeLinkusing;// 改变后的链路使用情况
		public double[] changeLinkusingcapacity;// 改变后使用了链路的带宽量
	}
	
	static private class Temppath // ���ڱ�����ʱ��·��Ԫ��
	{
		public double len;
		public int[] node;
		public int hop;
	}
	static private class Topologic
	{
		int Nodenum;// �ڵ�����
		int Linknum;// ��·����
		
		int[] Start;// ��·���
		int[] End;// ��·�յ�
		
		double[] Cap;// �ڵ���Դ���� IT resource capacity
		double[] Ban;// ��·��������Bandwidth resource capacity
		
		int[] Vd;// DC�Ľڵ� ��
		int[] Vs;// Switch�Ľڵ��
		
		double rho1;//   
		double rho2;//
	}
	
	static private class Graph
	{
		
		int Nodenum;// ͼ�Ľڵ�����
		
		public Graph(int nodenum)
		{
			Nodenum = nodenum;
		}
		
	
		int Linknum;// ͼ����·����
		
		double[][] Weight;// ͼ�нڵ���ڵ����·Ȩ�� 
		double[][] FakeWeight;// ��·Ȩ�ر���
		
		double[][] Shortestpath;// ����֮������·
		double[][][] ShortestKpath = new double[Nodenum+1][Nodenum+1][];// ����֮��ĵ�k�̵�··������ ����Ϊ i,j,k
		int[][][][] ShortestKPathNode = new int[Nodenum+1][Nodenum+1][][];// ��K�̵�··�������ĵ� ����Ϊ i,j,k,node
		
		int[][][][] AlternativeRoad = new int[Nodenum+1][Nodenum+1][][];// ��ѡ·���ڵ� i,j,alternativenum,number
		double[][][] AlternativeLength = new double[Nodenum+1][Nodenum+1][];//  ��ѡ·������
		int AlternativeNum[][] = new int[Nodenum+1][Nodenum+1];// ��ѡ·������
		boolean AlternativePath[][][] = new boolean[Nodenum+1][Nodenum+1][];// ���ĳ����ѡ·���Ƿ����
		
		public void InitialtheWeight()// ��ʼ��·��Ȩ��
		{
			int i;
			int j;
			
			Weight = new double[this.Nodenum+1][this.Nodenum+1];
			Shortestpath = new double[this.Nodenum+1][this.Nodenum+1];
			for (i = 0; i <= this.Nodenum; i++)// ��ʼ������
			{
				for (j = 0; j <= this.Nodenum; j++)
				{
					if (i == j )
					{
						this.Weight[i][j] = 0;
						this.Shortestpath[i][j] = 0;
//						System.out.println('#');
					}
					else
					{
						this.Weight[i][j] = 100000;
						this.Shortestpath[i][j] = 100000;
//						System.out.println('#');
					}
				}
			}
		}
		
		// ����Ȩ��
		public void RejudgeTheWeight(double[][] a)
		{
			for (int i = this.Nodenum; i >= 0 ; i--)
				for (int j = this.Nodenum; j >=0 ; j--)
				{
					if((i == 0)||(j == 0))
					{
						this.Weight[i][j] = 0;
//						System.out.println('#');
					}
					else
					{
						this.Weight[i][j] = a[i-1][j-1];
//						System.out.println('#');
					}
				}
			
			FakeWeight = new double[Nodenum+1][Nodenum+1];
			for (int m = 1; m <= Nodenum; m++)
				for ( int n = 1; n <= Nodenum; n++)
					FakeWeight[m][n] = Weight[m][n];// �ָ�ԭ��Ȩ��
		}
		
		// ��ʼdijkstra�㷨�����ڵ�һ�μ���ͼ�����нڵ�����·����������ڱ�ѡ������
		public void InitDijkstra(int a, int b)
		{ 
			// ������·
			boolean[] OpenMarked = new boolean[Nodenum+1];// �ڵ��Ƿ��Ѿ�������Open����
		 

			 
			for (int i = 1; i <= Nodenum; i++)
				for (int j = 1; j <= Nodenum; j++)
					 Shortestpath[i][j] = Weight[i][j];
			
//			 for (int i = 1; i <= Nodenum; i++)
//			 {
				int i = a;
				 int prenode[] = new int[Nodenum+1]; // ǰ���ڵ�
				 
				 // ��ʼ��
				 for (int j = 1; j <= Nodenum; j++)
				 {
					 prenode[j] = j;
					 OpenMarked[j] = false;
				 }
//				 System.out.println("step 1 ready");
				
				 OpenMarked[i] = true;
				 
				 boolean temp;
				 while (true)
				 {
					 double min = 1000000;// ��̾���
					 int nearest = 0;// ����ڵ�
					 temp = false;// ��ʱ���
					 
					 for (int j = 1; j <= Nodenum; j++)
					 {
						 if (OpenMarked[j] != true)
						 {		
							 temp = true;
							 if (Shortestpath[i][j] < min)
							 {
								 min = Shortestpath[i][j];
								 nearest = j;
							 }
//							 if (Shortestpath[i][j] > 90000)
//							 {
//								 OpenMarked[j] = true;
//							 }
						 }
//						 System.out.println("step 2.1."+j+" ready");
					 }
//					 System.out.println(nearest);
					 if (temp == false) break;
					 
					 OpenMarked[nearest] = true;
					 for (int j = 1; j <= Nodenum; j++)
					 {
						 if (Shortestpath[i][j] > min + Shortestpath[nearest][j])
						 {
							 Shortestpath[i][j] = min + Shortestpath[nearest][j];
							 prenode[j] = nearest;
						 }
						 else if (Shortestpath[i][j] == min + Shortestpath[nearest][j])
						 {
							 if (prenode[j] == j)
								 prenode[j] = i;// �����·����û��Ѱ�������νڵ㣬�������Ϊ�����νڵ�
						 }
					 }
//					 System.out.println(Weight[1][3]);
					 
				 }
				 
//				 System.out.println(Shortestpath);
				 
				 // �������·���
				 for (int j = 1; j <= Nodenum; j++ )
				 {
					 Shortestpath[j][i] = Shortestpath[i][j];
				 }
				 
				// �����·������뵽��ѡ·����
					 
				 for (int j = i; j <= Nodenum; j++)
				 {
					 if (j == b) {
					 if (prenode[j] != j)
					 {
//						 if ((i == 1) && (j == 2))
//						 	System.out.println(AlternativeNum[1][2]);
						 
						 if (AlternativeNum[i][j] > 0)
						 {
							 temp = true;
//							 if ((i == 1) && ( j == 2))
//							 {
//								 System.out.println(temp);
//							 }
							 for (int m = 1; m <= AlternativeNum[i][j]; m++)
							 {
								 if(Shortestpath[i][j] == AlternativeLength[i][j][m])
								 {
									 temp = false;
									 int n = AlternativeRoad[i][j][m].length-1;// ��ʱ����
									 int tempnode = j;
									 
									 while ((n != 0) && (tempnode != i))
									 {
										 if (AlternativeRoad[i][j][m][n] != tempnode)
										 {
											 temp = true;
											 break;
										 }
										 tempnode = prenode[tempnode];
										 n--;
									 }
									 
									 if ((n > 0) && (temp == false))
									 {
										 if(AlternativeRoad[i][j][m][n] != tempnode)
											 temp = true;
									 }
									 
									 if (temp == true) break;
								 }
							 }
							 
							 
							 if (temp == true)
							 {
								int s = ++AlternativeNum[i][j] ;
								AlternativeLength[i][j][s] = Shortestpath[i][j];
								int n = 1;
								int tempnode = j;
								do
								{
									AlternativeRoad[j][i][s][n] = tempnode;
									tempnode = prenode[tempnode];
									n++;
								}while (tempnode != i);
								AlternativeRoad[j][i][s][n] = i;
										
								int l = 1;//��ʱ����		 
								do 
								{
									AlternativeRoad[i][j][s][l] = AlternativeRoad[j][i][s][n];
									n--;
									l++;
								}
								while(n > 0);
								
								AlternativePath[i][j][s] = false;
								AlternativePath[j][i][s] = false;
							 }							 
						 }
						 else
						 {
							int s =  ++AlternativeNum[i][j] ;
							AlternativeLength[i][j][s] = Shortestpath[i][j];
							int n = 1;
							int tempnode = j;
							do
							{
								AlternativeRoad[j][i][s][n] = tempnode;
								tempnode = prenode[tempnode];
								n++;
//								System.out.println(tempnode);
							}while (tempnode != i);
							AlternativeRoad[j][i][s][n] = i;
										
							int l = 1;//��ʱ����		 
							do 
							{
								AlternativeLength[j][i][s] = AlternativeLength[i][j][s];
								AlternativeNum[j][i] = AlternativeNum[i][j];
								AlternativeRoad[i][j][s][l] = AlternativeRoad[j][i][s][n];
								n--;
								l++;
							}
							while(n > 0);
							AlternativePath[i][j][AlternativeNum[i][j]] = false;
							AlternativePath[j][i][AlternativeNum[j][i]] = false;
						 }
						 
					 }
				 }
				 
			 }
			 
		}
		
		public Temppath Dijkstra(int a, int b)
		{ 
			// ������·
			boolean[] OpenMarked = new boolean[Nodenum+1];// �ڵ��Ƿ��Ѿ�������Open����
		 

			 
			for (int i = 1; i <= Nodenum; i++)
				for (int j = 1; j <= Nodenum; j++)
					 Shortestpath[i][j] = Weight[i][j];
			
//			 for (int i = 1; i <= Nodenum; i++)
//			 {
				int i = a;
				int prenode[] = new int[Nodenum+1]; // ǰ���ڵ�
				 
				 // ��ʼ��
				for (int j = 1; j <= Nodenum; j++)
				{
					prenode[j] = j;
					OpenMarked[j] = false;
				}
//				 System.out.println("step 1 ready");
				
				 OpenMarked[i] = true;
				 
				 boolean temp;
				 while (true)
				 {
					 double min = 1000000;// ��̾���
					 int nearest = 0;// ����ڵ�
					 temp = false;// ��ʱ���
					 
					 for (int j = 1; j <= Nodenum; j++)
					 {
						 if (OpenMarked[j] != true)
						 {		
							 temp = true;
							 if (Shortestpath[i][j] < min)
							 {
								 min = Shortestpath[i][j];
								 nearest = j;
							 }
//							 if (Shortestpath[i][j] > 90000)
//							 {
//								 OpenMarked[j] = true;
//							 }
						 }
//						 System.out.println("step 2.1."+j+" ready");
					 }
//					 System.out.println(nearest);
					 if (temp == false) break;
					 
					 OpenMarked[nearest] = true;
					 for (int j = 1; j <= Nodenum; j++)
					 {
						 if (Shortestpath[i][j] > min + Shortestpath[nearest][j])
						 {
							 Shortestpath[i][j] = min + Shortestpath[nearest][j];
							 prenode[j] = nearest;
						 }
						 else if (Shortestpath[i][j] == min + Shortestpath[nearest][j])
						 {
							 if (prenode[j] == j)
								 prenode[j] = i;// �����·����û��Ѱ�������νڵ㣬�������Ϊ�����νڵ�
						 }
					 }
				 }
				 
				 Temppath temppath = new Temppath();
				 temppath.len = 0;
				 temppath.hop = 0;
				 temppath.node = new int[Nodenum + 1];
				 for (int j = 1; j <= Nodenum; j ++)
				 {
					 if ( j == b)
					 {
						 if (Shortestpath[i][j] >= 100000)
						 {
							 temppath.len = -1;
							 break;
						 }
						 temppath.len = Shortestpath[i][j];
						 int tempnode = j;
						
						 temppath.hop = 1;
						 while (tempnode != i)
						 {
							 tempnode = prenode[tempnode];
							 temppath.hop++;
						 }
						 
						 int m = temppath.hop;
						 tempnode = j;
						 while (m > 0)
						 {
							 temppath.node[m] = tempnode;
							 tempnode = prenode[tempnode];
							 m--; 
						 }
						 
					 }
				 }
				 
				 return temppath;
		}
		
		public void SetKShortestPath(int k)// Ѱ��k���·
		{
			AlternativeRoad = new int[Nodenum+1][Nodenum+1][(Nodenum-1)*k+2][Nodenum+1];
			AlternativeLength = new double[Nodenum+1][Nodenum+1][(Nodenum-1)*k+2];
			AlternativeNum = new int[Nodenum+1][Nodenum+1];
			AlternativePath = new boolean[Nodenum+1][Nodenum+1][(Nodenum-1)*k+2];
			
			// ��ʹ��dijkstra�ҵ�����ڵ������·
			for (int i = 1; i <= Nodenum; i++)
				for (int j = i; j <= Nodenum; j++)
					this.InitDijkstra(i,j);
			
//			for (int i = 1; i <= this.Nodenum; i++)
//				System.out.println(this.AlternativeNum[i][i]);

//			for (int i = 1 ; i <= this.Nodenum; i++)
//			{
//				for (int j = 1; j <= this.Nodenum; j++)
//				{
////					System.out.print(this.Weight[i][j]+" ");
////					System.out.print(this.Shortestpath[i][j] + " ");
////					System.out.print(this.AlternativeLength[i][j][1]+" ");
//					for (int m = 1; m <= this.Nodenum; m ++)
//						System.out.print(this.AlternativeRoad[i][j][1][m] + " ");
//				}
//				System.out.println("");
//			}
			
			this.ShortestKpath = new double[Nodenum+1][Nodenum+1][k+2];
			this.ShortestKPathNode = new int[Nodenum+1][Nodenum+1][k+2][Nodenum+1];
			for (int i = 1; i <= Nodenum; i++)
				for (int j = i; j <= Nodenum; j++)
				{
			
					if(this.AlternativeNum[i][j] != 0)// Ϊ�˷�ֹ�����·�����
					{
						this.ShortestKpath[i][j][1] = this.AlternativeLength[i][j][AlternativeNum[i][j]]; 
						ShortestKpath[j][i][1] = AlternativeLength[j][i][AlternativeNum[j][i]];
//						System.out.println(AlternativeRoad[i][j][AlternativeNum[i][j]].length);
						for (int l = 1; l <= AlternativeRoad[i][j][AlternativeNum[i][j]].length-1; l++)
						{
							ShortestKPathNode[i][j][1][l] = AlternativeRoad[i][j][AlternativeNum[i][j]][l];
							ShortestKPathNode[j][i][1][l] = AlternativeRoad[j][i][AlternativeNum[j][i]][l];
						}
						AlternativePath[i][j][AlternativeNum[i][j]] = true;
						AlternativePath[j][i][AlternativeNum[j][i]] = true;
					}
					else
					{
						ShortestKpath[i][j][1] = 100000;
						ShortestKpath[i][j][1] = 100000;
						ShortestKPathNode[i][j][1][1] = -1;
						ShortestKPathNode[j][i][1][1] = -1;
					}
				}
				 					 
			// Ѱ��k���·
			
			if (k == 1) return;
			
			int add = 1;// ͳ�Ƶ�ǰ�Ѿ��ҵ������·����

			while (add < k)
			{
//				System.out.println("��ǰ������· "+ add);
				boolean flag = false;// ��¼�Ƿ��и����µ����·��
				int[] Rootlist = new int[Nodenum+1];// ��¼��ǰ����·���Ľڵ�
				for (int i = 1; i <= Nodenum; i++)
					for (int j = i; j <= Nodenum; j++)
					{
						// �ҵ��µı�ѡ·
//						System.out.println(ShortestKPathNode[i][j][add][1]);
//						System.out.println(this.ShortestKpath[i][j][add]);

						if (ShortestKPathNode[i][j][add][1] != -1)
						{
							int sum = 1;// ͳ�Ƶ�ǰ�ı��·����
							int start;
							int end;
							do
							{
								int[] Pathlist = new int[Nodenum+1];// ��ʱ����߹���·��
								double Pathlength = 0;// ��ʱ��õ�·������
								
								start = ShortestKPathNode[i][j][add][sum];
								Rootlist[sum] = start;
								for (int m = 1; m <= AlternativeNum[i][j]; m++)
								{
									boolean SameFlag = true;// ��¼�Ƿ����뵱ǰ����·����ͬ�ı�ѡ·
									for (int n = 1; n <= sum; n++)
									{
										if (AlternativeRoad[i][j][m][n] != Rootlist[n])
										{
											SameFlag = false;
											break;
										}
									}
									if (SameFlag == true)
									{
										end = AlternativeRoad[i][j][m][sum+1];
										if (end == 0) break;
										this.Weight[start][end] = 100000;
										this.Weight[end][start] = 100000;
									}
								}
								end = ShortestKPathNode[i][j][add][sum+1];
								if (end == 0) break;
								
								for (int m = 1; m <= sum; m++)
								{
									Pathlist[m] = ShortestKPathNode[i][j][add][m];
									if (m > 1)
										Pathlength = Pathlength + FakeWeight[Pathlist[m-1]][Pathlist[m]];
								}
								
								int Hop;
								double Len;
								int[] Node = new int[Nodenum + 1];
								Temppath temppath = Dijkstra(start, j);
								Len = temppath.len;
								Hop = temppath.hop-1;
								Node = temppath.node;

//								if ((i == 2) && (j <= 3))
//								{
//									System.out.println(start);
//									System.out.println(end);
//									System.out.println(Len);
//									System.out.println(Hop);
////									System.out.println(j);
//								}
								
								for (int m = 1; m <= Nodenum; m++)
									if (m != start)
									{
										Weight[start][m] = 100000;
										Weight[m][start] = 100000;
									}
								
								if (Len == -1) 
									{
										sum++;
										continue;
									}
								for (int m = 1; m <= Hop; m++)
								{
									Pathlist[sum + m] = Node[m+1];
								}
								Pathlength = Pathlength + Len;
//								System.out.println(Pathlength);
								
//								if ((add == 2) && (i == 1) && (j == 5))
//								{
//									for (int m = 1; m <= Nodenum; m++ )
//									{
//										System.out.print(Node[m]+" ");
//									}
//									System.out.println("");
//									System.out.println(Hop);
//								}
								
								boolean IsSameRoad = false;// �ж���·�Ƿ������б���
								for (int m = 1; m <= AlternativeNum[i][j]; m++)
								{
									if (AlternativeLength[i][j][m] == Pathlength)
									{
										IsSameRoad = true;
										for (int n = 1; n <= Nodenum; n ++)
										{
											if (AlternativeRoad[i][j][m][n] != Pathlist[n])
											{
												IsSameRoad = false;
												break;
											}
										}
									}
								}
								
								if(IsSameRoad == false)// �������·����·��������뵽��ѡ·����
								{
									AlternativeNum[i][j] ++;
									int s = AlternativeNum[i][j];
//									AlternativeNum[j][i] = AlternativeNum[j][i];
									AlternativeLength[i][j][s] = Pathlength;
//									AlternativeLength[j][i][s] = Pathlength;
									for (int m = 1; m <= Nodenum; m ++)
										AlternativeRoad[i][j][s][m] = Pathlist[m];
									AlternativePath[i][j][s] = false;
								}
								
//								System.out.println("Step1 finished "+i+" "+j);
//								if (i == 1)
//								{
//									for (int s = 1 ; s <= this.Nodenum; s++)
//									{
//										for (int e = 1; e <= this.Nodenum; e++)
//										{
//											System.out.print(this.Shortestpath[s][e] + " ");
//										}
//										System.out.println("");
//									}
//								}
//								System.out.println(AlternativeNum[1][2]);
								sum++;
//								System.out.println(AlternativeNum[i][j]);
							}
							while(end != j);
							
							for (int m = 1; m <= Nodenum; m++)
								for ( int n = 1; n <= Nodenum; n++)
									Weight[m][n] = FakeWeight[m][n];// �ָ�ԭ��Ȩ��
						}
						
						// �ҵ���ǰ��ѡ·�е����·
						// ����Сֵ������
						double min = 100000;// ��ǰ���·����
						int temp = -1;// ����Ƿ����ҵ����ñ�ѡ·
					    for (int l = 1; l <= AlternativeNum[i][j]; l++)
					    {
//					    	System.out.println(AlternativeNum[1][2]);
					    	if (AlternativePath[i][j][l] == false)
					    	{
					    		if (AlternativeLength[i][j][l] < min)
					    		{
					    			min = AlternativeLength[i][j][l];
					    			temp = l;
					    		}
					    			
					    	}
					    }
//					    System.out.println("Step 2 finished");
//					    System.out.println("min = "+ min+ " temp = " + temp);
					    
					    // temp������-1��˵�����ڿ��õ����·
					    if (temp != -1)
					    {
					    	ShortestKpath[i][j][add+1] = min;
					    	ShortestKpath[j][i][add+1] = min;
					    	for (int l = 1; l <= AlternativeRoad[i][j][temp].length-1; l++)
					    	{
					    		ShortestKPathNode[i][j][add+1][l] = AlternativeRoad[i][j][temp][l];
					    		ShortestKPathNode[j][i][add+1][l] = AlternativeRoad[j][i][temp][l];
					    	}
					    	AlternativePath[i][j][temp] = true;
					    	flag = true;
					    }
					    else 
					    {
					    	ShortestKpath[i][j][add+1] = 100000;
					    	ShortestKpath[j][i][add+1] = 100000;
					    	ShortestKPathNode[i][j][add+1][1] = -1;
					    	ShortestKPathNode[j][i][add+1][1] = -1;
					    }
					}
				
				if (flag == false) break;// ���û�п��Լ�����������·�����˳�ѭ��
				add++;
			}
			
//			int add = 1;// ��ǰ���е����·������
//			while (add < k)
//			{
//				double[][] FakeWeight = Weight;// ���汾���Ȩ����Ϣ
//				
//				for (int i = 1; i  <= Nodenum;i ++)
//					for (int j = i; j <= Nodenum; j++)
//					{
//						
//					}
//			}
			
			// ������K���·��ͼ��ԭ
			for (int l = 1; l <= k; l++)
				for (int i = 1; i <= this.Nodenum; i++)
					for (int j = 1; j <= this.Nodenum; j++)
					{
						if (i > j)
						{
							ShortestKpath[i][j][l] = ShortestKpath[j][i][l];
							int tempi = 1;
							while (ShortestKPathNode[j][i][l][tempi] != 0)
								tempi++;
							tempi--;
							int tempj = 1;
							while (tempi != 0)
							{
								ShortestKPathNode[i][j][l][tempj] = ShortestKPathNode[j][i][l][tempi];
								tempi --;
								tempj ++;
							}
						}
					}
		} 
	}
	
	
	private static class Link//���ҵ������б�ѡ·������Link��
	{
		int num;// ·������e
		int[] Start;// ·��Դ�ڵ�
		int[] Destination;// ·��Ŀ�Ľڵ�
		double[] Length;// ����·��������
		int[][] Delta;// ĳ��·�����Ƿ�ʹ����·e���� e,��		
	}
	
	private static class Sfc
	{
		int Hmax;// ÿ�������϶�ڵ���������
		int num;// Sfc��������
		int[] Maxuser;// ĳ�ַ����ܷ��������û���
		double[] Consumption;// ĳ�ַ������ĵĽڵ���Դ����
	}
	
    private static class User
    {
    	int Usernum;// �û�����i
    	int[] Access;// ĳ���û�Ҫ�����Դ�ڵ�
    
    	int[] Sfcnum;// ĳ���û���SFC����SFC��
    	int[][][] g; // ĳ���û� i�ĵ�j����������p����SFC g i,j,p
    	
    	int[] Source;// ĳ�û�Ҫ�������յ�
    	int[] Beta;// �û�iҪ��ķ������ڽ�SFC�Ĵ�������
    	int[] r;// �����û�i��ɺ��õ�����
    	
    	int[][][] d;// �ڵ�ǰ����ʱ��֮ǰ���û�i�ĵ�j��vnf�ǵ�p��vnf di,j,p
    	int[][][] a;// ��ǰ����֮ǰ���û�i�ĵ�j��vnf��������������v�� ai,j,v
    	int[][][] f;// ��ǰ����֮ǰ���û�i�Ƿ��ڵ�v�����������ϲ���p���� fi,v,p
    	
    	
    }
    
    static private class Deployed
    {
    	double[] x;// �û�i�ڸ÷���ʱ���ڱ�����
    	double[][] y;// ��p��������ڽڵ�v�ϣ�y v,p
    	double[][][] z;// �û�i�ĵ�j���������ڽڵ�v�� z i,j,v
    	double[][][] w;// �û�i�ĵ�j-��(j+1)�ķ���ʹ������·������ w i,j,��
    	

    }
    
	static private class Column
	{
		int length;// Column������c
		double[][] m;// ��c���ǹ�Ӧ���û�i�ļƻ� m i,c
		double[][][] a;// ��c�аѵ�p��VNF��������������v�� a c,v,p
	    double[] h;// ��c���ṩ���û�i�Ĳ�������
		double[][] b;// ��c�з�������·e�Ĵ������� b c,e
	}
	
	static private class MPsolve
	{
		double[] lamda;// ָʾ��c�в��𷽰��Ƿ�ʵʩ
		double[][] y;// ��p��������ڽڵ�v�ϣ�y v,p
	}

	private static final int[] MpGoal = null;
		
//	public  static void main(String[] args) {// for test
//		Graph graph = new Graph(6);
//		graph.Linknum = 11;
////		double[][] b = graph.Weight;
////		System.out.print(graph.Nodenum);
////		System.out.print(graph.Weight.length);
//		graph.InitialtheWeight();
//		double[][] a= {{0,5,2,7,1,100000},
//		                {5,0,9,4,2,6},
//		                {2,9,0,100000,100000,3},
//		                {7,4,100000,0,100000,1},
//		                {1,2,100000,100000,0,3},
//		                {100000,6,3,1,3,0}};
//		
//		graph.RejudgeTheWeight(a);
//		
//		Temppath temppath = new Temppath();
////		temppath = graph.Dijkstra(1,3);
//		graph.SetKShortestPath(3);
//		
////		for (int i = 1; i <= graph.Nodenum; i ++)
////		{
////			System.out.print(temppath.node[i]);
////		}
//		
////		for (int i = 1; i <= graph.AlternativeNum[1][2]; i ++)
////		{
////			System.out.println(graph.AlternativeLength[1][2][i]);
////		}
//		
//		for (int n = 1 ; n <= 3; n++)
//		{
//			System.out.println("road "+n);
//			for (int j = 1; j <= graph.Nodenum; j++)
//			{
//				System.out.println(graph.ShortestKpath[2][j][n]);
//				for (int m = 1; m <= graph.ShortestKPathNode[2][j][n].length-1; m ++)
//				{
//					System.out.print(graph.ShortestKPathNode[2][j][n][m] + " ");
//				}
//				System.out.println("");
//			}
//			System.out.println("");
//		}
//		
//		
////		for (int i = 1 ; i <= graph.Nodenum; i++)
////		{
////			for (int j = 1; j <= graph.Nodenum; j++)
////			{
//////				System.out.print(graph.Weight[i][j]+" ");
//////				System.out.print(graph.Shortestpath[i][j] + " ");
////				System.out.print(graph.AlternativeLength[i][j][1]+" ");
////			}
////			System.out.println("");
////		}
////		graph.Weight[0][0] = 0;
//		
//	}
	
	public static void Mainplan() throws IOException {
		// TODO Auto-generated method stub
    	        
		int nodenum =  info.vertexnum;
		int linknum = info.edgenum;
		
		final int PPsize = 8000;
		final int SETK = 3;
		
//		double[][] Tempb = {{0,1,100000,100000,100000,100000,1,1,100000,100000},
//		         {1,0,1,100000,100000,1,1,100000,100000,100000},
//		         {100000,1,0,1,100000,100000,100000,100000,100000,100000},
//		         {100000,100000,1,0,1,1,100000,100000,100000,1},
//		         {100000,100000,100000,1,0,1,1,100000,1,1},
//		         {100000,1,100000,1,1,0,1,100000,100000,100000},
//		         {1,1,100000,100000,1,1,0,1,1,100000},
//		         {1,100000,100000,100000,100000,100000,1,0,1,100000},
//		         {100000,100000,100000,100000,1,100000,1,1,0,1},
//		         {100000,100000,100000,1,1,100000,100000,100000,1,0}};// ·���������
		double[][] Tempb = new double[nodenum][nodenum];
		
		for (int i = 0; i < nodenum; i++)
			for (int j = 0; j < nodenum; j++)
			{
				if (i == j)
					Tempb[i][j] = 0;
				else 
					Tempb[i][j] = 100000;
			}
		
		for (int i = 1; i <= linknum; i++)
		{
			int a = info.edgestart[i] - 1;
			int b = info.edgeend[i] - 1;
			
			Tempb[a][b] = 1;
			Tempb[b][a] = 1;
		}
		// һ�󲨳�ʼ��
		
		// ��ʼ��������
		try
		{
			File file = new File("./topology/Testinfo.txt");
			FileWriter fw = new FileWriter(file,false);
			PrintWriter out = new PrintWriter(new BufferedWriter(fw));
			out.println("------------ check Info ---------------");
            out.println("Vertex number: " + info.vertexnum);
            out.println("RemaimITresource:");
            for (int j = 1; j <= info.vertexnum; j++)
            {
            	out.println("Vertex " + j + " :");
            	out.println(info.RemainITresource[j]);
            }
            out.println("Edge number: " + info.edgenum);
            for (int j = 1; j <= info.edgenum; j++)
            {
            	out.println("Edge "+j+" :");
            	out.println("Start: " + info.edgestart[j]);
            	out.println("End: " + info.edgeend[j]);
            	out.println("RemainbandWidth: " + info.Remainbandwidth[j]);
            }
            out.println("VNFtype: " + info.VNFtype);
            for (int j = 1; j <= info.VNFtype; j++)
            {
            	out.println("VNF " + j + " IT resource: " + info.VNFITresource[j]);
            }
            
            out.println("SFCnum: " + info.SFCnum);
            for (int j = 1; j <= info.SFCnum; j++)
            {
            	out.println("Sfc " + j + " :");
            	out.println("Source: " + info.SFCsource[j]);
            	out.println("Sink: " + info.SFCsink[j]);
            	out.println("Bandewidth: " + info.SFCbandwidth[j]);
            	out.println("VNFnum: " + info.VNFnum[j]);
            	for (int k = 1; k <= info.VNFnum[j]; k++)
            	{
            		out.println("VNFtype: " + info.VNFtypeinSFC[j][k]);
            		out.println("VNFdeployed: " + info.VNFdeployedon[j][k]);
            	}
            	
            	out.println("Sfc "+ j + " using " + info.SFCusinglinknum[j] + " links:");
            	for (int k = 1; k <= info.SFCusinglinknum[j] ; k++)
            	{
            		out.println("using link " + info.SFCusinglink[j][k]);
            	}
            	out.println("");
            	out.flush();
            }
			out.println("Comparison Algorithm Begin Test!!");
			out.println("-----------------------------------------------------");
		}catch (FileNotFoundException e) {
            System.err.println("文件不存在！");
        }catch (IOException e) {
            e.printStackTrace();
        }
		
		for (int times = 1; times <=5 ; times ++) {
			double lastQ = 0;
			System.out.println("test time: "+ times);
			Check check = new Check();
		
			// 随机生成要改变的SFC
			int SFCchange = Configure.random.nextInt(info.SFCnum - 1) +1;// 随机改变的SFC标识
	        int changeSFCsource = Configure.random.nextInt(info.vertexnum - 1) + 1; // 随机改变的SFC起点
	        int changeSFCsink = 0;
	        do
	        {
	        	changeSFCsink = Configure.random.nextInt(info.vertexnum - 1) + 1;// 随机改变的SFC终点
	        }
	        while (changeSFCsink == changeSFCsource);
	        
	        int lastSource = info.SFCsource[SFCchange];
	        int lastSink = info.SFCsink[SFCchange];
	        
	        info.SFCsource[SFCchange] = changeSFCsource;
	        info.SFCsink[SFCchange] = changeSFCsink;
	        
	        for (int i = 1; i <= info.VNFnum[SFCchange] ; i++)// 归还计算资源消耗
	        {
	        	info.RemainITresource[info.VNFdeployedon[SFCchange][i]] += info.VNFITresource[info.VNFtypeinSFC[SFCchange][i]];
	        }
	
	        for (int i = 1; i <= info.SFCusinglinknum[SFCchange]; i++)// 归还带宽占用
	        {
	        	info.Remainbandwidth[info.SFCusinglink[SFCchange][i]] += info.SFCbandwidth[SFCchange];
	        }
	        
	        check.changeSFC = SFCchange;
	        check.changeSource = info.SFCsource[SFCchange];
	        check.changeSink = info.SFCsink[SFCchange];
	        check.changeSFCITresource = 5;
	        check.changeLinkBandwidth = 5;
	        check.changeVNFnum = 0;
	        check.changeLinkusingnum = 0;
	        check.changeVNFdeployon = new int[info.vertexnum];
	        check.changeLinkusing = new int[info.edgenum];
	        check.changeLinkusingcapacity = new double[info.edgenum];
	        
	//        // 测试随机代码
	        System.out.println("change SFC numble: " + SFCchange);
	        System.out.println("last SFC Source numble: " + lastSource);
	        System.out.println("last SFC Sink numble: " + lastSink);
	        System.out.println("change SFC Source numble: " + info.SFCsource[SFCchange]);
	        System.out.println("change SFC Sink numble:" + info.SFCsink[SFCchange]);
	        
//	        try {
//	        	File file2 = new File("./topology/RandomSFC.txt");
//	        	FileWriter fw2 = new FileWriter(file2, true);
//	        	PrintWriter out2 = new PrintWriter(new BufferedWriter(fw2));
//	        	
//	        	out2.println("Change SFC "+ times + ":");
//	        	out2.println("SFCnumber        origin source         origin sink            change source           change sink");
//		        out2.println("change SFC number: " + SFCchange);
//		        out2.println("last SFC Source number: " + lastSource);
//		        out2.println("last SFC Sink number: " + lastSink);
//		        out2.println("change SFC Source number: " + info.SFCsource[SFCchange]);
//		        out2.println("change SFC Sink number: " + info.SFCsink[SFCchange]);
//		        out2.println("----------------------------------------------------");
//		        out2.flush();
//	        }
//	        catch (FileNotFoundException e)
//	        {
//	        	System.err.println("文件 RandomSFC.txt 未找到");
//	        }
//	        catch (IOException e)
//	        {
//	        	e.printStackTrace();
//	        }
//	        
	//        for (int i = 1; i <= info.VNFnum[SFCchange] ; i++)// 归还计算资源消耗
	//        {
	//        	System.out.println("ITresource on vertex "+ info.VNFdeployedon[SFCchange][i]+ " :");
	//        	System.out.println(info.RemainITresource[info.VNFdeployedon[SFCchange][i]]);
	//        }
	//
	//        for (int i = 1; i <= info.SFCusinglinknum[SFCchange]; i++)// 归还带宽占用
	//        {
	//        	System.out.println("Bandwidth on link " + info.SFCusinglink[SFCchange][i]+ " :");
	//        	System.out.println(info.Remainbandwidth[info.SFCusinglink[SFCchange][i]]);
	//        }
	        
	        // 计时
	        double	begintime = System.currentTimeMillis(); 
	        
	        // 开始初始化
			System.out.println("begin initialization!");
			System.out.println("initial the Toppologic");
			Topologic top = new Topologic();
			top.Nodenum = nodenum;
			top.Start = new int[linknum + 1];
			top.End = new int[linknum + 1];
			for (int i = 1; i <= top.Nodenum-1; i++)
				for (int j = i; j <= top.Nodenum; j++)
				{
					if (Tempb[i-1][j-1] == 1)
					{
						top.Linknum ++;
						top.Start[top.Linknum] = i;
						top.End[top.Linknum] = j;
					}
				}
			int[]Tempa = {1,3,5,6,8,10};
			top.Vd = new int[Tempa.length + 1];
			for (int i = 1; i <= Tempa.length; i ++)
				top.Vd[i] = Tempa[i-1];
			Tempa = null;
			top.rho1 = 1;
			top.rho2 = 1;
			top.Cap = new double[top.Nodenum+1];
			for (int i = 1; i <= top.Nodenum; i++)
			{
				top.Cap[i] = info.RemainITresource[i];
			}
			top.Ban = new double[top.Linknum + 1];
			for (int i = 1; i <= top.Linknum; i++)
			{
	//			int temp = 1;
	//			while ((top.Start[i] != info.edgestart[temp]) || (top.End[i] != info.edgeend[temp]))
	//			{
	//				temp++;
	//			}
				top.Ban[i] = info.Remainbandwidth[i];
			}
			
			// ��ʼ�����繹ͼ,������Ϊ����
			System.out.println("Initial the Initgraph");
			Graph Initgraph = new Graph(top.Nodenum);
			Initgraph.Linknum = top.Linknum;
			Initgraph.InitialtheWeight();
			Initgraph.RejudgeTheWeight(Tempb);
			Initgraph.SetKShortestPath(SETK);
			
			// ��ʼ����·������ҵ�k���·
			System.out.println("Initial the Link");
			Link link = new Link();
			link.num = top.Nodenum * top.Nodenum *SETK;
			link.Start = new int[link.num+1];
			link.Destination = new int[link.num+1];
			link.Length = new double[link.num + 1];
			link.Delta = new int[top.Linknum + 1][link.num + 1];
			int tempcount = 0;
			for (int i = 1; i <= top.Nodenum; i++)
				for (int j = 1; j <= top.Nodenum; j++)
					for (int k = 1; k <= SETK; k++)
					{
						tempcount ++;// ��ǰpath ��
						link.Start[tempcount] = i;
						link.Destination[tempcount] = j;
						link.Length[tempcount] = Initgraph.ShortestKpath[i][j][k];
						for (int l = 1; l <= top.Nodenum-1; l++)
						{
							if (Initgraph.ShortestKPathNode[i][j][k][l+1] == 0)
								break;
							else 
							{
								int tempCount = 1;// ��ǰ������link e
								while (tempCount <= top.Linknum)
								{
									if (((top.Start[tempCount] == Initgraph.ShortestKPathNode[i][j][k][l])&&(top.End[tempCount] == Initgraph.ShortestKPathNode[i][j][k][l+1]))||((top.End[tempCount] == Initgraph.ShortestKPathNode[i][j][k][l])&&(top.Start[tempCount] == Initgraph.ShortestKPathNode[i][j][k][l+1])))
									{
										link.Delta[tempCount][tempcount] = 1;
										break;
									}
									else
										tempCount ++;
								}
							}
						}
					}
	
			// ��ʼ��vnf
			System.out.println("Initial the Sfc");
			Sfc sfc = new Sfc();
			// sfc����������
			sfc.Hmax = 100;
			// vnf������
			sfc.num = info.VNFtype + 1;
			// vnf��ĳ�ַ����ܷ��������û���
			sfc.Maxuser = new int[sfc.num+1];
			for (int i = 1; i <= sfc.num-1 ; i++)
			{
				sfc.Maxuser[i] = 3;
			}
			sfc.Maxuser[sfc.num] = 100;
			// ĳ�ַ������ĵĽڵ���Դ����
			sfc.Consumption= new double[sfc.num + 1];
			for (int i = 1; i <= sfc.num - 1 ; i++)
			{
				sfc.Consumption[i] = info.VNFITresource[i];
			}
			sfc.Consumption[sfc.num] = 0;
		
			// �û���ʼ��
			System.out.println("Initial the Users");
			User user = new User();
			// �û�����
			user.Usernum = 1;
			// �û���������
			user.Access = new int[user.Usernum + 1];
			for (int i = 0; i <= user.Usernum; i++)
				user.Access[i] = info.SFCsource[SFCchange];
			// �û����������VNF����
			user.Sfcnum = new int[user.Usernum + 1];
			for (int i = 0; i <= user.Usernum; i++)
				user.Sfcnum[i] = info.VNFnum[SFCchange] + 2;
			// �û�i �� ��j������ ����p���ͷ��� g i,j,p
			user.g = new int[user.Usernum + 1][][];
			user.g[0] = new int[0][0];
			for (int i = 1; i <= user.Usernum ; i++)
			{
				user.g[i] = new int[user.Sfcnum[i] + 1][sfc.num+1];
			}
			for (int i = 1; i <= user.Usernum ; i++)
			{
				for (int j = 1; j <= user.Sfcnum[i] ; j++)
				{
					if ((j == 1) || (j == user.Sfcnum[i]))
					{
						user.g[i][j][sfc.num] = 1;
					}
					else
					{
						user.g[i][j][info.VNFtypeinSFC[SFCchange][j-1]] = 1;
					}
				}
			}
			
	//		user.g[1][1][4] = 1;
	//		user.g[1][2][1] = 1;
	//		user.g[1][3][2] = 1;
	//		user.g[1][4][4] = 1;
	//		
			// �û������ĩβ��
			user.Source = new int[user.Usernum + 1];
			for (int i = 0; i <= user.Usernum; i++)
				user.Source[i] = info.SFCsink[SFCchange];
			
			// �û��������е��ڽ�����Ĵ�������
			user.Beta = new int[user.Usernum + 1];
			for (int i = 0; i <= user.Usernum; i++)
				user.Beta[i] = info.SFCbandwidth[SFCchange];
	
			// �û�����
			user.r = new int[user.Usernum + 1];
			user.r[1] = 100;
			
			// �û��ڷ���ʱ��֮ǰ��vnf����
			user.d = new int[user.Usernum + 1][][];
			user.d[0] = new int[0][0];
			for (int i = 1; i <= user.Usernum; i++)
			{
				user.d[i] = new int[user.Sfcnum[i] + 1][sfc.num + 1];
			}
			for (int i = 1; i <= user.Usernum; i++)
				for (int j = 1; j <= user.Sfcnum[i]; j++)
					for (int k = 1; k <= sfc.num; k++)
					{
						user.d[i][j][k] = user.g[i][j][k];
					}
			
			// �û��ڷ���ʱ��֮ǰ��vnf����
			user.a = new int[user.Usernum + 1][][];
			user.a[0] = new int[0][0];
			for (int i = 1; i <= user.Usernum; i++)
			{
				user.a[i] = new int[user.Sfcnum[i] + 1][top.Nodenum+1];
			}
			
			for (int i = 1; i <= user.Usernum; i++)
				for(int j = 1; j <= user.Sfcnum[i]; j++)
				{
					if ( j == 1)
					{
						user.a[i][j][lastSource] = 1;
					}
					else if ( j == user.Sfcnum[i] )
					{
						user.a[i][j][lastSink] = 1;
					}
					else
					{
						user.a[i][j][info.VNFdeployedon[SFCchange][j-1]] = 1;
					}
				}
			
			// �û��ڷ���ʱ��֮ǰ��vnf���Ͳ���
			user.f = new int[user.Usernum + 1][top.Nodenum + 1][sfc.num + 1];
			for (int i = 1; i <= user.Usernum; i++)	
				for (int j = 1; j <= top.Nodenum; j++)// v
					for ( int k = 1; k <= sfc.num; k++)// p
					{
						for ( int l = 0; l <= user.Sfcnum[i]; l++)// j
							user.f[i][j][k] = user.f[i][j][k] + user.a[i][l][j] * user.d[i][l][k]; 
					}
			
			System.out.println("Initial the column");
			Column column = new Column();
			column.length = 1;
			column.m = new double[user.Usernum + 1][10000];
			column.a = new double[10000][top.Nodenum + 1][sfc.num + 1];
			column.h = new double[10000];
			column.b = new double[10000][top.Linknum + 1];
			
			MPsolve mpsolve = new MPsolve();
			
	//		Optimator opt = new Optimator();
			
			Deployed deployed = new Deployed();
			class Init
			{
		    	public void InitDeployed()
		    	{
		    		deployed.x = new double[user.Usernum + 1];
		    		deployed.y = new double[top.Nodenum + 1][sfc.num+1];
		    		deployed.z = new double[user.Usernum+1][][];
		    		deployed.w = new double[user.Usernum+1][][];
		    		deployed.z[0] = new double[0][0];
		    		deployed.w[0] = new double[0][0];
		    		for (int i = 1; i <= user.Usernum; i++)
		    		{
		    			deployed.z[i] = new double[user.Sfcnum[i]+1][top.Nodenum + 1];
		    			deployed.w[i] = new double[user.Sfcnum[i]][link.num+1];
		    		}
		    	}
			}
			Init init = new Init();
			
	
			double Q = 1;
			do
			{
				System.out.println("times begin");
				// MP�����ϵ������Ϊ[��1��...��c��y1.1,y1.2,...,y1.p,y2.1,...,yv.p] ��c+v*p��ϵ��
				double[] MpGoal = new double[1000];// MP����Ŀ�꺯����ϵ��
				double[][] MpCond = new double[1000][1000];// MP����Լ��������ϵ��
				double[] MpRest = new double[1000];// MP����Լ��������Լ��
				
				// MP�����Ŀ�꺯����ϵ��
	//			MpGoal[0] = 0;
				int sumMp = -1;// MpĿ�꺯����ϵ������
				int i = 0; // ѭ������
				int j = 0; // ѭ������
				
				for (i = 1; i <= column.length ; i++)
				{
					double sum1 = 0; // ͳ�Ƹ�����ܺ�
					double sum2 = 0; // ������ʱ��������
					for (j = 1; j <= top.Linknum; j++)
					{
						sum2 = sum2 + column.b[i][j];
					}
					sum1 = sum1 + top.rho2 * sum2;
					sum2 = 0;
					for (j = 1; j <= user.Usernum; j++)
					{
						sum2 = sum2 + user.r[j] * column.m[j][i];
					}
					sum1 = sum1 - sum2;
					sumMp++;
					MpGoal[sumMp] = sum1;
				}
				
				for (i = 1; i <= top.Nodenum; i++)
				{
					for (j = 1; j <= sfc.num; j++)
					{
						sumMp++;
						MpGoal[sumMp] = top.rho1 * sfc.Consumption[j];
					}
				}
				
				// MP����ı���
				double[] MpVar = new double[sumMp+2];
	
				// MP�����Լ��������ϵ��
				int sumCd = -1;// Լ�������ĸ���
				int sumCdvar = -1;// Լ�������б����ĸ���
	//			double sum1 = 0;// ������ʱ��������  
				
	//			for(i = 1;i <= sumMp; i++)
	//				MpCond[sumCd][i] = 0;
				
				sumCd++;
				
				//Լ��1��15��
				for(i = 0; i <= column.length-1; i++)
				{
					MpCond[sumCd][i] = column.h[i];
				}
				for(i = column.length; i <= sumMp-1; i++)
				{
					MpCond[sumCd][i] = 0;
				}
				MpRest[sumCd] = sfc.Hmax;
	
				//Լ��2��16��
				int k; //ѭ������
				for (i = 1 ; i <= top.Nodenum; i++)
				{
					sumCd++;
					sumCdvar = -1;
					for (j = 1; j <= sumMp; j++)
						MpCond[sumCd][j-1] = 0;
					for (j = 1; j <= column.length; j++)
						sumCdvar ++;
					for (j = 1; j <= top.Nodenum; j++)
						for (k = 1; k <= sfc.num; k++)
						{
							sumCdvar ++;
							if (i == j)
							{
								MpCond[sumCd][sumCdvar] = sfc.Consumption[k];
							}
						}
				    if (top.Cap[i] <= 6)
				    	MpRest[sumCd] = top.Cap[i];
				    else
				    	MpRest[sumCd] = 6;
				}
				
				//Լ��3��17��
				for (i = 1;i <= top.Linknum; i++)
				{
					sumCd++;
					for (j = 1; j <= sumMp; j++)
						MpCond[sumCd][j-1] = 0;
					for (j = 1; j <= column.length; j++)
					{
						MpCond[sumCd][j-1] = column.b[j][i];
					}
					
					if (top.Ban[i] <= 6)
						MpRest[sumCd] = top.Ban[i];
					else
						MpRest[sumCd] = 6;
					
				}
				
				// Լ��4��18��
				for (i = 1; i <= user.Usernum ; i++)
				{
					sumCd++;
					for (j = 1; j <= sumMp; j++)
						MpCond[sumCd][j-1] = 0;
					for (j = 1; j <= column.length; j++)
					{
						MpCond[sumCd][j-1] = column.m[i][j];
					}
					MpRest[sumCd] = 1;
					
				}
				
				// Լ��5��19��
				sumCd++;
				for(i = 1 ; i <= sumMp; i++)
					MpCond[sumCd][i-1] = 0;
				for(i = 1 ; i <= column.length; i++)
					MpCond[sumCd][i-1] = 1;
				MpRest[sumCd] = user.Usernum;
	
				// Լ��6��20��
	//			int l;// ѭ������
	//			int m;// ѭ������
				for (j = 1; j <= top.Nodenum; j++)
					for (i = 1; i <= sfc.num; i++)
					{
						sumCd++;
						for(k = 1; k <= sumMp; k++)
							MpCond[sumCd][k-1] = 0;
							
						for(k = 1; k <= column.length ;k++)
							MpCond[sumCd][k-1] = column.a[k][j][i];
						
						sumCdvar = column.length -1;
						for (k = 1; k <= top.Nodenum; k++)
							for (int l = 1; l <= sfc.num; l++)
							{
								sumCdvar ++;
								if ((k == j) && (i == l))
									MpCond[sumCd][sumCdvar] = - sfc.Maxuser[i];
							}
						
						MpRest[sumCd] = 0;
					}
				
				double[] Ups = new double[1000];// ��������
				double[] Downs = new double[1000];// ��������
				double[][] MpEqCond = new double[1][1000];
				double[] MpEqRest = new double[1];
				
				for (i = 0; i <= column.length-1; i++)
				{
					Ups[i] = 1;
					Downs[i] = 0;
				}
				for (i = column.length; i <= (sumMp-1); i++)
				{
					Ups[i] = user.Usernum;
					Downs[i] = 0;
				}
				
	//			try 
	//			{
	//				opt.optimate(MpGoal, MpCond, null, MpRest, null, Ups);
	//				System.out.println(opt.getObjective());
	//				System.out.println(Arrays.toString(opt.getVariables()));
	//				for (i = 1 ; i <= sumMp; i++)
	//					MpVar[i] = opt.getVariables()[i-1];
	//				
	//			}catch (LpSolveException e)
	//			{
	//				  e.printStackTrace();
	//			}
	//		        System.out.println( System.getProperty("java.library.path"));
				double theAns = 0;
				Optimator optimator;
				   try {  
				    	  optimator = new Optimator();
				    	  Object[] rs = optimator.optimator(2,MpGoal, MpCond,  MpRest,MpEqCond, MpEqRest, Downs,Ups);
	//			    	  System.out.println(rs[0]);
	//			    	  System.out.println(rs[1]);
				    	  
	//			    	  MWNumericArray is = new MWNumericArray(Double.valueOf(rs[0].toString()),MWClassID.DOUBLE);
				    	  MWNumericArray is = (MWNumericArray)rs[0];
				    	  for(i = 1; i <= sumMp+1 ; i++)
				    	  {
				    		  MpVar[i] = is.getDouble(new int[]{i, 1});
	//			    		  System.out.println(MpVar[i]);
				    	  }
				    	  MWNumericArray Ans = (MWNumericArray)rs[1];
				    	  theAns = Ans.getDouble(new int[] {1,1});
				    	  System.out.println(Ans.getDouble(new int[] {1,1}));
				    	  rs = null;
				    	  is = null;
				     	}  
				  
				   catch (Exception e) {  
				        System.out.println("Exception: " + e.toString());  
				   }  
				     finally {
				    	 System.gc();
				     }
	
					
	//		     break;
	//		
	//	}while (true);
		
				// ���ö�ż����
				sumMp ++;
				int Dualsum = 0; //  ����ͳ�ƶ�ż����
				
				Dualsum ++;
				double phi = MpRest[Dualsum];
				phi = phi * theAns;
	//			for (i = 1; i <=sumMp; i++)
	//			{
	//				phi = phi - MpCond[Dualsum][i] * MpVar[i];
	//			}
	//			phi = - phi;
				
				double[] tau = new double[top.Nodenum + 1];
				for (i = 1 ; i <= top.Nodenum; i++)
				{
					Dualsum++;
					tau[i] = MpRest[Dualsum];
					tau[i] = tau[i] * theAns;
	//				for (j = 1; j <= sumMp; j++)
	//				{
	//					tau[i] = tau[i] - MpCond[Dualsum][j]*MpVar[j];
	//				}
	//				tau[i] = -tau[i];
				}
				
				double[] alpha = new double[top.Linknum + 1];
				for (i = 1;i <= top.Linknum; i++)
				{
					Dualsum++;
					alpha[i] = MpRest[Dualsum];
					alpha[i] = alpha[i] * theAns;
	//				for (j = 1; j <= sumMp; j++)
	//				{
	//					alpha[i] = alpha[i] - MpCond[Dualsum][j]*MpVar[j];
	//				}
	//				alpha[i] = -alpha[i];
				}
				
				double[] gamma = new double[user.Usernum + 1];
				for (i = 1; i <= user.Usernum ; i++)
				{
					Dualsum++;
					
					gamma[i] = MpRest[Dualsum];
					gamma[i] = gamma[i] * theAns;
	//				for (j = 1; j <= sumMp; j++)
	//				{
	//					gamma[i] = gamma[i] - MpCond[Dualsum][j]*MpVar[j]; 
	//				}
	//				gamma[i] = - gamma[i];
				}
				
				double eta;
				Dualsum++;
				eta = MpRest[Dualsum];
				eta = eta * theAns;
	//			for(i = 1 ; i <= sumMp; i++)
	//				eta = eta - MpCond[Dualsum][i]*MpVar[i]; 
	//			eta = - eta;
				
				double[][] chi = new double[top.Nodenum + 1][sfc.num + 1];
				for (j = 1; j <= top.Nodenum; j++)
					for (i = 1; i <= sfc.num; i++)
					{
						Dualsum++;
						chi[j][i] = MpRest[Dualsum];
						chi[j][i] = chi[j][i] * theAns;
	//					for(k = 1; k <= sumMp; k++)
	//						chi[j][i] = chi[j][i] - MpCond[Dualsum][k]*MpVar[k];
						
	//					chi[j][i] = - chi[j][i];
					}
				
				System.out.println("Master Problem is solved!");
			
				
				// Pricing Problem
				double[] PPGoal = new double[PPsize];//PP Ŀ�꺯��ϵ��
				int sumPPG = -1;// PPĿ�꺯���ı�������
				int sumPP;// PPĿ�꺯����Լ��
				
				double[][][] zeta = new double[user.Usernum + 1][][]; // PP����ϵ��һ
				for (i = 1; i <= user.Usernum; i++)
				{
					zeta[i] = new double[user.Sfcnum[i] + 1][link.num + 1];
				}
				double sum1;// ��ʱ���
				int l;// ѭ������
				
				for (i = 1; i <= user.Usernum; i++ )
					for (j = 1; j <= user.Sfcnum[i] -1; j++)
						for (k = 1; k <= link.num; k++)
						{
							sumPPG ++;
							
							sum1 = 0;
							for (l = 1; l <= top.Linknum ; l++)
							{
								sum1 = sum1 + (top.rho2 + alpha[l])*link.Delta[l][k];
							}
							
							PPGoal[sumPPG] = sum1 * user.Beta[i];
							zeta[i][j][k] = PPGoal[sumPPG];
						}
				
				double[][][] zeta2 = new double[user.Usernum + 1][][]; // PP����ϵ����
				for (i = 1; i <= user.Usernum; i++)
				{
					zeta2[i] = new double[user.Sfcnum[i] + 1][top.Nodenum + 1];
				}
				for (i = 1; i <= user.Usernum; i++)
					for (j = 1; j <= user.Sfcnum[i]; j++)
						for (k = 1; k <= top.Nodenum; k++)
						{
							sumPPG ++;
							sum1 = 0;
							
							for (l = 1; l <= sfc.num; l++)
							{
								sum1 = sum1 + ((1 - user.f[i][k][l]) * phi + chi[k][l])*user.g[i][j][l];
							}
							
							PPGoal[sumPPG] = sum1;
							zeta2[i][j][k] = PPGoal[sumPPG];
						}
				
				double[] zeta3 = new double[user.Usernum + 1];// PP����ϵ����
				for (i = 1; i <= user.Usernum; i++)
				{
					sumPPG ++;
					
					PPGoal[sumPPG] = gamma[i] - user.r[i];
					zeta3[i] = PPGoal[sumPPG];
				}
				
				double[] PPVar = new double[PPsize];// PP�������
				
				double[][] PPCond = new double[PPsize][PPsize];// PP����Լ��������ϵ��
				double[] PPRest = new double[PPsize];// PP����Լ������������
				double[][] PPEq = new double[1000][PPsize];//PP�����ʽԼ��������ϵ��
				double[] PPEqRest = new double[1000];//PP�����ʽԼ������������
				double[] Ups1 = new double[PPsize]; 
				double[] Downs1 = new double[PPsize];
				for (i = 0; i <= PPsize - 1; i++)
				{
					Ups1[i] = 1;
					Downs1[i] = 0;
				}
				double[] PPRange = new double[PPsize];
				for (i = 0; i <= PPsize - 1; i++)
				{
					PPRange[i] = i+1;
				}
				
				int sumPPcd = -1;// PPԼ������
				
				
				// PPԼ��1��28�� ��ʽ
				sumPP = -1;// PP��������
				int PPEqnum = -1;// PP��ʽԼ������
				int PPEqvar = -1;// PP��ʽԼ��ϵ������
				
				PPEqnum ++;
				for (i = 1; i <= user.Usernum; i++ )
					for (j = 1; j <= user.Sfcnum[i] - 1; j++)
						for(k = 1; k <= link.num; k++)
						{
							sumPP ++;
							PPEq[0][sumPP] = 0;
						}
				
				int sumw = sumPP+1;// w�����ĸ���
				
				for (i = 1; i <= user.Usernum; i++)
					for (j = 1; j <= user.Sfcnum[i]; j++)
						for (k = 1; k <= top.Nodenum; k++)
						{
							sumPP++;
							PPEq[0][sumPP] = 0;
						} 
				
				for (i = 1; i <= user.Usernum; i++)
				{
					sumPP++;
					PPEq[0][sumPP] = 1;
				}
				
				PPEqRest[0] = 1;
				sumPP = sumPP +1;
	
				// PP��ʽԼ�� ��1��
				for (i = 1; i <= user.Usernum ; i++)
					for (j = 2; j <= user.Sfcnum[i]; j++)
					{
						PPEqnum ++;
						PPEqvar = sumw - 1;
						for (k = 1; k <= user.Usernum; k++)//i
							for (l = 1; l <= user.Sfcnum[k]; l++)//j
								for (int m = 1; m <= top.Nodenum; m++)//v
								{
									PPEqvar++;
									if ((k == i)&&(l == j))
									{
										PPEq[PPEqnum][PPEqvar] = 1;
									}
								}
						
						for (k = 1; k <= user.Usernum; k++)
						{
							PPEqvar ++;
							if (k == i)
								PPEq[PPEqnum][PPEqvar] = -1;
						}
						
						PPEqRest[PPEqnum] = 0;
					}
				
				// PP��ʽԼ�� ��2��
				for (i = 1; i <= user.Usernum; i++)
				{
					PPEqnum ++;
					PPEqvar = sumw -1;
					
					for (k = 1; k <= user.Usernum; k++)
						for (l = 1; l <= user.Sfcnum[k]; l++)
							for (int m = 1; m <= top.Nodenum; m++)
							{
								PPEqvar ++;
								if ((i == k) && (l == 1) && (m == user.Access[i]))
									PPEq[PPEqnum][PPEqvar] = 1;
							}
					
					for (k = 1; k <= user.Usernum; k++)
					{
						PPEqvar ++;
						if (i == k)
							PPEq[PPEqnum][PPEqvar] = -1;
					}
					
					PPEqRest[PPEqnum] = 0;
				}
				
				// PP��ʽԼ�� ��3�� 
				for (i = 1; i <= user.Usernum; i++)
				{
					PPEqnum ++;
					PPEqvar = sumw -1;
					
					for (k = 1; k <= user.Usernum; k++)
						for (l = 1; l <= user.Sfcnum[k]; l++)
							for (int m = 1; m <= top.Nodenum; m++)
							{
								PPEqvar ++;
								if ((i == k) && (l == user.Sfcnum[k]) && (m == user.Source[i]))
									PPEq[PPEqnum][PPEqvar] = 1;
							}
					
					for (k = 1; k <= user.Usernum; k++)
					{
						PPEqvar ++;
						if (i == k)
							PPEq[PPEqnum][PPEqvar] = -1;
					}
					
					PPEqRest[PPEqnum] = 0;
				}
				
				// PP��ʽԼ����8��
				for (i = 1; i <= user.Usernum;i++)
					for (j = 1; j <= user.Sfcnum[i] - 1; j++)
					{
						PPEqnum ++;
						PPEqvar = -1;
						
						for (k = 1; k <= user.Usernum; k ++)
							for (l = 1; l <= user.Sfcnum[k] - 1; l++)
								for (int m = 1; m <= link.num; m++)
								{
									PPEqvar ++;
									if ((i == k) && (j == l))
									{
										PPEq[PPEqnum][PPEqvar] = 1;
									}
								}
						
						for (k = 1; k <= user.Usernum; k++)
							for (l = 1; l <= user.Sfcnum[k]; l++)
								for (int m = 1; m <= top.Nodenum; m++)
								{
									PPEqvar ++;
								}
						
						for (k = 1; k <= user.Usernum; k++)
						{
							PPEqvar ++;
							if (k == i)
								PPEq[PPEqnum][PPEqvar] = -1;
						}
						
						PPEqRest[PPEqnum] = 0;
					}
				
				// PP��ʽԼ�� ��9��
				for (i = 1; i <= user.Usernum;i++)
					for (j = 1; j <= user.Sfcnum[i] - 1; j++)
						for (int n = 1; n <= top.Nodenum; n++)
					{
						PPEqnum ++;
						PPEqvar = -1;
						
						for (k = 1; k <= user.Usernum; k ++)
							for (l = 1; l <= user.Sfcnum[k] - 1 ; l++)
								for (int m = 1; m <= link.num; m++)
								{
									PPEqvar ++;
									if ((i == k) && (j == l) && (link.Start[m] == n))
									{
										PPEq[PPEqnum][PPEqvar] = 1;
									}
								}
						
						for (k = 1; k <= user.Usernum; k++)
							for (l = 1; l <= user.Sfcnum[k]; l++)
								for (int m = 1; m <= top.Nodenum; m++)
								{
									PPEqvar ++;
									if ((i == k) && (j == l) && (m == n))
									{
										PPEq[PPEqnum][PPEqvar] = -1;
									}
								}
						
						for (k = 1; k <= user.Usernum; k++)
						{
							PPEqvar ++;
						}
						
						PPEqRest[PPEqnum] = 0;
					}
				
				// PP��ʽԼ��  ��10��
				for (i = 1; i <= user.Usernum;i++)
					for (j = 1; j <= user.Sfcnum[i] - 1; j++)
						for (int n = 1; n <= top.Nodenum; n++)
					{
						PPEqnum ++;
						PPEqvar = -1;
						
						for (k = 1; k <= user.Usernum; k ++)
							for (l = 1; l <= user.Sfcnum[k] - 1 ; l++)
								for (int m = 1; m <= link.num; m++)
								{
									PPEqvar ++;
									if ((i == k) && (j == l) && (link.Destination[m] == n))
									{
										PPEq[PPEqnum][PPEqvar] = 1;
									}
								}
						
						for (k = 1; k <= user.Usernum; k++)
							for (l = 1; l <= user.Sfcnum[k]; l++)
								for (int m = 1; m <= top.Nodenum; m++)
								{
									PPEqvar ++;
									if ((i == k) && ((j+1) == l) && (m == n))
									{
										PPEq[PPEqnum][PPEqvar] = -1;
									}
								}
						
						for (k = 1; k <= user.Usernum; k++)
						{
							PPEqvar ++;
						}
						
						PPEqRest[PPEqnum] = 0;
					}
				
				// PPԼ��2��29��
				for (k = 1; k <= top.Nodenum; k++)
				{
					sumPPcd ++;
					for (j = 1; j <= sumPP; j++)
						PPCond[sumPPcd][j-1] = 0;
					
					int t = 0;// ��ʱ����
					double sum2 = 0;// ��ʱ����
					int m;// ѭ������
					for (i = 1; i <= user.Usernum; i++)
						for (j = 1; j <= user.Sfcnum[i]; j++)
						{
							for (l = 1;l <= top.Nodenum ; l++)
							{
								t ++;
								if (l == k)
								{
									sum2 = 0;
									for (m = 1; m <= sfc.num; m++)
									{
	////									System.out.println(i + " " + j + " " + m);
										sum2 = sum2 + sfc.Consumption[m] * user.g[i][j][m];
									}
									PPCond[sumPPcd][sumw + t-1] = sum2;
								}
							}
						}
					
					if (top.Cap[k] <= 6)
						PPRest[sumPPcd] = top.Cap[k];
					else
						PPRest[sumPPcd] = 6;
				}
				
	
				// PPԼ��3��29��
				for (l = 1; l <= top.Linknum; l++)
				{
					sumPPcd ++;
					for (i = 1 ; i <= sumPP; i++)
					{
						PPCond[sumPPcd][i-1] = 0;
					}
					
					int t = 0;// ��ʱ����
					for(i = 1; i <= user.Usernum; i++)
						for(j = 1; j <= user.Sfcnum[i] - 1; j++)
							for(k = 1; k <= link.num; k++)
							{
								t++;
								PPCond[sumPPcd][t-1] = link.Delta[l][k]*user.Beta[i];
							}
					
					if (top.Ban[l] <= 6)
						PPRest[sumPPcd] = top.Ban[l];
					else
						PPRest[sumPPcd] = 6;
				}
				
				// �����㷨2
				Graph graph = new Graph(top.Nodenum);
				graph.Linknum = top.Linknum;
				
				graph.InitialtheWeight();
				for (i = 0; i <= graph.Nodenum; i++)// ��ʼ������
					for (j = 0; j <= graph.Nodenum; j++) 
					{
						if (i == j )
						{
							graph.Weight[i][j] = 0;
							graph.Shortestpath[i][j] = 0;
						}
						else
						{
							graph.Weight[i][j] = 100000;
							graph.Shortestpath[i][j] = 100000;
						}
					}
				
	//			double[][] a = new double[graph.Nodenum][graph.Nodenum];
	//			for (i = 1; i <= graph.Nodenum ; i++)// ����Ȩ��
	//			{
	//				a[top.Start[i]-1][top.End[i]-1] = top.rho2 + alpha[i];
	//			}
	
				for (i = 1; i <= graph.Linknum; i++)
				{
					graph.Weight[top.Start[i]][top.End[i]] = top.rho2 + alpha[i];
				}
	//			graph.RejudgeTheWeight(a);
				graph.SetKShortestPath(1);//����ڵ����3�����·
				
				boolean[] Nodeflag = new boolean[graph.Nodenum+1];// ��ʶĳ���ڵ��Ƿ�ʹ�ù�
				
				double minQ = 1000000;// ��ʼ����СQֵ
				ArrayList<Integer> Bestlist = new ArrayList<Integer>();// ��ѵĲ��𷽰�
				ArrayList<Integer> Nowlist = new ArrayList<Integer>();// ��ǰ�Ĳ��𷽰�
				int BestUser = -1;// ��ѵĲ����
				
				for (i = 1; i <= user.Usernum; i++)// ����ÿһ��user
				{
					Nowlist.clear();
					boolean[] DCflag = new boolean[top.Nodenum +1];// ��ʶĳ��DC�Ƿ��ù�
					int nowvnf = 2;// ��ǰ���ǵ�vnf
					
					int lastDC = user.Access[i];// ��һ�������
					DCflag[lastDC] = true;
					double tempQ = 0;// ��ʱ����������Qֵ
	
					while(nowvnf <= (user.Sfcnum[i]-1))
					{
						double min = 100000;
						int bestDC = -1;// ��ǰ�ҵ���õ�DC�����
						for (j = 1; j <= top.Nodenum; j++)// ��ÿһ��DC���г���
						{
							if (DCflag[j] == true)
								continue;
							if ((zeta2[i][nowvnf][j] + zeta3[i] + user.Beta[i] * graph.ShortestKpath[lastDC][j][1] ) < min)
							{
								min = zeta2[i][nowvnf][j] + zeta3[i] +  user.Beta[i] * graph.ShortestKpath[lastDC][j][1];
								bestDC = j;
							}
							
						}
						
						if(bestDC != -1)
						{
							tempQ = tempQ + min;
							DCflag[bestDC] = true;
							Nowlist.add(bestDC);
							lastDC = bestDC;
							nowvnf ++;
						}
					}
					
					if (tempQ < minQ) 
					{
						BestUser = i;
						Bestlist.clear();
						Bestlist.addAll(Nowlist);
						minQ = tempQ;// ����С��Qֵ
					}
					
					System.out.println("User " + i +"'s deploy is��" + Nowlist );
					
				}
				
				// %%%%%%%%%%%%%%%
	//			boolean ProblemSolve = true;// ָʾ�����㷨�ܷ���
				boolean ProblemSolve = false;
				if (minQ >= 0)   
					ProblemSolve = false;
				boolean Continue = true;//��ʾ�Ƿ��������
	//			if (minQ >= 0)   Continue = false;
					
				init.InitDeployed(); // ��ʼ�����𷽰�����д�ú�����
				if (minQ < 0)
				{	
					System.out.println(minQ);
					// ���ݵ�ǰ�ҵ���DC�������ʹ�õ�·��
	//				ArrayList<Integer> PastNode = new ArrayList<Integer>();// ��¼�Ѿ���ӽ�·����·
	//				ArrayList<Integer> TempNode = new ArrayList<Integer>();// ��¼ÿһ���ҵ���·
	//				ArrayList<Integer> PathChoose = new ArrayList<Integer>();// ��¼��֧·ѡ��������·
					
	//				j = user.Access[BestUser];
	//				for (i = 0; i <= user.Sfcnum[BestUser]; i++)
	//				{	
	//					boolean flag = true;// ָʾ�Ƿ��ҵ����ʵ�·
	//					
	//					if (i <= (user.Sfcnum[BestUser]-1))
	//						k = Bestlist.get(i);
	//					else 
	//						k = user.Source[BestUser];
	//					
	//					int nowk = 1;// ��ǰ�ҵĵڼ���·��
	//					int nownode = 2;
	//					
	//					while (nowk <= 5)
	//					{
	//						TempNode.clear();
	//						flag = true;// ָʾ�Ƿ��ҵ����ʵ�·
	//						while ((nownode <= top.Nodenum) && (Initgraph.ShortestKPathNode[j][k][nowk][nownode] != 0))
	//						{            
	//							if (PastNode.contains(Initgraph.ShortestKPathNode[j][k][nowk][nownode]) == true)
	//							{
	//								flag = false;
	//								break;
	//							}
	//							else
	//							{
	//								TempNode.add(Initgraph.ShortestKPathNode[j][k][nowk][nownode]);
	//								nownode ++;
	//							}
	//						}
	//						
	//						if (flag == true)
	//						{
	//							PastNode.addAll(TempNode);
	//							PathChoose.add(nowk);
	//							break;
	//						}
	//						
	//						nowk ++;
	//					}
	//					
	//					if (flag == false)
	//					{
	//						ProblemSolve = false;
	//						break;
	//					}
	//					else 
	//						j = k;
	//				}
					
					if (ProblemSolve == true)
					{// �趨����
						deployed.x[BestUser] = 1;
						
						j = user.Access[BestUser];
						deployed.z[BestUser][1][j] = 1;
						for (i = 2; i <= user.Sfcnum[BestUser]; i++)
						{
							if (i <= user.Sfcnum[BestUser]-1)
								k = Bestlist.get(i-2);
							else 
								k = user.Source[BestUser];
	//						l = PathChoose.get(i-1);
							
	//						deployed.z[BestUser][i][(j-1)*top.Nodenum*5+(k-1)*5+l] = 1;
							deployed.z[BestUser][i][k] = 1;
							
							j = k;
						}
					}
				}
				
				
				if (ProblemSolve == false) // ��������㷨�޷��жϣ����PP����
				{
					System.out.println("Solving Price Problem");
					
					try {  
			    	  Intoptimator intoptimator = new Intoptimator();
			    	  Object[] rs1 = intoptimator.intoptimator(2,PPGoal, PPRange, PPCond,  PPRest,PPEq, PPEqRest, Downs1,Ups1);
					    	  
	//				    	  MWNumericArray is = new MWNumericArray(Double.valueOf(rs[0].toString()),MWClassID.DOUBLE);
					    	  MWNumericArray is1 = (MWNumericArray)rs1[0];
					    	  for(i = 1; i <= sumPPG+1 ; i++)
					    	  {
					    		  PPVar[i] = is1.getDouble(new int[]{i, 1});
	//				    		  System.out.println(PPVar[i]);
					    	  }
					    	  System.out.println(rs1[1]);
					    	  MWNumericArray is2 = (MWNumericArray)rs1[1];
					    	  Q = is2.getDouble(new int[] {1,1}) + eta;
					    	  System.out.println("Q = " + Q);
					    	  rs1 = null;
					    	  is1 = null;
					    	  is2 = null;
					     	}  
					  
					catch (Exception e) {  
					     System.out.println("Exception: " + e.toString());  
					 }  
					finally {
				   	 	System.gc();
					}
					
					double Qchange;
					Qchange = Math.abs(lastQ - Q); 
					lastQ = Q;
					if ((Q >= 0) || (Qchange <= 1.0E-7) )
					{
						Continue = false;
					}
	//				else
	//				{
						init.InitDeployed();
						int sumPPvar = 0;
	
						for(i = 1; i <= user.Usernum; i++)
							for (j = 1; j <= user.Sfcnum[i]-1; j++)
								for (k = 1; k <= link.num; k++)
								{
									sumPPvar++;
									deployed.w[i][j][k] = PPVar[sumPPvar];
								}
						
						for(i = 1; i <= user.Usernum; i++)
							for ( j = 1;j <= user.Sfcnum[i]; j++)
								for (k = 1; k <= top.Nodenum; k++)
								{
									sumPPvar++;
									deployed.z[i][j][k] = PPVar[sumPPvar];
								}
						
						for (i = 1; i <= user.Usernum; i++)
						{
							sumPPvar++;
							deployed.x[i] = PPVar[sumPPvar];
						}
							
	//				}
				}
				
				if (column.length == 300)
					Continue = false;
						
				
	//			if (Continue == false)
	//			{
	//				System.out.println("can't continue");
	//				break;
	//			}
	//			
	//			System.out.println("stage2");
	
				
				// ������
				column.length ++;
				//m[i][c]
				for(i = 1; i <= user.Usernum; i++)
				{
					column.m[i][column.length] = deployed.x[i];
				}
				
				//h[c]
				column.h[column.length] = 0; 
				for(i = 1; i <= user.Usernum; i++)
					for (j = 2; j <= user.Sfcnum[i]; j++ )
						for ( k = 1; k <= top.Nodenum; k++)
						{
							double sum = 0;
							for (l = 1; l <= sfc.num; l++)
							{
								sum = sum + (1 - user.f[i][k][l])*user.g[i][j][l];
							}
							
							column.h[column.length]= column.h[column.length] + sum * deployed.z[i][j][k]; 
						}
				//a[c][v][p]
				for(i = 1; i <= top.Nodenum; i++)// v
					for (j = 1; j <= sfc.num; j++)// p
					{
						column.a[column.length][i][j]  = 0;
						for (k = 1; k <= user.Usernum; k++)// i
							for (l = 1; l <= user.Sfcnum[k]; l++)// j
								column.a[column.length][i][j] = column.a[column.length][i][j] + user.g[k][l][j] * deployed.z[k][l][i];
					}
				
				// b[c][e]
				for(i = 1; i <= top.Linknum; i++)// e
				{
					column.b[column.length][i] = 0;
					for(j = 1; j <= user.Usernum; j++)// i
						for(k = 1; k <= user.Sfcnum[j] -1; k++)// j
							for(l = 1; l <= link.num; l++)// ��
							{
								column.b[column.length][i] = column.b[column.length][i] + user.Beta[j]*link.Delta[i][l]*deployed.w[j][k][l];
							}
				}
				
				// reduced cost of ��c
	//			double ReducedCost = 0;
	//			for (i = 1; i <= user.Usernum; i++)// i
	//			{
	//				ReducedCost = ReducedCost + column.m[i][column.length] * (gamma[i] - user.r[i]);
	//			}
	//			
	//			for (i = 1; i <= top.Linknum; i++)// e
	//			{
	//				ReducedCost = ReducedCost + column.b[column.length][i] * (top.rho2 + alpha[i]);
	//			}
	//			
	//			for (i = 1; i <= top.Nodenum; i++)// v
	//				for (j = 1; j <= sfc.num; j++)// p
	//				{
	//					ReducedCost = ReducedCost + chi[i][j] * column.a[column.length][i][j];
	//				}
	//			
	//			ReducedCost = ReducedCost + phi * column.h[column.length] + eta;
	//			
	//			System.out.println("ReducedCost is:" + ReducedCost);
				
				if (Continue == false)
				{
					System.out.println("can't continue");
					break;
				}
				
				System.out.println("stage2");
				
			}while(true);
	
			// ��������������һ��ILM-MP���⣬������մ�
			// MP�����ϵ������Ϊ[��1��...��c��y1.1,y1.2,...,y1.p,y2.1,...,yv.p] ��c+v*p��ϵ��
			double[] MpGoal = new double[1000];// MP����Ŀ�꺯����ϵ��
			double[][] MpCond = new double[1000][1000];// MP����Լ��������ϵ��
			double[] MpRest = new double[1000];// MP����Լ��������Լ��
			
			// MP�����Ŀ�꺯����ϵ��
	//		MpGoal[0] = 0;
			int sumMp = -1;// MpĿ�꺯����ϵ������
			int i = 0; // ѭ������
			int j = 0; // ѭ������
			
			for (i = 1; i <= column.length ; i++)
			{
				double sum1 = 0; // ͳ�Ƹ�����ܺ�
				double sum2 = 0; // ������ʱ��������
				for (j = 1; j <= top.Linknum; j++)
				{
					sum2 = sum2 + column.b[i][j];
				}
				sum1 = sum1 + top.rho2 * sum2;
				sum2 = 0;
				for (j = 1; j <= user.Usernum; j++)
				{
					sum2 = user.r[j] * column.m[j][i];
				}
				sum1 = sum1 - sum2;
				sumMp++;
				MpGoal[sumMp] = sum1;
			}
			
			for (i = 1; i <= top.Nodenum; i++)
			{
				for (j = 1; j <= sfc.num; j++)
				{
					sumMp++;
					MpGoal[sumMp] = top.rho1 * sfc.Consumption[j];
				}
			}
			
			// MP����ı���
			double[] MpVar = new double[sumMp+2];
	
			// MP�����Լ��������ϵ��
			int sumCd = -1;// Լ�������ĸ���
	//		double sum1 = 0;// ������ʱ��������  
			
	//		for(i = 1;i <= sumMp; i++)
	//			MpCond[sumCd][i] = 0;
			
			sumCd++;
			
			//Լ��1��15��
			for(i = 0; i <= column.length-1; i++)
			{
				MpCond[sumCd][i] = column.h[i];
			}
			for(i = column.length; i <= sumMp-1; i++)
			{
				MpCond[sumCd][i] = 0;
			}
			MpRest[sumCd] = sfc.Hmax;
	
			//Լ��2��16��
			int k; //ѭ������
			for (i = 1 ; i <= top.Nodenum; i++)
			{
				sumCd++;
				for (j = 1; j <= sumMp; j++)
					MpCond[sumCd][j-1] = 0;
				for (j = column.length+(i-1)*sfc.num+1; j <= column.length+i*sfc.num; j++)
				{
					for (k = 1; k <= sfc.num; k ++)
						MpCond[sumCd][j-1] = sfc.Consumption[k];
				}
				MpRest[sumCd] = top.Cap[i];
				
			}
			
			//Լ��3��17��
			for (i = 1;i <= top.Linknum; i++)
			{
				sumCd++;
				for (j = 1; j <= sumMp; j++)
					MpCond[sumCd][j-1] = 0;
				for (j = 1; j <= column.length; j++)
				{
					MpCond[sumCd][j-1] = column.b[j][i];
				}
				MpRest[sumCd] = top.Ban[i];
				
			}
			
			// Լ��4��18��
			for (i = 1; i <= user.Usernum ; i++)
			{
				sumCd++;
				for (j = 1; j <= sumMp; j++)
					MpCond[sumCd][j-1] = 0;
				for (j = 1; j <= column.length; j++)
				{
					MpCond[sumCd][j-1] = column.m[i][j];
				}
				MpRest[sumCd] = 1;
				
			}
			
			// Լ��5��19��
			sumCd++;
			for(i = 1 ; i <= sumMp; i++)
				MpCond[sumCd][i-1] = 0;
			for(i = 1 ; i <= column.length; i++)
				MpCond[sumCd][i-1] = 1;
			MpRest[sumCd] = user.Usernum;
	
			// Լ��6��20��
	//		int l;// ѭ������
	//		int m;// ѭ������
			for (j = 1; j <= top.Nodenum; j++)
				for (i = 1; i <= sfc.num; i++)
			{
				sumCd++;
				for(k = 1; k <= sumMp; k++)
					MpCond[sumCd][k-1] = 0;
					
				for(k = 1; k <= column.length ;k++)
					MpCond[sumCd][k-1] = column.a[k][j][i];
				MpCond[sumCd][column.length+(j-1)*(top.Nodenum)+i-1] = -sfc.Maxuser[i];
				MpRest[sumCd] = 0;
			}
			
			double[] Ups = new double[1000];// ��������
			double[] Downs = new double[1000];// ��������
			double[][] MpEqCond = new double[1][1000];
			double[] MpEqRest = new double[1];
			
			for (i = 0; i <= column.length-1; i++)
			{
				Ups[i] = 1;
				Downs[i] = 0;
			}
			for (i = column.length; i <= (sumMp-1); i++)
			{
				Ups[i] = user.Usernum;
				Downs[i] = 0;
			}
			
	//		try 
	//		{
	//			opt.optimate(MpGoal, MpCond, null, MpRest, null, Ups);
	//			System.out.println(opt.getObjective());
	//			System.out.println(Arrays.toString(opt.getVariables()));
	//			for (i = 1 ; i <= sumMp; i++)
	//				MpVar[i] = opt.getVariables()[i-1];
	//			
	//		}catch (LpSolveException e)
	//		{
	//			  e.printStackTrace();
	//		}
	//	        System.out.println( System.getProperty("java.library.path"));
			double[] MpRange = {1,1000};
			
			Intoptimator intoptimator;
			   try {  
			    	  intoptimator = new Intoptimator();
			    	  Object[] rs = intoptimator.intoptimator(2,MpGoal, MpRange, MpCond,  MpRest,MpEqCond, MpEqRest, Downs,Ups);
	//		    	  System.out.println(rs[0]);
	//		    	  System.out.println(rs[1]);
			    	  
	//		    	  MWNumericArray is = new MWNumericArray(Double.valueOf(rs[0].toString()),MWClassID.DOUBLE);
			    	  MWNumericArray is = (MWNumericArray)rs[0];
			    	  for(i = 1; i <= sumMp+1 ; i++)
			    	  {
			    		  MpVar[i] = is.getDouble(new int[]{i, 1});
	//		    		  System.out.println(MpVar[i]);
			    	  }
			    	  rs = null;
			    	  is = null;
			     	}  
			  
			   catch (Exception e) {  
			        System.out.println("Exception: " + e.toString());  
			   }  
			   finally {
			   	 System.gc();
			   }
			   
			mpsolve.lamda = new double[column.length + 1];
			mpsolve.y = new double[top.Nodenum + 1][sfc.num + 1];
			for (i = 1; i <= column.length; i++)
			{
				mpsolve.lamda[i] = MpVar[i];
				System.out.println(mpsolve.lamda[i]);
			}
			
			tempcount = column.length;
			for (i = 1; i <= (top.Nodenum); i++)
				for (j = 1; j <= (sfc.num); j++)
				{
					tempcount = tempcount + 1;
					mpsolve.y[i][j] =  MpVar[tempcount];
				}
			
			
	//		for (i = 1; i <= (top.Nodenum); i++)
	//		{
	//			System.out.println("in data center" + i);
	//			for (j = 1; j <= (sfc.num); j++)
	//			{
	//				System.out.print(mpsolve.y[i][j] + " ");
	//			}
	//			System.out.println("");
	//		}
			
			double endtime = System.currentTimeMillis();
			// 持续时间
			double duringtime = endtime - begintime;
			// 记录数据
			for (i = 1; i <= sfc.num; i++)
			{
				for (j = 1; j <= top.Nodenum; j ++)
				{
					if (column.a[column.length][j][i] >= 0.99)
					{
						
						System.out.println("VNF " + i + "deployed on Node" + j);
						if (i != sfc.num)
						{
							check.changeVNFnum++;
							check.changeVNFdeployon[check.changeVNFnum] = j;
						}
					}
				}
			} 
			
			// 迁移开销
			int changeConsumption = 0;
			for (i = 1; i <= user.Usernum; i++)
				for (j = 2; j <= (user.Sfcnum[i]-1); j++)
					{
						if (deployed.z[i][j][info.VNFdeployedon[SFCchange][j-1]] != 1)
						{
							changeConsumption ++;
						}
					}
			
			for (i = 1; i <= top.Linknum; i++)
			{
				if (column.b[column.length][i] >= (check.changeLinkBandwidth - 1))
				{
					System.out.println("Link " + i + "is used!!");
					System.out.println("from " + top.Start[i] + " to " + top.End[i]);
					System.out.println("Using Capacity: " + column.b[column.length][i]);
					check.changeLinkusingnum ++;
					check.changeLinkusing[check.changeLinkusingnum] = i;
					check.changeLinkusingcapacity[check.changeLinkusingnum] = column.b[column.length][i];
				}
			}
			
			double usingBandwidth = check.changeLinkusingnum*info.SFCbandwidth[check.changeSFC];
			
					
			// 检查数据
			boolean ReDeployedSuccess = true;
			int[] deployednum = new int[top.Nodenum + 1];
			for (i = 1; i <= check.changeVNFnum; i++)
			{
				deployednum[check.changeVNFdeployon[i]] ++;
			}
			
			for (i = 1; i <= top.Nodenum; i++)
			{
				if (info.RemainITresource[i] < (5 * deployednum[i]))
				{
					System.out.println("This ReDeployed is fail");
					ReDeployedSuccess = false;
					break;
				}
			}
			
			if (ReDeployedSuccess == true)
			{
				for (i = 1; i <= check.changeLinkusingnum; i++)
				{
					if (info.Remainbandwidth[check.changeLinkusing[i]] < check.changeLinkusingcapacity[i])
					{
						System.out.println("This ReDeployed fail !!");
						ReDeployedSuccess = false;
						break;
					}
				}
			}
			
			if (ReDeployedSuccess == true)
				System.out.println("This ReDeployed Success!!");
			
			// 若数据正确，则原info发生调整
			if (ReDeployedSuccess == true)
			{
				info.SFCsource[SFCchange] = check.changeSource;
				info.SFCsink[SFCchange] = check.changeSink;
				for (i = 1; i <= info.VNFnum[SFCchange]; i++)
				{
					info.VNFdeployedon[SFCchange][i] = check.changeVNFdeployon[i];
				}
				info.SFCusinglinknum[SFCchange] = check.changeLinkusingnum;
				info.SFCusinglink[SFCchange] = new int[50];
				for (i = 1; i <= info.SFCusinglinknum[SFCchange]; i++)
				{
					info.SFCusinglink[SFCchange][i] = check.changeLinkusing[i];
				}
				
				for (i = 1; i <= top.Nodenum; i++)
				{
					info.RemainITresource[i] -= 5 * deployednum[i];
				}
				
				for (i = 1; i <= check.changeLinkusingnum; i++)
				{
					info.Remainbandwidth[check.changeLinkusing[i]] -= check.changeLinkusingcapacity[i];
				}
				
				try 
				{
					File file1 = new File("./topology/Testinfo.txt"); 
					FileWriter fw1 = new FileWriter(file1, true);
					PrintWriter out1 = new PrintWriter(new BufferedWriter(fw1));
					
					
			        out1.println("change SFC numble: " + SFCchange);
			        out1.println("last SFC Source numble: " + lastSource);
			        out1.println("last SFC Sink numble: " + lastSink);
			        out1.println("change SFC Source numble: " + info.SFCsource[SFCchange]);
			        out1.println("change SFC Sink numble: " + info.SFCsink[SFCchange]);
			        
			        out1.println("Algorithm During time: " + duringtime);
			        out1.println("Algorithm Bandwidth Using: " + usingBandwidth);
			        out1.println("Algorithm Changging Using: " + changeConsumption);
			        out1.println("----------------------------------------------------------");
			        out1.flush();
				}
				catch (FileNotFoundException e)
				{
					System.err.println("文件Testinfo.txt不存在");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
			}
			System.out.println("");
			
	
	 }
	}
}
