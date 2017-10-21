const chalk = require('chalk');
const packagejs = require('../../package.json');
const semver = require('semver');
const BaseGenerator = require('generator-jhipster/generators/generator-base');
const jhipsterConstants = require('generator-jhipster/generators/generator-constants');
const mkdirp = require('mkdirp');
const modifyFileContentUtils = require('./modifyFileContentUtils');


module.exports = class extends BaseGenerator {
    get initializing() {
        return {
            readConfig() {
                this.jhipsterAppConfig = this.getJhipsterAppConfig();
                if (!this.jhipsterAppConfig) {
                    this.error('Can\'t read .yo-rc.json');
                }
            },
            displayLogo() {
            },
            checkJhipster() {
                const currentJhipsterVersion = this.jhipsterAppConfig.jhipsterVersion;
                const minimumJhipsterVersion = packagejs.dependencies['generator-jhipster'];
                if (!semver.satisfies(currentJhipsterVersion, minimumJhipsterVersion)) {
                    this.warning(`\nYour generated project used an old JHipster version (${currentJhipsterVersion})... you need at least (${minimumJhipsterVersion})\n`);
                }
            }
        };
    }

    prompting() {
    }

    writing() {
        // read config from .yo-rc.json
        this.baseName = this.jhipsterAppConfig.baseName;
        this.packageName = this.jhipsterAppConfig.packageName;
        this.packageFolder = this.jhipsterAppConfig.packageFolder;
        this.clientFramework = this.jhipsterAppConfig.clientFramework;
        this.clientPackageManager = this.jhipsterAppConfig.clientPackageManager;
        this.buildTool = this.jhipsterAppConfig.buildTool;

        // use constants from generator-constants.js
        const javaDir = `${jhipsterConstants.SERVER_MAIN_SRC_DIR + this.packageFolder}/`;
        const resourceDir = jhipsterConstants.SERVER_MAIN_RES_DIR;

        this.template('storage/config/StorageConfiguration.java', `${javaDir}/config/StorageConfiguration.java`);

        mkdirp(`${javaDir}/config/storage`);
        this.prefixName = `${this.packageName.split('.')[1]}.storage`;
        this.template('storage/config/StorageProperties.java', `${javaDir}/config/storage/StorageProperties.java`);

        mkdirp(`${javaDir}/service/storage`);
        this.template('storage/service/StorageService.java', `${javaDir}/service/storage/StorageService.java`);

        // 修改application.yml文件
        const entityPath = `${resourceDir}config/application.yml`;
        const entityInfo = this.fs.read(entityPath, {
            defaults: ''
        });
        if (!entityInfo.includes('storage:')) {
            modifyFileContentUtils.rewriteFile({
                file: entityPath,
                needle: 'spring:',
                flow: false,
                splicable: [
                    'storage:\n\ts3:\n\t\t' +
                    'access-key: heyirdc\n\t\t\t' +
                    'secret-key: heyirdc\n\t\t\t' +
                    'endpoint: 192.168.0.3'
                ]
            }, this);
        }
    }

    install() {
        let logMsg =
            `To install your dependencies manually, run: ${chalk.yellow.bold(`${this.clientPackageManager} install`)}`;

        if (this.clientFramework === 'angular1') {
            logMsg =
                `To install your dependencies manually, run: ${chalk.yellow.bold(`${this.clientPackageManager} install & bower install`)}`;
        }
        const injectDependenciesAndConstants = (err) => {
            if (err) {
                this.warning('Install of dependencies failed!');
                this.log(logMsg);
            } else if (this.clientFramework === 'angular1') {
                this.spawnCommand('gulp', ['install']);
            }
        };
        const installConfig = {
            bower: this.clientFramework === 'angular1',
            npm: this.clientPackageManager !== 'yarn',
            yarn: this.clientPackageManager === 'yarn',
            callback: injectDependenciesAndConstants
        };
        if (this.options['skip-install']) {
            this.log(logMsg);
        } else {
            this.installDependencies(installConfig);
        }
    }

    end() {
        this.log('End of generator-jhipster-storage generator');
    }
};
