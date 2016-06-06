package ru.kai.c0.objects;

import java.util.Vector;

public class C0Function extends C0SubProgram
{
	private String name;
	private Vector<C0Variable> params = new Vector<C0Variable>();
	private Vector<C0Variable> variables = new Vector<C0Variable>();
	private Vector<C0Operator> operators = new Vector<C0Operator>();
	
	private C0Program program;
	
	public int getParamByName(String name)
	{
		int variable = -1;
		for (int i = 0; i < params.size(); i++)
			if (params.get(i).getName().compareTo(name) == 0)
				variable = i;
		return variable;			
	}
	
	public int getVariableByName(String name)
	{
		int variable = -1;
		for (int i = 0; i < variables.size(); i++)
			if (variables.get(i).getName().compareTo(name) == 0)
				variable = i;
		return variable;			
	}	
	
	public void setName(String name)
	{
		if (name == null) throw new NullPointerException();
		this.name = name;
	}	
	
	public String getName()
	{
		return name;
	}	
	
	public Vector<C0Variable> getParams()
	{
		return params;
	}
	
	public Vector<C0Variable> getVariables()
	{
		return variables;
	}
	
	public Vector<C0Operator> getOperators()
	{
		return operators;
	}	
	
	C0Function(C0Program program)
	{
		this.program = program;
	}
	
	public C0Program getProgram()
	{
		return program;
	}

	@Override
	public boolean hasVariable(String name)
	{
		return getVariableByName(name) >= 0 || getParamByName(name) >= 0;
	}
}
