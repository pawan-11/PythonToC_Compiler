#!/bin/bash


for file in `ls ./exampleInputs`; do
	outputFile="./exampleOutputs/${file}_IR.txt"
	> "${outputFile}" #clear output file

	filePath="./exampleInputs/${file}"	
	code="$filePath"
	title="Generating syntax tree for code in $file into $outputFile"
	echo "$title"
	java -jar parser.jar "$code" >> "${outputFile}"
done