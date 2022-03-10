package main;

import java.util.ArrayList;
import java.util.List;

import main.TacConverter.Tac;
import main.Vector.Tuple;

public class STConverter {

	
	public class Tac {
		
		public Tac(String op, String arg1, String arg2, int result) {
			
		}
	}
	
	private Tree ast;
	private static List<Tac> tacs = new ArrayList<Tac>();

	public void setAst(Tree ast) {
		this.ast = ast;
		tacs.clear();
	}
	
	public List<Tac> convert() {
		//ast is root of all statements.
		for (Tree statement: ast.children) {
			
		}
		
		return tacs;
	}
	
	public static void main(String[] args) {
		String code;
		if (args.length > 0) {
			code = Parser.getCode(args);
		}
		else 
			code = Parser.codes[0]; //Parser.codes.length-1];
		code = "a = −b + c ∗ d";
		test(code);
		
	}
	
	public static void test(String code) {
		Tokenizer t = new Tokenizer();
		List<Tuple> tokens = t.tokenize(code);
		
		Parser p = new Parser();
		p.setTokens(tokens);
		Tree ast = p.parse();
		
		STConverter c = new STConverter();
		c.setAst(ast);
		c.convert();
	}
	
}
