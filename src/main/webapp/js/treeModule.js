var foundry = foundry || {};

foundry.TreeModule = function () {
    function treeSel2JSONPathHandler(element, selected) {
        var selectedNode = selected.node;

        function toJSONPath(aNode) {
            var idx, text = aNode.text;
            if (!text) {
                return null;
            }
            idx = -1;
            if (!$.isNumeric(text)) {
                idx = text.indexOf(' : ');
            }
            if (idx !== -1) {
                return "'" + $.trim(text.substring(0, idx)) + "'";
            } else {
                if ($.isNumeric(text)) {
                    return text;
                } else {
                    return "'" + text + "'";
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
        return s;
    }

    function extractName(str) {
        var idx, idx2;
        if (!str) {
            console.log(str);
        }
        idx = str.indexOf(' : ');
        if (idx != -1) {
            return str.substring(0, idx);
        } else {
            idx = str.indexOf('[]');
            if (idx != -1 && (idx + 2) == str.length) {
                return str.substring(0, idx);
            }
        }
        idx = str.indexOf('\'');
        if (idx != -1) {
            idx2 = str.lastIndexOf('[');
            if (idx2 == -1) {
                return str.substring(idx + 1, str.length - 1);
            } else {
                return str.substring(idx + 1, idx2 - 1);
            }
        }
        return str;
    }

    function extractIndex(str) {
        var numStr, idx = str.lastIndexOf('[');
        if (idx != -1) {
            numStr = $.trim(str.substring(idx + 1, str.length - 1));
            return parseInt(numStr);
        }
        return -1;
    }

    function findMatchingNode(name, parentNode, jstInst, matchList) {
        var i, child;
        for (i = 0; i < parentNode.children.length; i++) {
            child = jstInst.get_node(parentNode.children[i]);
            if (extractName(child.text) == name) {
                matchList.push(child);
                return;
            } else {
                findMatchingNode(name, child, jstInst, matchList);
            }
        }
    }

    function satisfiedEq(refValue, value) {
        return refValue && value && refValue === value;
    }

    function parsePredicate(predicateStr) {
        var m, re = /\?\(@\.'?([^'\s]+)'?\s*([=><]+)\s*'([^']+)'\)/;
        if ((m = re.exec(predicateStr)) !== null) {
            if (m[2] !== '=') {
                return null;
            }
            var refValue = m[3];
            return {
                name: m[1], satisfied: function (value) {
                    return satisfiedEq(refValue, value);
                }
            };
        }
        return null;
    }

    function checkPredicate(parentNode, jstInst, predicateInfo) {
        var k, childNode, nodeName, value, idx;
        for (k = 0; k < parentNode.children.length; k++) {
            childNode = jstInst.get_node(parentNode.children[k]);
            nodeName = extractName(childNode.text);
            if (nodeName === predicateInfo.name) {
                idx = childNode.text.indexOf(' : ');
                value = $.trim(childNode.text.substr(idx + 3));
                return predicateInfo.satisfied(value);
            }
        }
        return false;
    }

    function tokenizeJsonPath(jsonPath) {
        var chars, c, i = 0, len, buf = '', parts = [];
        if (jsonPath.indexOf('[?(@.') != -1) {
            chars = jsonPath.split('');
            len = chars.length;
            for (i = 0; i < len; i++) {
                c = chars[i];
                if (c === '.') {
                    if (i > 4 && chars[i - 1] === '@' && chars[i - 2] === '(' && chars[i - 3] === '?' && chars[i - 4] === '[') {
                        buf += c;
                    } else {
                        parts.push(buf);
                        buf = '';
                    }
                } else {
                    buf += c;
                }
            }
            if (buf.length > 0) {
                parts.push(buf);
            }
            return parts;
        }
        return jsonPath.split('.');
    }

    function handlePredicate(parentNode /* nodes[j]*/, jstInst, predicateInfo, filtered) {
        var k, i, arrNode, aNode;
        for (k = 0; k < parentNode.children.length; k++) {
            arrNode = jstInst.get_node(parentNode.children[k]);
            if ($.isNumeric(arrNode.text)) {
                if (arrNode.children.length > 0) {
                    if (checkPredicate(arrNode, jstInst, predicateInfo)) {
                        for (i = 0; i < arrNode.children.length; i++) {
                            aNode = jstInst.get_node(arrNode.children[i]);
                            filtered.push(aNode);
                        }
                    }
                } else {
                    if (checkPredicate(arrNode, jstInst, predicateInfo)) {
                        filtered.push(jstInst.get_node(arrNode));
                    }
                }
            } else {
                if (checkPredicate(arrNode, jstInst, predicateInfo)) {
                    filtered.push(jstInst.get_node(arrNode));
                }
            }
        }
    }

    function jsonPath2TreeNodeSelector(jsonPath, instance) {
        var jstInst = instance.jstree(true);
        instance.jstree("deselect_all");
        var nodes = [], filtered, rootNode = jstInst.get_node('ul > li:first');
        console.log('rootNode:' + rootNode);
        console.dir(rootNode);
        var nodeName, name, arrNode, aNode;
        var arrIdx, wildcard, isArray, child;
        var i, j, k, parts, len, jpWild = false, matchList = [], predicateInfo = null;
        if (jsonPath.match(/^\$\.\./)) {
            jsonPath = jsonPath.substr(3);
            jpWild = true;
        } else {
            if (!jsonPath.match(/^\$\.[^\.]+/)) {
                return;
            }
            jsonPath = jsonPath.substr(2);
        }
        parts = tokenizeJsonPath(jsonPath);
        len = parts.length;

        name = extractName(parts[0]);
        if (jpWild) {
            if (extractName(rootNode.text) != name) {
                findMatchingNode(name, rootNode, jstInst, matchList);
            }
            if (matchList.length > 0) {
                nodes[0] = matchList[0];
            }
        } else {
            if (extractName(rootNode.text) != name) {
                for (i = 0; i < rootNode.children.length; i++) {
                    child = jstInst.get_node(rootNode.children[i]);
                    if (extractName(child.text) == name) {
                        rootNode = child;
                        break;
                    }
                }
            }
            nodes[0] = rootNode;
        }
        i = 0;

        while (i < len) {
            isArray = !!parts[i].match(/\]$/);
            arrIdx = -1;
            wildcard = false;
            if (isArray) {
                wildcard = !!parts[i].match(/\[\*\]$/);
                if (!wildcard) {
                    if (parts[i].indexOf('[?(@.') != -1) {
                        predicateInfo = parsePredicate(parts[i]);
                    } else {
                        arrIdx = extractIndex(parts[i]);
                    }
                }
            }

            name = extractName(parts[i]);
            filtered = [];
            for (j = 0; j < nodes.length; j++) {
                nodeName = extractName(nodes[j].text);
                if (name == nodeName) {
                    if (nodes[j].children && nodes[j].children.length > 0) {
                        if (isArray) {
                            if (wildcard) {
                                if (nodes[j].children.length === 1) {
                                    filtered.push(jstInst.get_node(nodes[j].children[0]));
                                } else {
                                    for (k = 0; k < nodes[j].children.length; k++) {
                                        arrNode = jstInst.get_node(nodes[j].children[k]);
                                        if ($.isNumeric(arrNode.text)) {
                                            if (arrNode.children.length > 0) {
                                                filtered.push(jstInst.get_node(arrNode.children[0]));
                                            } else {
                                                filtered.push(jstInst.get_node(arrNode));
                                            }
                                        } else {
                                            filtered.push(jstInst.get_node(arrNode));
                                        }
                                    }
                                }
                            } else if (predicateInfo !== null) {
                                handlePredicate(nodes[j], jstInst, predicateInfo, filtered);
                            }
                        } else {
                            for (k = 0; k < nodes[j].children.length; k++) {
                                filtered.push(jstInst.get_node(nodes[j].children[k]));
                            }
                        }
                    } else if ((i + 1) <= len) {
                        filtered.push(nodes[j]);
                    }
                }
            }
            if (filtered) {
                nodes = filtered;
            } else {
                nodes = [];
                break;
            }
            i++;
        } // while
        console.log("selected");
        console.dir(nodes);
        if (nodes && nodes.length > 0) {
            for (i = 0; i < nodes.length; i++) {
                jstInst.select_node(nodes[i]);
            }
        }
    };

    return {
        treeSel2JSONPathHandler: treeSel2JSONPathHandler,
        jsonPath2TreeNodeSelector: jsonPath2TreeNodeSelector
    };
}();
