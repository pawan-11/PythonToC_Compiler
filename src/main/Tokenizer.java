package main;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.Util.Function;
import main.Vector.Tuple;

public class Tokenizer {


	private String[] reserved = {"not", "while", "if", "elif", "else", "def", "return", "True", "False"};
	private Token[] tokens = {
			new Token("IGNORE", s->{ return s.matches("\\s") && !s.matches("[\n\t]"); }), //whitespace but not newline or tab
			new Token("NEWLINE", s->{ return s.matches("[\n]"); }),
			new Token("TAB", s->{ return s.matches("\t"); }),

			new Token("NUMBER", s->{ 
				try {
					Double.parseDouble(s);
					return s.trim().length() == s.length(); //no white space around s
				}
				catch (NumberFormatException e){
					return false;
				}
			}),
			new Token("STRING", s->{
				return s.matches("[\"][^\"]*[\"]");
			}),
			new Token("RESERVED", s->{
				for (String reserve: reserved) {
					if (reserve.equals(s)) {		
						return true;
					}
				}
				
				return false;
			}),
			new Token("RELATIONAL_OP", s->{ return s.matches("([<>]=?)|(==)|(and)|(or)"); }),
			//new Token("LOGICAL_OP", s->{ return s.matches(""); }),
			new Token("ID", s->{ 
				return s.matches("[a-zA-Z_][a-zA-Z_0-9]*");
			}),
			new Token("QUOTE", s->{ return s.matches("[\"']"); }),
			//new Token("SEMICOLON", s->{ return s.matches(";"); }),
			new Token("COLON", s->{ return s.matches(":"); }),
			new Token("ARITHMETIC_OP", s->{ return s.matches("[-\\+/\\*]"); }),
			new Token("COMMA", s->{ return s.matches(","); }),
			new Token("EQ", s->{ return s.matches("="); }),
			new Token("LPAREN", s->{ return s.matches("\\("); }),
			new Token("RPAREN", s->{ return s.matches("\\)"); }),
			new Token("LBRACK", s->{ return s.matches("\\["); }),
			new Token("RBRACK", s->{ return s.matches("\\]"); }),
	};	
	public static Map<String, Token> tokenMap = new HashMap<String, Token>();


	public class Token {

		final String name;
		final Function<String, Boolean> filter;
		public Token(String name, Function<String, Boolean> filter) {
			this.name = name;
			this.filter = filter;
			tokenMap.put(name, this);
		}
	}

	/**
	 * add brackets, for -x to (-x), x and y < 4 to (x and y) < 4, -a+b*c to ((-a)+b)*c
	 * -(a)+x to (-(a))+x, -(a+x) to (-(a+x))
	 */
	public String fix(List<Tuple> tokens) {
		String fixedcode = "";
		
		int i = 0;
		List<Tuple> lineTokens = new ArrayList<Tuple>(); 
		List<Tuple> newTokens = new ArrayList<Tuple>();
		
		while (i < tokens.size()) {
			Tuple token = tokens.get(i);
			int lineNo = (int)token.get(2);
			int startIdx = i;
			
			while (i < tokens.size() && (int)token.get(2) == lineNo) {
				lineTokens.add(token);
				i++;
				token = tokens.get(i);
			}
			
			String type = (String)token.get(0);
			String word = (String)token.get(1);
			
			
			i++;
		}
		
		return fixedcode;
	}
	
	public Token findToken(String word) {
		for (Token t: tokens) {
			if (t.filter.apply(word)) {
				return t;			
			}
		}
		return null;
	}

