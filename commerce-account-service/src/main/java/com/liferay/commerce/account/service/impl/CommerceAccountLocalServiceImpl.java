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

package com.liferay.commerce.account.service.impl;

import com.liferay.commerce.account.constants.CommerceAccountConstants;
import com.liferay.commerce.account.exception.CommerceAccountNameException;
import com.liferay.commerce.account.exception.DuplicateCommerceAccountException;
import com.liferay.commerce.account.internal.search.CommerceAccountIndexer;
import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.account.service.base.CommerceAccountLocalServiceBaseImpl;
import com.liferay.portal.kernel.dao.orm.QueryDefinition;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.SystemEventConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.BaseModelSearchResult;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.systemevent.SystemEvent;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ServiceProxyFactory;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;
import com.liferay.portal.spring.extender.service.ServiceReference;
import com.liferay.users.admin.kernel.file.uploads.UserFileUploadsSettings;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Leo
 * @author Alessio Antonio Rendina
 */
public class CommerceAccountLocalServiceImpl
	extends CommerceAccountLocalServiceBaseImpl {

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceAccount addCommerceAccount(
			String name, long parentCommerceAccountId, String email,
			String taxId, boolean active, String externalReferenceCode,
			ServiceContext serviceContext)
		throws PortalException {

		// Commerce Account

		User user = userLocalService.getUser(serviceContext.getUserId());

		parentCommerceAccountId = getParentCommerceAccountId(
			serviceContext.getCompanyId(), parentCommerceAccountId);

		validate(serviceContext.getCompanyId(), 0, name, externalReferenceCode);

		long commerceAccountId = counterLocalService.increment();

		CommerceAccount commerceAccount = commerceAccountPersistence.create(
			commerceAccountId);

		commerceAccount.setCompanyId(user.getCompanyId());
		commerceAccount.setUserId(user.getUserId());
		commerceAccount.setUserName(user.getFullName());
		commerceAccount.setName(name);
		commerceAccount.setParentCommerceAccountId(parentCommerceAccountId);
		commerceAccount.setEmail(email);
		commerceAccount.setTaxId(taxId);
		commerceAccount.setActive(active);
		commerceAccount.setExternalReferenceCode(externalReferenceCode);
		commerceAccount.setExpandoBridgeAttributes(serviceContext);

		commerceAccountPersistence.update(commerceAccount);

		// Group

		groupLocalService.addGroup(
			user.getUserId(), GroupConstants.DEFAULT_PARENT_GROUP_ID,
			CommerceAccount.class.getName(), commerceAccountId,
			GroupConstants.DEFAULT_LIVE_GROUP_ID, getLocalizationMap(name),
			null, GroupConstants.TYPE_SITE_PRIVATE, false,
			GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION, null, false, true,
			null);

		// Resources

		resourceLocalService.addResources(
			user.getCompanyId(), GroupConstants.DEFAULT_LIVE_GROUP_ID,
			user.getUserId(), CommerceAccount.class.getName(),
			commerceAccount.getCommerceAccountId(), false, false, false);

		// Workflow

		return WorkflowHandlerRegistryUtil.startWorkflowInstance(
			commerceAccount.getCompanyId(), WorkflowConstants.DEFAULT_GROUP_ID,
			user.getUserId(), CommerceAccount.class.getName(),
			commerceAccountId, commerceAccount, serviceContext,
			new HashMap<>());
	}

	@Indexable(type = IndexableType.DELETE)
	@Override
	@SystemEvent(type = SystemEventConstants.TYPE_DELETE)
	public CommerceAccount deleteCommerceAccount(
			CommerceAccount commerceAccount)
		throws PortalException {

		// Commerce account organization rels

		commerceAccountOrganizationRelLocalService.
			deleteCommerceAccountOrganizationRelsByCommerceAccountId(
				commerceAccount.getCommerceAccountId());

		// Commerce account user rels

		commerceAccountUserRelLocalService.
			deleteCommerceAccountUserRelsByCommerceAccountId(
				commerceAccount.getCommerceAccountId());

		// Commerce account

		commerceAccountPersistence.remove(commerceAccount);

		// Resources

		resourceLocalService.deleteResource(
			commerceAccount, ResourceConstants.SCOPE_INDIVIDUAL);

		// Expando

		expandoRowLocalService.deleteRows(
			commerceAccount.getCommerceAccountId());

		return commerceAccount;
	}

	@Override
	public CommerceAccount deleteCommerceAccount(long commerceAccountId)
		throws PortalException {

		CommerceAccount commerceAccount =
			commerceAccountPersistence.findByPrimaryKey(commerceAccountId);

		return commerceAccountLocalService.deleteCommerceAccount(
			commerceAccount);
	}

	@Override
	public void deleteLogo(long commerceAccountId) throws PortalException {
		CommerceAccount commerceAccount =
			commerceAccountPersistence.findByPrimaryKey(commerceAccountId);

		_portal.updateImageId(commerceAccount, false, null, "logoId", 0, 0, 0);
	}

	@Override
	public CommerceAccount fetchCommerceAccount(long companyId, String name) {
		return commerceAccountPersistence.fetchByC_N(companyId, name);
	}

	@Override
	public List<CommerceAccount> getUserCommerceAccounts(
		long userId, Long parentCommerceAccountId, int start, int end) {

		QueryDefinition<CommerceAccount> queryDefinition =
			new QueryDefinition<>();

		queryDefinition.setAttribute(
			"parentCommerceAccountId", parentCommerceAccountId);
		queryDefinition.setStart(start);
		queryDefinition.setEnd(end);

		return commerceAccountFinder.getUserCommerceAccounts(
			userId, queryDefinition);
	}

	@Override
	public int getUserCommerceAccountsCount(
		long userId, Long parentCommerceAccountId) {

		QueryDefinition<CommerceAccount> queryDefinition =
			new QueryDefinition<>();

		queryDefinition.setAttribute(
			"parentCommerceAccountId", parentCommerceAccountId);

		return commerceAccountFinder.getUserCommerceAccountsCount(
			userId, queryDefinition);
	}

	@Override
	public BaseModelSearchResult<CommerceAccount> searchCommerceAccounts(
			long companyId, long parentCommerceAccountId, String keywords,
			Boolean active, int start, int end, Sort sort)
		throws PortalException {

		SearchContext searchContext = buildSearchContext(
			companyId, parentCommerceAccountId, active, start, end, sort);

		searchContext.setKeywords(keywords);

		return searchCommerceAccounts(searchContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceAccount updateCommerceAccount(
			long commerceAccountId, String name, boolean logo, byte[] logoBytes,
			String email, String taxId, boolean active,
			ServiceContext serviceContext)
		throws PortalException {

		CommerceAccount commerceAccount =
			commerceAccountPersistence.findByPrimaryKey(commerceAccountId);

		validate(
			serviceContext.getCompanyId(),
			commerceAccount.getCommerceAccountId(), name,
			commerceAccount.getExternalReferenceCode());

		commerceAccount.setName(name);

		_portal.updateImageId(
			commerceAccount, logo, logoBytes, "logoId",
			_userFileUploadsSettings.getImageMaxSize(),
			_userFileUploadsSettings.getImageMaxHeight(),
			_userFileUploadsSettings.getImageMaxWidth());

		commerceAccount.setEmail(email);
		commerceAccount.setTaxId(taxId);
		commerceAccount.setActive(active);
		commerceAccount.setExpandoBridgeAttributes(serviceContext);

		commerceAccountPersistence.update(commerceAccount);

		// Workflow

		return WorkflowHandlerRegistryUtil.startWorkflowInstance(
			commerceAccount.getCompanyId(), WorkflowConstants.DEFAULT_GROUP_ID,
			commerceAccount.getUserId(), CommerceAccount.class.getName(),
			commerceAccountId, commerceAccount, serviceContext,
			new HashMap<>());
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceAccount updateStatus(
			long userId, long commerceAccountId, int status,
			ServiceContext serviceContext,
			Map<String, Serializable> workflowContext)
		throws PortalException {

		User user = userLocalService.getUser(userId);
		Date now = new Date();

		CommerceAccount commerceAccount =
			commerceAccountPersistence.findByPrimaryKey(commerceAccountId);

		if ((status == WorkflowConstants.STATUS_APPROVED) &&
			(commerceAccount.getDisplayDate() != null) &&
			now.before(commerceAccount.getDisplayDate())) {

			status = WorkflowConstants.STATUS_SCHEDULED;
		}

		Date modifiedDate = serviceContext.getModifiedDate(now);

		commerceAccount.setModifiedDate(modifiedDate);

		if (status == WorkflowConstants.STATUS_APPROVED) {
			Date expirationDate = commerceAccount.getExpirationDate();

			if ((expirationDate != null) && expirationDate.before(now)) {
				commerceAccount.setExpirationDate(null);
			}
		}

		if (status == WorkflowConstants.STATUS_EXPIRED) {
			commerceAccount.setExpirationDate(now);
		}

		commerceAccount.setStatus(status);
		commerceAccount.setStatusByUserId(user.getUserId());
		commerceAccount.setStatusByUserName(user.getFullName());
		commerceAccount.setStatusDate(modifiedDate);

		commerceAccountPersistence.update(commerceAccount);

		return commerceAccount;
	}

	@Override
	public CommerceAccount upsertCommerceAccount(
			String name, long parentCommerceAccountId, boolean logo,
			byte[] logoBytes, String email, String taxId, boolean active,
			String externalReferenceCode, ServiceContext serviceContext)
		throws PortalException {

		CommerceAccount commerceAccount =
			commerceAccountPersistence.fetchByC_ERC(
				serviceContext.getCompanyId(), externalReferenceCode);

		if (commerceAccount == null) {
			commerceAccount = commerceAccountPersistence.fetchByC_N(
				serviceContext.getCompanyId(), name);
		}

		if (commerceAccount != null) {
			return commerceAccountLocalService.updateCommerceAccount(
				commerceAccount.getCommerceAccountId(), name, logo, logoBytes,
				email, taxId, active, serviceContext);
		}

		return commerceAccountLocalService.addCommerceAccount(
			name, parentCommerceAccountId, email, taxId, active,
			externalReferenceCode, serviceContext);
	}

	protected SearchContext buildSearchContext(
		long companyId, long parentCommerceAccountId, Boolean active, int start,
		int end, Sort sort) {

		SearchContext searchContext = new SearchContext();

		searchContext.setAttribute(
			CommerceAccountIndexer.FIELD_PARENT_COMMERCE_ACCOUNT_ID,
			parentCommerceAccountId);

		if (active != null) {
			searchContext.setAttribute(
				CommerceAccountIndexer.FIELD_ACTIVE, active);
		}

		searchContext.setCompanyId(companyId);
		searchContext.setStart(start);
		searchContext.setEnd(end);

		QueryConfig queryConfig = searchContext.getQueryConfig();

		queryConfig.setHighlightEnabled(false);
		queryConfig.setScoreEnabled(false);

		if (sort != null) {
			searchContext.setSorts(sort);
		}

		return searchContext;
	}

	protected List<CommerceAccount> getCommerceAccounts(Hits hits)
		throws PortalException {

		List<Document> documents = hits.toList();

		List<CommerceAccount> commerceAccounts = new ArrayList<>(
			documents.size());

		for (Document document : documents) {
			long commerceAccountId = GetterUtil.getLong(
				document.get(Field.ENTRY_CLASS_PK));

			CommerceAccount commerceAccount =
				commerceAccountPersistence.fetchByPrimaryKey(commerceAccountId);

			if (commerceAccount == null) {
				commerceAccounts = null;

				Indexer<CommerceAccount> indexer =
					IndexerRegistryUtil.getIndexer(CommerceAccount.class);

				long companyId = GetterUtil.getLong(
					document.get(Field.COMPANY_ID));

				indexer.delete(companyId, document.getUID());
			}
			else if (commerceAccount != null) {
				commerceAccounts.add(commerceAccount);
			}
		}

		return commerceAccounts;
	}

	protected long getParentCommerceAccountId(
		long companyId, long parentCommerceAccountId) {

		if (parentCommerceAccountId !=
				CommerceAccountConstants.DEFAULT_PARENT_ACCOUNT_ID) {

			// Ensure parent account exists and belongs to the proper
			// company

			CommerceAccount parentCommerceAccount =
				commerceAccountPersistence.fetchByPrimaryKey(
					parentCommerceAccountId);

			if ((parentCommerceAccount == null) ||
				(companyId != parentCommerceAccount.getCompanyId())) {

				parentCommerceAccountId =
					CommerceAccountConstants.DEFAULT_PARENT_ACCOUNT_ID;
			}
		}

		return parentCommerceAccountId;
	}

	protected BaseModelSearchResult<CommerceAccount> searchCommerceAccounts(
			SearchContext searchContext)
		throws PortalException {

		Indexer<CommerceAccount> indexer =
			IndexerRegistryUtil.nullSafeGetIndexer(CommerceAccount.class);

		for (int i = 0; i < 10; i++) {
			Hits hits = indexer.search(searchContext, _SELECTED_FIELD_NAMES);

			List<CommerceAccount> commerceAccounts = getCommerceAccounts(hits);

			if (commerceAccounts != null) {
				return new BaseModelSearchResult<>(
					commerceAccounts, hits.getLength());
			}
		}

		throw new SearchException(
			"Unable to fix the search index after 10 attempts");
	}

	protected void validate(
			long companyId, long commerceAccountId, String name,
			String externalReferenceCode)
		throws PortalException {

		if (Validator.isNull(name)) {
			throw new CommerceAccountNameException();
		}

		CommerceAccount commerceAccount = null;
		String errorMessage = null;

		if (Validator.isNotNull(externalReferenceCode)) {
			commerceAccount = commerceAccountPersistence.fetchByC_ERC(
				companyId, externalReferenceCode);

			errorMessage =
				"There is another commerce account with external reference " +
					"code " + externalReferenceCode;
		}

		if (commerceAccount == null) {
			commerceAccount = commerceAccountPersistence.fetchByC_N(
				companyId, name);

			errorMessage = "There is another commerce account named " + name;
		}

		if ((commerceAccount != null) &&
			(commerceAccount.getCommerceAccountId() != commerceAccountId)) {

			throw new DuplicateCommerceAccountException(errorMessage);
		}
	}

	private static final String[] _SELECTED_FIELD_NAMES =
		{Field.ENTRY_CLASS_PK, Field.COMPANY_ID};

	private static volatile UserFileUploadsSettings _userFileUploadsSettings =
		ServiceProxyFactory.newServiceTrackedInstance(
			UserFileUploadsSettings.class,
			CommerceAccountLocalServiceImpl.class, "_userFileUploadsSettings",
			false);

	@ServiceReference(type = Portal.class)
	private Portal _portal;

}