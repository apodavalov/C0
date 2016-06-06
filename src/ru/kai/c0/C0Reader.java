package ru.kai.c0;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import ru.kai.c0.exceptions.C0Exception;

public class C0Reader
{
	private static final int maxNameLength = 64;
	private static final long maxConstValue = 2147483648L;
	private Keywords keywords;
	private InputStreamReader inputStream;

	private int currentLine = 1;
	private int status;
	private StringBuilder currentValue;
	private long currentLongValue;
	private boolean isValueTooLong;
	
	protected void startParse()
	{
	
	}

	protected void onLexem(Lexem lexem)
	{
		
	}

	protected void endParse()
	{
		
	}

	protected void onError(C0Exception ex)
	{
		
	}

	protected void onIOException(IOException ex)
	{
		
	}

	private boolean isSpaceChar(char value)
	{
		if (value == '\t' || value == ' ' || value == '\r' || value == '\n')
			return true;
		return false;
	}

	private boolean isLetter(char value)
	{
		return Character.isLetter(value);
	}

	private boolean isLetterOrDigit(char value)
	{
		if (isDigit(value) || isLetter(value))
			return true;
		return false;
	}

	private boolean isDigit(char value)
	{
		if (value >= '0' && value <= '9')
			return true;
		return false;
	}

	private C0Reader(InputStreamReader inputStreamReader)
	{
		this.inputStream = inputStreamReader;
		keywords = new Keywords();
		keywords.put("if", LexemType.LX_IF);
		keywords.put("int", LexemType.LX_INT);
		keywords.put("while", LexemType.LX_WHILE);
		keywords.put("return", LexemType.LX_RETURN);
	}

	public C0Reader(InputStream inputStream)
	{
		this(new InputStreamReader(inputStream));
	}

	public C0Reader(InputStream inputStream, Charset charset)
	{
		this(new InputStreamReader(inputStream, charset));
	}

	public C0Reader(InputStream inputStream, CharsetDecoder charsetDecoder)
	{
		this(new InputStreamReader(inputStream, charsetDecoder));
	}

	public C0Reader(InputStream inputStream, String string) throws UnsupportedEncodingException
	{
		this(new InputStreamReader(inputStream, string));
	}

	private void beginLexem(char character, boolean isEnd)
	{
		status = 0;
		if (!isEnd)
			if (isLetter(character))
			{
				status = 5;
				currentValue = new StringBuilder();
				currentValue = currentValue.append(character);
				isValueTooLong = false;
			}
			else if (isDigit(character))
			{
				status = 6;
				currentLongValue = Character.getNumericValue(character);
				isValueTooLong = false;
			}
			else if (character == '+')
				onLexem(new Lexem(LexemType.LX_PLUS));
			else if (character == '-')
				onLexem(new Lexem(LexemType.LX_MINUS));
			else if (character == '*')
				onLexem(new Lexem(LexemType.LX_ASTERISK));
			else if (character == '/')
				onLexem(new Lexem(LexemType.LX_SLASH));
			else if (character == '%')
				onLexem(new Lexem(LexemType.LX_PERCENT));
			else if (character == ',')
				onLexem(new Lexem(LexemType.LX_COMMA));
			else if (character == ';')
				onLexem(new Lexem(LexemType.LX_SEMICOLON));
			else if (character == '(')
				onLexem(new Lexem(LexemType.LX_LPARENTHESIS));
			else if (character == ')')
				onLexem(new Lexem(LexemType.LX_RPARENTHESIS));
			else if (character == '{')
				onLexem(new Lexem(LexemType.LX_LCBRACKET));
			else if (character == '}')
				onLexem(new Lexem(LexemType.LX_RCBRACKET));
			else if (character == '=')
				status = 1;
			else if (character == '!')
				status = 2;
			else if (character == '<')
				status = 3;
			else if (character == '>')
				status = 4;
			else if (character == '\n')
				currentLine++;
			else if (!isSpaceChar(character))
				try
				{
					throw new C0Exception("invalid_character", character);					
				}
				catch (C0Exception ex)
				{
					onError(ex);
				}
	}

	private void onChar(char character, boolean isEnd)
	{
		switch (status)
		{
			case 0:
				beginLexem(character, isEnd);
				break;
			case 1:
				if (isEnd || character != '=')
				{
					onLexem(new Lexem(LexemType.LX_EQUALS));
				}
				else
				{
					onLexem(new Lexem(LexemType.LX_DOUBLE_EQUALS));
					status = 0;
					break;
				}
				beginLexem(character, isEnd);
				break;
			case 2:
				if (isEnd || character != '=')
					try
					{
						throw new C0Exception("invalid_character", character);					
					}
					catch (C0Exception ex)
					{
						onError(ex);
					}
				else
				{
					onLexem(new Lexem(LexemType.LX_EXLAMATION_EQUALS));
					status = 0;
					break;
				}
				beginLexem(character, isEnd);
				break;
			case 3:
				if (isEnd || character != '=')
					onLexem(new Lexem(LexemType.LX_LESSTHAN));
				else
				{
					onLexem(new Lexem(LexemType.LX_LESSEQUALS));
					status = 0;
					break;
				}
				beginLexem(character, isEnd);
				break;
			case 4:
				if (isEnd || character != '=')
					onLexem(new Lexem(LexemType.LX_GREATERTHAN));
				else
				{
					onLexem(new Lexem(LexemType.LX_GREATEREQUALS));
					status = 0;
					break;
				}
				beginLexem(character, isEnd);
				break;
			case 5:
				if (!isEnd && isLetterOrDigit(character))
				{
					if (currentValue.length() < maxNameLength)
						currentValue = currentValue.append(character);
					else
						isValueTooLong = true;
				}
				else
				{
					String currentNameString = currentValue.toString();
					if (isValueTooLong)
						try
						{
							throw new C0Exception("name_too_long",currentNameString);
						}
						catch (C0Exception ex)
						{
							onError(ex);
						}					
					LexemType lexemType = keywords.get(currentNameString);
					if (lexemType != null)
						onLexem(new Lexem(lexemType));
					else
						onLexem(new Lexem(currentNameString));
					currentValue = null;
					beginLexem(character, isEnd);
				}
				break;
			case 6:
				if (!isEnd && Character.isDigit(character))
				{
					if (currentLongValue < maxConstValue)
					{
						currentLongValue *= 10;
						currentLongValue += Character
								.getNumericValue(character);
					}
					else if (currentLongValue > maxConstValue)
						isValueTooLong = true;
				}
				else
				{
					if (isValueTooLong)
						try
						{
							throw new C0Exception("value_out_of_range",currentLongValue);
						}
						catch (C0Exception ex)
						{
							onError(ex);
							currentLongValue = maxConstValue;
						}
					onLexem(new Lexem(currentLongValue));
					currentLongValue = 0;
					beginLexem(character, isEnd);
				}
				break;
		}
	}
	
	public int getCurrentLine()
	{
		return currentLine;
	}

	public void read()
	{
		try
		{
			char[] buffer = new char[1048576];
			int size;
			boolean isData = true;
			status = 0;
			startParse();
			do
			{
				size = inputStream.read(buffer);
				if (size == -1)
				{
					isData = false;
					onChar('0', true);
				}
				else
					for (int i = 0; i < size; i++)
						onChar(buffer[i], false);
			} while (isData);
		}
		catch (IOException ex)
		{
			onIOException(ex);
		}
		endParse();
	}
}
