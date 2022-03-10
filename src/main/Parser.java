package main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import main.Vector.Tuple;



public class Parser {

	private List<Tuple> tokens;

	public Parser() {
	}

	public void setTokens(List<Tuple> tokens) {
		this.tokens = tokens;
	}

	private int getTabs(int i) {
		if (i >= tokens.size()) return 0;
		Tuple t = tokens.get(i);
		String tokenType = (String)t.get(0);

		int tabs = 0;
		int lineNo = (int)t.get(2);
		while (tokenType.equals("TAB") && (int)t.get(2) == lineNo) { //count the tabs
			tabs++;
			t = tokens.get(i+tabs);
			tokenType = (String)t.get(0);
		}
		return tabs;
	}

	public Tuple assignmentCase(int i) {
		Tuple id = tokens.get(i);
		Tuple equalSign = tokens.get(i+=1);
		Tuple valueResult = getTree(i+1, 0);

		Tree root = new Tree("ASSIGNMENT");

		Tree valueTree = (Tree)valueResult.get(0);
		root.addChild(new Tree((String)id.get(1))); //id name
		root.addChild(valueTree);

		return new Tuple(root, (int)valueResult.get(1));
	}

	public Tuple getArgResult(int i) {
		List<Tree> argTrees = new ArrayList<Tree>();

		Tuple argResult = getTree(i, 0);
		Tree argTree = (Tree)argResult.get(0);
		i = (int)argResult.get(1);

		while (argTree != null) {
			argTrees.add(argTree);
			String nextTokenType = (String)(tokens.get(i).get(0));
			if (!nextTokenType.equals("COMMA"))
				break;
			argResult = getTree(i+1, 0);
			argTree = (Tree)argResult.get(0);
			i = (int)argResult.get(1);
		}

		return new Tuple(argTrees, i);
	}

	public Tuple functionCase(int i, int tabs) {
		Tuple token = tokens.get(i);
		String word = (String)token.get(1);
		Tree root = new Tree("FUNCTION");

		Tuple name = tokens.get(i+=1);
		Tuple openBracket = tokens.get(i+=1); //parameters start (args..)

		Tuple argResult = getArgResult(i+1);
		List<Tree> argTrees = (List<Tree>)argResult.get(0);
		i = (int)argResult.get(1);

		Tree nameTree = new Tree(name.get(1));
		root.addChild(nameTree);

		Tree parametersTree = new Tree("PARAMETERS");
		parametersTree.addChild(argTrees);
		root.addChild(parametersTree);

		Tuple rParan = tokens.get(i);
		Tuple colon = tokens.get(i+=1);
		//Util.print("rparan ,"+rParan+" colon- "+colon);

		Tree body = new Tree("BODY");
		Tuple bodyResult = parse(i+1, tabs+1);
		List<Tree> bodyTrees = (List<Tree>)bodyResult.get(0);
		i = (int)bodyResult.get(1);

		body.addChild(bodyTrees);

		root.addChild(body);

		return new Tuple(root,i);
	}

