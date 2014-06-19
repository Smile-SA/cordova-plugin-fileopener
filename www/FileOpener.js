var exec = require('cordova/exec');

exports.canOpenFile = function (arg0, success, error) {
    exec(success, error, "FileOpener", "canOpenFile", [arg0]);
};

exports.openFile = function (arg0, success, error) {
    exec(success, error, "FileOpener", "openFile", [arg0]);
};

