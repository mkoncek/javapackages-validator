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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Marián Konček
 */
public class XML_document
{
	public static class XML_node
	{
		private String name = null;
		private String content = null;
		private ArrayList<XML_node> descendants = null;
		
		private StringBuilder dump(StringBuilder result)
		{
			if (content == null && descendants == null)
			{
				result.append("<" + name + "/>");
			}
			else
			{
				result.append("<" + name + ">");
				
				if (content != null)
				{
					result.append(content);
				}
				else
				{
					descendants.stream().forEach(d -> d.dump(result));
				}
				
				result.append("</" + name + ">");
			}
			
			return result;
		}
		
		public final String dump()
		{
			return dump(new StringBuilder()).toString();
		}
		
		public final String name()
		{
			return name;
		}
		
		public final String content()
		{
			return content;
		}
		
		public final XML_node get(String name)
		{
			return get_from(name, descendants.stream());
		}
		
		public final Stream<XML_node> gets(String name)
		{
			return gets_from(name, descendants.stream());
		}
	}
	
	private static final Stream<XML_node> gets_from(String name, Stream<XML_node> stream)
	{
		return stream.filter(d -> d.name.equals(name));
	}
	
	private static final XML_node get_from(String name, Stream<XML_node> stream)
	{
		try
		{
			return gets_from(name, stream).reduce((lhs, rhs) ->
			{
				throw new RuntimeException("Multiple entries of \"" + name + "\" found");
			}).get();
		}
		catch (NoSuchElementException ex)
		{
			throw new NoSuchElementException(ex.getMessage() + ": \"" + name + "\"");
		}
	}
	
	private ArrayList<XML_node> nodes = new ArrayList<>();
	
	public XML_document(InputStream is) throws IOException, XMLStreamException
	{
		try (var reader = new BufferedReader(new InputStreamReader(is)))
		{
			XMLEventReader event_reader = null;
			
			try
			{
				event_reader = XMLInputFactory.newInstance().createXMLEventReader(reader);
				
				while (event_reader.hasNext())
				{
					nodes.add(read_node(event_reader));
				}
				
				nodes.remove(nodes.size() - 1);
			}
			catch (XMLStreamException ex)
			{
				event_reader.close();
				throw ex;
			}
		}
	}
	
	private XML_node read_node(XMLEventReader event_reader) throws XMLStreamException
	{
		var result = new XML_node();
		XMLEvent event = null;
		
		loop: while (event_reader.hasNext())
		{
			event = event_reader.peek();
			
			switch (event.getEventType())
			{
			case XMLStreamConstants.START_ELEMENT:
				if (result.name == null)
				{
					result.name = event.asStartElement().getName().getLocalPart();
					event_reader.nextEvent();
				}
				else
				{
					if (result.descendants == null)
					{
						result.content = null;
						result.descendants = new ArrayList<>();
					}
					
					result.descendants.add(read_node(event_reader));
					
					/// OMITTED
					/// event_reader.nextEvent();
				}
				
				break;
				
			case XMLStreamConstants.END_ELEMENT:
				var end_name = event.asEndElement().getName().getLocalPart();
				
				if (! end_name.equals(result.name))
				{
					throw new RuntimeException(MessageFormat.format(
							"XML_document: Read en end-element \"{0}\" that is " +
							"different from the current node named \"{1}\"",
							end_name, result.name));
				}
				
				event_reader.nextEvent();
				break loop;
				
			case XMLStreamConstants.CHARACTERS:
				var content = event.asCharacters();
				
				if (result.descendants == null)
				{
					if (result.content == null)
					{
						result.content = "";
					}
					
					result.content += content;
				}
				
				event_reader.nextEvent();
				break;
				
			default:
				event_reader.nextEvent();
				break;
			}
		}
		
		return result;
	}
	
	public final XML_node get(String name)
	{
		return get_from(name, nodes.stream());
	}
	
	public final Stream<XML_node> gets(String name)
	{
		return gets_from(name, nodes.stream());
	}
	
	public final String dump()
	{
		var sb = new StringBuilder();
		nodes.stream().forEach(n -> n.dump(sb));
		return sb.toString();
	}
}
