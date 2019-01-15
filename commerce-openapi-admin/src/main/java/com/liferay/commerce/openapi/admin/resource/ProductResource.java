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

package com.liferay.commerce.openapi.admin.resource;

import com.liferay.commerce.openapi.admin.model.ProductDTO;

import javax.annotation.Generated;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * @author Igor Beslic
 */
@Generated(value = "OSGiRESTModuleGenerator")
@Path("/1.0/product")
public interface ProductResource {

	@DELETE
	@Path("/{id}")
	public Response deleteProduct(@PathParam("id") String id);

	@GET
	@Path("/{id}")
	@Produces({"application/*", "application/json"})
	public Response getProduct(
		@PathParam("id") String id, @QueryParam("group_id") long groupId);

	@GET
	@Path("/")
	@Produces({"application/*", "application/json"})
	public Response getProducts(
		@QueryParam("group_id") long groupId, @QueryParam("page") int page,
		@QueryParam("pageSize") int pageSize);

	@Consumes("application/*")
	@Path("/{id}")
	@Produces("application/json")
	@PUT
	public Response updateProduct(
		@PathParam("id") String id, ProductDTO productDTO);

	@Consumes("application/*")
	@Path("/")
	@POST
	@Produces("application/json")
	public Response upsertProduct(ProductDTO productDTO);

}