package ru.kai.c0.objects;

import java.util.Vector;

public class C0Program extends C0SubProgram
{
	private Vector<C0Variable> variables = new Vector<C0Variable>();
	private Vector<C0Function> functions = new Vector<C0Function>();
	
	public Vector<C0Variable> getVariables()
	{
		return variables;
	}
	
	public Vector<C0Function> getFunctions()
	{
		return functions;
	}	
	
	public C0Function createFunction()
	{
		C0Function function = new C0Function(this);
		functions.add(function);
		return function;
	}
	
	public C0Function getByIndex(int index)
	{
		return functions.get(index);
	}
	
	public int getFunctionByName(String name)
	{
		int res = -1;
		for (int i = 0; i < functions.size(); i++)
			if (functions.get(i).getName().compareTo(name) == 0)
				res = i;
		return res;
	}
	
	public int getVariableByName(String name)
	{
		int res = -1;
		for (int i = 0; i < variables.size(); i++)
			if (variables.get(i).getName().compareTo(name) == 0)
				res = i;
		return res;
	}

	@Override
	public boolean hasVariable(String name)
	{
		return getVariableByName(name) >= 0;
	}	
}
