var bioQACtrls = angular.module('bioQAControllers', ['bioqaServices', 'ui.bootstrap']);


bioQACtrls.controller('QuestionController', ['$scope', 'dataService',
    function ($scope, dataService) {
        $scope.answerSentences = [];
        $scope._do_rerank = true;
        $scope._resultSizes = ["20","50","100","all"];
        $scope._resultSize = $scope._resultSizes[1];
        $scope.ask = function () {
            var question = $scope._question;
            var doRerank = $scope._do_rerank;
            var resultSize = $scope._resultSize;
            console.log("resultSize:" + resultSize + " doRerank:" + doRerank);
            dataService.ask(question, resultSize, doRerank).then(function (response) {
                    $scope.answerSentences = response.data.answerSentences;
                    $scope.question = question;
                    $scope._question = '';
                    console.dir($scope.answerSentences)
                },
                function (response) {
                    console.dir(response);
                });
        };
    }
]);

bioQACtrls.controller('AnswerController', ['$scope', '$state', '$stateParams','dataService',
    function ($scope, $state, $stateParams, dataService) {

    }
]);