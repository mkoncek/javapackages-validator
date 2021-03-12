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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Ansi_colors.Decorator;

/**
 * @author Marián Konček
 */
public final class Config
{
	static final Pattern int_range_pattern = Pattern.compile("(['_0-9]*)\\s*-\\s*(['_0-9]*)");
	static final String int_range_replacement_regex = "[_']";
	
	private Map<String, Rule> rules = new LinkedHashMap<>();
	static final Decorator decor = Package_test.color_decorator();
	
	static final Map<String, String> message_map = new HashMap<>();
	static
	{
		message_map.put("files", "[Files]");
		
		message_map.put("provides", "[Provides]");
		message_map.put("requires", "[Requires]");
		message_map.put("conflicts", "[Conflicts]");
		message_map.put("obsoletes", "[Obsoletes]");
		message_map.put("recommends", "[Recommends]");
		message_map.put("suggests", "[Suggests]");
		message_map.put("supplements", "[Supplements]");
		message_map.put("enhances", "[Enhances]");
		message_map.put("order-with-requires", "[Order with requires]");
		
		message_map.put("java-bytecode", "[Bytecode version]");
		message_map.put("rpm-file-size-bytes", "[RPM File size in bytes]");
		
		for (final var pair : message_map.entrySet())
		{
			pair.setValue(decor.decorate(pair.getValue(), Ansi_colors.Type.bold) + " ");
		}
	}
	
	final Rule.Match read_predicate(final String end, XMLEventReader event_reader) throws Exception
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
					result = new Rule.Method_match(
							RpmInfo.class.getMethod("getName"), Pattern.compile(content));
					break;
					
				case "arch":
					result = new Rule.Method_match(
							RpmInfo.class.getMethod("getArch"), Pattern.compile(content));
					break;
					
				case "release":
					result = new Rule.Method_match(
							RpmInfo.class.getMethod("getRelease"), Pattern.compile(content));
					break;
				
				case "distribution":
					final boolean want_source;
					
					if (content.equals("source"))
					{
						want_source = true;
					}
					else if (content.equals("binary"))
					{
						want_source = false;
					}
					else
					{
						throw new RuntimeException(MessageFormat.format(
								"Found unrecognized type \"{0}\" inside <distribution>",
								content));
					}
					
					result = new Rule.Match()
					{
						@Override
						public boolean test(RpmInfo rpm_info)
						{
							return rpm_info.isSourcePackage() == want_source;
						}
						
						@Override
						public String to_xml()
						{
							return MessageFormat.format("<{0}>{1}</{0}>", end, content);
						}
					};
					
					break;
					
				case "rule":
					var rule = rules.get(content);
					
					if (rule == null)
					{
						throw new RuntimeException(MessageFormat.format(
								"Referring to a nonexisting rule \"{0}\" in the <match> field",
								content));
					}
					
					var old_match = rule.match;
					var result_match = new Rule.Rule_match(rule);
					
					rule.match = new Rule.Match()
					{
						@Override
						public boolean test(RpmInfo rpm_info)
						{
							result_match.result = old_match.test(rpm_info);
							return result_match.result;
						}
						
						@Override
						public String to_xml()
						{
							return old_match.to_xml();
						}
					};
					
					result = result_match;
					
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
	
	final List<Rule.Match> read_predicate_list(
			final String end, XMLEventReader event_reader) throws Exception
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
	
