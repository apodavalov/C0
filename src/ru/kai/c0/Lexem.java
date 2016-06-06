package ru.kai.c0;

public class Lexem
{
	public LexemType lexemType;
	public String stringValue;
	public long longValue;

	public Lexem(LexemType lexemType)
	{
		this.lexemType = lexemType;
	}

	public Lexem(String value)
	{
		this.lexemType = LexemType.LX_NAME;
		this.stringValue = value;
	}

	public Lexem(long value)
	{
		this.lexemType = LexemType.LX_CONST;
		this.longValue = value;
	}
}
