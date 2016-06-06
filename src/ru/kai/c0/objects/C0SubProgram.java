package ru.kai.c0.objects;

import java.util.Vector;

public abstract class C0SubProgram extends C0Object
{
	abstract public boolean hasVariable(String name);
	abstract public Vector<C0Variable> getVariables();
}
