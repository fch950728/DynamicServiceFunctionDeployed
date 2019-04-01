package base;

/**
 * 创造一个类似C++中的Pair类，用于存储临时数据
 * 采用泛型保证Pair的通用性
 * @author ly
 *
 */
public class Pair<E extends Object, F extends Object> {

	private E first;
	private F second;
	
	public Pair() {
		
	}
	
	public Pair(E val1, F val2) {
		this.first = val1;
		this.second = val2;
	}

	public E getFirst() {
		return first;
	}

	public void setFirst(E first) {
		this.first = first;
	}

	public F getSecond() {
		return second;
	}

	public void setSecond(F second) {
		this.second = second;
	}

	public Pair<E, F> makePair(E val1, F val2) {
		return new Pair<E, F>(val1, val2);
	}
}
