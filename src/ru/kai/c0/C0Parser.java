package ru.kai.c0;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;

import ru.kai.c0.exceptions.C0Exception;
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
import ru.kai.c0.objects.C0SubProgram;
import ru.kai.c0.objects.C0Variable;
import ru.kai.c0.objects.C0VariableType;
import ru.kai.c0.objects.C0VariableUsing;

public class C0Parser extends C0Reader implements Runnable
{
	public C0Parser(InputStream inputStream)
	{
		super(inputStream);
	}

	public C0Parser(InputStream inputStream, Charset charset)
	{
		super(inputStream, charset);
	}

	public C0Parser(InputStream inputStream, CharsetDecoder charsetDecoder)
	{
		super(inputStream, charsetDecoder);
	}

	public C0Parser(InputStream inputStream, String string) throws UnsupportedEncodingException
	{
		super(inputStream, string);
	}
	
	private LexemState lexemState;
	
	private boolean isLastLexem;
	
	private Lexem getNextLexem() throws IOException, C0Exception
	{
		if (isLastLexem) return null;
		
		synchronized (lexemState)
		{
			waitingForStatus(true);
			
			LexemStatus lexemStatus = lexemState.lexemStatus;
			lexemState.lexemStatus = LexemStatus.LS_NOT_READY;
			
			lexemState.notifyAll();
			
		    if (lexemStatus == LexemStatus.LS_READY)
		    {
		    	if (lexemState.lexem == null) isLastLexem = true;
		    	return lexemState.lexem;
		    }
		    else if (lexemStatus == LexemStatus.LS_ERROR)
		    	throw lexemState.c0Exception;
		    else if (lexemStatus == LexemStatus.LS_IO_ERROR)
		    	throw lexemState.ioException;
		    else
		    	throw new NullPointerException();
		}		    
	}

	@Override
	public void endParse()
	{
		synchronized (lexemState)
		{			
			waitingForStatus(false);
			
		    lexemState.lexemStatus = LexemStatus.LS_READY;
		    lexemState.lexem = null;
		    
		    lexemState.notifyAll();
		}
	}

	@Override
	public void onError(C0Exception ex)
	{
		synchronized (lexemState)
		{			
			waitingForStatus(false);
			
		    lexemState.lexemStatus = LexemStatus.LS_ERROR;
		    lexemState.c0Exception = ex;	
		    
		    lexemState.notifyAll();
		}			
	}

	@Override
	public void onIOException(IOException ex)
	{
		synchronized (lexemState)
		{			
			waitingForStatus(false);
			
		    lexemState.lexemStatus = LexemStatus.LS_IO_ERROR;
		    lexemState.ioException = ex;	
		    
		    lexemState.notifyAll();
		}		
	}

	private void waitingForStatus(boolean equ)
	{
		while ((lexemState.lexemStatus == LexemStatus.LS_NOT_READY) == equ)
		{
			try
			{
				lexemState.wait();
			}
			catch (InterruptedException e) 
			{ 
				
			}				
		}
	}

	@Override
	public void onLexem(Lexem lexem)
	{
		synchronized (lexemState)
		{			
			waitingForStatus(false);				
		    lexemState.lexemStatus = LexemStatus.LS_READY;
		    lexemState.lexem = lexem;			    
		    lexemState.notifyAll();
		}
	}
	
	public void run()
	{
		read();
	}
	
	private Lexem currentLexem = null;

	private void readLexem() throws IOException, C0Exception
	{
		try
		{
			currentLexem = getNextLexem();
		}
		catch (C0Exception ex)
		{
			throw ex;
		}
	}
	
	public C0Program parse() throws IOException, C0Exception
	{		
		Thread thread = new Thread(this);
		lexemState = new LexemState();
		lexemState.lexemStatus = LexemStatus.LS_NOT_READY;	
		thread.start();
		try 
		{
			return readProgram();
		}
		finally
		{
			thread.stop();
		}
	}
	
