/* global describe, beforeEach, it */

const path = require('path');
const fse = require('fs-extra');
const assert = require('yeoman-assert');
const helpers = require('yeoman-test');

describe('JHipster generator storage', () => {
    describe('Test aws s3 setting', () => {
        beforeEach((done) => {
            helpers
                .run(path.join(__dirname, '../generators/app'))
                .inTmpDir((dir) => {
                    fse.copySync(path.join(__dirname, '../test/templates'), dir);
                })
                .withOptions({
                    testmode: true
                })
                .withPrompts({
                    updateType: 'all'
                })
                .on('end', done);
        });

        it('generate template file', () => {
            assert.file([
                'src/main/java/org/bigbug/dummy/config/StorageConfiguration.java',
                'src/main/java/org/bigbug/dummy/config/storage/StorageProperties.java',
                'src/main/java/org/bigbug/dummy/service/storage/StorageService.java',
            ]);
        });

        it('generate application.yml content', () => {
            assert.fileContent('src/main/resources/config/application.yml', 'storage:\n\ts3:\n\t\t');
        });
    });
});
