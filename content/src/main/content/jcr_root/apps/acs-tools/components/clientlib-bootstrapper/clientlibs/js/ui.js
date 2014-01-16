$(function(){
    var loader = function(path, callback) {
        $.get(path + ".pages.json", {
            predicate: "hierarchyNotFile"
        },
        function(data) {
            var pages = data.pages;
            var result = [];
            for(var i = 0; i < pages.length; i++) {
                result.push(pages[i].label);
            }
            if (callback) callback(result);
        }, "json");
        return false;
    };
    
    var select = $("input[name='basePath']");
    select.pathBrowser({
        rootPath: "/",
        optionLoader: loader
    });
    
});