	public Tuple conditionCase(int i, int tabs) { //tokentype is if, elif, else, while
		Tuple token = tokens.get(i);
		String word = (String)token.get(1);

		Tuple leftResult = getTree(i+1, 0);
		Tree leftTree = (Tree)leftResult.get(0);
		i = (int)leftResult.get(1);

		Tuple nextToken = tokens.get(i); //either a colon or a RELATIONAL_OP
		String nextTokenType = (String)nextToken.get(0);

		Tree conditionTree = new Tree("CONDITION");
		Tree conditionRoot;

		switch(nextTokenType) {
		case "RELATIONAL_OP":
			Tuple rightResult = getTree(i+1, 0);
			Tree rightTree = (Tree)rightResult.get(0);
			i = (int)rightResult.get(1);

			conditionRoot = new Tree(nextToken.get(1));
			conditionRoot.addChild(leftTree);
			conditionRoot.addChild(rightTree);
			conditionTree.addChild(conditionRoot);

			break;
		case "COLON":
			conditionRoot = leftTree;
			conditionTree.addChild(conditionRoot);
			break;
		}

		Tree root = new Tree(word); //if, elif etc.
		root.addChild(conditionTree);

		Tuple colon = tokens.get(i);
		if (!((String)colon.get(1)).equals(":")) {
			Util.print("expected colon, but got "+colon);
			return null;
		}
		Tuple bodyResult = parse(i+1, tabs+1);
		List<Tree> bodyTrees = (List<Tree>)bodyResult.get(0);
		i = (int)bodyResult.get(1);

		if (bodyTrees.isEmpty()) {
			Util.print("empty body for condition "+bodyTrees.size());
			return null;
		}

		Tree body = new Tree("BODY");
		body.addChild(bodyTrees);
		root.addChild(body);

		//TODO: check for elifs and elses
		int nextTabs = getTabs(i);

		if (nextTabs != tabs) {
			Util.print("bad indentation error");
			return new Tuple(null, i);
		}
		
		nextToken = tokens.get(i+=nextTabs);
		nextTokenType = (String)nextToken.get(1);
		//Util.print("next token after if body is "+nextTokenType);

		switch (nextTokenType) {
		case "elif": 
			Tuple elifResult = conditionCase(i, tabs);
			Tree elifTree = (Tree)elifResult.get(0);
			i = (int)elifResult.get(1);
			root.addChild(elifTree);
			break;
		case "else":
			Tuple elseResult = elseCase(i, tabs);
			Tree elseTree = (Tree)elseResult.get(0);
			i = (int)elseResult.get(1);
			root.addChild(elseTree);
			break;
		default:
			root.addChild(new Tree("else")); //empty else tree if there is no else
		}

		return new Tuple(root, i);
	}

	public Tuple functionCallCase(int i) {
		Tuple func = tokens.get(i);
		String funcName = (String)func.get(1);

		Tuple lParan = tokens.get(i+=1);

		Tree root = new Tree("FUNC_CALL");
		root.addChild(new Tree(funcName)); //function name/id

		Tuple argResult = getArgResult(i+1);
		List<Tree> argTrees = (List<Tree>)argResult.get(0);
		i = (int)argResult.get(1);

		Tree argumentsTree = new Tree("ARGUMENTS");
		argumentsTree.addChild(argTrees); //arguments
		root.addChild(argumentsTree);

		Tuple rParan = tokens.get(i);		
		return new Tuple(root, i+1);
	}

	public Tuple operatorCase(Tree leftTree, int i) { // <= +, == etc.
		Tuple operatorToken = tokens.get(i);
		String operator = (String)operatorToken.get(1);
		Tree root = new Tree(operator);
		root.addChild(leftTree);

		Tuple rightResult = getTree(i+1, 0);
		Tree rightTree = (Tree)rightResult.get(0);
		i = (int)rightResult.get(1);

		//check left and right tree's to see which operator should be the root

		root.addChild(rightTree);
		return new Tuple(root, i);
	}

	public Tuple elseCase(int i, int tabs) {
		Tuple elsee = tokens.get(i);
		Tree root = new Tree(elsee.get(1));
		Tuple colon = tokens.get(++i);
		if (!((String)colon.get(1)).equals(":")) {
			Util.print("expected colon, but got "+colon);
			return null;
		}
		Tuple bodyResult = parse(i+1, tabs+1);
		List<Tree> bodyTrees = (List<Tree>)bodyResult.get(0);
		i = (int)bodyResult.get(1);
		Tree body = new Tree("BODY");
		body.addChild(bodyTrees);

		root.addChild(body);

		return new Tuple(root, i);
	}

