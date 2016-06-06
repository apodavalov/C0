package ru.kai.c0.objects;

public class C0OperatorIf extends C0Operator
{
	private C0Expression condition;
	private C0Operator operator;
	
	public C0Expression getCondition()
	{
		return condition;
	}
	public void setCondition(C0Expression condition)
	{
		this.condition = condition;
	}
	public C0Operator getOperator()
	{
		return operator;
	}
	public void setOperator(C0Operator operator)
	{
		this.operator = operator;
	}
}
