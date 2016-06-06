package ru.kai.c0.objects;

public class C0VariableUsing extends C0Expression
{
	private int index;	
	private C0VariableType variableType;
	
	public int getIndex()
	{
		return index;
	}
	public void setIndex(int index)
	{
		this.index = index;
	}
	public C0VariableType getVariableType()
	{
		return variableType;
	}
	public void setVariableType(C0VariableType variableType)
	{
		this.variableType = variableType;
	}	
}
