# Atlassian DC Migration Assistant

Atlassian server app for migrating to Data Center on AWS

[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) ![Build](https://github.com/atlassian-labs/dc-migration-assistant/workflows/Tests/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

## Usage

Build the plugin via `mvn package` then install the plugin into your Jira instance through the "Manage Apps" menu and uploading the app .jar or .obr found in the `target` directory.

## Developing the plugin

### Dependencies

* [Maven](https://maven.apache.org/) (3.6.1 or higher)
* Node/NPM ([NVM](https://github.com/nvm-sh/nvm) is recommended to manage
  versions)
* Yarn (can be installed from package or with `npm install -g yarn`)

### Frontend

#### Stack

The frontend is built on a stack of:

* [React](https://reactjs.org/)
* [Typescript](https://www.typescriptlang.org/)
* [styled components](https://styled-components.com/)
* Built with [Webpack](https://webpack.js.org/) and [Babel](https://babeljs.io/)

In the project directory, you can run:

##### `yarn start`

It builds the frontend and puts it in the watch mode with hot reload. 
In other words, if you have the whole plugin and an instance already working, 
this will enable you to make quick changes with instant preview.

Go to localhost:3333 to run the frontend without Jira

If you want to test in-app, run atlas-run --product \[jira|refapp|confluence\] while the dev server is running

### Backend

Here are the SDK commands you'll use immediately:

* Run the plugin in a product: `mvn amps:run -Dproduct=[jira|confluence|refapp] -Dproduct.version=<version e.g. 8.5.0>`
* Build the plugin: `mvn package`
* When in doubt, `mvn clean` it out
* [Full atlassian maven reference](https://developer.atlassian.com/server/framework/atlassian-sdk/working-with-maven/#using-the-amps-maven-plugin-directly)

Testing:
| Username | Password |
| -------- | -------- |
| admin    | admin    |

Manage from http://localhost:2990/jira/plugins/servlet/upm

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK

## Executing and Testing

##### `mvn verify`

Will run all tests

##### `yarn test`

For running just UI tests.

##### `yarn lint`

Checks the frontend plugin for styling errors. 

The ruleset is set to be compatible with other Server plugins, 
so please mind that when considering making changes to it.

##### `yarn lint --fix`

Will additionally fix any automatically-fixable issues.

## Contributions

Contributions to DC Migration Assistant are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details


## License

This library is licensed under the Apache 2.0 License. 

[![With ❤️ from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-with-thanks.png)](https://www.atlassian.com)