	private void addStandardFunctions(C0Program program)
	{
		C0Function function = program.createFunction();
		function.setName("putchar");
		C0Variable param = new C0Variable();
		param.setName("value");
		function.getParams().add(param);
	
		function = program.createFunction();
		function.setName("putn");
		param = new C0Variable();
		param.setName("value");
		function.getParams().add(param);	
		
		function = program.createFunction();
		function.setName("getchar");
	}
	
	private boolean isCurrentLexemTypeEq(LexemType lexemType)
	{
		if (currentLexem != null && lexemType != null)
			if (currentLexem.lexemType == lexemType)
				return true;
			else
				return false;
		else
			if (currentLexem == null && lexemType == null)
				return true;
			else
				return false;
	}

	private C0Program readProgram() throws IOException, C0Exception
	{
		C0Program program = new C0Program();
		addStandardFunctions(program);		
		readLexem();
		
		while (currentLexem != null)
			readFunctionsAndVariables(program);
		return program;
	}
	
	private void readFunctionsAndVariables(C0SubProgram program) throws IOException, C0Exception
	{
		if (isCurrentLexemTypeEq(LexemType.LX_INT))
		{
			readLexem();				
			if (isCurrentLexemTypeEq(LexemType.LX_NAME))
			{
				String name = currentLexem.stringValue;
				readLexem();
				if (!program.hasVariable(name))
				{
					if (isCurrentLexemTypeEq(LexemType.LX_LPARENTHESIS) && program instanceof C0Program)
					{						
						C0Program prg = (C0Program)program;
						if (prg.getFunctionByName(name) < 0)
						{
							C0Function function = prg.createFunction();
							function.setName(name);
							readFunction(function);
						}
						else
							throw new C0Exception("duplicate_identifier",name);
					}
					else if (isCurrentLexemTypeEq(LexemType.LX_COMMA) || isCurrentLexemTypeEq(LexemType.LX_SEMICOLON))
					{
						C0Variable variable = new C0Variable();
						variable.setName(name);
						program.getVariables().add(variable);
						readVariables(program);			
					}
					else
						throw new C0Exception("semicolon_comma_expected");
					readLexem();
				}
				else
					throw new C0Exception("duplicate_identifier",name);
			}
			else
				throw new C0Exception("identifier_expected");
		}
		else
			throw new C0Exception("int_expected");
	}
	
	private void readParams(C0Function function) throws IOException, C0Exception
	{
		boolean isFirst = true;
		do
		{
			readLexem();
			if (!isCurrentLexemTypeEq(LexemType.LX_RPARENTHESIS) || !isFirst)
			{
				if (isCurrentLexemTypeEq(LexemType.LX_INT))
				{
					readLexem();
					if (isCurrentLexemTypeEq(LexemType.LX_NAME))
					{
						String name = currentLexem.stringValue;
						if (!function.hasVariable(name))
						{
							C0Variable variable = new C0Variable();
							variable.setName(name);
							function.getParams().add(variable);					
						}
						else
							throw new C0Exception("duplicate_identifier",name);
						readLexem();
					}
					else
						throw new C0Exception("identifier_expected");
				}
				else
					throw new C0Exception("int_expected");
			}
			isFirst = false;
		} while (isCurrentLexemTypeEq(LexemType.LX_COMMA));
	}
	
	private C0OperationType getOperationType(LexemType lexemType)
	{
		if (lexemType == LexemType.LX_COMMA)
			return C0OperationType.OT_COMMA;
		if (lexemType == LexemType.LX_EQUALS)
			return C0OperationType.OT_APPROPRIATE;
		if (lexemType == LexemType.LX_ASTERISK)
			return C0OperationType.OT_MUL;		
		if (lexemType == LexemType.LX_SLASH)
			return C0OperationType.OT_DIV;
		if (lexemType == LexemType.LX_PERCENT)
			return C0OperationType.OT_MOD;
		if (lexemType == LexemType.LX_DOUBLE_EQUALS)
			return C0OperationType.OT_EQU;
		if (lexemType == LexemType.LX_EXLAMATION_EQUALS)
			return C0OperationType.OT_NOTEQU;
		if (lexemType == LexemType.LX_LESSTHAN)
			return C0OperationType.OT_LESS;		
		if (lexemType == LexemType.LX_GREATERTHAN)
			return C0OperationType.OT_GREATER;		
		if (lexemType == LexemType.LX_LESSEQUALS)
			return C0OperationType.OT_LESSEQU;		
		if (lexemType == LexemType.LX_GREATEREQUALS)
			return C0OperationType.OT_GREATEREQU;		
		if (lexemType == LexemType.LX_MINUS)
			return C0OperationType.OT_SUB;
		if (lexemType == LexemType.LX_PLUS)
			return C0OperationType.OT_ADD;
		if (lexemType == LexemType.LX_UNARYMINUS)
			return C0OperationType.OT_NEG;
		
		return null;
	}
	
