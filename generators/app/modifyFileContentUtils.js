const path = require('path');

function appendContent(args) {
    // check if splicable is already in the body text
    const re = new RegExp(args.splicable.map(line => `\\s*${escapeRegExp(line)}`).join('\n'));

    if (re.test(args.haystack)) {
        return args.haystack;
    }

    const lines = args.haystack.split('\n');

    let otherwiseLineIndex = -1;
    lines.forEach((line, i) => {
        if (line.indexOf(args.needle) !== -1) {
            otherwiseLineIndex = i;
            if (args.flow) {
                otherwiseLineIndex += 1;
            }
        }
    });

    if (otherwiseLineIndex !== -1) {
        let spaces = 0;
        while (lines[otherwiseLineIndex].charAt(spaces) === ' ') {
            spaces += 1;
        }

        let spaceStr = '';

        while ((spaces -= 1) >= 0) { // eslint-disable-line no-cond-assign
            spaceStr += ' ';
        }

        lines.splice(otherwiseLineIndex, 0, args.splicable.map(line => spaceStr + line).join('\n'));
    }

    return lines.join('\n');
}


function escapeRegExp(str) {
    return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&'); // eslint-disable-line
}

/**
 * 重写generator-jhipster/generators/utils的rewriteFile方法
 * @param args
 * @param generator
 */
function rewriteFile(args, generator) {
    args.path = args.path || process.cwd();
    const fullPath = path.join(args.path, args.file);

    args.haystack = generator.fs.read(fullPath);
    const body = appendContent(args);
    generator.fs.write(fullPath, body);
}

module.exports = {
    rewriteFile
};
