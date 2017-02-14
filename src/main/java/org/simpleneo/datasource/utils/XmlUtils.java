package org.simpleneo.datasource.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlUtils  {

	public static <T>T getByXpath(String resource, String xpathExpression, Class<T> type) {
		XmlUtils xmlUtils = new XmlUtils();
		File file = xmlUtils.getFile(resource);

	    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

	    try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			Document document = builder.parse(file);
			XPath xpath =  XPathFactory.newInstance().newXPath();
			XPathExpression xPathExpression = xpath.compile(xpathExpression);
			NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);

			if(String.class.isAssignableFrom(type) && nodeList.getLength() == 1) {
				return type.cast(nodeList.item(0).getNodeValue());
			}

			if(NodeList.class.isAssignableFrom(type)) {
				return type.cast(nodeList);
			}

			if(List.class.isAssignableFrom(type)) {
				List<String> result = new ArrayList<>();
				for(int i=0; i < nodeList.getLength(); i++) {
					result.add(nodeList.item(i).getNodeValue());
				}
				return type.cast(result);
			}

			return null;
		} catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public URL getResource(String resource) {
		return this.getClass().getResource(resource);
	}

	private File getFile(String resource) {
		try {
			File xmlFile = new File(this.getClass().getResource(resource).toURI());
			return xmlFile;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

    public static String JsonToXml(Object object, List<String> asAttributes) {
    	return JsonToXml(object, null, asAttributes);
    }

    public static String JsonToXml(Object object, String tagName) {
    	return JsonToXml(object, tagName, null);
    }

    //TODO: fox asAttributes
    public static String JsonToXml(Object object, String tagName, List<String> asAttributes) {

    	StringBuilder result = new StringBuilder();
        JSONArray jsonArray;
        Map<String, Object> jsonObject;
        String key;
        Iterator<String> keys;
        String string;
        Object value;

        //if (object instanceof JSONObject) {
        if (object instanceof Map<?, ?>) {

            jsonObject = (Map<String, Object>) object;
            Set<String> keySet = jsonObject.keySet();
            keys = jsonObject.keySet().iterator();

            // Emit <tagName>
            if (tagName != null) {
            	result.append('<');
            	result.append(tagName);

            	if(containsAny(keySet, asAttributes)) {
            		attributes(jsonObject, result);
            	}

            	result.append('>');
            }

            while (keys.hasNext()) {
                key = keys.next();
                value = jsonObject.get(key);

                if (value == null) {
                    value = "";
                } else if (value.getClass().isArray()) {
                    //value = new JSONArray(value);
                	value = new JSONArray();
                }
                string = value instanceof String ? (String) value : null;

                // Emit content in body
                if ("content".equals(key)) {
                    if (value instanceof JSONArray) {
                    	jsonArray = (JSONArray) value;
                        int i = 0;
                        for (Object val : jsonArray) {
                            if (i > 0) {
                            	result.append('\n');
                            }
                            result.append(escape(val.toString()));
                            i++;
                        }
                    } else {
                    	result.append(escape(value.toString()));
                    }

                    // Emit an array of similar keys

                } else if (value instanceof JSONArray) {
                	jsonArray = (JSONArray) value;
                    for (Object val : jsonArray) {
                        if (val instanceof JSONArray) {
                        	result.append('<');
                        	result.append(key);
                        	result.append('>');
                        	result.append(JsonToXml(val, asAttributes));
                        	result.append("</");
                        	result.append(key);
                        	result.append('>');
                        } else {
                        	result.append(JsonToXml(val, key));
                        }
                    }
                } else if ("".equals(value)) {
                	result.append('<');
                	result.append(key);
                	result.append("/>");

                    // Emit a new tag <k>

                } else {
                	result.append(toString(value, key, asAttributes));
                }
            }
            if (tagName != null) {

                // Emit the </tagname> close tag
            	result.append("</");
            	result.append(tagName);
            	result.append('>');
            }
            return result.toString();

        }

        if (object != null) {
            if (object.getClass().isArray()) {
            	System.out.println("IS ARRAY!");
                //object = new JSONArray(object);
            }

            if (object instanceof JSONArray) {
            	System.out.println("JsonToXml ARRAY");
            	jsonArray = (JSONArray) object;
                for (Object val : jsonArray) {
                    // XML does not have good support for arrays. If an array
                    // appears in a place where XML is lacking, synthesize an
                    // <array> element.

                	result.append(toString(val, tagName == null ? "array" : tagName, asAttributes));

                }
                return result.toString();
            }
        }

        string = (object == null) ? "null" : escape(object.toString());
        return (tagName == null) ? "\"" + string + "\""
                : (string.length() == 0) ? "<" + tagName + "/>" : "<" + tagName
                        + ">" + string + "</" + tagName + ">";

    }

    public static String escape(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, length = string.length(); i < length; i++) {
            char c = string.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toString(Object object, List<String> asAttributes) {
        return toString(object, null);
    }

    public static String toString(Object object, String tagName, List<String> asAttributes) {

        StringBuilder sb = new StringBuilder();
        JSONArray jsonArray;
        JSONObject jsonObject;
        String key;
        Iterator<String> keys;
        String string;
        Object value;

        if (object instanceof JSONObject) {

            jsonObject = (JSONObject) object;
            Set<String> keySet = jsonObject.keySet();

            // Emit <tagName>
            if (tagName != null) {
            	if(asAttributes == null || !asAttributes.contains(tagName)) {
	                sb.append('<');
	                sb.append(tagName);

	            	if(containsAny(keySet, asAttributes)) {
	            		attributes(jsonObject, sb);
	            	}

	                sb.append('>');
            	}
            }

            // Loop thru the keys.

            keys = jsonObject.keySet().iterator();
            while (keys.hasNext()) {
                key = keys.next();
                //value = jsonObject.opt(key);
                value = jsonObject.get(key);
                if (value == null) {
                    value = "";
                } else if (value.getClass().isArray()) {
                    //value = new JSONArray(value);
                	value = new JSONArray();
                }
                string = value instanceof String ? (String) value : null;

                // Emit content in body
                if ("content".equals(key)) {
                    if (value instanceof JSONArray) {
                    	jsonArray = (JSONArray) value;
                        int i = 0;
                        for (Object val : jsonArray) {
                            if (i > 0) {
                                sb.append('\n');
                            }
                            sb.append(escape(val.toString()));
                            i++;
                        }
                    } else {
                        sb.append(escape(value.toString()));
                    }

                    // Emit an array of similar keys

                } else if (value instanceof JSONArray) {
                	jsonArray = (JSONArray) value;
                    for (Object val : jsonArray) {
                        if (val instanceof JSONArray) {
                            sb.append('<');
                            sb.append(key);
                            sb.append('>');
                            sb.append(toString(val, asAttributes));
                            sb.append("</");
                            sb.append(key);
                            sb.append('>');
                        } else {
                            sb.append(toString(val, key, asAttributes));
                        }
                    }
                } else if ("".equals(value)) {
                    sb.append('<');
                    sb.append(key);
                    sb.append("/>");

                    // Emit a new tag <k>

                } else {
                	if(asAttributes == null || !asAttributes.contains(tagName)) {
                		sb.append(toString(value, key, asAttributes));
                	}
                }
            }
            if (tagName != null) {

            	if(asAttributes == null || !asAttributes.contains(tagName)) {
                    // Emit the </tagname> close tag
                    sb.append("</");
                    sb.append(tagName);
                    sb.append('>');
            	}

            }
            return sb.toString();

        }

        if (object != null) {
            if (object.getClass().isArray()) {
                //object = new JSONArray(object);
            	object = new JSONArray();
            }

            if (object instanceof JSONArray) {
            	System.out.println("toString ARRAY");
            	jsonArray = (JSONArray) object;
                for (Object val : jsonArray) {
                    // XML does not have good support for arrays. If an array
                    // appears in a place where XML is lacking, synthesize an
                    // <array> element.
                    sb.append(toString(val, tagName == null ? "array" : tagName, asAttributes));
                }
                return sb.toString();
            }
        }

        string = (object == null) ? "null" : escape(object.toString());
        return (tagName == null) ? "\"" + string + "\""
                : (string.length() == 0) ? "<" + tagName + "/>" : "<" + tagName
                        + ">" + string + "</" + tagName + ">";

    }

    public static void attributes(Map<String, Object> jsonObject, StringBuilder result) {
        Iterator<String> keys = jsonObject.keySet().iterator();
        String key;
        Object value;
        while (keys.hasNext()) {
            key = keys.next();
            value = jsonObject.get(key);
            if(value instanceof JSONObject) {
            	attributes((Map<String, Object>) value, result);
            } else if(value instanceof String || value instanceof Integer) {
            	result.append(" "+key+"=\""+value+"\"");
            }
		}
    }

    public static boolean containsAny(Collection<?> c1, Collection<?> c2) {
    	if(c1 == null || c2 == null) {
    		return false;
    	}
    	for (Object object : c1) {
			if(c2.contains(object)) {
				return true;
			}
		}
    	return false;
    }

}
