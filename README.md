##  Overview

	This guide shows you how to get set up on your home machine for building AI applications,
	tools, and custom harnesses.
	
	You need speed, predictability, ease of use, pertinence (anything you learn here can be
	re-implemented at work), and it needs to be cheap (preferably free).

	Disclaimer: DO NOT DEVELOP AT HOME AND MAIL CONTENT TO WORK. 

	This is a learning opportunity.

##  Somewhere to Take Notes

	You might be tempted to use NotebookLM, but I am going to recommend you download Joplin instead.
	
	It is open-source, available on every platform, and can be encrypted.  The big win for me
	personally is the offline-first approach that Joplin takes, while still offering mobile, web,
	and desktop sync through any of the cloud providers you already use (including Microsoft
	OneDrive, Google Drive, Dropbox, and iCloud).  It's available for Windows, Mac, Linux, Android, and iOS.

	Best of all, Joplin allows bidirectional note management from VSCode and your browser.

	Download it here: https://joplinapp.org/download/

##  Hardware for Hosting At Home for Free

	To host models at home, you need to be able to load the model into memory to be fast,
	although there are models engineered to run in environments without GPU availability.
	One of the primary reasons to host from home is to be able to test and expand your awareness
	of guardrails without setting off alarm bells with remote/subscription-based model services.

	You need hardware acceleration, but there are affordable options.  Please be aware that desktop video cards
	use a LOT of power, but laptop video cards with tons of VRAM are much more affordable and can still load
	models in memory and are very usable.

	The following at-home hardware solutions for hosting (to be entirely free) are listed in order of performance.
	
	Desktops:

	D1. Mac M5 Ultra (~$18k)
		+ 512 GB of Unified Memory (can load a 430 GB model that would usually require six H100's at $60k apiece)
		+ 1.2 TB/s throughput
		+ 6× Thunderbolt 5 ports (enabling clustering with typical SMP overhead)
		+ can run 400B token models

	D2. NVIDIA DGX Spark (~$7k)
		+ 128 GB LPDDR5X (shared CPU + GPU)
		+ 273 GB/s throughput
		+ single instance can run 200B token models

	D3. Mac M4 MAX (~$5k)
		+ 128 GB of Unified Memory (only 100GB usable)
		+ 546 GB/s throughput
		+ can run 120B token models

	D4. NVIDIA RTX 5090 ($2500)
		+ 32 GB of VRAM
		+ 1,792 GB/s throughput (with a 512-bit bus, but EXTREME power hog)
		+ can run 70B token models

	D5. NVIDIA RTX 4070 Ti Super ($800)
		+ 16 GB of VRAM
		+ 672 GB/s throughput
		+ can run 32B token models

	D6. NVIDIA RTX 3060 (~$250)
		+ 12 GB of VRAM
		+ 360 GB/s throughput
		+ can run 14B token models

	D7. Any machine with an NVIDIA card with 8 GB of VRAM (there are so many deals)

	Laptops: 
		+ Windows -> look for deals on any Intel-based laptop with a 4070, or a 5060.
		+ MacOS -> look for an M4 or an M5 with the most RAM you can find.

	NOTE: System RAM is not the same as video RAM, and companies will do their best to trick you,
	but there are incredible deals to be had on Black Friday and Cyber Monday.

	How do I find out how much memory I have on my current machine? 
		[Windows Key] -> dxdiag -> [ENTER] -> Display 1 -> Display 2

##  Select a Code Editor

	You need a good code editor that supports AI development.

	I HIGHLY recommend VSCode.  In addition to being a well-supported editor across all platforms,
	it has great options at home as well as at work.  

	The Antigravity IDE is a preconfigured VSCode download that has been optimized for use with 
	a Google AI Pro subscription.  A Google AI Pro subscription retails for $20 a month, with a 
	50% discount for Verizon customers and another 50% off of that for Verizon employees.

	For $5 a month, you get 5 TB of cloud storage, extended quotas for Gemini models, GPT, and
	Claude Sonnet and Opus.  You also get additional integrations enabled between gemini.google.com
	and Google Drive, Docs, Slides, Sheets, Mail, Calendar, and NotebookLM (everything except Picasa).

	To enable the discount, open your Verizon phone, go to the My Verizon Application and 
	go to "Plan Perks" and choose "Productivity Perks" and choose "Google AI Pro" for $5.

##  Choose a Model Server

	In order to build your own AI applications, you need to enable programmatic access to AI models.

	You have options here, but there are a lot of reasons to go with solutions
	that support the OpenAI API (using /v1/chat/completions or /v1/responses).

	Subscription-based model servers:

	1. OpenAI (GPT)
	2. Google (Gemini //vi AI Studio or Vertex)
	3. Anthropic (Claude)
	4. xAI (Grok)

	Free model servers:

	1. Ollama (cli // home use)
	2. LMStudio (UI + Huggingface // home use)
	3. vllm (cli // horizontally scales)

	NOTE: If you have more than one machine with an accelerated video card or a Mac M4/M5 processor,
	you can download NVIDIA PAIR and use it as a dynamic OpenAI-based proxy that you connect to in
	the same manner as the above servers.

##  Install the Model Server

	For this guide, you should download and install Ollama from here -> https://ollama.com/download/windows

	Ollama hosts all your models on port 11434 by default, providing two interfaces: 
		1. a native Ollama API (and)
		2. an OpenAPI compatible interface using /v1/chat/completions

	Ollama models are managed similarly to Docker images.

	Here are a list of basic ollama commands:
		1. ollama --help -> see all available commands
		2. ollama list -> shows the models that have been downloaded and are available for loading
		3. ollama ps -> shows the models currently loaded (and shows their size and if they are GPU-enabled)
		4. ollama stop <slugName> -> unload a currently loaded model
		5. ollama rm <slugName> -> deletes the currently loaded model from being accessible
		6. ollama pull <slugName> -> download a model from a registry (defaults to HuggingFace)
		7. ollama -> starts up a chat interface that allows you to interact with accessible models

	How do you find out which slug to use?  Go to gemini.google.com and tell it how much memory you have
	and ask it for the best model based on what you plan on using it for.

##  Select Your Model(s)

	In the beginning, I recommend you start with a small IBM Granite model -> granite3-moe:3b

	Granite is a great multi-purpose model provided by IBM that is origined in the United States
	and has been vetted by multiple national security agencies and is available using an entirely
	Permissive licensing model.  Best of all, Granite is approved for use in our corporate environment.

	Use the popular_models.md list to select other models for specific task types.

	To download and activate the granite model, run: ollama pull granite3-moe:3b

	I HIGHLY recommend you download LMStudio to simplify model searching in the future.
	It is a free application and provides many advanced features.

##  Create a New Project

	Create a folder BEFORE you open up the Antigravity IDE / VSCode.  If you open VSCode and then
	use the embedded AI agent to create your application, the context of the agent will be reset
	when you open the project folder.  If you create the project folder first, and then use your
	IDE to open that folder, the context used to create the project remains available for reuse
	in the future (simplifying code changes).

	1. For this guide, I'm am going to create c:\java\project\learningai
	2. Now, open the Antigravity IDE (not the CLI tool) 
	3. Choose "Open Folder" and select the folder you created in Step 1.

##  Create a Reusable Prompt for Baseline Projects


		**For the AI Assistant**  
		For Java:
			Please create an implementation plan named plan/implementation_plan.md that conveys
			how to construct a new Java project in this folder using JDK 25, Micronaut, and Gradle for
			dependency management. 
		
		For Python:
			Please create an implementation plan named plan/implementation_plan.md that conveys how to construct a new Python project in this folder using Python 3.12+, FastAPI, and uv (or Poetry) for dependency management.  
		
		Universal:
			Use GoF patterns where appropriate and provide a web application
			that is hosted on port 9443 using HTTPS, with a REST API, and an MCP server that uses
			Streamable HTTP with full support for existing REST API calls.  Please ensure that all
			three modes are protected by the use of an OAUTH Bearer token for authentication and
			authorization.

			Make sure to only utilize open-source libraries and versions with zero active CVEs and
			that utilize entirely Permissive licensing.

			Guarantee that unit tests and container tests are always kept up-to-date and have them
			utilize ephemeral containers for testing in lieu of mock objects. 

			The implementation_plan.md must always be kept current to reflect and be able to drive
			changes to the codebase.

			Do not implement the plan until I give you the go ahead to do so.

##  Implement the Plan 

	Review the plan with the AI Assistant to ensure it is still valid and up-to-date.