	private boolean isOperation(Lexem lexem)
	{
		return getOperationType(lexem.lexemType) != null;
	}

	private C0VariableUsing getVariableUsing(C0Function function, String name) throws C0Exception
	{
		int index;
		index = function.getVariableByName(name);
		C0VariableType type = C0VariableType.VT_LOCAL;
		if (index < 0)
		{
			type = C0VariableType.VT_PARAM;
			index = function.getParamByName(name);
			if (index < 0) 
			{
				type = C0VariableType.VT_GLOBAL;
				index = function.getProgram().getVariableByName(name);
				if (index < 0)
					throw new C0Exception("unknown_identifier",name);
			}
		}
		C0VariableUsing variable = new C0VariableUsing();					
		variable.setIndex(index);
		variable.setVariableType(type);
		return variable;
	}
	
	private C0Expression interpretateOperation(C0Operation operation)
	{
		Vector<C0Expression> terms = operation.getTerms();
		C0OperationType type = operation.getOperationType();
		
		boolean allConstant = true;
		
		for (int i = 0; i < terms.size(); i++)
			if (!(terms.get(i) instanceof C0Constant))
			{
				allConstant = false;
				break;
			}
		
		if (allConstant && type != C0OperationType.OT_APPROPRIATE && type != C0OperationType.OT_COMMA)
		{						
			C0Constant[] constants = new C0Constant[terms.size()];
			for (int i = 0; i < terms.size(); i++)
				constants[i] = (C0Constant)(terms.get(i));
			
			C0Constant result = new C0Constant();

			if (type == C0OperationType.OT_ADD)
				result.setValue(constants[0].getValue() + constants[1].getValue());
			else if (type == C0OperationType.OT_SUB)
				result.setValue(constants[0].getValue() - constants[1].getValue());
			else if (type == C0OperationType.OT_MUL)
				result.setValue(constants[0].getValue() * constants[1].getValue());			
			else if (type == C0OperationType.OT_DIV)
				result.setValue(constants[0].getValue() / constants[1].getValue());
			else if (type == C0OperationType.OT_MOD)
				result.setValue(constants[0].getValue() % constants[1].getValue());			
			else if (type == C0OperationType.OT_EQU)
				result.setValue(constants[0].getValue() == constants[1].getValue() ? 1 : 0);
			else if (type == C0OperationType.OT_NOTEQU)
				result.setValue(constants[0].getValue() != constants[1].getValue() ? 1 : 0);
			else if (type == C0OperationType.OT_LESS)
				result.setValue(constants[0].getValue() < constants[1].getValue() ? 1 : 0);
			else if (type == C0OperationType.OT_GREATER)
				result.setValue(constants[0].getValue() > constants[1].getValue() ? 1 : 0);
			else if (type == C0OperationType.OT_LESSEQU)
				result.setValue(constants[0].getValue() <= constants[1].getValue() ? 1 : 0);			
			else if (type == C0OperationType.OT_GREATEREQU)
				result.setValue(constants[0].getValue() >= constants[1].getValue() ? 1 : 0);
			else if (type == C0OperationType.OT_NEG)
				result.setValue(-constants[0].getValue());

			return result;
		}
		else
			return operation;
	}
	
