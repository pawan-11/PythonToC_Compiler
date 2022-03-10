package main;


import java.util.Arrays;
import java.util.List;

public class Vector<T> {
	
	protected T[] xs;
	
	public Vector(T[] xs) { //could add boolean final, to indicate whether this Vector should ever be modified	
		copy(xs);
	}

	public static class Tuple extends Vector<Object> {
		
		public Tuple(Object... o) {
			super(o);
		}
		
		public String toString() {
			return Arrays.toString(xs);
		}
	}
	
	public void set(int idx, T x) {
		xs[idx] = x;
	}
	
	public Vector<T> flip() {
		for (int i = 0; i < getDim()/2; ++i) {
			T tmp = xs[i];
			xs[i] = xs[getDim()-1-i];
			xs[getDim()-i-1] = tmp;
		}
		return this;
	}
	
	public void setXs(T[] xs) { this.xs = xs; }
	public T get(int dim) { return xs[dim]; }
	public T[] getXs() { return xs; }
	public Vector<T> copy() { return new Vector<T>(xs.clone()); }
	public Vector<T> copy(Vector<T> p) { return copy(p.getXs().clone()); }
	
	public Vector<T> copy(T[] newXs) {
		setXs(newXs);
		return this;
	}
	
	public boolean equals(Vector<T> p) {
		return equals(p.getXs());
	}

	public boolean equals(T[] xs) {
		for (int i = 0; i < getDim(); i++)
			if (xs[i] != this.xs[i])
				return false;
		return true;
	}
	
	public int getDim() {
		return xs.length;
	}
	
	public String toString() {
		return "Vector"+getDim()+"D: "+Arrays.toString(xs);
	}
}
