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
package com.adobe.acs.tools.clientlib_bootstrapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.jsp.PageContext;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;

import tldgen.Function;

import com.day.cq.widget.ClientLibrary;
import com.day.cq.widget.HtmlLibraryManager;

public class ClientLibBootstrapFunctions {
    
    @Function
    public static Map<String, String> getBootstrapVersions() throws Exception {
        Map<String, String> map = new LinkedHashMap<String, String>();
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("https://api.github.com/repos/twbs/bootstrap/releases");
        httpClient.executeMethod(get);
        JSONArray result = new JSONArray(get.getResponseBodyAsString());
        for (int i = 0; i < result.length(); i++) {
            JSONObject release = result.getJSONObject(i);
            if (!release.getBoolean("draft")) {
                map.put(release.getString("name"), release.getString("zipball_url"));
            }
        }
        return map;
    }

    @Function
    public static String[] getClientLibraryCategories(PageContext pageContext) {
        if (pageContext == null) {
            return new String[0];
        }

        final SlingBindings bindings = (SlingBindings) pageContext.getRequest().getAttribute(
                SlingBindings.class.getName());
        if (bindings == null) {
            return new String[0];
        }

        HtmlLibraryManager libraryManager = bindings.getSling().getService(HtmlLibraryManager.class);

        Set<String> result = new TreeSet<String>();

        Map<String, ClientLibrary> libraries = libraryManager.getLibraries();
        for (ClientLibrary library : libraries.values()) {
            String[] libraryCats = library.getCategories();
            if (libraryCats != null) {
                for (String cat : libraryCats) {
                    result.add(cat);
                }
            }
        }

        return result.toArray(new String[0]);
    }

}
