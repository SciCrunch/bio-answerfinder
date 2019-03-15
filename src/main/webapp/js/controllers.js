var bioQACtrls = angular.module('bioQAControllers', ['bioqaServices', 'ui.bootstrap']);


bioQACtrls.controller('QuestionController', ['$scope', 'dataService',
    function ($scope, dataService) {
        $scope.answerSentences = [];
        $scope.ask = function () {
            var question = $scope._question;
            dataService.ask(question).then(function (response) {
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