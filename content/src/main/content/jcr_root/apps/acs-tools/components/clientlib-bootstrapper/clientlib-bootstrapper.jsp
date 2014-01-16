<%--
  #%L
  ACS AEM Tools Package
  %%
  Copyright (C) 2013 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@include file="/libs/foundation/global.jsp"%><%
%><%@taglib prefix="cb" uri="http://www.adobe.com/consulting/acs-aem-tools/clientlib-bootstrapper" %><%
%><!doctype html>
<html ng-app="clientlibBoostrapper">
<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

<title>Client Library Boostrapper | ACS AEM Tools</title>

<cq:includeClientLib css="clientlib-bootstrapper.app" />
</head>

<body ng-controller="MainCtrl"
    data-clientlib-list-url="<%= resource.getPath() %>.clientlib-list.json"
    data-post-url="<%= resource.getPath() %>.create.json">
    <header class="top">
    
        <div class="logo">
            <span ng-hide="running"><a href="/"><i class="icon-marketingcloud medium"></i></a></span>
            <span ng-show="running"><span class="spinner"></span></span>
        </div>
    
        <nav class="crumbs">
            <a href="/miscadmin">Tools</a>
            <a href="<%= currentPage.getPath() %>.html">JSP Code Display</a>
        </nav>
    
    </header>

    <div class="page" role="main">

        <div class="content">

            <form class="vertical">
                <section class="fieldset">
                    <h1>Client Library Bootstrapper</h1>
                    <div>Using this tool, you can easily create a CQ Client Library folder.</div>
                    <br/>

                    <div class="alert error" ng-show="error">
                        <button class="close" data-dismiss="alert">&times;</button>
                        <strong>ERROR</strong><div>{{errorMessage}}</div>
                    </div>

                    <div class="alert success" ng-show="success">
                        <button class="close" data-dismiss="alert">&times;</button>
                        <strong>Success</strong><div>{{successMessage}}</div>
                    </div>

                    <label class="fieldlabel" for="basePath">Provide the base path for your client library:</label>
                    <input type="text" name="basePath"><br/>

                    <label class="fieldlabel" for="name">Library Name:</label>
                    <input class="field" type="text" name="name" ng-model="data.name"><br/>

                    <label class="fieldlabel" for="categories">Categories:</label>
                    <ul name="categories">
                        <li ng-repeat="category in data.categories">
                            <label>Name:</label> <input ng-model="category.name" type="text"/>
                            <a ng-click="data.categories.splice($index, 1)"><i class="icon-delete">Delete</i></a>
                        </li>
                        <li>
                            <a ng-click="data.categories.push({})"><i class="icon-add">Add</i></a>
                        </li>
                    </ul>

                    <label class="fieldlabel" for="dependencies">Dependencies:</label>
                    <ul name="dependencies">
                        <li ng-repeat="library in data.dependencies">
                            <label>Name:</label> <select ng-model="library.name">
                                    <c:forEach var="lib" items="${cb:getClientLibraryCategories(pageContext)}">
                                    <option>${lib}</option>
                                    </c:forEach>
                                 </select>
                            <a ng-click="data.dependencies.splice($index, 1)"><i class="icon-delete">Delete</i></a>
                        </li>
                        <li>
                            <a ng-click="data.dependencies.push({})"><i class="icon-add">Add</i></a>
                        </li>
                    </ul>

                    <label class="fieldlabel" for="embeds">Embeds:</label>
                    <ul name="embeds">
                        <li ng-repeat="library in data.embeds">
                            <label>Name:</label> <select ng-model="library.name">
                                    <c:forEach var="lib" items="${cb:getClientLibraryCategories(pageContext)}">
                                    <option>${lib}</option>
                                    </c:forEach>
                                 </select>
                            <a ng-click="data.embeds.splice($index, 1)"><i class="icon-delete">Delete</i></a>
                        </li>
                        <li>
                            <a ng-click="data.embeds.push({})"><i class="icon-add">Add</i></a>
                        </li>
                    </ul>
                    
                    <label><input type="checkbox" name="includeJs" ng-model="data.includeJs" /><span>Includes JS?<span></span></label>
                    
                    <label><input type="checkbox" name="includeCss" ng-model="data.includeCss" /><span>Includes CSS?</span></label>

                    <button class="primary" ng-click="createLibrary()">Go</button>
                </section>
            </form>

        </div>
    </div>

    <cq:includeClientLib js="jquery,jquery-ui,clientlib-bootstrapper.app" />
      
</body>
</html>