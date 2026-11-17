	**For the AI Assistant**

	Please create an implementation plan named plan/implementation_plan.md that conveys	how to construct a new project in the folder "c:\java\project\chat5000" named chat5000 using the following baseline:

		Use Java (JDK 25), Micronaut, and Gradle for dependency management.  
        
        Do NOT use any Spring-related libraries.

		Use GoF patterns where appropriate and provide a web application that is hosted on port 9443 using HTTPS, with a REST API, and an MCP server that uses Streamable HTTP with full support for existing REST API calls.  Please ensure that all three modes are protected by the use of an OAUTH Bearer token for authentication and authorization.

		Make sure to only utilize open-source libraries and versions with zero active CVEs and that use entirely Permissive licensing.

		Guarantee that unit tests and container tests are always kept up-to-date and have them utilize ephemeral containers for testing in lieu of mock objects.

		The implementation_plan.md must always be kept current to reflect and be able to drive changes to the codebase.

		Include an OpenAI compatible service class during the implementation that will allow you to interact with a specific Ollama model (using port 11434) by using the base_url "http://localhost:11434" in lieu of the normal OpenAI base URL, with the particular model name configurable via a configuration file or environment variables as well as the temperature and max_output_token size for the model conversation.  Be sure to include support for conversational chat sessions as well as single question/answer API calls.

		Implement caching where appropriate and use an ephemeral data store like Valkey to cache responses from the AI model.

		Implement rate limiting, request validation, and metrics.

		Select ciphers and bit strengths that are guaranteed to be PQC compliant.

		Do not implement the plan until I give you the go ahead to do so.

        The intent of the application is to satisfy the expectations from the following list:

I need a UI that can enable a conversation with an Ollama model, to prove that interaction with local models is:
+ possible/feasible,
+ affordable/free,
+ fast/accelerated, 
+ private/secure, 
+ load-bearing, (and)
+ horizontally scalable.

The UI must be simple, modern, and be able to support:
+ text input,
+ streaming output,
+ conversation history,
+ the ability to switch models easily.