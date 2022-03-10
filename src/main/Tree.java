package main;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;    

import main.Vector.Tuple;

public class Tree { //Abstract Syntax Tree

	public Object val;
	public List<Tree> children;
	
	public Tree(Object val, Tree... children) {
		this.val = val;
		if (children != null) {
			this.children = new ArrayList<Tree>();
			this.children.addAll(Arrays.asList(children));
		}
		else
			this.children = new ArrayList<Tree>(2);
	}
	
	public Tree(Object val) {
		this(val, new Tree[] {});
	}
	
	public void addChild(Tree child) {
		this.children.add(child);
	}
	
	public void addChild(Collection<Tree> children) {
		this.children.addAll(children);
	}
	
	public static int getHeight(Tree root) {
		if (root == null)
			return 0;
		int maxHeight = 1, height = 0;
		for (Tree child: root.children)
			if ((height = getHeight(child)+1) > maxHeight) 
				maxHeight = height;
		return maxHeight;
	}
	
	public static List<Tuple> bfsTraversal(Tree root, int maxDepth) {
		LinkedList<Tuple> queue = new LinkedList<Tuple>();
		List<Tuple> bfs = new ArrayList<Tuple>();
		queue.add(new Tuple(root, 0, null));
		while (!queue.isEmpty()) {
			Tuple t = queue.poll();
			Tree ttree = (Tree)t.get(0);
			int depth = (int)t.get(1);
			
			bfs.add(new Tuple((ttree == null)?null:ttree.val, depth, t.get(2)));
			if (ttree != null)
				for (Tree child: ttree.children) {
					queue.add(new Tuple(child, depth+1, ttree));
				
				}
		}
		return bfs;
	}
	
	public String printTree(int offset) {
		StringBuilder branch = new StringBuilder();
		branch.replace(0, offset, new String(new char[offset]).replace('\0', ' '));
		for (int i = 0; i < offset; i+=2) {
			branch.setCharAt(i, '|');
		}
		String spaces = branch.toString();
		if (offset > 0) {
			branch.setCharAt(offset-1, '-');
			branch.append('>');
		}
		
		String s = spaces+"\n"+branch.toString()+"("+this.val+")"+"\n";
		
		for (Tree child: children) {
			s += child.printTree(offset+2);
		}
		return s;
	}
	
	public JSONObject toJson() {
		 Map<Object, Object> json = new HashMap<Object, Object>();
		 JSONArray list = new JSONArray();
		 json.put(val, list);
		 for (Tree child: children) {
			 list.add(child.toJson());
		 }
		 return new JSONObject(json);
	}
	
	public static void main(String[] args) {
		Tree root = new Tree("+", new Tree("4", new Tree("3")), new Tree("5"));
		Util.print("syntax tree:\n"+root.printTree(0));
		JSONObject json = root.toJson();
		//Util.print(json.toString());
	}
}
