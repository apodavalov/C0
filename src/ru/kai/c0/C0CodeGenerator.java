package ru.kai.c0;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import ru.kai.c0.objects.C0Operator;
import ru.kai.c0.objects.C0Expression;
import ru.kai.c0.objects.C0Operation;
import ru.kai.c0.objects.C0OperationType;
import ru.kai.c0.objects.C0CompoundOperator;
import ru.kai.c0.objects.C0Constant;
import ru.kai.c0.objects.C0Function;
import ru.kai.c0.objects.C0FunctionCall;
import ru.kai.c0.objects.C0OperatorIf;
import ru.kai.c0.objects.C0OperatorReturn;
import ru.kai.c0.objects.C0OperatorWhile;
import ru.kai.c0.objects.C0Program;
import ru.kai.c0.objects.C0SimpleOperator;
import ru.kai.c0.objects.C0Variable;
import ru.kai.c0.objects.C0VariableType;
import ru.kai.c0.objects.C0VariableUsing;

public class C0CodeGenerator
{	
	private PrintStream printStream;
	private C0Program program;	
	
	public C0CodeGenerator(PrintStream printStream, C0Program program)
	{
		this.printStream = printStream;
		this.program = program;		
	}	
	
	public C0CodeGenerator(OutputStream output, C0Program program)
	{
		this(new PrintStream(output),program);
	}
	
	private void generateExpressionCode(C0Expression term, C0Function function)
	{		
		if (term instanceof C0Operation)
		{
			C0Operation c0Operation = (C0Operation)term;
			printStream.print("(");
			Vector<C0Expression> terms = c0Operation.getTerms(); 
			if (c0Operation.getOperationType() == C0OperationType.OT_NEG)
			{
				String operation = "-";			
				printStream.print(operation);
				generateExpressionCode(terms.get(0),function);
			}
			else 
			{
				generateExpressionCode(terms.get(0),function);
				printStream.print(" ");
				String operation = "";
				if (c0Operation.getOperationType() == C0OperationType.OT_APPROPRIATE)
					operation = "=";
				else if (c0Operation.getOperationType() == C0OperationType.OT_MUL)
					operation = "*";		
				else if (c0Operation.getOperationType() == C0OperationType.OT_ADD)
					operation = "+";
				else if (c0Operation.getOperationType() == C0OperationType.OT_SUB)
					operation = "-";			
				else if (c0Operation.getOperationType() == C0OperationType.OT_MOD)
					operation = "%";			
				else if (c0Operation.getOperationType() == C0OperationType.OT_DIV)
					operation = "/";
				else if (c0Operation.getOperationType() == C0OperationType.OT_EQU)
					operation = "==";			
				else if (c0Operation.getOperationType() == C0OperationType.OT_NOTEQU)
					operation = "!=";						
				else if (c0Operation.getOperationType() == C0OperationType.OT_LESS)
					operation = "<";
				else if (c0Operation.getOperationType() == C0OperationType.OT_GREATER)
					operation = ">";
				else if (c0Operation.getOperationType() == C0OperationType.OT_LESSEQU)
					operation = "<=";
				else if (c0Operation.getOperationType() == C0OperationType.OT_GREATEREQU)
					operation = ">=";
				else if (c0Operation.getOperationType() == C0OperationType.OT_GREATEREQU)
					operation = ">=";
				printStream.print(operation);				
				printStream.print(" ");	
				generateExpressionCode(terms.get(1),function);
			}
			printStream.print(")");
		}	
		else if (term instanceof C0FunctionCall)
		{
			C0FunctionCall fc = (C0FunctionCall)term;			
			printStream.print(program.getByIndex(fc.getIndex()).getName());
			printStream.print("(");
			Vector<C0Expression> params = fc.getParams();
			for (int i = 0; i < params.size(); i++)
			{
				generateExpressionCode(params.get(i),function);
				if (i != params.size() - 1)
					printStream.print(", ");
			}
			printStream.print(")");			
		}
		else if (term instanceof C0VariableUsing)
		{
			C0VariableUsing vu = (C0VariableUsing)term;
			C0VariableType vt = vu.getVariableType();
			String varName = null;
			if (vt == C0VariableType.VT_GLOBAL)
				varName = program.getVariables().get(vu.getIndex()).getName();
			else if (vt == C0VariableType.VT_LOCAL)
				varName = function.getVariables().get(vu.getIndex()).getName();
			else if (vt == C0VariableType.VT_PARAM)
				varName = function.getParams().get(vu.getIndex()).getName();			
			printStream.print(varName);
		}
		else if (term instanceof C0Constant)
		{
			C0Constant vu = (C0Constant)term;
			printStream.print(Long.valueOf(vu.getValue()));
		}		
	}
	
