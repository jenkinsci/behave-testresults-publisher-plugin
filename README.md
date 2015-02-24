# Publish pretty Html reports on [Jenkins](http://jenkins-ci.org/)
 
This is a Python Jenkins plugin which publishes pretty html reports showing the results of Behave runs. 


## Background

Behave is a test automation tool following the principles of BDD
Specifications are written in a concise human readable form and executed in continuous integration. 

This plugin allows Jenkins to publish the results as pretty html reports hosted by the Jenkins build server. In order for this plugin to work you must be using the JUnit runner and generating a json report using behave  --format=json.pretty --outfile=resultsfilenameofyourchoice.json. The plugin converts the json report into an overview html linking to separate feature file htmls with stats and results. 


