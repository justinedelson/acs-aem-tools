/*
 * #%L
 * ACS AEM Tools Package
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

/*global angular: false */

var clientlibBoostrapper = angular.module('clientlibBoostrapper', []);


clientlibBoostrapper.controller('MainCtrl', function($scope, $http) {
    $scope.data = {};
    $scope.data.basePath = '';
    $scope.data.name = '';
    $scope.data.overwriteExisting = false;
    $scope.data.dependencies = [];
    $scope.data.embeds = [];
    $scope.data.categories = [];
    $scope.data.includeJs = true;
    $scope.data.includeCss = true;
    $scope.data.includeBootstrap = false;
    $scope.data.bootstrapVersion = '';
    $scope.data.bootstrapWrapperClass = '';
    $scope.data.includeSemanticGrid = false;

    $scope.running = false;
    $scope.error = false;
    $scope.errorMessage = '';

    $scope.success = false;
    $scope.successMessage = '';

    $scope.createLibrary = function() {
        $scope.data.basePath = $("[name='basePath']").val();

        $scope.error = false;
        $scope.success = false;

        $scope.running = true;

        $http.post($('body').data("post-url"), $scope.data).success(
            function(data) {
            if (data.success) {
                $scope.success = true;
                $scope.successMessage = data.successMessage;
            } else {
                $scope.error = true;
                $scope.errorMessage = data.errorMessage;
            }
            $scope.running = false;
        }).error(
            function(data) {
                $scope.error = true;
                $scope.errorMessage = "Unknown error";
                $scope.running = false;
        });
        
    };
});