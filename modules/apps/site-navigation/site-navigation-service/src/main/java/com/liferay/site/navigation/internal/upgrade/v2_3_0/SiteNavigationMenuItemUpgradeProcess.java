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

package com.liferay.site.navigation.internal.upgrade.v2_3_0;

import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Lourdes Fernández Besada
 */
public class SiteNavigationMenuItemUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				"select siteNavigationMenuItemId, typeSettings from " +
					"SiteNavigationMenuItem where type_ = 'display_page'");
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update SiteNavigationMenuItem set type_ = ? where " +
						"siteNavigationMenuItemId = ?");
			ResultSet resultSet = preparedStatement1.executeQuery()) {

			while (resultSet.next()) {
				UnicodeProperties typeSettingsUnicodeProperties =
					UnicodePropertiesBuilder.fastLoad(
						resultSet.getString("typeSettings")
					).build();

				long classNameId = GetterUtil.getLong(
					typeSettingsUnicodeProperties.getProperty("classNameId"));

				preparedStatement2.setString(
					1, PortalUtil.getClassName(classNameId));

				preparedStatement2.setLong(
					2, resultSet.getLong("siteNavigationMenuItemId"));

				preparedStatement2.addBatch();
			}

			preparedStatement2.executeBatch();
		}
	}

}