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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

/**
 * @author Marián Konček
 */
public final class Config
{
	static final Pattern int_range_pattern = Pattern.compile("([0-9]*)\\s*-\\s*([0-9]*)");
	
	ArrayList<Rule> rules = new ArrayList<>();
	
	static final Rule.Match read_predicate(String end, XMLEventReader event_reader) throws Exception
	{
		switch (end)
		{
		case "and":
			return new Rule.And_match(read_predicate_list(end, event_reader));
		case "or":
			return new Rule.Or_match(read_predicate_list(end, event_reader));
		default:
			break;
		}
		
		Rule.Match result = null;
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals(end))
				{
					break loop;
				}
				
				break;
				
			case XMLStreamConstants.START_ELEMENT:
				var start_name = event.asStartElement().getName().getLocalPart();
				
				if (end.equals("not"))
				{
					if (result != null)
					{
						throw new RuntimeException("<not> must contain exactly one element");
					}
					
					result = new Rule.Not_match(read_predicate(start_name, event_reader));
				}
				
				break;
				
			case XMLStreamConstants.CHARACTERS:
				var content = event.asCharacters().getData().strip();
				
				if (result != null)
				{
					continue;
				}
				
				switch (end)
				{
				case "name":
					result = new Rule.Method_match(RpmInfo.class.getMethod("getName"), Pattern.compile(content));
					break;
				case "arch":
					result = new Rule.Method_match(RpmInfo.class.getMethod("getArch"), Pattern.compile(content));
					break;
				}
				
				break;
			}
		}
		
		if (result == null)
		{
			throw new RuntimeException("Could not read predicate");
		}
		
		return result;
	}
	
	static final List<Rule.Match> read_predicate_list(String end, XMLEventReader event_reader) throws Exception
	{
		var result = new ArrayList<Rule.Match>();
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				var start_name = event.asStartElement().getName().getLocalPart();
				
				result.add(read_predicate(start_name, event_reader));
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals(end))
				{
					break loop;
				}
				
				break;
			}
		}
		
		if (result.isEmpty())
		{
			throw new RuntimeException("Trying to read an empty list");
		}
		
		return result;
	}
	
	static final Rule.Match read_match(XMLEventReader event_reader) throws Exception
	{
		Rule.Match result = null;
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals("match"))
				{
					break loop;
				}
				
				break;
				
			case XMLStreamConstants.START_ELEMENT:
				var start_name = event.asStartElement().getName().getLocalPart();
				
				if (result == null)
				{
					result = read_predicate(start_name, event_reader);
				}
				else
				{
					throw new RuntimeException("<match> can contain at most one element");
				}
				
				break;
			}
		}
		
		if (result == null)
		{
			throw new RuntimeException("Could not read <match>");
		}
		
		return result;
	}
	
	static final Validator read_validator(String end, XMLEventReader event_reader) throws Exception
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
				case "all":
					result = new Validator.All_validator(read_validator_list(start_name, event_reader));
					break;
				case "any":
					result = new Validator.Any_validator(read_validator_list(start_name, event_reader));
					break;
				case "none":
					result = new Validator.None_validator(read_validator_list(start_name, event_reader));
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
	
	static final ArrayList<Validator> read_validator_list(String end, XMLEventReader event_reader) throws Exception
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
	
	static final Rule read_rule(XMLEventReader event_reader) throws Exception
	{
		Rule result = new Rule();
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				final var start_name = event.asStartElement().getName().getLocalPart();
				final var decor = Package_test.color_decorator();
				
				switch (start_name)
				{
				case "match":
					result.match = read_match(event_reader);
					break;
					
				case "files":
					result.validators.put(start_name,
							new Validator.Delegating_validator(read_validator(start_name, event_reader))
					{
						@Override
						protected Test_result do_validate(String value)
						{
							return delegate.do_validate(value).prefix(decor.decorate("[Files]", Ansi_colors.Type.bold) + ": ");
						}
					});
					break;
					
				case "requires":
					result.validators.put(start_name,
							new Validator.Delegating_validator(read_validator(start_name, event_reader))
					{
						@Override
						protected Test_result do_validate(String value)
						{
							return delegate.do_validate(value).prefix(decor.decorate("[Requires]", Ansi_colors.Type.bold) + ": ");
						}
					});
					break;
					
				case "provides":
					result.validators.put(start_name,
							new Validator.Delegating_validator(read_validator(start_name, event_reader))
					{
						@Override
						protected Test_result do_validate(String value)
						{
							return delegate.do_validate(value).prefix(decor.decorate("[Provides]", Ansi_colors.Type.bold) + ": ");
						}
					});
					break;
					
				case "java-bytecode":
					result.jar_validator = new Jar_validator.Jar_class_validator(
							new Jar_validator.Class_bytecode_validator(
									read_validator(start_name, event_reader)));
					break;
				}
				
				if (start_name.startsWith("rpm-file-size-"))
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
					
					result.validators.put("rpm-file-size",
							new Validator.Delegating_validator(converting_validator)
					{
						@Override
						protected Test_result do_validate(String value)
						{
							return delegate.do_validate(value).prefix(Package_test.color_decorator()
									.decorate("[File size in " + suffix + "]", Ansi_colors.Type.bold) + ": ");
						}
					});
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
	
	public final ArrayList<Rule> rules()
	{
		return rules;
	}
	
	public final String to_xml()
	{
		var result = new StringBuilder();
		
		result.append("<config>");
		
		for (var rule : rules)
		{
			result.append(rule.to_xml());
		}
		
		result.append("</config>");
		
		return result.toString();
	}
}
