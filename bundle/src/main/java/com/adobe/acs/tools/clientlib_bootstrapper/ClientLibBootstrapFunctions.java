package com.adobe.acs.tools.clientlib_bootstrapper;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.jsp.PageContext;

import org.apache.sling.api.scripting.SlingBindings;

import tldgen.Function;

import com.day.cq.widget.ClientLibrary;
import com.day.cq.widget.HtmlLibraryManager;

public class ClientLibBootstrapFunctions {

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