	/**
	 * @param i : index that statement tokens start from
	 * @return Tuple(SyntaxTree of token at i, index of next token)
	 */
	public Tuple getTree(int i, int tabs) {  //line number so that not \n False on same tabs is not allowed
		if (i >= tokens.size()) return new Tuple(null, i);

		Tree root = null;
		Tuple t = tokens.get(i);
		String tokenType = (String)t.get(0);
		String word = (String)t.get(1);
		Util.print("getting tree for token "+i+","+tokenType+","+word);

		switch(tokenType) {
		case "RESERVED":
			root = new Tree(word);
			switch(word) {
			case "if": case "elif": case "while": //word (expr):
				return conditionCase(i, tabs);
			case "else":
				return elseCase(i, tabs);
			case "def": //def name(x...):
				return functionCase(i, tabs);
			case "not": //not x, not <expr>
				Tuple expr = getTree(i+1, tabs);
				root.addChild((Tree)expr.get(0));
				return new Tuple(root, (int)expr.get(1));
			case "return":
				expr = getTree(i+1, tabs);
				root.addChild((Tree)expr.get(0));
				return new Tuple(root, (int)expr.get(1));
			case "False": case "True":
				return new Tuple(root, i+1);
			}

		case "ID": //id = expr,  id + id, id + 5, ID(), ID <= 5

			String secondTokenType = i+1 < tokens.size()?(String)tokens.get(i+1).get(0):"";
			switch (secondTokenType) {
			case "EQ":
				return assignmentCase(i);
			case "LPAREN": //ID(args...)
				return functionCallCase(i);
			case "ARITHMETIC_OP": case "RELATIONAL_OP": //ID + expr
				return operatorCase(new Tree(word), i+1);
			default: // ID,... or just ID
				return new Tuple(new Tree(word), i+1);
			}
		case "NUMBER": case "STRING": //eg. 5, 5+5, 5 < ... 5,..
			Tree numTree = new Tree(word);
			secondTokenType = i+1<tokens.size()?(String)tokens.get(i+1).get(0):"";
			switch(secondTokenType) {
			case "ARITHMETIC_OP": case "RELATIONAL_OP":
				return operatorCase(numTree, i+1);
			default:
				return new Tuple(numTree, i+1); //i+1 is next idx
			}

		case "LPAREN": //eg. (not (x + y)) * (y + z) + (x + y + z)
		
			Tuple result = getTree(i+1, 0);
			Tree resultTree = (Tree)result.get(0);
			i = (int)result.get(1);
			
			if (i+1 < tokens.size()) { //(1 + 1) + (..) or (1) + ..
				Tuple nextToken = tokens.get(i);
				String nextTokenType = (String)nextToken.get(0);
				Util.print("next token type "+nextTokenType);
				if (nextTokenType.equals("RPAREN")) {
					Util.print("ending brackets with "+i+","+tokens.get(i));
					i+=1;
					if (i < tokens.size())
						nextToken = tokens.get(i);	
				}
				nextTokenType = (String)nextToken.get(0);
				
				switch (nextTokenType) { //(...) + ...

				case "ARITHMETIC_OP": case "RELATIONAL_OP": //ID + expr
					result = operatorCase(resultTree, i);
					resultTree = (Tree)result.get(0);
					i = (int)result.get(1);
				}
			}
			//Util.print("ending brackets with "+tokens.get(i));
			if (i < tokens.size() && tokens.get(i).get(0).equals("RPAREN"))
				i+=1;
			
			// just (...)
			return new Tuple(resultTree, i);
		case "ARITHMETIC_OP": //-a+x -(a+x)
			switch(word) {
			case "+":
				return parse(i+1, tabs);
			case "-":
				root = new Tree(word);
				Tuple expr = getTree(i+1, tabs);
				root.addChild(new Tree("0")); //0 - x
				root.addChild((Tree)expr.get(0));
				return new Tuple(root, (int)expr.get(1));
			}
		}

		return new Tuple(null, i);
	}

	public Tuple parse(int startIdx, int expectedTabs) {	//edit lines starting from startIdx that meet the prefix
		List<Tree> statements = new ArrayList<Tree>();

		int i = startIdx;
		while (i < tokens.size() ) {	
			int lineNo = (int)tokens.get(i).get(2);
			int tabs = getTabs(i);
			if (tabs != expectedTabs) {
				//Util.print("unexpected tabs for "+i+","+tokens.get(i)+", expected "+expectedTabs+", got"+tabs);
				break;
			}
			i += tabs;
			//if ((int)tokens.get(i).get(2) != lineNo) continue;
			Util.print("parsing from token "+i+","+tokens.get(i));
			Tuple result = getTree(i, tabs);
			Tree t = (Tree)result.get(0);
			if (t == null) {
				Util.print("null tree received for token "+i+","+tokens.get(i));
				return null;
				//break;
			}
			statements.add(t);
			i = (int)result.get(1);
		}
		return new Tuple(statements, i);
	}

	@SuppressWarnings("unchecked")
	public Tree parse() {
		Tuple t = parse(0,0);
		
		Tree root = new Tree("STATEMENTS");
		root.addChild((List<Tree>)t.get(0));
		return root;
	}

