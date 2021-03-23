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
		bold, underline,
		
		black, red, green, yellow, blue, magenta, cyan, white,
		
		bright_black, bright_red, bright_green, bright_yellow,
		bright_blue, bright_magenta, bright_cyan, bright_white,
	}
	
	/**
	 * @param string The string to decorate.
	 * @param types Decorations to be applied to the string. May contains at most
	 * one color value, if none if provided then the default color is white.
	 * @return Decorated string.
	 */
	public static String decorate(Object object, Type... types)
	{
		Type color = null;
		
		for (var type : types)
		{
			if (type != Type.bold && type != Type.underline)
			{
				if (color != null)
				{
					throw new IllegalArgumentException("Multiple colors specified");
				}
				else
				{
					color = type;
				}
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
			
		case bright_black:
			result.append("90m");
			break;
		case bright_red:
			result.append("91m");
			break;
		case bright_green:
			result.append("92m");
			break;
		case bright_yellow:
			result.append("93m");
			break;
		case bright_blue:
			result.append("94m");
			break;
		case bright_magenta:
			result.append("95m");
			break;
		case bright_cyan:
			result.append("96m");
			break;
		case bright_white:
			result.append("97m");
			break;
			
		default:
			break;
		}
		
		result.append(object.toString());
		result.append("\033[0m");
		
		return result.toString();
	}
	
	static interface Decorator
	{
		public String decorate(Object object, Type... types);
	}
	
	static class Default_decorator implements Decorator
	{
		public String decorate(Object object, Type... types)
		{
			return Ansi_colors.decorate(object, types);
		}
	}
	
	static class No_decorator implements Decorator
	{
		public String decorate(Object object, Type... types)
		{
			return object.toString();
		}
	}
}
