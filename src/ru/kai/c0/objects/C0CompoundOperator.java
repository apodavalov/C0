package ru.kai.c0.objects;

import java.util.Vector;

public class C0CompoundOperator extends C0Operator
{
	private Vector<C0Operator> operators = new Vector<C0Operator>();

	public Vector<C0Operator> getOperators()
	{
		return operators;
	}
}
