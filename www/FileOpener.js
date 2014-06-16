var exec = require('cordova/exec');

exports.isFileSupported = function (arg0, success, error) {
    exec(success, error, "FileOpener", "isFileSupported", [arg0]);
};

exports.openFile = function (arg0, success, error) {
    exec(success, error, "FileOpener", "openFile", [arg0]);
};

