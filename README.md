# PythonToC_Compiler by Pawanpreet Mundi

#Csc488 Compiler and Interpreter Project
#Building Python Compiler that translates a subset of python code to C language
#Written in Java without the use of parsing libraries

#Steps to translation
#1. Lexify/ Tokenize the input code in Tokenizer.java
# 2. Transform the tokens patterns to an Abstract Syntax Tree in Parser.java
# 3. Transform the AST to Simple Three Address Code instructions in TacConverter.java
# 4. (Current step) Translate the TAC instructions to target language C
# 5. (Next steps) Optimization, add dynamic data type list, error handling, fix more code cases that cause error.

#basic subset of code includes
#while loops, if elif else statements, function definitions, calls, assignment statements, expressions, data types are string, number.

#script is provided, which runs the executable jar file created from TacConverter.java, running the example inputs in exampleInputs and placing the outputs in exampleOutputs/ folder. 