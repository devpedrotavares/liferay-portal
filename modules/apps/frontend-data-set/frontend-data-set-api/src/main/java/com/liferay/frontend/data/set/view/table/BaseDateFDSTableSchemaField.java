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

package com.liferay.frontend.data.set.view.table;

import com.liferay.portal.kernel.json.JSONObject;

/**
 * @author Guilherme Camacho
 */
public class BaseDateFDSTableSchemaField extends FDSTableSchemaField {

	public String getFormat() {
		if (_format == null) {
			return null;
		}

		return _format.toString();
	}

	public void setFormat(JSONObject formatJSONObject) {
		_format = formatJSONObject;
	}

	/**
	 * @deprecated As of Cavanaugh (7.4.x), replaced by {@link
	 *           #setFormat(JSONObject)}
	 */
	@Deprecated
	public void setFormat(String format) {
		_format = format;
	}

	@Override
	public JSONObject toJSONObject() {
		JSONObject jsonObject = super.toJSONObject();

		return jsonObject.put("format", _format);
	}

	private Object _format;

}