/*
 * #%L
 * ACS AEM Tools Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.tools.clientlib_bootstrapper.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
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
import org.apache.sling.commons.mime.MimeTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;

@SuppressWarnings("serial")
@SlingServlet(resourceTypes = "acs-tools/components/clientlib-bootstrapper", selectors = "create",
        extensions = "json", methods = "POST")
public class CreateServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(CreateServlet.class);

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    @Reference
    private MimeTypeService mimeTypeService;

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
            boolean overwriteExisting = requestObject.getBoolean("overwriteExisting");

            boolean includeBoostrap = requestObject.getBoolean("includeBootstrap");

            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource parent = resourceResolver.getResource(basePath);
            if (parent == null) {
                returnError(response, String.format("The base path '%s' does not exist.", basePath));
                return;
            }

            Resource existing = parent.getChild(name);
            if (existing != null) {
                if (overwriteExisting) {
                    resourceResolver.delete(existing);
                    resourceResolver.commit();
                } else {
                    returnError(response,
                            String.format("A node named '%s' already exists under '%s'.", name, basePath));
                    return;
                }
            }

            Resource created = resourceResolver.create(parent, name, properties);

            StringBuilder jsTxt = new StringBuilder();
            StringBuilder cssTxt = new StringBuilder();

            if (includeBoostrap) {
                addBootstrap(created, requestObject, jsTxt, cssTxt);
            }

            if (includeJs) {
                try {
                    JcrUtils.putFile(created.adaptTo(Node.class), "js.txt", "text/plain",
                            new ByteArrayInputStream(jsTxt.toString().getBytes()));
                } catch (RepositoryException e) {
                    returnError(response, "Unable to create js.txt");
                }
            }
            if (includeCss) {
                try {
                    JcrUtils.putFile(created.adaptTo(Node.class), "css.txt", "text/plain",
                            new ByteArrayInputStream(cssTxt.toString().getBytes()));
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
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }

    }

    private void
            addBootstrap(Resource created, JSONObject requestObject, StringBuilder jsTxt, StringBuilder cssTxt)
                    throws JSONException, HttpException, IOException, ServletException, RepositoryException {
        String version = requestObject.getString("bootstrapVersion");
        if (StringUtils.isNotBlank(version)) {
            GetMethod getMethod = new GetMethod(version);
            new HttpClient().executeMethod(getMethod);
            Node createdNode = created.adaptTo(Node.class);
            Map<String, Node> paths = unzipContents(createdNode, getMethod.getResponseBodyAsStream());

            String basePath = paths.keySet().iterator().next();

            List<String> jsPaths = getJSPaths(paths.keySet(), basePath);
            for (String jsPath : jsPaths) {
                jsTxt.append(jsPath).append("\n");
            }

            Node variablesNode = paths.get(basePath + "less/variables.less");
            InputStream variablesFile = JcrUtils.readFile(variablesNode);
            List<String> lines = IOUtils.readLines(variablesFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("@icon-font-path")) {
                    line = line.replace("../", basePath);
                    lines.set(i, line);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.writeLines(lines, "\n", baos);
            Binary binary = variablesNode.getSession().getValueFactory()
                    .createBinary(new ByteArrayInputStream(baos.toByteArray()));
            variablesNode.getNode(JcrConstants.JCR_CONTENT).setProperty(JcrConstants.JCR_DATA, binary);

            StringBuilder customFile = new StringBuilder();
            customFile.append("//add custom LESS code here which has access to Bootstrap mixins\n");
            putFile(createdNode, "bootstrap-custom.less", "text/css", customFile);

            String wrapperClass = requestObject.getString("bootstrapWrapperClass");

            StringBuilder wrapperFile = new StringBuilder();
            if (StringUtils.isNotBlank(wrapperClass)) {
                if (!wrapperClass.startsWith(".")) {
                    wrapperFile.append(".");
                }
                wrapperFile.append(wrapperClass).append("{\n");
            }

            wrapperFile.append("@import \"").append(basePath + "less/bootstrap.less").append("\";").append("\n");
            wrapperFile.append("@import \"").append("bootstrap-custom.less").append("\";").append("\n");

            if (StringUtils.isNotBlank(wrapperClass)) {
                wrapperFile.append("}").append("\n");
            }

            putFile(createdNode, "wrapper.less", "text/css", wrapperFile);

            cssTxt.append("wrapper.less");

            log.info("added " + paths);
        }
    }

    private static final Node putFile(Node node, String name, String mimeType, CharSequence content)
            throws RepositoryException {
        return JcrUtils.putFile(node, name, mimeType, new ByteArrayInputStream(content.toString().getBytes()));
    }

    private List<String> getJSPaths(final Collection<String> paths, final String basePath) {
        List<String> result = new ArrayList<String>();
        final String jsPath = basePath + "js/";
        for (String path : paths) {
            if (path.startsWith(jsPath) && path.endsWith(".js") && !path.contains("test")) {
                result.add(path);
            }
        }
        return result;
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

    private Map<String, Node> unzipContents(final Node node, InputStream zip) throws IOException, ServletException {
        Map<String, Node> result = new LinkedHashMap<String, Node>();
        ZipInputStream zipStream = new ZipInputStream(zip);
        try {
            final String folderNodeType = "nt:folder";
            final Session session = node.getSession();

            ZipEntry entry = zipStream.getNextEntry();
            while (entry != null) {
                String path = entry.getName();
                // do some path cleanup which really shouldn't be necessary
                path = path.replace('\\', '/');
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                // find the last path segment; this will be the file name
                // everything before it is the intermediate path
                int idx = path.lastIndexOf('/');
                final Node parent;
                final String fileName;
                if (idx == -1) {
                    parent = node;
                    fileName = path;
                } else {
                    final String parentPath = node.getPath() + "/" + path.substring(0, idx);
                    parent = JcrUtil.createPath(parentPath, folderNodeType, session);
                    fileName = path.substring(idx + 1);
                }

                Node fileNode;
                if (!fileName.equals("")) {

                    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[2048];

                    int size;
                    while ((size = zipStream.read(buffer, 0, buffer.length)) != -1) {
                        bytes.write(buffer, 0, size);
                    }

                    String mimeType = getMimeType(fileName);

                    if (entry.getTime() > 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(entry.getTime());
                        fileNode = JcrUtils.putFile(parent, fileName, mimeType,
                                new ByteArrayInputStream(bytes.toByteArray()), cal);
                    } else {
                        fileNode = JcrUtils.putFile(parent, fileName, mimeType,
                                new ByteArrayInputStream(bytes.toByteArray()));
                    }

                } else {
                    fileNode = parent;
                }
                result.put(path, fileNode);
                entry = zipStream.getNextEntry();
            }
        } catch (RepositoryException e) {
            throw new ServletException("Unable to import zip file", e);
        } finally {
            zipStream.close();
        }
        return result;
    }

    private String getMimeType(final String fileName) {
        if (fileName.endsWith(".less")) {
            return "text/css";
        }
        String mimeType = mimeTypeService.getMimeType(fileName);
        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        return mimeType;
    }
}
