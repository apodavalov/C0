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

public class LinuxI386CodeGenerator
{
	private PrintStream printStream;
	private C0Program program;
	
	int currentLabel;
	
	public LinuxI386CodeGenerator(PrintStream printStream, C0Program program)
	{
		this.printStream = printStream;
		this.program = program;		
	}	
	
	public LinuxI386CodeGenerator(OutputStream output, C0Program program)
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
				generateExpressionCode(terms.get(1), function);
				printStream.println("\tpopl\t%eax");
				printStream.println("\tmovl\t%eax, " + getVariableName(variableUsing, function));	
			}
			else if (operation.getOperationType() == C0OperationType.OT_ADD)
			{
				moveTermsToEaxEbx(operation,function);				
				printStream.println("\taddl\t%ebx, %eax");
			}
			else if (operation.getOperationType() == C0OperationType.OT_SUB)
			{
				moveTermsToEaxEbx(operation,function);				
				printStream.println("\tsubl\t%ebx, %eax");
			}
			else if (operation.getOperationType() == C0OperationType.OT_MUL)
			{
				moveTermsToEaxEbx(operation,function);	
				printStream.println("\timull\t%ebx");
			}
			else if (operation.getOperationType() == C0OperationType.OT_MOD || operation.getOperationType() == C0OperationType.OT_DIV)
			{
				moveTermsToEaxEbx(operation,function);	
				printStream.println("\tcdq");				
				printStream.println("\tidivl\t%ebx");
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
				printStream.println("\tpopl\t%eax");
				printStream.println("\tnegl\t%eax");
			}
			
			if (operation.getOperationType() == C0OperationType.OT_MOD)
				printStream.println("\tpushl\t%edx");
			else
				printStream.println("\tpushl\t%eax");
		}	
		else if (term instanceof C0FunctionCall)
		{
			C0FunctionCall fc = (C0FunctionCall)term;			
			Vector<C0Expression> params = fc.getParams();
			for (int i = 0; i < params.size(); i++)
				generateExpressionCode(params.get(i),function);
			C0Function func = program.getByIndex(fc.getIndex());
			
			printStream.println("\tcall\t" + getFunctionName(func));
			if (func.getParams().size() != 0)
				printStream.println("\taddl\t $" + func.getParams().size() * 4 + ", %esp");
			printStream.println("\tpushl\t%eax");
		}
		else if (term instanceof C0VariableUsing)
		{
			C0VariableUsing variableUsing = (C0VariableUsing)term;
			printStream.println("\tpushl\t" + getVariableName(variableUsing,function));
		}
		else if (term instanceof C0Constant)
		{
			C0Constant vu = (C0Constant)term;
			printStream.println("\tpushl\t$" + vu.getValue());
		}		
	}
	
	private void generateOperatorCode(C0Operator operator, C0Function function, int returnLabel)
	{
		if (operator instanceof C0OperatorIf)
		{
			C0OperatorIf operatorIf = (C0OperatorIf)operator;
			generateExpressionCode(operatorIf.getCondition(),function);
			printStream.println("\tpopl\t%eax");
			
			int label = getNewLabel();
			
			printStream.println("\ttestl\t%eax, %eax");
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
			printStream.println("\tpopl\t%eax");			
			
			printStream.println("\ttestl\t%eax, %eax");
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
			printStream.println("\tpopl\t%eax");
			printStream.println("\tjmp\t" + getLabelText(returnLabel));
		}
		else if (operator instanceof C0SimpleOperator)
		{
			C0SimpleOperator simpleOperator = (C0SimpleOperator)operator;
			generateExpressionCode(simpleOperator.getExpression(),function);
			printStream.println("\taddl\t$4, %esp");
		}
	}
	
	public void generate()
	{
		currentLabel = 0;
		printStream.println("\t.text");
		
		printStream.println(".globl _start");
		printStream.println("_start:");
		printStream.println("\tcall\t_main");
		printStream.println("\tmovl\t%eax, %ebx");		
		printStream.println("\txorl\t%eax, %eax");
		printStream.println("\tincl\t%eax");
		printStream.println("\tint\t$0x80");
		printStream.println("\thlt");

		for (Enumeration<C0Function> functions = program.getFunctions().elements(); functions.hasMoreElements();)
		{
			C0Function function = functions.nextElement();
			printStream.println(".globl " + getFunctionName(function));
			printStream.println("\t.type\t" + getFunctionName(function) + ", @function");
			printStream.println(getFunctionName(function) + ":");
		
			printStream.println("\tpushl\t%ebp");
			printStream.println("\tmovl\t%esp, %ebp");
			if (function.getVariables().size() > 0)
				printStream.println("\tsubl\t$" + function.getVariables().size() * 4 + ", %esp");
			
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
			
			printStream.println("\t.size " + getFunctionName(function) + ", .-" + getFunctionName(function));
		}
		
		Vector<C0Variable> variables = program.getVariables();
		
		for (int i = 0; i < variables.size(); i++)
			printStream.println("\t.comm\t __g_" + variables.get(i).getName() + ",4,4");
		
		printStream.println("\t.comm\t ct,1,1");
		
		printStream.println("\t.ident\t\"C0\"");
		printStream.println("\t.section\t.note.GNU-stack,\"\",@progbits");
	}

	private void makeRet(C0Function function, int returnLabel)
	{
		printStream.println(getLabelText(returnLabel) + ":");
		
		if (function.getVariables().size() > 0)
			printStream.println("\taddl\t$" + function.getVariables().size() * 4 + ", %esp");
		
		printStream.println("\tpopl\t%ebp");
		
		printStream.println("\tret");
	}
	
	private int getNewLabel()
	{
		return currentLabel++;
	}
	
	private String getLabelText(int label)
	{
		return ".lb" + label;
	}
	
	private String getFunctionName(C0Function function)
	{
		return "_" + function.getName();
	}

	private String getVariableName(C0VariableUsing variableUsing, C0Function function)
	{
		C0VariableType vt = variableUsing.getVariableType();
		String name = null;
		if (vt == C0VariableType.VT_GLOBAL)
			name = "__g_" + program.getVariables().get(variableUsing.getIndex()).getName();
		else if (vt == C0VariableType.VT_LOCAL)
			name = "-" + (4 + variableUsing.getIndex() * 4) + "(%ebp)";
		else if (vt == C0VariableType.VT_PARAM)
			name = (8 + variableUsing.getIndex() * 4) + "(%ebp)";
		return name;
	}
	
	private void moveTermsToEaxEbx(C0Operation operation, C0Function function)
	{		
		Vector<C0Expression> terms = operation.getTerms();
		generateExpressionCode(terms.get(0), function);
		generateExpressionCode(terms.get(1), function);
		printStream.println("\tpopl\t%ebx");
		printStream.println("\tpopl\t%eax");
	}
	
	private void makeConditionOperation(C0Operation operation, C0Function function, String jmpCommand)
	{
		int label = getNewLabel();
		moveTermsToEaxEbx(operation,function);				
		printStream.println("\tcmpl\t%ebx, %eax");
		printStream.println("\tmovl\t$1, %eax");
		printStream.println("\t" + jmpCommand + "\t" + getLabelText(label));
		printStream.println("\txorl\t%eax, %eax");				
		printStream.println(getLabelText(label) + ":");	
	}
	
	private void generateGetChar()
	{
		printStream.println("\tmovl\t$1, %edx");		
		printStream.println("\tmovl\t$3, %eax");
		printStream.println("\txorl\t%ebx, %ebx");		
		printStream.println("\tleal\tct, %ecx");
		printStream.println("\tint\t$0x80");
		printStream.println("\txorl\t%eax, %eax");
		printStream.println("\tmovb\tct, %al");
	}
	
	private void generatePutChar()
	{
		printStream.println("\tmovl\t8(%ebp), %eax");
		printStream.println("\tmovb\t%al, ct");
		printStream.println("\tmovl\t$1, %edx");		
		printStream.println("\tmovl\t$4, %eax");
		printStream.println("\tmovl\t$1, %ebx");		
		printStream.println("\tleal\tct, %ecx");
		printStream.println("\tint\t$0x80");
	}
	
	private void generatePutN()
	{
		printStream.println("\tmovl\t8(%ebp), %eax");
		printStream.println("\tcmpl\t$0, %eax");
		printStream.println("\tjge\t._more_than_zero");
		printStream.println("\tpushl\t%eax");		
		printStream.println("\tpushl\t$0x2D");
		printStream.println("\tcall\t_putchar");
		printStream.println("\taddl\t$4, %esp");
		printStream.println("\tpopl\t%eax");		
		printStream.println("\tnegl\t%eax");
		printStream.println("._more_than_zero:");		
		printStream.println("\tmovl\t$10, %ebx");
		printStream.println("\txorl\t%ecx, %ecx");
		printStream.println("._gen_nums_cycle:");
		printStream.println("\tcdq");
		printStream.println("\tidivl\t%ebx");
		printStream.println("\taddl\t$0x30, %edx");
		printStream.println("\tpushl\t%edx");
		printStream.println("\tincl\t%ecx");
		printStream.println("\ttestl\t%eax, %eax");
		printStream.println("\tjnz\t._gen_nums_cycle");
		printStream.println("._cycle_output:");
		printStream.println("\tpopl\t%eax");
		printStream.println("\tpushl\t%ecx");
		printStream.println("\tpushl\t%eax");
		printStream.println("\tcall\t_putchar");
		printStream.println("\taddl\t$4, %esp");		
		printStream.println("\tpopl\t%ecx");
		printStream.println("\tloop\t._cycle_output");					
	}
}
