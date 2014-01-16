package com.adobe.acs.tools.clientlib_bootstrapper.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@SlingServlet(resourceTypes = "acs-tools/components/clientlib-bootstrapper", selectors = "create",
        extensions = "json", methods = "POST")
public class CreateServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(CreateServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            JSONObject requestObject = new JSONObject(IOUtils.toString(request.getReader()));

            String basePath = requestObject.getString("basePath");
            String name = requestObject.getString("name");

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("jcr:primaryType", "cq:ClientLibraryFolder");
            properties.put("categories", getArrayValues(requestObject, "categories"));
            properties.put("dependencies", getArrayValues(requestObject, "dependencies"));
            properties.put("embeds", getArrayValues(requestObject, "embeds"));

            Iterator<Map.Entry<String, Object>> entries = properties.entrySet().iterator();
            while (entries.hasNext()) {
                if (entries.next().getValue() == null) {
                    entries.remove();
                }
            }

            boolean includeJs = requestObject.getBoolean("includeJs");
            boolean includeCss = requestObject.getBoolean("includeCss");

            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource parent = resourceResolver.getResource(basePath);
            if (parent == null) {
                returnError(response, String.format("The base path '%s' does not exist.", basePath));
                return;
            }

            Resource existing = parent.getChild(name);
            if (existing != null) {
                returnError(response,
                        String.format("A node named '%s' already exists under '%s'.", name, basePath));
                return;
            }

            Resource created = resourceResolver.create(parent, name, properties);

            if (includeJs) {
                try {
                    JcrUtils.putFile(created.adaptTo(Node.class), "js.txt", "text/plain",
                            new ByteArrayInputStream(new byte[0]));
                } catch (RepositoryException e) {
                    returnError(response, "Unable to create js.txt");
                }
            }
            if (includeCss) {
                try {
                    JcrUtils.putFile(created.adaptTo(Node.class), "css.txt", "text/plain",
                            new ByteArrayInputStream(new byte[0]));
                } catch (RepositoryException e) {
                    returnError(response, "Unable to create css.txt");
                }
            }

            resourceResolver.commit();

            response.setContentType("application/json");
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("success");
            writer.value(true);
            writer.key("successMessage");
            writer.value("Created new client library.");
            writer.endObject();

        } catch (JSONException e) {
            throw new ServletException(e);
        }

    }

    private void returnError(SlingHttpServletResponse response, String message) throws IOException, JSONException {
        response.setContentType("application/json");
        JSONWriter writer = new JSONWriter(response.getWriter());
        writer.object();
        writer.key("error");
        writer.value(true);
        writer.key("errorMessage");
        writer.value(message);
        writer.endObject();
    }

    private String[] getArrayValues(JSONObject obj, String key) throws JSONException {
        JSONArray arr = obj.getJSONArray(key);
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++) {
            values.add(arr.getJSONObject(i).getString("name"));
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.toArray(new String[0]);
    }
}
