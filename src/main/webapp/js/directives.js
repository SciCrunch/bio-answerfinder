/**
 *
 * Created by bozyurt on 11/13/15.
 */

var foundryDirectives = angular.module('foundryDirectives', []);


foundryDirectives.directive('jstree', function () {
    var linker = function (scope, element, attrs) {
        scope.$watch('sourceData', function () {
            if (!scope.sourceData || $.isEmptyObject(scope.sourceData)) {
                return;
            }
            console.log('sourceData:' + scope.sourceData);
            scope.data = scope.sourceData;
            if (scope.jst) {
                var instance = element.jstree(true);
                instance.destroy();
                scope.jst = null;
            }
            scope.jst = element.jstree({
                'core': {'data': scope.data, 'check_callback': true},
                plugins: ["contextmenu"]
            });
            scope.jst.on('select_node.jstree', function (node, selected, e) {
                var selectedNode = selected.node;

                function toJSONPath(aNode) {
                    var idx, text = aNode.text;
                    if (!text) {
                        return null;
                    }
                    idx = text.indexOf(' : ');
                    if (idx !== -1) {
                        return '"' + $.trim(text.substring(0, idx)) + '"';
                    } else {
                        if ($.isNumeric(text)) {
                            return text;
                        } else {
                            return '"' + text + '"';
                        }
                    }
                }

                var pn, jpFrag, i, idx, s = '$.', len = selectedNode.parents.length;
                var instance = element.jstree(true);
                for (i = len - 2; i >= 0; --i) {
                    pn = instance.get_node(selectedNode.parents[i]);
                    if (pn) {
                        jpFrag = toJSONPath(pn);
                        if (jpFrag) {
                            if ($.isNumeric(jpFrag)) {
                                idx = s.lastIndexOf('[]');
                                if (idx !== -1) {
                                    s = s.substring(0, idx) + '[' + jpFrag + ']' + s.substring(idx + 2);
                                }
                            } else {
                                s += jpFrag + '.';
                            }
                        }
                    }
                }
                s += toJSONPath(selectedNode);
                console.log(s);
                scope.setSelectedJsonPath(s);
            });
        });
    };
    return {
        restrict: 'A',
        link: linker
    };
});

foundryDirectives.directive('srcTree', function () {
    var linker = function (scope, element, attrs) {
        scope.$watch('sourceData', function () {
            var instance;
            if (!scope.sourceData || $.isEmptyObject(scope.sourceData)) {
                if (scope.srcJST) {
                    instance = element.jstree(true);
                    instance.destroy();
                    scope.srcJST = null;
                }
                return;
            }
            console.log('sourceData:' + scope.sourceData);
            scope.data = scope.sourceData;
            if (scope.srcJST) {
                instance = element.jstree(true);
                instance.destroy();
                scope.srcJST = null;
            }
            scope.srcJST = element.jstree({'core': {'data': scope.data}});
            scope.srcJST.on('select_node.jstree', function (node, selected, e) {
                var s = foundry.TreeModule.treeSel2JSONPathHandler($(element), selected);
                console.log(s);
                scope.selectedSrcJsonPath = s;
            });
        });
    };
    return {
        restrict: 'A',
        link: linker
    }
});

foundryDirectives.directive('destTree', function () {
    var linker = function (scope, element, attrs) {
        scope.$watch('destData', function () {
            var instance;
            if (!scope.destData || $.isEmptyObject(scope.destData)) {
                if (scope.destJST) {
                    instance = element.jstree(true);
                    instance.destroy();
                    scope.destJST = null;
                }
                return;
            }
            console.log('sourceData:' + scope.destData);
            if (scope.destJST) {
                instance = element.jstree(true);
                instance.destroy();
                scope.destJST = null;
            }
            scope.destJST = element.jstree({'core': {'data': scope.destData}});
            scope.destJST.on('loaded.jstree', function () {
                element.jstree('open_all');
            });
        });
    };
    return {
        restrict: 'A',
        link: linker
    }
});



