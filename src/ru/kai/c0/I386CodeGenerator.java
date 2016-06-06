package ru.kai.c0;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import ru.kai.c0.objects.C0Operation;
import ru.kai.c0.objects.C0OperationType;
import ru.kai.c0.objects.C0CompoundOperator;
import ru.kai.c0.objects.C0Constant;
import ru.kai.c0.objects.C0Expression;
import ru.kai.c0.objects.C0Function;
import ru.kai.c0.objects.C0FunctionCall;
import ru.kai.c0.objects.C0Operator;
import ru.kai.c0.objects.C0OperatorIf;
import ru.kai.c0.objects.C0OperatorReturn;
import ru.kai.c0.objects.C0OperatorWhile;
import ru.kai.c0.objects.C0Program;
import ru.kai.c0.objects.C0SimpleOperator;
import ru.kai.c0.objects.C0Variable;
import ru.kai.c0.objects.C0VariableType;
import ru.kai.c0.objects.C0VariableUsing;

public class I386CodeGenerator
{
	private PrintStream printStream;
	private C0Program program;
	
	int currentLabel;
	
	public I386CodeGenerator(PrintStream printStream, C0Program program)
	{
		this.printStream = printStream;
		this.program = program;		
	}	
	
	public I386CodeGenerator(OutputStream output, C0Program program)
	{
		this(new PrintStream(output),program);
	}
	
	private void generateExpressionCode(C0Expression term, C0Function function)
	{		
		if (term instanceof C0Operation)
		{
			C0Operation operation = (C0Operation)term;
			if (operation.getOperationType() == C0OperationType.OT_APPROPRIATE)
			{
				Vector<C0Expression> terms = operation.getTerms();
				C0VariableUsing variableUsing = (C0VariableUsing)(terms.get(0));
				C0Variable var = getVariable(variableUsing,function);
				generateExpressionCode(terms.get(1), function);
				printStream.println("\tpop\teax");
				printStream.println("\tmov\t" + getVariableName(var,variableUsing.getVariableType()) + ",eax");	
			}
			else if (operation.getOperationType() == C0OperationType.OT_ADD)
			{
				moveTermsToEaxEbx(operation,function);				
				printStream.println("\tadd\teax,ebx");
			}
			else if (operation.getOperationType() == C0OperationType.OT_SUB)
			{
				moveTermsToEaxEbx(operation,function);				
				printStream.println("\tsub\teax,ebx");
			}
			else if (operation.getOperationType() == C0OperationType.OT_MUL)
			{
				moveTermsToEaxEbx(operation,function);	
				printStream.println("\timul\tebx");
			}
			else if (operation.getOperationType() == C0OperationType.OT_MOD || operation.getOperationType() == C0OperationType.OT_DIV)
			{
				moveTermsToEaxEbx(operation,function);	
				printStream.println("\tcdq");				
				printStream.println("\tidiv\tebx");
			}
			else if (operation.getOperationType() == C0OperationType.OT_EQU)
				makeConditionOperation(operation,function,"je");
			else if (operation.getOperationType() == C0OperationType.OT_NOTEQU)
				makeConditionOperation(operation,function,"jne");
			else if (operation.getOperationType() == C0OperationType.OT_LESS)
				makeConditionOperation(operation,function,"jl");
			else if (operation.getOperationType() == C0OperationType.OT_GREATER)
				makeConditionOperation(operation,function,"jg");
			else if (operation.getOperationType() == C0OperationType.OT_LESSEQU)
				makeConditionOperation(operation,function,"jle");
			else if (operation.getOperationType() == C0OperationType.OT_GREATEREQU)
				makeConditionOperation(operation,function,"jge");
			else if (operation.getOperationType() == C0OperationType.OT_NEG)
			{
				generateExpressionCode(operation.getTerms().get(0),function);
				printStream.println("\tpop\teax");
				printStream.println("\tneg\teax");
			}
			
			if (operation.getOperationType() == C0OperationType.OT_MOD)
				printStream.println("\tpush\tedx");
			else
				printStream.println("\tpush\teax");
		}	
		else if (term instanceof C0FunctionCall)
		{
			C0FunctionCall fc = (C0FunctionCall)term;			
			Vector<C0Expression> params = fc.getParams();
			for (int i = 0; i < params.size(); i++)
				generateExpressionCode(params.get(i),function);
			C0Function func = program.getByIndex(fc.getIndex());
			
			printStream.println("\tcall\t" + getFunctionName(func));
			printStream.println("\tpush\teax");
		}
		else if (term instanceof C0VariableUsing)
		{
			C0VariableUsing variableUsing = (C0VariableUsing)term;
			C0Variable var = getVariable(variableUsing,function);
			printStream.println("\tpush\t" + getVariableName(var,variableUsing.getVariableType()));
		}
		else if (term instanceof C0Constant)
		{
			C0Constant vu = (C0Constant)term;
			printStream.println("\tpush\t" + vu.getValue());
		}		
	}
	
