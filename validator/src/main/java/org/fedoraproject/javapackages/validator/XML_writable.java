package org.fedoraproject.javapackages.validator;

public interface XML_writable
{
	void to_xml(StringBuilder result);
	
	default String to_xml()
	{
		var result = new StringBuilder();
		to_xml(result);
		return result.toString();
	}
}
