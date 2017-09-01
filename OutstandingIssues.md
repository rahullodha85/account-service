(Each category ordered from highest to lowest value/effort ratio, as determined by the team)

Action Items:

- What does it take to set up an environment? Action item is to create this list
	- What are the components
	- How hard to automate them (are they already automated)
	- Who controls this task currently in evnironment setup, knows how it works
	- Can we hook up pieces to a button for "reset all to prod/base"
	Specics we identified so far:
		- BM attributes
		- Reference mongo databases
		- Batch processes integrating upstream/downstream systems
		- BM reference data
		- UC4 jobs (Cron jobs + ETL's)
		- DB Query

- Toggles - automated ways to reset and transfer between environments.
- Determine what an AEM health check consists of. Develop or use from Sapient team.
- Propogate fast-fail on misconfigured environment variables / property files to all services
	- Get developer control of evnironment variables for lower environments
- Include correlation ID's in responses to front-end; headers.
- Dev mode in microservices to allow errors to bubble up to front-end during development
- Switch to new play version to utilize new functionality (in microservices template)
- Replace fake Play App in test suite, or refactor to be faster / less flaky.
- Bring visibility of Selenium front-end test suite to dev team. Be able to debug failures.
- Artifact Promotion for pipelines
	- Currently promoting lots of untested/broken services
- Spike infra, deployment and API work to split saks.com into multiple repos
	- Need to support different banners, features, themes
	- Different versions of modules accross repos
	- Lots of testing requried for each change right now, since affects multiple banners 
- Remove unnecessary indirection from "ControllerPayload" scala object (account-service, micro template)
- Better error messages for build/deploy pipelines on microservices. Any metrics around inconsistent errors?
- Extract product info in order-history to come from microservices (rather than BM)


Decisions:
- Controller timer tests - refactor or remove? What is value?
- Unused http endpoints on services other than account service. Should we remove them?


Items to develop further:
- Basic troubleshooting skills
	- knowledge shared by members/wiki/gist
- What should services be returning on failure? 200, 500? Html, JSON? Does this depend on the 'type' of service?
	- How do we differenciate between expected and unexpected errors in API's? Need consistency. Front-end has multiple error parsing methods, fragile.
- Unstable checkout code/configuration makes all dev/testing of features that interact with it difficult. What can we do about this?
	- all instances using same database. contention. low visibility of changes
- No domain objects used in mid level services. What are the problems / implications of this? Can we fix it? How much work? (integration + unit tests)
- Why are people not using  dev-machine still? What are the remaining barriers to entry? What doens't work easily/well? What value is it missing?
	- Still to hard to install, flaky. Difficult entry point
-Low coverage for front-end tests. How can we make better? Can we do anything to make it easier to write front-end tests? (Testing pyramid)
	- Unit testing in genereal
- How do we monitor/determine performance of BM itself? Better logging?
	- there is existing stats framework. instruct people how to use
- How do we support an integration test suite that works accross banners? For banner specific features?


How do we keep ourselves accountable for this?