	private C0Expression getOperation(Stack<Lexem> stack, Stack<C0Expression> output) throws C0Exception 
	{
		C0Expression operation = null;
		
		try
		{
			Lexem lexem = stack.pop();
			
			C0OperationType operationType = getOperationType(lexem.lexemType);
			if (operationType != null)
			{
				C0Operation c0Operation = new C0Operation();
				int argsCount = operationType.getOperationArgumentsCount();
				for (int i = 0; i < argsCount; i++)
					c0Operation.getTerms().add(0, output.pop());
				if (operationType == C0OperationType.OT_APPROPRIATE)
				{
					if (!(c0Operation.getTerms().get(0) instanceof C0VariableUsing))
					{
						throw new C0Exception("lvalue_must_be_a_variable");
					}
				}
				else if (operationType == C0OperationType.OT_MOD || operationType == C0OperationType.OT_DIV)
				{
					C0Expression term = c0Operation.getTerms().get(1);
					if (term instanceof C0Constant)
					{
						C0Constant constant = (C0Constant)term;
						if (constant.getValue() == 0)
							throw new C0Exception("division_by_zero");
					}
				}
				c0Operation.setOperationType(operationType);
				operation = interpretateOperation(c0Operation);
			}			
			
		}
		catch (EmptyStackException ex)
		{
			operation = null;
		}
		
		if (operation == null)
			throw new C0Exception("invalid_terminator");
		
		return operation;
	}
	
	private C0FunctionCall getFunctionCall(Stack<Lexem> stack, Stack<C0Expression> output, C0Function function) throws C0Exception, EmptyStackException 
	{		
		C0FunctionCall functionCall = new C0FunctionCall();
		
		Lexem lexem = stack.pop();
		
		int index = function.getProgram().getFunctionByName(lexem.stringValue);
		if (index < 0)
			throw new C0Exception("unknown_identifier",lexem.stringValue);
		
		C0Expression expression = output.pop();
		
		boolean flag;
		
		do
		{
			flag = false;
			if (expression instanceof C0Operation)
			{
				C0Operation operation = (C0Operation)expression;
				if (operation.getOperationType() == C0OperationType.OT_COMMA)
				{
					Vector<C0Expression> terms = operation.getTerms();
					functionCall.getParams().insertElementAt(terms.get(1), 0);
					expression = terms.get(0);
					flag = true;
				}
			}
		} while (flag);
		
		functionCall.getParams().insertElementAt(expression, 0);
				
		if (function.getProgram().getFunctions().elementAt(index).getParams().size() != functionCall.getParams().size())
			throw new C0Exception("wrong_argument_count",functionCall.getParams().size());
		
		functionCall.setIndex(index);
		
		return functionCall;
	}
	
