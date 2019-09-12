var bioqaServices = angular.module('bioqaServices', []);

app.factory("transformRequestAsFormPost", function () {
    function transformRequest(data, getHeaders) {
        return ( serializeData(data) );
    }

    function serializeData(data) {
        if (!angular.isObject(data)) {
            return ( ( data == null ) ? "" : data.toString() );
        }
        var buffer = [];
        // Serialize each key in the object.
        for (var name in data) {
            if (!data.hasOwnProperty(name)) {
                continue;
            }
            var value = data[name];
            buffer.push(encodeURIComponent(name) + "=" +
                encodeURIComponent(( value == null ) ? "" : value)
            );
        }
        // Serialize the buffer and clean it up for transportation.
        var source = buffer.join("&").replace(/%20/g, "+");
        // console.log('source:' + source);
        return source;
    }

    return transformRequest;
});

bioqaServices.service('dataService', ['$rootScope', '$http', 'transformRequestAsFormPost',
    function ($rootScope, $http, transformRequestAsFormPost) {
        this.ask = function (question, resultSize, doRerank) {
            return $http({
                    method: 'post', url: '/bio-answerfinder/api/bioqa/ask',
                    transformRequest: transformRequestAsFormPost,
                    data: {
                        query: question,
                        resultSize: resultSize,
                        useReranking: doRerank
                    }, headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                }
            );
        };
    }
]);