	private void generateOperatorCode(C0Operator operator, C0Function function, int returnLabel)
	{
		if (operator instanceof C0OperatorIf)
		{
			C0OperatorIf operatorIf = (C0OperatorIf)operator;
			generateExpressionCode(operatorIf.getCondition(),function);
			printStream.println("\tpop\teax");
			
			int label = getNewLabel();
			
			printStream.println("\ttest\teax,eax");
			printStream.println("\tjz\t" + getLabelText(label));
			
			generateOperatorCode(operatorIf.getOperator(),function, returnLabel);
			
			printStream.println(getLabelText(label) + ":");
		}
		else if (operator instanceof C0OperatorWhile)
		{
			C0OperatorWhile operatorWhile = (C0OperatorWhile)operator;
			
			int cycleLable = getNewLabel();
			int afterCyclelabel = getNewLabel();
			
			printStream.println(getLabelText(cycleLable) + ":");
			
			generateExpressionCode(operatorWhile.getCondition(),function);
			printStream.println("\tpop\teax");			
			
			printStream.println("\ttest\teax,eax");
			printStream.println("\tjz\t" + getLabelText(afterCyclelabel));
			
			generateOperatorCode(operatorWhile.getOperator(),function,returnLabel);
			
			printStream.println("\tjmp\t" + getLabelText(cycleLable));
			
			printStream.println(getLabelText(afterCyclelabel) + ":");

		}
		else if (operator instanceof C0CompoundOperator)
		{
			C0CompoundOperator compoundOperator = (C0CompoundOperator)operator;
			Vector<C0Operator> operators = compoundOperator.getOperators();
			for (int i = 0; i < operators.size(); i++)
				generateOperatorCode(operators.get(i), function, returnLabel);
		}
		else if (operator instanceof C0OperatorReturn)
		{
			C0OperatorReturn operatorReturn = (C0OperatorReturn)operator;
			generateExpressionCode(operatorReturn.getExpression(),function);
			printStream.println("\tpop\teax");
			printStream.println("\tjmp\t" + getLabelText(returnLabel));
		}
		else if (operator instanceof C0SimpleOperator)
		{
			C0SimpleOperator simpleOperator = (C0SimpleOperator)operator;
			generateExpressionCode(simpleOperator.getExpression(),function);
			printStream.println("\tadd\tesp,4");
		}
	}
	
