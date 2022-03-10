package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;

import javax.naming.spi.DirStateFactory.Result;

import main.Vector.Tuple;

public class TacConverter { //converts ast to three address code, 3 since there can be 3 operands x, y, z, 1 operator
	//x = y + z
 
	public class Tac { 
		/*forms of Tac:
		 * dst = arg1 op arg2
		 * dst = arg1
		 * dst = not arg1
		 * label x, list<tac>
		 * if, ,condition, label
		*/
		String opr, dst;
		Object op1, op2;  
		
		public Tac(String dst, String opr, Object op1, Object op2) {
			this.dst = dst; this.opr = opr; this.op1 = op1; this.op2 = op2;
		}
		
		public String toString() {
			if (dst.equals("if")) {
				Tac op1 = (Tac)this.op1; //condition
				Tac op2 = (Tac)this.op2; //label
				return dst+" "+op1.dst+" goto "+op2.dst;
			}
			else if (dst.equals("param")) {
				return dst+" "+op1;
			}
			else if (dst.equals("arg")) {
				Tac op1 = (Tac)this.op1;
				return dst+" "+op1.dst;
			}
			else if (dst.contains("label")) {
				String str = "\ndefine "+dst+":";
				List<Tac> tacs = (List<Tac>)op1;
				for (Tac tac: tacs) {
					str+="\n\t"+tac.toString();
				}
				return str+"\n";
			}
			else if (dst.equals("goto")) {
				return dst+" "+op1;
			}
			else if (op1 == null) //just a variable, number, string, stored in dst
				return dst;
			else if (opr != null && opr.equals("not")) {
				Tac op1 = (Tac)this.op1;
				return dst+" = "+opr+" "+op1.dst;
			}
			
			try {
				Tac op1 = (Tac)this.op1;
				Tac op2 = (Tac)this.op2;
				return String.format("%s = %s %s %s", dst, op1==null?"":op1.dst, op2==null?"":opr, op2==null?"":op2.dst);
			}
			catch (Exception e) {
				//e.printStackTrace();
				//Util.print("cannot cast "+this.op1+","+this.op2);
				//return "";
				return String.format("%s = %s %s %s", dst, op1==null?"":op1, op2==null?"":opr, op2==null?"":op2);
			}
		}
	}

	
	public int addTacs(Tree statement, List<Tac> tacs, int counter) {
		String val = (String)statement.val;
		//Util.print("getting tac for "+val);
		String dst = "", opr = null;
		Object op1 = null;
		Object op2 = null;
		
		switch (val) {
		case "+": case "-": case "/": case "*": case "and": case "or": case"<":case">":case"==":case"<=":case">=":
			Tree t1 = (Tree)statement.children.get(0);
			Tree t2 = (Tree)statement.children.get(1);
			
			counter = addTacs(t1, tacs, counter);
			op1 = tacs.get(tacs.size()-1);
			
			counter = addTacs(t2, tacs, counter);
			op2 = tacs.get(tacs.size()-1);
			
			dst = "tmp"+(counter++);
			opr = val;
			break;
		case "not":			
			t1 = (Tree)statement.children.get(0);
			
			counter = addTacs(t1, tacs, counter);
			op1 = tacs.get(tacs.size()-1);
					
			dst = "tmp"+(counter++); //using a tmp var tmpcounter
			opr = val;
			break;
		case "else":
			Tuple result1 = convert(statement.children.get(0), counter); //convert else body statements to tacs
			List<Tac> bodyTacs = (List<Tac>)(result1.get(0));
			counter = (int)result1.get(1);	
			
			tacs.addAll(bodyTacs);
			return counter;
		case "if": case "elif": //has a condition tree, a body tree of statements, and an else tree
			t1 = new Tree("not");
			//negate the condition, to jump to else, or continue if body
			t1.addChild((Tree)(statement.children.get(0).children.get(0))); //condition is child of 'CONDITION' root
			
			t2 = (Tree)statement.children.get(1); //body
			Tree t3 = (Tree)statement.children.get(2); //else body

			counter = addTacs(t1, tacs, counter);
			op1 = tacs.get(tacs.size()-1); //op1 is condition tac
			
			//put labels on blocks to jump to
			result1 = convert(t2, counter); //if body lines parsed into tacs
			bodyTacs = (List<Tac>)(result1.get(0));
			counter = (int)result1.get(1);			
			
			Tuple result2 = convert(t3.children.get(0), counter); //convert else body child's statements
			List<Tac> elseBodyTacs = (List<Tac>)(result1.get(0));
			counter = (int)result1.get(1);	
			
			//List<Tac> elseBodyTacs = new ArrayList<Tac>();
			//counter = addTacs(t3, elseBodyTacs, counter);
			
			//Util.print("defining if else labels "+(counter+1)+", "+(counter+2));
			Tac ifLabel = new Tac("label"+(counter++), null, bodyTacs, null); //label the if block
			Tac elseLabel = new Tac("label"+(counter++), null, elseBodyTacs, null); //label the else block
			
			tacs.add(ifLabel); //define the labels before goto calls
			tacs.add(elseLabel); 
			
			tacs.add(new Tac("if", null, op1, ifLabel)); //if the condition op1, then jump to label
			tacs.add(new Tac("goto", null, elseLabel.dst, null)); //if didnt jump according to if, then enter else block
			//tacs.addAll(bodyTacs); //the if body code			
			return counter;
		case "FUNCTION": //create label for function
			t1 = (Tree)statement.children.get(0); //name
			t2 = (Tree)statement.children.get(1); //parameters
			t3 = (Tree)statement.children.get(2); //body
			
			result1 = convert(t3, counter); //if body lines parsed into tacs
			bodyTacs = new ArrayList<Tac>();
			
			for (Tree param: t2.children) { //add parameters to label's body
				bodyTacs.add(new Tac("param", null, param.val, null));
			}
			
			bodyTacs.addAll((List<Tac>)(result1.get(0)));
			counter = (int)result1.get(1);	
			
			Tac label = new Tac("label_"+(String)t1.val, null, bodyTacs, null);
			tacs.add(label);
			break;
		case "FUNC_CALL":
			t1 = (Tree)statement.children.get(0); //function name
			t2 = (Tree)statement.children.get(1); //function arguments
			
			String labelName = (String)t1.val;
			
			for (Tree argTree: t2.children) {
				counter = addTacs(argTree, tacs, counter);
				Tac argResult = tacs.get(tacs.size()-1);
				Tac argTac = new Tac("arg", null, argResult, null); //argResult
				tacs.add(argTac);
			}
			
			dst = "goto";
			op1 = labelName;
			break;	
		//TODO: while, if, function bodies are new scope
		case "while":
			//similar to if
			t1 = new Tree("not");
			t1.addChild(statement.children.get(0).children.get(0)); //condition's child
			t2 = statement.children.get(1); //loop body
			
			counter = addTacs(t1, tacs, counter); 
			op1 = tacs.get(tacs.size()-1);
			
			
			result1 = convert(t2, counter); //body lines parsed into tacs
			bodyTacs = (List<Tac>)(result1.get(0));
			counter = (int)result1.get(1);	
			
			Tac whileLabel = new Tac("label"+(counter++), null, bodyTacs, null); 
			
			bodyTacs.add(0, new Tac("if", null, op1, new Tac("break", null, null, null))); //break condition at start of loop body
			bodyTacs.add(new Tac("goto", null, whileLabel.dst, null)); //go back to start of loop body
			
			
			t2 = statement.children.get(1); //while loop's body
			
			tacs.add(whileLabel);
			tacs.add(new Tac("goto", null, whileLabel.dst, null));
			return counter;
		case "ASSIGNMENT": //id = expr
			dst = (String)(statement.children.get(0).val);
			t1 = (Tree)statement.children.get(1);
			
			counter = addTacs(t1, tacs, counter);
			op1 = tacs.get(tacs.size()-1);
			break;
		default: //a number, string, id, has no effect in program
			//Util.print("default "+val);
			//dst = val;
			op1 = val;
			dst = "tmp"+(counter++);
		}
		
		//if (dst == "")
		//	Util.print("empty dst for: "+val);
		
		Tac tac = new Tac(dst, opr, op1, op2);
		tacs.add(tac);
		
		return counter;
	}
	