	public static String getCode(String[] args) {
		String code = "";
		if (args.length == 1) {
			File f = new File(args[0]);
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(f));
				String line;
				while ((line = reader.readLine()) != null) {
					code+=line+"\n";
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			for (String line: args) {
				code += line+"\n";
			}
		}
		return code;
	}

	public static void main(String[] args) {
		//Util.print(String.format("[%d] args: [%s]", args.length, args.toString()));
		if (args.length > 0) {
			String code = codes[0]; //Parser.getCode(args);

			Tokenizer t = new Tokenizer();
			Parser p = new Parser();

			List<Tuple> tokens = t.tokenize(code);
			//Util.print(tokens);

			p.setTokens(tokens);
			Tree tree = p.parse();
			Util.print("----Input code----\n\n"+code);
			//Util.print(tree.toJson());
			Util.print("\n\n"+"----Syntax Tree----\n"+tree.printTree(0));
		}
		else {
			Util.print("Usage1: java -jar parser.jar $(< filePath)");
			Util.print("Usage2: java -jar parser.jar \"input code line 1\" \"line 2\"... ");
			myTests();
		}
	}

	private static void myTests() {		
		Tokenizer t = new Tokenizer();

		//sampe input codes used for testing
		String code = codes[codes.length-1];
		Parser p = new Parser();
		//Util.print(t.tokenize(code)+"\n\n");
		List<Tuple> tokens = t.tokenize(code);
		p.setTokens(tokens);
		//Util.print(p.tokens);

		Util.print(tokens);
		Tree tree = p.parse();
		Util.print("----Input code----\n\n"+code);
		Util.print("\n----Syntax Tree----\n");
		Util.print(tree.printTree(0));
		//Util.print(tree.toJson());
	}

	public static String[] codes = {
			"( not ( x + y) and (y + z) or (x + y + z))",
			"((not (x + y))+x)"
			,"while x < y:\n"
			+ "\tx = x + 1\n"
			+ "print(\"loop ended\")"
			
			,"def func(x,y,z):\n"
			+ "\tprint('hello')\n"
			+ "print('the end')\n"
			
			,"if y:\n"
					+ "\tif x:\n"
					+ "\t\tf(x,-y)\n"
					+ "\telif 0:\n"
					+ "\t\tx=1\n"
					+ "\telif 1 < 5:\n"
					+ "\t\t'hi'\n"
					+ "\telse:\n"
					+ "\t\t'why'\n"+
					"else:\n"
					+ "\tprint('done')\n"
					+ "y = x\n"
					+ "x=y"

			,"print(u,-x)"
			,"xyz = 5\ny=5+5\n   f   = \"hello\" if x <= 5:\n\tprint(5)\nelif x == 3:\n\ty=x*5"
			,"def print(x,y):\n\ty=x+3"
			,"def print(x,y):\n\tif (x+y) <=4:\n\t\tif (x+4)+9 >= 5:\n\t\t\ty=(x+3)"
			,"print(\"hello\")"
			,"ievcs"
			,"print(\"hi\nyo\")\nx = y"
			,"if x == 5:\n\tprint(\"hello\")\nelse:\n\t\"hi\""


			,"y = 1\ndef x(u,x,etbuyhf,shfsdfd):\n" + 
					"	printf(\"heuuuu\")\n" + 
					"\n" + 
					"x = 25\n" + 
					"y=35+42*24\n" + 
					"\n" + 
					"if x > y:\n" + 
					"	print(x)\n" + 
					"else:\n" + 
					"	print(y)\n" + 
					"\n" + 
					"print(\"DONE\")"
					,"def test():\n" + 
							"	a = 0\n" + 
							"	b = 0\n" + 
							"	c = 5\n" + 
							"	while c > 0:\n" + 
							"		a = 1 + 2 * i\n" + 
							"		b = (1 + 2) * i\n" + 
							"		c = c-1\n" + 
							"\n" + 
							"	if (a > b) and False:\n" + 
							"		print(a)\n" + 
							"	elif a == b:\n" + 
							"		print(a, d)\n" + 
							"	else:\n" + 
							"		PRINT(b)\n" + 
							"	\n" + 
							"	return \"test complete!\""
	};

}