	public void generate()
	{
		currentLabel = 0;
		printStream.println("includelib\tkernel32.lib");
		printStream.println("extrn	__imp__ExitProcess@4:dword");
		printStream.println("ExitProcess@4\tequ\t__imp__ExitProcess@4");
		printStream.println("extrn	__imp__GetStdHandle@4:dword");
		printStream.println("GetStdHandle@4\tequ\t__imp__GetStdHandle@4");
		printStream.println("extrn	__imp__WriteConsoleA@20:dword");
		printStream.println("WriteConsoleA@20\tequ\t__imp__WriteConsoleA@20");
		printStream.println("extrn	__imp__ReadConsoleA@20:dword");
		printStream.println("ReadConsoleA@20\tequ\t__imp__ReadConsoleA@20");		
		printStream.println("\tSTD_OUTPUT_HANDLE\tequ\t-11");
		printStream.println("\tSTD_INPUT_HANDLE\tequ\t-10");		
		printStream.println(".386");
		printStream.println(".model flat");
		printStream.println(".data");
		printStream.println("_string\tdb\t?,0,0,0");
		printStream.println(".data?");

		printStream.println("_rw\tdd\t?");
		printStream.println("_stdin\tdd\t?");
		printStream.println("_stdout\tdd\t?");
		
		Vector<C0Variable> variables = program.getVariables();
		
		for (int i = 0; i < variables.size(); i++)
			printStream.println(getVariableName(variables.get(i),C0VariableType.VT_GLOBAL) + "\tdd\t?");
		
		printStream.println(".code");	
		printStream.println("_start:");
		printStream.println("\tpush\tSTD_INPUT_HANDLE");
		printStream.println("\tcall\tGetStdHandle@4");
		printStream.println("\tmov\t_stdin,eax");			
		printStream.println("\tpush\tSTD_OUTPUT_HANDLE");
		printStream.println("\tcall\tGetStdHandle@4");
		printStream.println("\tmov\t_stdout,eax");
		printStream.println("\tcall\t_main@0");
		printStream.println("\tpush\teax");
		printStream.println("\tcall\tExitProcess@4");

		for (Enumeration<C0Function> functions = program.getFunctions().elements(); functions.hasMoreElements();)
		{
			C0Function function = functions.nextElement();
			printStream.println(getFunctionName(function) + "\tproc near");
			
			Vector<C0Variable> params = function.getParams();
			for (int j = 0; j < params.size(); j++)
				printStream.println(getVariableName(params.get(j),C0VariableType.VT_PARAM) + "\tequ\t[ebp" + "+" + (8 + j * 4) + "]");
			
			Vector<C0Variable> locals = function.getVariables();
			for (int j = 0; j < locals.size(); j++)
				printStream.println(getVariableName(locals.get(j),C0VariableType.VT_LOCAL) + "\tequ\t[ebp" + "-" + (4 + j * 4) + "]");
			
			printStream.println("\tpush\tebp");
			printStream.println("\tmov\tebp,esp");
			if (function.getVariables().size() > 0)
				printStream.println("\tsub\tesp," + function.getVariables().size() * 4);
			
			int returnLabel = getNewLabel();
			
			if (function.getName() == "putchar")
				generatePutChar();				
			else if (function.getName() == "putn")
				generatePutN();
			else if (function.getName() == "getchar")
				generateGetChar();
			else 
			{
				Vector<C0Operator> operators = function.getOperators();
				
				for (int i = 0; i < operators.size(); i++)
					generateOperatorCode(operators.get(i), function, returnLabel);
			}
			
			makeRet(function, returnLabel);
			
			printStream.println(getFunctionName(function) + "\tendp");
		}	
		printStream.println("end\t_start");
	}

	private void makeRet(C0Function function, int returnLabel)
	{
		printStream.println(getLabelText(returnLabel) + ":");
		
		if (function.getVariables().size() > 0)
			printStream.println("\tadd\tesp," + function.getVariables().size() * 4);
		
		printStream.println("\tpop\tebp");
		
		printStream.println("\tret" + (function.getParams().size() != 0 ? "\t" + function.getParams().size() * 4 : ""));
	}
	
	private int getNewLabel()
	{
		return currentLabel++;
	}
	
	private String getLabelText(int label)
	{
		return "lb" + label;
	}
	
	private String getFunctionName(C0Function function)
	{
		return "_" + function.getName() + "@" + function.getParams().size() * 4;
	}
	
