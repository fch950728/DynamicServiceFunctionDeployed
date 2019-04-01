package BFS;

import java.util.LinkedList;

public class PathAndDistence {
	public LinkedList<Integer> path;
	public LinkedList<Integer> edgepath;
	public Integer distence;
	public PathAndDistence(LinkedList<Integer> path,LinkedList<Integer> edgepath,Integer distence) {
		this.path=path;
		this.distence=distence;
		this.edgepath=edgepath;
		
	}

}
