/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.commerce.openapi.util;

/**
 * @author Igor Beslic
 */
public class PropertyDefinition {

	public PropertyDefinition(String name, String type, String format) {
		_name = name;
		_type = type;
		_format = format;

		HttpParameterType httpParameterType = HttpParameterType.OBJECT;

		if (type != null) {
			httpParameterType = HttpParameterType.fromDefinition(type);
		}

		HttpParameterFormat httpParameterFormat =
			HttpParameterFormat.fromHttpParameterTypeAndDefinition(
				httpParameterType, _format);

		_httpParameterFormat = httpParameterFormat;
	}

	public PropertyDefinition(
		String name, String type, String itemsType, String itemsFormat) {

		_name = name;
		_type = type;
		_format = itemsFormat;

		HttpParameterType httpParameterType = HttpParameterType.OBJECT;

		if (type != null) {
			httpParameterType = HttpParameterType.fromDefinition(itemsType);
		}

		HttpParameterFormat httpParameterFormat =
			HttpParameterFormat.fromHttpParameterTypeAndDefinition(
				httpParameterType, _format);

		_httpParameterFormat = httpParameterFormat;
	}

	public String getExample() {
		return _example;
	}

	public String getItemFormat() {
		return _itemFormat;
	}

	public String getItemType() {
		return _itemType;
	}

	public String getJavaType() {
		if ("array".equals(_type)) {
			return _httpParameterFormat.getJavaType() + "[]";
		}

		return _httpParameterFormat.getJavaType();
	}

	public String getName() {
		return _name;
	}

	public String getType() {
		return _type;
	}

	public boolean isRequired() {
		return _required;
	}

	public void setExample(String example) {
		_example = example;
	}

	public void setItemFormat(String itemFormat) {
		_itemFormat = itemFormat;
	}

	public void setItemType(String itemType) {
		_itemType = itemType;
	}

	public void setName(String name) {
		_name = name;
	}

	public void setRequired(boolean required) {
		_required = required;
	}

	public void setType(String type) {
		_type = type;
	}

	@Override
	public String toString() {
		if (_toString != null) {
			return _toString;
		}

		StringBuilder sb = new StringBuilder();

		sb.append("{example=");
		sb.append(_example);
		sb.append(", format=");
		sb.append(_format);
		sb.append(", itemFormat=");
		sb.append(_itemFormat);
		sb.append(", itemType=");
		sb.append(_itemType);
		sb.append(", name=");
		sb.append(_name);
		sb.append(", required=");
		sb.append(_required);
		sb.append(", type=");
		sb.append(_type);
		sb.append("}");

		_toString = sb.toString();

		return _toString;
	}

	private String _example;
	private final String _format;
	private final HttpParameterFormat _httpParameterFormat;
	private String _itemFormat;
	private String _itemType;
	private String _name;
	private boolean _required;
	private String _toString;
	private String _type;

}