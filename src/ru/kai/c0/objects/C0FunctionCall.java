package ru.kai.c0.objects;

import java.util.Vector;

public class C0FunctionCall extends C0Expression
{
	private int index;
	private Vector<C0Expression> params = new Vector<C0Expression>();
	
	public Vector<C0Expression> getParams()
	{
		return params;
	}
	public int getIndex()
	{
		return index;
	}
	public void setIndex(int index)
	{
		this.index = index;
	}
}
