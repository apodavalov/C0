package ru.kai.c0;

import java.io.IOException;

import ru.kai.c0.exceptions.C0Exception;

class LexemState 
{
	public LexemStatus lexemStatus;
	public Lexem lexem;
	public IOException ioException;
	public C0Exception c0Exception; 
} 	