	static final String read_content(final String end, XMLEventReader event_reader) throws Exception
	{
		String result = null;
		
		loop: while (event_reader.hasNext())
		{
			XMLEvent event = event_reader.nextEvent();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.END_ELEMENT:
				final var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals(end))
				{
					break loop;
				}
				
				break;
				
			case XMLStreamConstants.START_ELEMENT:
				throw new RuntimeException("Reading XML node but expected content");
				
			case XMLStreamConstants.CHARACTERS:
				 result = event.asCharacters().getData().strip();
				
				if (result != null)
				{
					continue;
				}
				
				break;
			}
		}
		
		if (result == null)
		{
			throw new RuntimeException("Could not read XML node content");
		}
		
		return result;
	}
	
	final Rule.Match read_match(XMLEventReader event_reader) throws Exception
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
					throw new RuntimeException("<match> can contain at most one node");
				}
				
				break;
			}
		}
		
		if (result == null)
		{
			result = new Rule.All_match();
		}
		
		return result;
	}
	
	static final Validator read_validator(final String end, XMLEventReader event_reader) throws Exception
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
				case "all":
					result = new Validator.All_validator(read_validator_list(start_name, event_reader));
					break;
					
				case "any":
					result = new Validator.Any_validator(read_validator_list(start_name, event_reader));
					break;
					
				case "none":
					result = new Validator.None_validator(read_validator_list(start_name, event_reader));
					break;
				
				case "pass":
					result = new Validator.Pass_validator();
					break;
					
				case "fail":
					result = new Validator.Fail_validator();
					break;
				}
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (end_name.equals(end))
				{
					/// We probably read an empty XML body
					if (result == null)
					{
					}
					
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
				case "text":
					result = new Validator.Text_validator(content);
					break;
					
				case "regex":
					result = new Validator.Regex_validator(content);
					break;
					
				case "int-range":
					var matcher = int_range_pattern.matcher(content);
					
					if (! matcher.matches())
					{
						throw new RuntimeException("Could not match <int-range>");
					}
					
					var min_group = matcher.group(1).replaceAll(int_range_replacement_regex, "");
					var min = min_group.equals("") ? Long.MIN_VALUE : Long.parseLong(min_group);
					
					var max_group = matcher.group(2).replaceAll(int_range_replacement_regex, "");
					var max = max_group.equals("") ? Long.MAX_VALUE : Long.parseLong(max_group);
					
					result = new Validator.Int_range_validator(min, max);
					break;
				}
				
				break;
			}
		}
		
		if (result == null)
		{
			throw new RuntimeException("internal error: function read_validator returned null, end is " + end);
		}
		
		return result;
	}
	
	static final ArrayList<Validator> read_validator_list(
			final String end, XMLEventReader event_reader) throws Exception
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
	
	final Rule read_rule(XMLEventReader event_reader) throws Exception
	{
		Rule result = new Rule();
		
		try
		{
			loop: while (event_reader.hasNext())
			{
				XMLEvent event = event_reader.nextEvent();
				
				switch (event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					final var start_name = event.asStartElement().getName().getLocalPart();
					
					switch (start_name)
					{
					case "name":
						final var name = read_content(start_name, event_reader);
						
						if (result.name != null)
						{
							throw new RuntimeException(MessageFormat.format(
									"Rule contains multiple name fields: \"{0}\" and \"{1}\"",
									result.name, name));
						}
						
						result.name = name;
						
						if (rules.containsKey(result.name))
						{
							throw new RuntimeException(MessageFormat.format(
									"Found a rule with the same name as a previous rule: \"{0}\"",
									name));
						}
						
						rules.put(result.name, result);
						break;
						
					case "parent":
						throw new RuntimeException("<parent> no longer used");
						
					case "exclusive":
						throw new RuntimeException("<exclusive> no longer used");
						
					case "match":
						result.match = read_match(event_reader);
						break;
					
					default:
						if (message_map.keySet().contains(start_name))
						{
							result.validators.put(start_name, read_validator(
									start_name, event_reader));
						}
						else
						{
							throw new RuntimeException(MessageFormat.format(
									"Found unrecognized node <{0}> inside <rule>", start_name));
						}
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
		}
		catch (Exception ex)
		{
			throw new RuntimeException(MessageFormat.format(
					"When reading rule named \"{0}\"", result.name), ex);
		}
		
		if (result.name == null)
		{
			throw new RuntimeException("Rule \"" + result.name + "\" does not have a <name>");
		}
		
		if (result.match == null)
		{
			throw new RuntimeException("Named rule \"" + result.name + "\" does not contain a <match>");
		}
		
		result.validators.values().forEach(v -> v.rule = result);
		
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
							read_rule(event_reader);
							break;
							
						case "config":
							break;
							
						default:
							throw new RuntimeException("Found unrecognized node <"
									+ start_name + "> during reading the configuration file");
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
	
	public final Collection<Rule> rules()
	{
		return rules.values();
	}
	
	public final String to_xml()
	{
		var result = new StringBuilder();
		
		result.append("<config>");
		
		for (var rule : rules())
		{
			result.append(rule.to_xml());
		}
		
		result.append("</config>");
		
		return result.toString();
	}
}
