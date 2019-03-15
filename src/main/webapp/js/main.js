/**
 * Created by bozyurt on 8/26/15.
 * requires jquery
 */


var foundry = foundry || {};

foundry.uiModule = function () {
    this.icConfigs = [];
    var that = this;
    that.opMode = 'none';

    function startup() {
        $('#loginBut').on('click', function (evt) {
            evt.preventDefault();
            var user, pwd;
            user = $.trim($('#loginId').val());
            pwd = $.trim($('#password').val());
            $.post("/api/user/login", {user: user, pwd: pwd},
                function (data) {
                    if (data.error) {
                        alert(data.error);
                    } else {
                        that.apiKey = data.apiKey;
                        $('#loginPanel').hide();
                        $('#mainNavBar').show();
                        $('#navTabs').show();
                        $('#tabContent').show();
                        initializeTabs();
                    }
                }
            )
        });
        $('#logoutBut').on('click', function (evt) {
            evt.preventDefault();
            that.apiKey = null;
            $('#loginPanel').show();
            $('#mainNavBar').hide();
            $('#navTabs').hide();
            $('#tabContent').hide();
        });
    }

    function initializeTabs() {
        $('#siTabLink').on('click', function (evt) {
            evt.preventDefault();
            $('#transformations').removeClass('active');
            $('#sourceIngestion').addClass('active');
            $('#trTabLink').closest('li').removeClass('active');
            $(this).closest('li').addClass('active');
            prepSourceIngestTab();
        }).trigger('click');
        $('#trTabLink').on('click', function (evt) {
            evt.preventDefault();
            $('#sourceIngestion').removeClass('active');
            $('#transformations').addClass('active');
            $('#siTabLink').closest('li').removeClass('active');
            $(this).closest('li').addClass('active');
            prepTrTab();
        });
    }

    function populateSourceForm(source) {
        var template$ = $('#paramTemplate'), container$ = $('#paramsPanel');
        $('#sourceID').val(source.sourceID);
        $('#sourceName').val(source.name);
        $('#dataSource').val(source.dataSource);

        var selIC = $.grep(that.icConfigs, function (e) {
            return e.name === source.type;
        });
        if (selIC) {
            console.log('selIC.name:' + selIC[0].name);
            $('#ingestorTypeCB').val(selIC[0].name).change();
            // prepareParamsPanel(selIC[0].params, template$, container$);
        }
        $('input:text', container$).each(function () {
            var fieldName = $(this).attr('id');
            if (source.params[fieldName]) {
                var value = source.params[fieldName];
                console.log(fieldName + ":" + value);
                $(this).val(value);
            }
        });
    }

    function cleanSourceDescPanel() {
        $('#sourceID').val('');
        $('#sourceName').val('');
        $('#dataSource').val('');
        var firstVal = $('#ingestorTypeCB option').eq(0).val();
        $('#ingestorTypeCB').val(firstVal).change();
        // console.log("firstVal:" + firstVal);
    }

    function prepareParamsPanel(params, template$, container$) {
        var paramCont$, i, fileTypeField$ = null;
        $('div', container$).remove();
        for (i = 0; i < params.length; i++) {
            if (params[i].choices) {
                paramCont$ = $('#selectParamTemplate1').contents().clone();
                $('label', paramCont$).text(params[i].name);
                $('select', paramCont$).attr('id', params[i].name);
                foundry.populateSelect($('select', paramCont$)[0], params[i].choices, function (o) {
                    return {name: o, value: o};
                });
                if (params[i].desc) {
                    $('select', paramCont$).attr('title', params[i].desc);
                }
                paramCont$.appendTo(container$);
                if (params[i].name == 'fileType') {
                   fileTypeField$ = $('select',paramCont$);
                }
            } else {
                paramCont$ = template$.contents().clone();
                $('label', paramCont$).text(params[i].name);
                $('input', paramCont$).attr('id', params[i].name);
                if (params[i].default) {
                    $('input', paramCont$).val(params[i].default);
                }
                if (params[i].desc) {
                    $('input', paramCont$).attr('title', params[i].desc);
                }
                if (params[i].required) {
                    $('input', paramCont$).addClass("validate[required]");
                }
                if (params[i].fileType) {
                    $('input', paramCont$).data('fileType', params[i].fileType);
                }
                paramCont$.appendTo(container$);
            }
        }
        if (fileTypeField$) {
            fileTypeField$.on('change', function(evt) {
                var newFileType = $(':selected', this).val();
                $('input', container$).each(function() {
                    var fileType = $(this).data('fileType');
                    if (fileType) {
                        if (fileType === newFileType) {
                            $(this).addClass("validate[required]");
                        } else {
                            $(this).removeClass("validate[required]");
                        }
                    }
                })
            }).trigger('change');
        }
    }

    function prepControlPanel() {
        $.getJSON("/api/sources", function (data) {
            that.sources = data;
            var optGenerator = function (o) {
                return {name: o.sourceID + '(' + o.name + ')', value: o.id};
            };
            foundry.populateSelect($('#siSourceCB')[0], data, optGenerator);

            $('#updateSourceBut').on('click', function (evt) {
                var selIdx = $(':selected', $('#siSourceCB')).val();
                var selSource = that.sources[selIdx];
                $.getJSON("/api/sources/source",
                    {
                        sourceID: selSource.sourceID,
                        dataSource: selSource.dataSource
                    },
                    function (data) {
                        $('#sdForm').show();
                        populateSourceForm(data);
                        that.opMode = 'update';
                    });
            });
            $('#newSourceBut').on('click', function (evt) {
                cleanSourceDescPanel();
                $('#sdForm').show();
                that.opMode = 'new';
            });
        });
    }

    function prepare(cbEl$, template$, container$) {
        $('#sdForm').hide();
        $.getJSON("/api/harvest", function (data) {
            that.icConfigs = data;
            var optGenerator = function (o) {
                return {name: o.name, value: o.name};
            };
            foundry.populateSelect(cbEl$[0], data, optGenerator);
            cbEl$.on('change', function (evt) {
                var selICName = $(':selected', this).val();
                var selIC = $.grep(that.icConfigs, function (e) {
                    return e.name === selICName;
                });
                prepareParamsPanel(selIC[0].params, template$, container$);
            }).trigger('change');
        });
        $('#saveSourceBut').on('click', function (evt) {
            evt.preventDefault();
            var payload = {};
            payload.opMode = that.opMode;
            payload.sourceID = $.trim($('#sourceID').val());
            payload.sourceName = $.trim($('#sourceName').val());
            payload.dataSource = $.trim($('#dataSource').val());
            payload.ingestMethod = $(':selected', cbEl$).val();
            payload.params = {};
            $('input:text', $('#paramsPanel')).each(function () {
                var fieldName = $(this).attr('id');
                payload.params[fieldName] = $(this).val();
            });
            $.post('/api/sources', {payload: JSON.stringify(payload)}, function (data, status) {
                alert("Saved source description");
            });

        });
        $('#sdForm').validationEngine();

    }

    function prepJSTree(that, treeData) {
        if (that.jst) {
            var instance = $('#docTreePanel').jstree(true);
            instance.destroy();
            that.jst = null;
        }
        if (treeData) {
            that.jst = $('#docTreePanel').jstree({
                'core': {'data': treeData}
            });
        } else {
            that.jst = $('#docTreePanel').jstree({
                'core': {
                    'data': {
                        "url": "./root.json",
                        "dataType": "json"
                    }
                }
            });
        }

        that.jst.on('select_node.jstree', function (node, selected, e) {
            var selectedNode = selected.node;

            function toJSONPath(aNode) {
                var idx, text = aNode.text;
                if (!text) {
                    return null;
                }
                idx = text.lastIndexOf(':');
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
            var instance = $('#docTreePanel').jstree(true);
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
            var session = that.trsEditor.getSession();
            session.insert({row: session.getLength(), column: 0}, "\n" + s);
        });

    }

    function prepSourceIngestTab() {
        $('#transformation').hide();
        $('#sourceIngestion').show();
        prepare($('#ingestorTypeCB'), $('#paramTemplate'), $('#paramsPanel'));
        prepControlPanel();
    }

    function prepTrTab() {
        $('#sourceIngestion').hide();
        $('#mainPanel').hide();
        $('#transformation').show();
        var that = this;
        $('#pkFieldAddBut').on('click', function (evt) {
            evt.preventDefault();
            var maxIdx = -1, i, cont$;
            $('.pk-path', $('#pkPanel')).each(function () {
                var id = $('input', $(this)).attr('id').substring(6);
                if (id > maxIdx) {
                    maxIdx = id;
                }
            });
            console.log("maxIdx:" + maxIdx);
            i = maxIdx < 0 ? 1 : maxIdx + 1;
            cont$ = $('#pkElemTemplate').contents().clone();
            $('input', cont$).attr('id', 'pkLoc_' + i);
            $('#pkPanel').append(cont$);
        });

        function prepPKPanel(primaryKeyJSONPath) {
            var i, cont$;
            $('.pk-path', $('#pkPanel')).remove();
            $('#pkLocInput').val(primaryKeyJSONPath[0]);
            if (primaryKeyJSONPath.length > 1) {
                for (i = 1; i < primaryKeyJSONPath.length; i++) {
                    cont$ = $('#pkElemTemplate').contents().clone();
                    $('input', cont$).attr('id', 'pkLoc_' + i).val(primaryKeyJSONPath[i]);
                    $('#pkPanel').append(cont$);
                }
                $('#pkPanel').on('click', 'a.pk-del', function (evt) {
                    evt.preventDefault();
                    $(this).closest('div.pk-path').remove();
                });
            }
        }


        $.getJSON("/api/sources", function (data) {
            that.sources = data;
            if (that.jst) {
                $('#docTreePanel').jstree(true).destroy();
                that.jst = null;
            }
            var optGenerator = function (o) {
                return {name: o.sourceID + '(' + o.name + ')', value: o.id};
            };
            foundry.populateSelect($('#sourceCB')[0], data, optGenerator);

            $('#sampleDataBut').on('click', function (evt) {
                if (that.jst) {
                    $('#docTreePanel').jstree(true).destroy();
                    that.jst = null;
                }
                var selIdx = $(':selected', $('#sourceCB')).val();
                that.selSource = that.sources[selIdx];
                $('#mainPanel').show();
                console.dir(that.selSource);
                if (!that.trsEditor) {
                    that.trsEditor = ace.edit('trsPanel');
                    // that.trsEditor.getSession().setMode("ace/mode/javascript");
                    that.trsEditor.resize();
                }
                if (that.trsEditor) {
                    if (that.selSource.transformScript) {
                        that.trsEditor.setValue(that.selSource.transformScript);
                    } else {
                        that.trsEditor.setValue("/* transformation script goes here */");
                    }
                }
                if (that.selSource.primaryKeyJSONPath) {
                    prepPKPanel(that.selSource.primaryKeyJSONPath);
//                    $('#pkLocInput').val(that.selSource.primaryKeyJSONPath[0]);
                }
                $.get("/api/sources/sample", {
                        sourceId: that.selSource.sourceID,
                        dataSource: that.selSource.dataSource
                    },
                    function (data, status) {
                        prepJSTree(that, data.sampleTree);
                    });
            });
            $('#testTrScriptBut').on('click', function (evt) {
                evt.preventDefault();
                $.post('/api/sources/testTransform',
                    {
                        sourceId: that.selSource.sourceID,
                        dataSource: that.selSource.dataSource,
                        transformScript: that.trsEditor.getValue()
                    }, function (data, status) {
                        console.log(data);
                        $('#trResultPanel').show();
                        if (!that.trResultEditor) {
                            that.trResultEditor = ace.edit('trResultPanel');
                            that.trResultEditor.getSession().setMode("ace/mode/json");
                            that.trResultEditor.resize();
                            that.trResultEditor.setReadOnly(true);
                        }
                        that.trResultEditor.setValue(data);
                    }, 'text');
            });
            $('#updateResourceBut').on('click', function (evt) {
                evt.preventDefault();
                var trScriptContent = $.trim(that.trsEditor.getValue());
                var v, payload = {};
                payload.sourceID = that.selSource.sourceID;
                payload.dataSource = that.selSource.dataSource;
                payload.transformScript = trScriptContent;
                v = $.trim($('#pkLocInput').val());
                payload.pkJsonPath = [];

                if (!payload.transformScript || !v) {
                    alert("Both the transformation script and primary key json path are required!");
                    return;
                }
                payload.pkJsonPath.push(v);
                $('.pk-path', $('#pkPanel')).each(function () {
                    var idx = parseInt($('input', $(this)).attr('id').substring(6));
                    payload.pkJsonPath[idx] = $.trim($('input', $(this)).val());
                });
                $.post('/api/sources/update',
                    {
                        payload: JSON.stringify(payload)
                    }, function (data, status) {
                        alert("Both transformation script and primary key definition are successfully saved!");
                    }, 'text');
            });
        });
    }

    return {
        prepSourceIngestTab: prepSourceIngestTab,
        prepTrTab: prepTrTab,
        startup: startup
    };
}();

foundry.populateSelect = function (selectEl, dataArr, optionGenFun) {
    $('option', selectEl).remove();
    if (selectEl.options === null || selectEl.options.length === 0) {
        var selIdx = 0;
        for (var i = 0; i < dataArr.length; i++) {
            var data = dataArr[i];
            var od = optionGenFun(data);
            var option = new Option(od.name, od.value);
            if (od.data) {
                jQuery(option).data('ctx', od.data);
            }
            if (i === 0) {
                option.selected = true;
                selIdx = i;
            }
            try {
                selectEl.add(option, null);
            } catch (e) {
                selectEl.add(option, -1);
            }
        }
    }
};
