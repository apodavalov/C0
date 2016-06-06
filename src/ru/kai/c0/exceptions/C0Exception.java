package ru.kai.c0.exceptions;

import java.util.Locale;
import java.util.ResourceBundle;

import ru.kai.helpers.Utf8ResourceBundle;

public class C0Exception extends Exception
{
	private static final long serialVersionUID = -3649129220778799485L;
	
	private String localizedMessage;
	private String message;
	
	private ResourceBundle messages = Utf8ResourceBundle.getBundle("i18n/Messages");
	private ResourceBundle rootMessages = Utf8ResourceBundle.getBundle("i18n/Messages",Locale.ROOT);
	
	public String getLocalizedMessage()
	{
		return localizedMessage;	
	}
	
	public String getMessage()
	{
		return message;		
	}
	
	public C0Exception(String format, Object... args)
	{
		message = String.format(Locale.ROOT, rootMessages.getString(format), args);
		localizedMessage = String.format(messages.getString(format), args);
		if (localizedMessage == null) localizedMessage = message;
	}
}
