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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Ansi_colors.Decorator;
import org.fedoraproject.javapackages.validator.XML_document.XML_node;

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
					public String to_xml()
					{
						return MessageFormat.format("<{0}>{1}</{0}>", node.name(), node.content());
					}
				};
				
				break;
				
			case "rule":
				var rule = rules.get(node.content());
				
				if (rule == null)
				{
					throw new RuntimeException(MessageFormat.format(
							"Referring to a nonexisting rule \"{0}\" in the <match> field",
							node.content()));
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
	
	final Validator read_validator_files(XML_node node) throws Exception
	{
		String match = node.get("match").content();
		
		Optional<String> symlink = null;
		
		{
			var op_sym = node.getop("symlink");
			if (op_sym.isPresent())
			{
				var op_target = op_sym.get().getop("target");
				if (op_target.isPresent())
				{
					symlink = Optional.of(op_target.get().content());
				}
				else
				{
					symlink = Optional.empty();
				}
			}
		}
		
		boolean want_directory = node.getop("directory").isPresent();
		
		return new Validator()
		{
			@Override
			public String to_xml()
			{
				/// TODO
				return "";
			}
			
			@Override
			protected Test_result do_validate(Object value, RpmInfo rpm_info)
			{
				return validate((CpioArchiveEntry) value, rpm_info);
			}
			
			private Test_result validate(CpioArchiveEntry value, RpmInfo rpm_info)
			{
				var result = new Test_result(true);
				
				if (match == null || value.getName().matches(match))
				{
					if (want_directory && ! value.isDirectory())
					{
						result.result = false;
					}
				}
				
				return result;
			}
		};
	}
	
	final Rule read_rule(XML_node node) throws Exception
	{
		Rule result = new Rule();
		
		try
		{
			node.gets().forEach(inner_node ->
			{
				switch (inner_node.name())
				{
				case "name":
					result.name = inner_node.content();
					if (rules.containsKey(result.name))
					{
						throw new RuntimeException(MessageFormat.format(
								"Found a rule with the same name as a previous rule: \"{0}\"",
								result.name));
					}
					
					rules.put(result.name, result);
					break;
					
				case "match":
					result.match = read_match(inner_node);
					break;
					
				case "files":
					/// TODO
					break;
					
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
			});
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
			
			for (var rule_node : document.nodes())
			{
				read_rule(rule_node);
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
