package ru.kai.c0.objects;

import java.util.Vector;

public class C0Operation extends C0Expression
{
	private Vector<C0Expression> terms = new Vector<C0Expression>();
	private C0OperationType operationType;
	
	public C0OperationType getOperationType()
	{
		return operationType;
	}
	
	public void setOperationType(C0OperationType operationType)
	{
		this.operationType = operationType;
	}
	
	public Vector<C0Expression> getTerms()
	{		
		return terms;
	}
}
