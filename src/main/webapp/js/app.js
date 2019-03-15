var app = angular.module('bioqaweb', ['ui.router', 'ui.bootstrap', 'smart-table',
    'bioQAControllers','ngLoadingSpinner']);

app.directive('ngIf', function() {
   return {
       link: function(scope, element, attrs) {
           if (scope.$eval(attrs.ngIf)) {
               element.replaceWith(element.children());
           } else {
               element.replaceWith(' ');
           }
       }
   }
});

app.config(['$stateProvider', '$urlRouterProvider',
    function ($stateProvider, $urlRouterProvider, $injector) {
        $urlRouterProvider.otherwise(function ($injector) {
            var $state = $injector.get('$state');
            $state.go("welcome");
        });

        $stateProvider.state('welcome', {
            url: "/",
            templateUrl: 'partials/search_panel.html',
            controller: 'QuestionController'
        }).state("answers", {
            url: '/answers',
            templateUrl: 'partials/answers_panel.html',
            controller: 'AnswerController'
        });
    }
]);




