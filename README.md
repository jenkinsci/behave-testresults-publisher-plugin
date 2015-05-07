# Publish Html reports on [Jenkins](http://jenkins-ci.org/)
 
This is a Python Jenkins plugin which publishes pretty html reports showing the results of Behave runs. 


## Background

Behave is a BDD test automation framework
Specifications are written in a human readable form and executed in continuous integration. 

This plugin allows Jenkins to publish the results as html reports hosted by the Jenkins build server. In order for this plugin to work you must be generating a json report using behave  --format=json.pretty --outfile=resultsfilenameofyourchoice.json.

The plugin converts the json report into an html linking to feature files