	private C0Expression readExpression(C0Function function) throws IOException, C0Exception
	{
		Stack<Lexem> stack = new Stack<Lexem>();
		Stack<C0Expression> output = new Stack<C0Expression>();
		
		boolean flag = true;
		boolean nextMayBeUnary = true;
		
		do
		{
			if (isCurrentLexemTypeEq(LexemType.LX_LPARENTHESIS))
			{
				nextMayBeUnary = true;
				if (stack.size() > 0 && stack.peek().lexemType == LexemType.LX_NAME)
				{	
					Lexem lexem = stack.peek();
					int index = function.getProgram().getFunctionByName(lexem.stringValue);
					if (index < 0)
						throw new C0Exception("unknown_identifier",lexem.stringValue);
					stack.push(currentLexem);
					readLexem();		
					if (isCurrentLexemTypeEq(LexemType.LX_RPARENTHESIS))
					{
						if (function.getProgram().getFunctions().get(index).getParams().size() != 0)
							throw new C0Exception("wrong_argument_count",0);
						C0FunctionCall functionCall = new C0FunctionCall();
						functionCall.setIndex(index);
						output.push(functionCall);
						stack.pop();
						stack.pop();
						readLexem();
						nextMayBeUnary = false;
					}
				}
				else
				{
					stack.push(currentLexem);
					readLexem();					
				}				
			}
			else
			{				
				if (stack.size() > 0 && stack.peek().lexemType == LexemType.LX_NAME)
				{
					Lexem lexem = stack.pop();
					output.push(getVariableUsing(function,lexem.stringValue));
				}
				
				if (isCurrentLexemTypeEq(LexemType.LX_RPARENTHESIS))
				{
					while (stack.size() > 0 && stack.peek().lexemType != LexemType.LX_LPARENTHESIS)
						output.push(getOperation(stack,output));
					if (stack.size() > 0)
					{
						stack.pop();
						if (stack.size() > 0 && stack.peek().lexemType == LexemType.LX_NAME)
							output.push(getFunctionCall(stack, output, function));						
						readLexem();
						nextMayBeUnary = false;
					}
					else
						flag = false;					
				}
				else if (isCurrentLexemTypeEq(LexemType.LX_NAME))
				{
					stack.push(currentLexem);
					readLexem();
					nextMayBeUnary = false;
				}			
				else if (isCurrentLexemTypeEq(LexemType.LX_CONST))
				{
					C0Constant constant = new C0Constant();
					constant.setValue(currentLexem.longValue);				
					output.push(constant);
					readLexem();
					nextMayBeUnary = false;
				}
				else if (isOperation(currentLexem))
				{					
					if (nextMayBeUnary)
						if (isCurrentLexemTypeEq(LexemType.LX_MINUS))
							currentLexem.lexemType = LexemType.LX_UNARYMINUS;
					
					C0OperationType operation = getOperationType(currentLexem.lexemType);
					
					int pr = operation.getPriority();
					if (operation.getOrder() == OperationOrderType.OOT_RTL)
						pr++;
					
					boolean fl = false;
					
					do 
					{
						if (fl)
							output.push(getOperation(stack,output));
						
						fl = false;
						
						if (stack.size() > 0)
						{
							C0OperationType opType = getOperationType(stack.peek().lexemType);
							if (opType != null && pr <= opType.getPriority()) 
								fl = true;
						}
					} while (fl);
					
					if (isCurrentLexemTypeEq(LexemType.LX_COMMA))
						try
						{
							Lexem[] lexems = { stack.pop(), stack.pop() };
							if (lexems[0].lexemType != LexemType.LX_LPARENTHESIS || lexems[1].lexemType != LexemType.LX_NAME)
								throw new C0Exception("invalid_terminator");
							for (int i = lexems.length - 1; i >= 0; i--)
								stack.push(lexems[i]);							
						}
						catch (EmptyStackException ex)
						{
							throw new C0Exception("invalid_terminator");
						}
						
					stack.push(currentLexem);
					readLexem();
					
					nextMayBeUnary = true;
				}
				else
				{
					while (stack.size() > 0)
						output.push(getOperation(stack,output));
					flag = false;
				}
			}
		} while (flag);
		
		if (output.size() != 1 || stack.size() != 0)
			throw new C0Exception("invalid_terminator");

		return output.pop();
	}
	
	private void readWhileOperator(C0OperatorWhile operator, C0Function function) throws IOException, C0Exception
	{		
		readLexem();
		if (isCurrentLexemTypeEq(LexemType.LX_LPARENTHESIS))
		{
			readLexem();				
			operator.setCondition(readExpression(function));
			if (isCurrentLexemTypeEq(LexemType.LX_RPARENTHESIS))
			{
				readLexem();
				operator.setOperator(readOperator(function));
			}			
			else
				throw new C0Exception("rbracket_expected");
		}
		else 
			throw new C0Exception("lbracket_expected");			
	}	
	
	private void readIfOperator(C0OperatorIf operator, C0Function function)  throws IOException, C0Exception
	{		
		readLexem();
		if (isCurrentLexemTypeEq(LexemType.LX_LPARENTHESIS))
		{
			readLexem();				
			operator.setCondition(readExpression(function));
			if (isCurrentLexemTypeEq(LexemType.LX_RPARENTHESIS))
			{
				readLexem();
				operator.setOperator(readOperator(function));
			}			
			else
				throw new C0Exception("rbracket_expected");
		}
		else 
			throw new C0Exception("lbracket_expected");
	}
	
