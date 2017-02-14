package org.simpleneo.datasource;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class XmlQuery {

	private String id;
	private String node;
	private String type;
	private String cypher;
	private String count;

	public XmlQuery setId(String id) {
		this.id = id;
		return this;
	}

	public String getId() {
		return id;
	}

	public XmlQuery setNode(String node) {
		this.node = node;
		return this;
	}

	public String getNode() {
		return node;
	}

	public XmlQuery setType(String type) {
		this.type = type;
		return this;
	}

	public String getType() {
		return type;
	}

	public XmlQuery setCypher(String cypher) {
		this.cypher = cypher;
		return this;
	}

	public String getCypher() {
		return cypher;
	}

	public XmlQuery setCount(String count) {
		this.count = count;
		return this;
	}

	public String getCount() {
		return count;
	}

	@Override
	public String toString() {
		ToStringBuilder to = new ToStringBuilder(this);
		return to.toString();
	}

}