var exec = require('cordova/exec');

exports.scan = function(options, success, error) {

    if (typeof options === "function") {
        error = success;
        success = options;
        options = {};
    }

    exec(
        success,
        error,
        'ScannerPlugin',
        'startScan',
        [options || {}]
    );
};