	private String getVariableName(C0Variable variable, C0VariableType type)
	{	
		if (type == C0VariableType.VT_GLOBAL)
			return "__g_" + variable.getName();
		else if (type == C0VariableType.VT_LOCAL)
			return "__l_" + variable.getName();
		else 
			return "__p_" + variable.getName();		
	}
	
	private C0Variable getVariable(C0VariableUsing variableUsing, C0Function function)
	{
		C0VariableType vt = variableUsing.getVariableType();
		C0Variable var = null;
		if (vt == C0VariableType.VT_GLOBAL)
			var = program.getVariables().get(variableUsing.getIndex());
		else if (vt == C0VariableType.VT_LOCAL)
			var = function.getVariables().get(variableUsing.getIndex());
		else if (vt == C0VariableType.VT_PARAM)
			var = function.getParams().get(variableUsing.getIndex());
		return var;
	}
	
	private void moveTermsToEaxEbx(C0Operation operation, C0Function function)
	{		
		Vector<C0Expression> terms = operation.getTerms();
		generateExpressionCode(terms.get(0), function);
		generateExpressionCode(terms.get(1), function);
		printStream.println("\tpop\tebx");
		printStream.println("\tpop\teax");
	}
	
	private void makeConditionOperation(C0Operation operation, C0Function function, String jmpCommand)
	{
		int label = getNewLabel();
		moveTermsToEaxEbx(operation,function);				
		printStream.println("\tcmp\teax,ebx");
		printStream.println("\tmov\teax,1");
		printStream.println("\t" + jmpCommand + "\t" + getLabelText(label));
		printStream.println("\txor\teax,eax");				
		printStream.println(getLabelText(label) + ":");	
	}
	
	private void generateGetChar()
	{
		printStream.println("\tpush\t0");
		printStream.println("\tpush\toffset _rw");
		printStream.println("\tpush\t1");
		printStream.println("\tpush\toffset _string");
		printStream.println("\tpush\t_stdin");
		printStream.println("\tcall\tReadConsoleA@20");
		printStream.println("\txor\teax,eax");
		printStream.println("\tmov\tal,_string");
	}
	
	private void generatePutChar()
	{
		printStream.println("\tmov\teax,__p_value");
		printStream.println("\tmov\t_string,al");
		printStream.println("\tpush\t0");
		printStream.println("\tpush\toffset _rw");
		printStream.println("\tpush\t1");
		printStream.println("\tpush\toffset _string");
		printStream.println("\tpush\t_stdout");
		printStream.println("\tcall\tWriteConsoleA@20");
	}
	
	private void generatePutN()
	{
		printStream.println("\tmov\teax,__p_value");
		printStream.println("\tcmp\teax,0");
		printStream.println("\tjge\t_more_than_zero");
		printStream.println("\tpush\teax");		
		printStream.println("\tpush\t2Dh");
		printStream.println("\tcall\t_putchar@4");
		printStream.println("\tpop\teax");		
		printStream.println("\tneg\teax");
		printStream.println("_more_than_zero:");		
		printStream.println("\tmov\tebx,10");
		printStream.println("\txor\tecx,ecx");
		printStream.println("_gen_nums_cycle:");
		printStream.println("\tcdq");
		printStream.println("\tidiv\tebx");
		printStream.println("\tadd\tedx,30h");
		printStream.println("\tpush\tedx");
		printStream.println("\tinc\tecx");
		printStream.println("\ttest\teax,eax");
		printStream.println("\tjnz\t_gen_nums_cycle");
		printStream.println("_cycle_output:");
		printStream.println("\tpop\teax");
		printStream.println("\tpush\tecx");
		printStream.println("\tpush\teax");
		printStream.println("\tcall\t_putchar@4");
		printStream.println("\tpop\tecx");
		printStream.println("\tloop\t_cycle_output");					
	}
}