	public Tuple convert(Tree statementTree, int counter) {
		 List<Tac> tacs = new ArrayList<Tac>();
		//ast is root of all statements.		
		for (Tree statement: statementTree.children) {
			//Util.print("at convert on "+statement.val);
			counter = addTacs(statement, tacs, counter);
			
		}
		return new Tuple(tacs, counter);
	}
	
	public static void main(String[] args) {
		String code;
		///Users/paw/Desktop/courses/488/project/sprint2/Python/testInput.py
		
		if (args.length > 0) {
			code = Parser.getCode(args);
		}
		else 
			code = Parser.codes[Parser.codes.length-1];
		//code = Parser.codes[0];
		//code = "a = ((-b) + c) * d";
		//code = "(not (x + y)) * (y + z) + (x + y + z)";
		//code = "if x:\n\tf(x,-y)\nelif 0:\n\tx=1\nelif x<y:\n\t'hi'\nelse:\n\t'why'";
		//code = Parser.codes[0];
		
		test(code);
		
	}
	
	public static void test(String code) {
		//Util.print("----Input code----\n\n"+code);

		Tokenizer t = new Tokenizer();
		List<Tuple> tokens = t.tokenize(code);
		
		Parser p = new Parser();
		p.setTokens(tokens);
		Tree ast = p.parse();
		
		TacConverter c = new TacConverter();
		//Util.print("\n ----- \n Syntax Tree\n ----- \n");
		//Util.print(ast.printTree(0));
		Tuple result = c.convert(ast, 0);
		
		List<Tac> tacs = (List<Tac>)(result.get(0));
		int tmps = (int)result.get(1);
		
		Util.print("\n\n ------\n IR Code Generated (Three Address Code):\n ------\n");
		for (Tac tac: tacs)
			Util.print(tac);
	}
	
	//Tuple result1 = getTac(t1, tacs, counter);
	//op1 = (Tac)(result1.get(0));
	//counter = (int)(result1.get(1));
	
}