	private void readOperatorReturn(C0OperatorReturn operator, C0Function function) throws IOException, C0Exception
	{
		readLexem();
		operator.setExpression(readExpression(function));
		if (isCurrentLexemTypeEq(LexemType.LX_SEMICOLON))
			readLexem();
		else
			throw new C0Exception("semicolon_expected");
	}
	
	private void readSimpleOperator(C0SimpleOperator operator, C0Function function) throws IOException, C0Exception
	{
		operator.setExpression(readExpression(function));
		if (isCurrentLexemTypeEq(LexemType.LX_SEMICOLON))
			readLexem();
		else
			throw new C0Exception("semicolon_expected");		
	}
	
	private C0Operator readOperator(C0Function function) throws IOException, C0Exception
	{
		C0Operator operator;
		if (isCurrentLexemTypeEq(LexemType.LX_IF))
		{
			operator = new C0OperatorIf();
			readIfOperator((C0OperatorIf)operator,function);
		}
		else if (isCurrentLexemTypeEq(LexemType.LX_WHILE))
		{
			operator = new C0OperatorWhile();
			readWhileOperator((C0OperatorWhile)operator,function);
		}
		else if (isCurrentLexemTypeEq(LexemType.LX_RETURN))
		{
			operator = new C0OperatorReturn();
			readOperatorReturn((C0OperatorReturn)operator,function);
		}
		else if (isCurrentLexemTypeEq(LexemType.LX_SEMICOLON))
		{					
			operator = new C0CompoundOperator();
			readLexem();
		}	
		else if (isCurrentLexemTypeEq(LexemType.LX_LCBRACKET))
		{
			C0CompoundOperator compoundOperator = new C0CompoundOperator();				
			readCompoundOperator(compoundOperator,function);
			if (compoundOperator.getOperators().size() == 1)
				operator = compoundOperator.getOperators().get(0);
			else
				operator = compoundOperator;
		}
		else 
		{
			operator = new C0SimpleOperator();
			readSimpleOperator((C0SimpleOperator)operator, function);
		}
		return operator;
	}
	
	private void readCompoundOperator(C0CompoundOperator compoundOperator, C0Function function) throws IOException, C0Exception
	{
		readLexem();
		while (!isCurrentLexemTypeEq(LexemType.LX_RCBRACKET) && !isCurrentLexemTypeEq(null))
			compoundOperator.getOperators().add(readOperator(function));
		if (isCurrentLexemTypeEq(null)) throw new C0Exception("frbracket_expected");
		readLexem();
	}
	
	private void readFunction(C0Function function) throws IOException, C0Exception
	{
		readParams(function);
		
		if (!isCurrentLexemTypeEq(LexemType.LX_RPARENTHESIS))
			throw new C0Exception("rbracket_expected");
		
		readLexem();
		if (isCurrentLexemTypeEq(LexemType.LX_LCBRACKET))
		{
			readLexem();
			while (isCurrentLexemTypeEq(LexemType.LX_INT))
				readFunctionsAndVariables(function);
			while (!isCurrentLexemTypeEq(LexemType.LX_RCBRACKET) && !isCurrentLexemTypeEq(null))
			{
				function.getOperators().add(readOperator(function));
			}			
		}
		else 
			throw new C0Exception("flbracket_expected");
	}
	
	private void readVariables(C0SubProgram program) throws IOException, C0Exception
	{
		while (isCurrentLexemTypeEq(LexemType.LX_COMMA))
		{
			readLexem();
			if (isCurrentLexemTypeEq(LexemType.LX_NAME))
			{
				String name = currentLexem.stringValue;
				if (!program.hasVariable(name))
				{
					C0Variable variable = new C0Variable();
					variable.setName(name);
					program.getVariables().add(variable);
				}
				else
					throw new C0Exception("duplicate_identifier");
				readLexem();
			}
			else
				throw new C0Exception("int_expected");
		}
		if (!isCurrentLexemTypeEq(LexemType.LX_SEMICOLON))
			throw new C0Exception("semicolon_expected");
	}
}
