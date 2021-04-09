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

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Ansi_colors.Decorator;
import org.fedoraproject.javapackages.validator.Validator.File_multivalidator;
import org.fedoraproject.javapackages.validator.Validator.File_validator;
import org.fedoraproject.javapackages.validator.XML_document.XML_node;

/**
 * @author Marián Konček
 */
public final class Config implements Xml_writable
{
	static final Pattern int_range_pattern = Pattern.compile("(['_0-9]*)\\s*-\\s*(['_0-9]*)");
	static final String int_range_replacement_regex = "[_']";
	
	private Map<String, Rule> rules_by_name = new LinkedHashMap<>();
	private Map<String, List<Rule>> rules_by_tag = new LinkedHashMap<>();
	private Rule.Structured rule;
	
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
	
	final Rule.Match read_match_primitive(XML_node node)
	{
		Rule.Match result = null;
		
		try
		{
			switch (node.name())
			{
			case "name":
				result = new Rule.Method_match(
						RpmInfo.class.getMethod("getName"), Pattern.compile(node.content()));
				break;
				
			case "arch":
				result = new Rule.Method_match(
						RpmInfo.class.getMethod("getArch"), Pattern.compile(node.content()));
				break;
				
			case "release":
				result = new Rule.Method_match(
						RpmInfo.class.getMethod("getRelease"), Pattern.compile(node.content()));
				break;
			
			case "distribution":
				final boolean want_source;
				
				switch (node.content())
				{
				case "source":
					want_source = true;
					break;
					
				case "binary":
					want_source = false;
					break;
					
				default:
					throw new RuntimeException(MessageFormat.format(
							"Found unrecognized type \"{0}\" inside <distribution>",
							node.content()));
				}
				
				result = new Rule.Match()
				{
					@Override
					public boolean test(RpmInfo rpm_info)
					{
						return rpm_info.isSourcePackage() == want_source;
					}
					
					@Override
					public void to_xml(StringBuilder result)
					{
						result.append("<" + node.name() + ">");
						result.append(node.content());
						result.append("</" + node.name() + ">");
					}
				};
				
				break;
				
			case "rule":
				var rule = rules_by_name.get(node.content());
				
				if (rule == null)
				{
					throw new RuntimeException(MessageFormat.format(
							"Referring to a nonexisting rule \"{0}\" in the <match> field",
							node.content()));
				}
				
				result = new Rule.Rule_match(rule);
				break;
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
		
		return result;
	}

	final Rule.Match read_match_inside(XML_node node)
	{
		switch (node.name())
		{
		case "and":
			return new Rule.And_match(read_match_list(node));
			
		case "or":
			return new Rule.Or_match(read_match_list(node));
			
		case "not":
			return new Rule.Not_match(read_match_inside(node.get()));
			
		default:
			return read_match_primitive(node);
		}
	}
	
	final List<Rule.Match> read_match_list(XML_node node)
	{
		var result = new ArrayList<Rule.Match>();
		node.gets().forEach(n -> result.add(read_match_inside(n)));
		if (result.isEmpty())
		{
			throw new RuntimeException("Read an empty list at <" + node.name() + ">");
		}
		return result;
	}
	
	final Rule.Match read_match(XML_node node)
	{
		Rule.Match result;
		
		var inner_node = node.getop();
		
		if (inner_node.isPresent())
		{
			result = read_match_inside(inner_node.get());
		}
		else
		{
			result = new Rule.All_match();
		}
		
		return result;
	}
	
	final Validator read_validator_body(XML_node node)
	{
		Validator result = null;
		
		switch (node.name())
		{
		case "all":
			result = new Validator.All_validator(read_validator_list(node));
			break;
			
		case "any":
			result = new Validator.Any_validator(read_validator_list(node));
			break;
			
		case "none":
			result = new Validator.None_validator(read_validator_list(node));
			break;
		
		case "pass":
			result = new Validator.Pass_validator();
			break;
			
		case "fail":
			result = new Validator.Fail_validator();
			break;
			
		case "text":
			result = new Validator.Text_validator(node.content());
			break;
			
		case "regex":
			result = new Validator.Regex_validator(node.content());
			break;
			
		case "int-range":
			var matcher = int_range_pattern.matcher(node.content());
			
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
		
		if (result == null)
		{
			throw new RuntimeException("Could not read validator named \"" + node.name() + "\"");
		}
		
		return result;
	}
	
	final ArrayList<Validator> read_validator_list(XML_node node)
	{
		var result = new ArrayList<Validator>();
		node.gets().forEach(n -> result.add(read_validator_body(n)));
		if (result.isEmpty())
		{
			throw new RuntimeException("Read an empty list at <" + node.name() + ">");
		}
		return result;
	}
	
	final File_validator read_validator_files(XML_node node)
	{
		var match = node.get("match");
		var result = new File_validator(match.content());
		
		var op_name = node.getop("name");
		if (op_name.isPresent())
		{
			result.name_validator = read_validator_body(op_name.get().get());
		}
		
		var op_symlink = node.getop("symlink");
		if (op_symlink.isPresent())
		{
			result.symlink_target = Optional.empty();
			
			var op_target = op_symlink.get().getop("target");
			if (op_target.isPresent())
			{
				result.symlink_target = Optional.of(read_validator_body(op_target.get().get()));
			}
		}
		
		if (node.getop("directory").isPresent())
		{
			result.want_directory = true;
		}
		
		return result;
	}
	
	final Rule.Structured read_execution_inside(XML_node node)
	{
		switch (node.name())
		{
		case "rule":
			return new Rule.Structured.Name(rules_by_name.get(node.content()));
			
		case "tag":
		{
			var tag = node.content() != null ? node.content() : "";
			return new Rule.Structured.Tag(tag, rules_by_tag.getOrDefault(tag, Collections.emptyList()));
		}	
		case "all":
		{
			var result = new Rule.Structured.All();
			node.gets().forEach(n -> result.rules.add(read_execution_inside(n)));
			return result;
		}	
		case "any":
		{
			var result = new Rule.Structured.Any();
			node.gets().forEach(n -> result.rules.add(read_execution_inside(n)));
			return result;
		}
		default:
			throw new RuntimeException("Unrecognized node <" + node.name() + "> inside <execution>");
		}
	}
	
	final Rule read_rule(XML_node node) throws Exception
	{
		Rule result = new Rule();
		
		try
		{
			String tag = null;
			
			for (var inner_node : node.getr())
			{
				switch (inner_node.name())
				{
				case "name":
					result.name = inner_node.content();
					if (rules_by_name.containsKey(result.name))
					{
						throw new RuntimeException(MessageFormat.format(
								"Found a rule with the same name as a previous rule: \"{0}\"",
								result.name));
					}
					
					rules_by_name.put(result.name, result);
					break;
					
				case "tag":
					tag = inner_node.content();
					break;
					
				case "match":
					result.match = read_match(inner_node);
					break;
					
				case "files":
				{
					result.validators.put(inner_node.name(),
							new File_multivalidator(inner_node.gets()
							.map((x) -> read_validator_files(x))
							.collect(Collectors.toCollection(ArrayList::new))));
					break;
				}
					
				default:
					if (message_map.keySet().contains(inner_node.name()))
					{
						result.validators.put(inner_node.name(), read_validator_body(inner_node.get()));
					}
					else
					{
						throw new RuntimeException(MessageFormat.format(
								"Found unrecognized node <{0}> inside <rule>", inner_node.name()));
					}
					
					break;
				}
			}
			
			tag = tag != null ? tag : "";
			var list = rules_by_tag.getOrDefault(tag, new ArrayList<Rule>());
			list.add(result);
			rules_by_tag.put(tag, list);
		}
		catch (Exception ex)
		{
			throw new RuntimeException(MessageFormat.format(
					"When reading rule named \"{0}\"", result.name), ex);
		}
		
		result.validators.values().forEach(v -> v.rule = result);
		
		return result;
	}
	
	public Config(InputStream is) throws Exception
	{
		try (var document = new XML_document(is))
		{
			document.start("config");
			
			XML_node execution = null;
			
			for (var node : document.nodes())
			{
				switch (node.name())
				{
				case "rule":
					read_rule(node);
					break;
					
				case "execution":
					execution = node;
					break;
				}
			}
			
			rule = read_execution_inside(execution.get());
		}
	}
	
	public final Rule.Structured rule()
	{
		return rule;
	}
	
	@Override
	public void to_xml(StringBuilder result)
	{
		result.append("<config>");
		
		result.append("<execution>");
		rule.to_xml(result);
		result.append("</execution>");
		
		for (var rule : rules_by_name.values())
		{
			rule.to_xml(result);
		}
		
		result.append("</config>");
	}
}
