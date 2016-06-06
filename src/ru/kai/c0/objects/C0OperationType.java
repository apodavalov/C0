package ru.kai.c0.objects;

import java.util.HashMap;

import ru.kai.c0.OperationOrderType;

public enum C0OperationType
{
	OT_ADD,
	OT_SUB,
	OT_MUL,
	OT_DIV,
	OT_MOD,
	OT_LESS,
	OT_LESSEQU,
	OT_GREATER,
	OT_GREATEREQU,
	OT_EQU,
	OT_NOTEQU,
	OT_APPROPRIATE,
	OT_NEG,
	OT_COMMA;

	public int getOperationArgumentsCount()
	{
		if (this == C0OperationType.OT_NEG)
			return 1;
		else
			return 2;
	}
	
	private static HashMap<C0OperationType,Integer> priorities;
	
	private static HashMap<C0OperationType,OperationOrderType> order;

	static 
	{
		priorities = new HashMap<C0OperationType, Integer>();
		order = new HashMap<C0OperationType, OperationOrderType>();
		
		int priority = 0;
		
		priorities.put(C0OperationType.OT_COMMA, priority);
		order.put(C0OperationType.OT_COMMA,OperationOrderType.OOT_LTR);		
		
		priority++;
		priorities.put(C0OperationType.OT_EQU, priority);
		order.put(C0OperationType.OT_EQU,OperationOrderType.OOT_RTL);
		
		priority++;
		priorities.put(C0OperationType.OT_APPROPRIATE, priority);
		order.put(C0OperationType.OT_APPROPRIATE,OperationOrderType.OOT_LTR);		
		priorities.put(C0OperationType.OT_NOTEQU, priority);
		order.put(C0OperationType.OT_NOTEQU,OperationOrderType.OOT_LTR);		
		
		priority++;
		priorities.put(C0OperationType.OT_LESS, priority);
		order.put(C0OperationType.OT_LESS,OperationOrderType.OOT_LTR);
		priorities.put(C0OperationType.OT_LESSEQU, priority);
		order.put(C0OperationType.OT_LESSEQU,OperationOrderType.OOT_LTR);
		priorities.put(C0OperationType.OT_GREATER, priority);
		order.put(C0OperationType.OT_GREATER,OperationOrderType.OOT_LTR);		
		priorities.put(C0OperationType.OT_GREATEREQU, priority);		
		order.put(C0OperationType.OT_GREATEREQU,OperationOrderType.OOT_LTR);		
		
		priority++;
		priorities.put(C0OperationType.OT_ADD, priority);
		order.put(C0OperationType.OT_ADD,OperationOrderType.OOT_LTR);
		priorities.put(C0OperationType.OT_SUB, priority);
		order.put(C0OperationType.OT_SUB,OperationOrderType.OOT_LTR);
		
		priority++;
		priorities.put(C0OperationType.OT_MOD, priority);
		order.put(C0OperationType.OT_MOD,OperationOrderType.OOT_LTR);		
		priorities.put(C0OperationType.OT_DIV, priority);
		order.put(C0OperationType.OT_DIV,OperationOrderType.OOT_LTR);		
		priorities.put(C0OperationType.OT_MUL, priority);
		order.put(C0OperationType.OT_MUL,OperationOrderType.OOT_LTR);
		
		priority++;
		priorities.put(C0OperationType.OT_NEG, priority);		
		order.put(C0OperationType.OT_NEG,OperationOrderType.OOT_RTL);
	}
	
	public OperationOrderType getOrder()
	{
		return order.get(this);	
	}
	
	public int getPriority()
	{
		return priorities.get(this);
	}
}
