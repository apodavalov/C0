/* ********************************************************* */
/* Компилятоp С0 Java-версия Д.Г. Хохлов А.А. Подавалов      */
/* 17.07.2008                                                */
/*                                                           */
/* Гpамматика С0:                                            */
/* пpогpамма ::= {описание-пеpеменных | описание-функции}... */
/* описание-пеpеменных ::= INT имя [,имя]... ;               */
/* описание-функции ::= INT имя([INT имя[,INT имя]...])      */
/*                  {[описание-пеpеменных]... [опеpатоp]...} */
/* опеpатоp ::= [выpажение]; | { [опеpатоp]...} |            */
/*              IF (выpажение) опеpатоp |                    */
/*          WHILE (выpажение) опеpатоp | RETURN [выpажение]; */
/* выpажение ::= теpм [{+|-|*|/|%|<|>|<=|>=|==|!=|=}теpм]... */
/* теpм ::= число | имя | имя([выpажение[,выpажение]...]) |  */
/*          -теpм | (выpажение)                              */
/* имя ::= буква [буква|цифpа]...                            */
/* число ::= цифpа...                                        */
/* буква ::= A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|  */
/*           X|Y|Z|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|  */
/*           u|v|w|x|y|z                                     */
/* цифpа ::= 0|1|2|3|4|5|6|7|8|9                             */
/* ********************************************************* */

package ru.kai.c0;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ResourceBundle;

import ru.kai.c0.exceptions.C0Exception;
import ru.kai.c0.objects.C0Program;
import ru.kai.helpers.Utf8ResourceBundle;

public class C0Main
{
	static ResourceBundle messages;
	
	private static String formatMessage(String fileName, int currentLine, String message, boolean isWarning)
	{
		String status;
		String statusLong;
		if (isWarning)
		{
			status = "w";
			statusLong = messages.getString("warning");
		}
		else
		{
			status = "e";
			statusLong = messages.getString("error");
		}
		String res = fileName + ":" + currentLine + ":" + status + ":" + statusLong + ":" + message; 
		return res;
	}	
	
	
	public static void main(String[] args)
	{
		messages = Utf8ResourceBundle.getBundle("i18n/Messages");
		InputStream input = null;
		OutputStream output = null;
		boolean isOutput = false;
		String inputFileName = "<stdin>";
		C0Parser c0Parser = null;
		try
		{
			for (int i = 0; i < args.length && (input == null || output == null); i++)
			{
				if (args[i].compareTo("-o") == 0)
					isOutput = true;
				else if (isOutput)
				{
					isOutput = false;
					if (output == null)
						output = new FileOutputStream(args[i]);
				}
				else
				{
					if (input == null)
					{
						input = new FileInputStream(args[i]);
						inputFileName = args[i];
					}
				}
			}
			
			if (input == null)
				input = System.in;
			if (output == null)
				output = System.out;

			c0Parser = new C0Parser(input);
			C0Program c0program = c0Parser.parse();

			if (System.getProperty("os.name").equals("Linux"))
			{
			    LinuxI386CodeGenerator generator = new LinuxI386CodeGenerator(output,c0program);
			    generator.generate();
			}
			else
			{
			    I386CodeGenerator generator = new I386CodeGenerator(output,c0program);
			    generator.generate();
			}
		}
		catch (FileNotFoundException e)
		{
			System.err.printf(messages.getString("file_open_error"),e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			System.err.printf(messages.getString("io_error"),e.getLocalizedMessage());
		}
		catch (C0Exception e)
		{
			System.err.print(formatMessage(inputFileName, c0Parser.getCurrentLine(), e.getLocalizedMessage(), false));
		}
	}
}
