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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * @author Marián Konček
 */
public class XML_document implements AutoCloseable
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
		
		public final Stream<XML_node> gets()
		{
			if (descendants != null)
			{
				return descendants.stream();
			}
			
			return Stream.empty();
		}
		
		public final Optional<XML_node> getop()
		{
			try
			{
				return gets().reduce((lhs, rhs) ->
				{
					throw new RuntimeException("Multiple entries of \"" + name + "\" found");
				});
			}
			catch (NoSuchElementException ex)
			{
				throw new NoSuchElementException(ex.getMessage() + ": \"" + name + "\"");
			}
		}
		
		public final XML_node get()
		{
			return getop().get();
		}
		
		public final Stream<XML_node> gets(String name)
		{
			return gets().filter(n -> n.name().equals(name));
		}
		
		public final Optional<XML_node> getop(String name)
		{
			return getop_from(name, gets());
		}
		
		public final XML_node get(String name)
		{
			return getop(name).get();
		}
	}
	
	public static class XML_node_iterator implements Iterator<XML_node>
	{
		private final XMLEventReader reader;
		
		public XML_node_iterator(XMLEventReader reader)
		{
			this.reader = reader;
		}
		
		@Override
		public boolean hasNext()
		{
			try
			{
				loop: while (reader.hasNext())
				{
					var event = reader.peek();
					
					switch (event.getEventType())
					{
					case XMLStreamConstants.END_ELEMENT:
					case XMLStreamConstants.END_DOCUMENT:
						break loop;
						
					case XMLStreamConstants.START_ELEMENT:
						return true;
					}
					
					reader.nextEvent();
				}
				
				return false;
			}
			catch (XMLStreamException ex)
			{
				throw new RuntimeException(ex);
			}
		}

		@Override
		public XML_node next()
		{
			try
			{
				return read_node(reader);
			}
			catch (XMLStreamException ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}
	
	private static final Optional<XML_node> getop_from(Stream<XML_node> stream)
	{
		return stream.reduce((lhs, rhs) -> {throw new IllegalStateException();});
	}
	
	private static final Optional<XML_node> getop_from(String name, Stream<XML_node> stream)
	{
		try
		{
			return getop_from(stream.filter(n -> n.name().equals(name)));
		}
		catch (NoSuchElementException ex)
		{
			throw new NoSuchElementException(ex.getMessage() + ": \"" + name + "\"");
		}
		catch (IllegalStateException ex)
		{
			throw new IllegalStateException("Multiple entries of \"" + name + "\" found");
		}
	}
	
	private BufferedReader reader;
	private XMLEventReader event_reader;
	
	@Override
	public void close() throws Exception
	{
		event_reader.close();
		reader.close();
	}
	
	public XML_document(InputStream is) throws IOException, XMLStreamException
	{
		reader = new BufferedReader(new InputStreamReader(is));
		event_reader = XMLInputFactory.newInstance().createXMLEventReader(reader);
	}
	
	public final XML_document start(String name) throws XMLStreamException
	{
		while (event_reader.hasNext())
		{
			var event = event_reader.nextEvent();
			if (event.getEventType() == XMLStreamConstants.START_ELEMENT &&
					event.asStartElement().getName().getLocalPart().equals(name))
			{
				break;
			}
		}
		
		return this;
	}
	
	public final XML_document end(String name) throws XMLStreamException
	{
		while (event_reader.hasNext())
		{
			var event = event_reader.nextEvent();
			if (event.getEventType() == XMLStreamConstants.END_ELEMENT &&
					event.asEndElement().getName().getLocalPart().equals(name))
			{
				break;
			}
		}
		
		return this;
	}
	
	public final Iterator<XML_node> iterator()
	{
		return new XML_node_iterator(event_reader);
	}
	
	public final Iterable<XML_node> nodes()
	{
		return new Iterable<XML_document.XML_node>()
		{
			@Override
			public Iterator<XML_node> iterator()
			{
				return XML_document.this.iterator();
			}
		};
	}
	
	private static XML_node read_node(XMLEventReader event_reader) throws XMLStreamException
	{
		var result = new XML_node();
		
		loop: while (event_reader.hasNext())
		{
			var event = event_reader.peek();
			
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
}
