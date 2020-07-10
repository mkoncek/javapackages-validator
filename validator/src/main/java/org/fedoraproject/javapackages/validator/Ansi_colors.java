/*-
 * Copyright (c) 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.javapackages.validator;

/**
 * @author Marián Konček
 */
public class Ansi_colors
{
	public enum Type
	{
		bold, underline, black, red, green, yellow, blue, magenta, cyan, white,
	}
	
	/**
	 * @param string The string to decorate.
	 * @param types Decorations to be applied to the string. May contains at most
	 * one color value, if none if provided then the default color is white.
	 * @return Decorated string.
	 */
	public static String decorate(String string, Type... types)
	{
		Type color = null;
		
		for (var type : types)
		{
			switch (type)
			{
			case black:
			case red:
			case green:
			case yellow:
			case blue:
			case magenta:
			case cyan:
			case white:
				if (color != null)
				{
					throw new IllegalArgumentException("Multiple colors specified");
				}
				
				color = type;
				break;
			default:
				continue;
			}
		}
		
		if (color == null)
		{
			color = Type.white;
		}
		
		StringBuilder result = new StringBuilder("\033[");
		
		for (var type : types)
		{
			switch (type)
			{
			case bold:
				result.append("1;");
				break;
			case underline:
				result.append("4;");
				break;
			default:
				continue;
			}
		}
		
		switch (color)
		{
		case black:
			result.append("30m");
			break;
		case red:
			result.append("31m");
			break;
		case green:
			result.append("32m");
			break;
		case yellow:
			result.append("33m");
			break;
		case blue:
			result.append("34m");
			break;
		case magenta:
			result.append("35m");
			break;
		case cyan:
			result.append("36m");
			break;
		case white:
			result.append("37m");
			break;
		default:
			break;
		}
		
		result.append(string);
		result.append("\033[0m");
		
		return result.toString();
	}
	
	static interface Decorator
	{
		public String decorate(String string, Type... types);
	}
	
	static class Default_decorator implements Decorator
	{
		public String decorate(String string, Type... types)
		{
			return Ansi_colors.decorate(string, types);
		}
	}
	
	static class No_decorator implements Decorator
	{
		public String decorate(String string, Type... types)
		{
			return string;
		}
	}
}