	private void generateOperatorCode(C0Operator operator, String sub, C0Function function)
	{
		if (operator instanceof C0OperatorIf)
		{
			C0OperatorIf operatorIf = (C0OperatorIf)operator;
			printStream.print(sub + "if (");
			generateExpressionCode(operatorIf.getCondition(),function);
			printStream.println(")");
			generateOperatorCode(operatorIf.getOperator(),sub + "\t", function);
		}
		else if (operator instanceof C0OperatorWhile)
		{
			C0OperatorWhile operatorWhile = (C0OperatorWhile)operator;	
			printStream.print(sub + "while (");
			generateExpressionCode(operatorWhile.getCondition(),function);
			printStream.println(")");
			generateOperatorCode(operatorWhile.getOperator(),sub + "\t", function);
		}
		else if (operator instanceof C0CompoundOperator)
		{
			C0CompoundOperator compoundOperator = (C0CompoundOperator)operator;
			String prevSub = sub.substring(1);
			printStream.println(prevSub + "{");
			Vector<C0Operator> operators = compoundOperator.getOperators();
			for (int i = 0; i < operators.size(); i++)
				generateOperatorCode(operators.get(i),sub, function);
			printStream.println(prevSub + "}");			
		}
		else if (operator instanceof C0OperatorReturn)
		{
			C0OperatorReturn operatorReturn = (C0OperatorReturn)operator;	
			printStream.print(sub + "return ");
			generateExpressionCode(operatorReturn.getExpression(),function);
			printStream.println(";");
		}
		else if (operator instanceof C0SimpleOperator)
		{
			C0SimpleOperator simpleOperator = (C0SimpleOperator)operator;
			printStream.print(sub);
			generateExpressionCode(simpleOperator.getExpression(),function);
			printStream.println(";");
		}
	}
	
	public void generate()
	{
		Vector<C0Variable> variables = program.getVariables();
		
		for (int i = 0; i < variables.size(); i++)
		{			
			printStream.println("int " + variables.get(i).getName() + ";");
		}
		
		// WARNING: общем случае нужно выводить еще и код putn, putchar и getchar, 
		// в данном случае мы переводим из с0 в c0 - поэтому этого делать не нужно
		for (Enumeration<C0Function> functions = program.getFunctions().elements(); functions.hasMoreElements();)
		{
			C0Function function = functions.nextElement();
			
			if (function.getName() == "putchar" || function.getName() == "putn" || function.getName() == "getchar")
				continue;
			printStream.print("int " + function.getName() + "(");
			
			Vector<C0Variable> params = function.getParams();
			for (int j = 0; j < params.size(); j++)
			{
				printStream.print("int " + params.get(j).getName());
				if (j != params.size() - 1)
					printStream.print(", ");
			}			
			printStream.println(")");
			
			printStream.println("{");
			
			Vector<C0Variable> locals = function.getVariables();
			for (int j = 0; j < locals.size(); j++)
			{
				printStream.print("\tint " + locals.get(j).getName());				
				printStream.println(";");
			}
			
			Vector<C0Operator> operators = function.getOperators();
			
			for (int i = 0; i < operators.size(); i++)
				generateOperatorCode(operators.get(i),"\t",function);	
			
			printStream.println("}");
		}		
	}
}