	/**
	 * @param code - code to find tokens from
	 * @return list of all token information in the code. Token Info is: (tokentype, tokenName, lineNo, offset, index)
	 */
	public List<Tuple> tokenize(String code) {
		List<Tuple> tokenInfo = new ArrayList<Tuple>();

		int i = 0, lineNo = 1, offset = 0;
		String fullWord = "";

		while (i < code.length()) {
			String word = code.charAt(i)+"";

			Token t = findToken(word);
			if (t == null) { //no matching token found
				Util.print("Unknown token in code: "+code.charAt(i) +" at "+i);
				i++;
				continue;
			}	

			String tokenType = t.name;
			//Util.print("token type "+tokenType);
			int j = i;
			if (tokenType.equals("QUOTE")) { //a string
				word = "\"";
				while (j++ < code.length()-1 && !tokenMap.get("QUOTE").filter.apply(code.charAt(j)+"")) {
					word += code.charAt(j);
				}
				j++;
				word += "\"";
				fullWord += word;
				tokenType = "STRING";
			}
			else {
				while (j++ < code.length()-1 && t.filter.apply(word+code.charAt(j))) {
					word += code.charAt(j);
					//fullWord += code.charAt(j);
				}
				fullWord += word;
			}
			i = j-1; //i+word.length();
			
			//TODO: if the token is something like equal, then reset fullWord.
			//allows parsing <= without spacing
			
			if (tokenType.equals("NEWLINE")) {
				lineNo++;
				offset = 0;
			}
			//if (tokenType.equals("TAB"))
			//	fullWord = "";
			if (tokenType.equals("LPAREN"))
				fullWord = "";
				
			if (!tokenType.equals("IGNORE") && !tokenType.equals("NEWLINE")) {
				tokenInfo.add(new Tuple(tokenType, word, lineNo, offset, i));
			}
			if (word.matches("[\\s\t\n:]") && fullWord.length() > 0) {	 //fullword ends, check if its a token
				fullWord = fullWord.substring(0, fullWord.length()-1);
				
				if (fullWord.length() > 1) {
					Token t2 = findToken(fullWord);
					
					if (t2 != null) {
						int k = tokenInfo.size();
						while ((k-=1) > -1) {
							Tuple info = tokenInfo.get(k);
							if ((int)info.get(4) >= i-fullWord.length()) {
								//Util.print("removed "+tokenInfo.get(k)+" for "+fullWord);
								tokenInfo.remove(k);
							}
						}
						tokenInfo.add(new Tuple(t2.name, fullWord, 
								lineNo, offset-fullWord.length(), i-fullWord.length()));
						if (tokenType.equals("COLON")) //add back the colon
							tokenInfo.add(new Tuple(tokenType, word, lineNo, offset, i));
						
					}
				}
				fullWord = "";
			}
			offset += word.length();
			i++;
		}
		return tokenInfo; 
	}
	

	public static void main(String[] args) {
		if (args.length > 0) {
			String code = Parser.codes[0];//.getCode(args);
			Tokenizer t = new Tokenizer();
			List<Tuple> tokens = t.tokenize(code);

			for (Tuple tok: tokens)
				Util.print(tok);
		}
		else
			myTests();
	}
	
	private static void myTests() {
		Tokenizer t = new Tokenizer();
		List<Tuple> tokens; // = t.tokenize("xyz = 4;\n 8y879 = 5;");
		tokens = t.tokenize(Parser.codes[0]);

		for (Tuple tok: tokens)
			Util.print(tok);
	}
	
	public static String[] codes = {
			"xyz = 5\ny=5+5\n   f   = \"hello\" if x <= 5:\n\tprint(5)\nelif x == 3:\n\ty=x*5"
			//, "\"hello\""
			, "if x == 5:\n\tprint(\"hello\")\nelse:\n\t\"hi\""
			
			, "y = 1\ndef x(u,x,etbuyhf,shfsdfd):\n" + 
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
			
			, "def test():\n" + 
					"	a = 0\n" + 
					"	b = 0\n" + 
					"	c = 5\n" + 
					"	\n" + 
					"	while c > 0:\n" + 
					"		a = 1 + 2 * i\n" + 
					"		b = (1 + 2) * i\n" + 
					"		c = c-1\n" + 
					"\n" + 
					"	if (a > b) and not False:\n" + 
					"		print(a)\n" + 
					"	elif a == b:\n" + 
					"		print(a, d)\n" + 
					"	else:\n" + 
					"		PRINT(b)\n" + 
					"	\n" + 
					"	return \"test complete!\""
			, "print(x, -3)"
			, "if not x:\n\tprint(x,+3)"
	};
}
