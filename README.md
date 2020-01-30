# aws-datacenter-jira-plugin2

Atlassian Jira Data Center Plugin2 for AWS EventBridge Integration

## Developing the plugin

### Frontend
In the project directory, you can run:

##### `yarn start`

It builds the frontend and puts it in the watch mode with hot reload. 
In other words, if you have the whole plugin and an instance already working, 
this will enable you to make quick changes with instant preview.

Go to localhost:3333 to run the frontend without Jira

If you want to test in-app, run atlas-run --product \[jira|refapp|confluence\] while the dev server is running

##### `yarn test`

For running UI tests.

##### `yarn lint`

Checks the frontend plugin for styling errors. 

The ruleset is set to be compatible with other Server plugins, 
so please mind that when considering making changes to it.

##### `yarn lint --fix`

Will additionally fix any automatically-fixable issues.

### Backend

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-help  -- prints description for all commands in the SDK
* atlas-mvn package -- build plugin package
* atlas-create-jira-plugin-module -- create plugin modules

Testing:
Login from http://localhost:2990/jira

| Username | Password |
| -------- | -------- |
| admin    | admin    |

Manage from http://localhost:2990/jira/plugins/servlet/upm

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK


# License

This library is licensed under the Apache 2.0 License. 
