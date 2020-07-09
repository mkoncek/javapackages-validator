/*-
 * Copyright (c) 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.javapackages.validator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

import org.fedoraproject.javapackages.validator.Rule.Match;

/**
 * @author Marián Konček
 */
public class Config
{
	static final Pattern int_range_pattern = Pattern.compile("([0-9]*)\\s*-\\s*([0-9]*)");
	
	ArrayList<Rule> rules = new ArrayList<>();
	
	static Match read_match(XMLEventReader event_reader) throws Exception
	{
		String match_type = null;
		Match result = null;
		
		class Method_match implements Match
		{
			Method getter;
			Pattern pattern;
			
			public Method_match(Method getter, Pattern match)
			{
				super();
				this.getter = getter;
				this.pattern = match;
			}
			
			public boolean matches(RpmInfo rpm_info)
			{
				try
				{
					return pattern.matcher((String) getter.invoke(rpm_info)).matches();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				var start_name = event.asStartElement().getName().getLocalPart();
				
				if (match_type == null)
				{
					match_type = start_name;
				}
				else
				{
					throw new RuntimeException("<match> can contain at most one element");
				}
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals("match"))
				{
					break loop;
				}
				
				break;
				
			case XMLStreamConstants.CHARACTERS:
				var content = event.asCharacters().getData().strip();
				
				if (match_type == null || result != null)
				{
					continue;
				}
				
				switch (match_type)
				{
				case "name":
					result = new Method_match(RpmInfo.class.getMethod("getName"), Pattern.compile(content));
					break;
				case "arch":
					result = new Method_match(RpmInfo.class.getMethod("getArch"), Pattern.compile(content));
					break;
				}
				
				break;
			}
		}
		
		return result;
	}
	
	static Validator read_validator(String end, XMLEventReader event_reader) throws Exception
	{
		String match_type = null;
		Validator result = null;
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				var start_name = event.asStartElement().getName().getLocalPart();
				
				if (match_type == null)
				{
					match_type = start_name;
				}
				else
				{
					throw new RuntimeException("A rule can contain at most one validator");
				}
				
				switch (start_name)
				{
				case "whitelist":
					result = new Validator.Whitelist_validator(read_validator_list(start_name, event_reader));
					break;
				case "blacklist":
					result = new Validator.Blacklist_validator(read_validator_list(start_name, event_reader));
					break;
				}
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals(end))
				{
					break loop;
				}
				
				break;
				
			case XMLStreamConstants.CHARACTERS:
				var content = event.asCharacters().getData().strip();
				
				if (match_type == null || result != null)
				{
					continue;
				}
				
				switch (match_type)
				{
				case "regex":
					result = new Validator.Regex_validator(Pattern.compile(content));
					break;
				case "int-range":
					var matcher = int_range_pattern.matcher(content);
					
					if (! matcher.matches())
					{
						throw new RuntimeException("Could not match <int-range>");
					}
					
					var min_group = matcher.group(1);
					var min = min_group.equals("") ? Long.MIN_VALUE : Long.parseLong(min_group);
					
					var max_group = matcher.group(2);
					var max = max_group.equals("") ? Long.MAX_VALUE : Long.parseLong(max_group);
					
					result = new Validator.Int_range_validator(min, max);
					break;
				}
				
				break;
			}
		}
		
		if (result == null)
		{
			throw new RuntimeException("internal error: function read_validator returned null");
		}
		
		return result;
	}
	
	static List<Validator> read_validator_list(String end, XMLEventReader event_reader) throws Exception
	{
		var result = new ArrayList<Validator>();
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.peek();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				var start_name = event.asStartElement().getName().getLocalPart();
				result.add(read_validator(start_name, event_reader));
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				event = event_reader.nextEvent();
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals(end))
				{
					break loop;
				}
				
				break;
			
			default:
				event = event_reader.nextEvent();
			}
		}
		
		if (result.isEmpty())
		{
			throw new RuntimeException("Trying to read an empty list");
		}
		
		return result;
	}
	
	public static Rule read_rule(XMLEventReader event_reader) throws Exception
	{
		Rule result = new Rule();
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				final var start_name = event.asStartElement().getName().getLocalPart();
				
				switch (start_name)
				{
				case "match":
					result.match = read_match(event_reader);
					break;
				case "requires":
					result.requires = new Validator.Delegating_validator(read_validator(start_name, event_reader))
					{
						@Override
						Test_result validate(String value)
						{
							return delegate.validate(value).prefix("[Requires]: ");
						}
					};
					break;
				case "provides":
					result.provides = new Validator.Delegating_validator(read_validator(start_name, event_reader))
					{
						@Override
						Test_result validate(String value)
						{
							return delegate.validate(value).prefix("[Provides]: ");
						}
					};
					break;
				case "java-bytecode":
					result.jar_validator = new Jar_validator.Jar_class_validator(
							new Jar_validator.Class_bytecode_validator(
									read_validator(start_name, event_reader)));
					break;
				}
				
				if (start_name.startsWith("filesize-"))
				{
					final var validator = read_validator(start_name, event_reader);
					
					long divisor;
					String suffix;
					
					if (start_name.endsWith("-b"))
					{
						divisor = 1;
						suffix = "bytes";
					}
					else if (start_name.endsWith("-kb"))
					{
						divisor = 1024;
						suffix = "kilobytes";
					}
					else if (start_name.endsWith("-mb"))
					{
						divisor = 1024 * 1024;
						suffix = "megabytes";
					}
					else
					{
						throw new RuntimeException("Invalid filesize suffix");
					}
					
					final var converting_validator = new Validator.Transforming_validator(validator)
					{
						long file_size;
						
						@Override
						protected String transform(String value)
						{
							file_size = Long.parseLong(value) / divisor;
							return Long.toString(file_size);
						}
					};
					
					result.rpm_file_size = new Validator.Delegating_validator(converting_validator)
					{
						@Override
						Test_result validate(String value)
						{
							return delegate.validate(value).prefix("[File size in " + suffix + "]: ");
						}
					};
				}
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals("rule"))
				{
					break loop;
				}
				
				break;
			}
		}
		
		return result;
	}
	
	public Config(InputStream is) throws Exception
	{
		try (var br = new BufferedReader(new InputStreamReader(is)))
		{
			var event_reader = XMLInputFactory.newInstance().createXMLEventReader(br);
			
			try
			{
				loop: while (event_reader.hasNext())
				{
					XMLEvent event = event_reader.nextEvent();
					
					switch (event.getEventType())
					{
					case XMLStreamConstants.START_ELEMENT:
						var start_name = event.asStartElement().getName().getLocalPart();
						
						switch (start_name)
						{
						case "rule":
							rules.add(read_rule(event_reader));
						}
						
						break;
						
					case XMLStreamConstants.END_ELEMENT:
						var end_name = event.asEndElement().getName().getLocalPart();
						
						if (end_name.equals("config"))
						{
							break loop;
						}
						
						break;
						
					default:
						continue;
					}
				}
			}
			finally
			{
				event_reader.close();
			}
		}
	}
	
	public List<Rule> rules()
	{
		return rules;
	}
}
