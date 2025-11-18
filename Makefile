VENV ?= .venv
BUNDLE_ID ?= dev.brianjames.AINotificationAgent
ENV_FILE ?= backend/.env
PAYLOAD ?= $(PWD)/payload.apns

.PHONY: setup backend ios dev push bundle-id openai-on openai-off test _toggle-openai

setup:
	python3 -m venv $(VENV)
	. $(VENV)/bin/activate && pip install -r backend/requirements.txt

backend:
	@test -d $(VENV) || $(MAKE) setup
	. $(VENV)/bin/activate && uvicorn backend.app.main:app --reload --port 8000

ios:
	open ios/AINotificationAgent/AINotificationAgent.xcodeproj

dev:
	@echo "Run in two terminals:"
	@echo "1) make backend"
	@echo "2) make ios (then ⌘R in Xcode)"

push:
	@device=$$(xcrun simctl list devices booted | grep -Eo "[0-9A-F-]{8}-([0-9A-F-]{4}-){3}[0-9A-F-]{12}" | head -n 1); \
	if [ -z "$$device" ]; then echo "No booted simulator found."; exit 1; fi; \
	if [ ! -f "$(PAYLOAD)" ]; then echo "Payload $(PAYLOAD) not found."; exit 1; fi; \
	echo "Pushing $(PAYLOAD) to $$device ($(BUNDLE_ID))"; \
	xcrun simctl push $$device $(BUNDLE_ID) "$(PAYLOAD)"

bundle-id:
	@echo $(BUNDLE_ID)

openai-on:
	@STATE=true ENV_FILE=$(ENV_FILE) $(MAKE) _toggle-openai

openai-off:
	@STATE=false ENV_FILE=$(ENV_FILE) $(MAKE) _toggle-openai

_toggle-openai:
	@STATE=$$STATE ENV_FILE=$(ENV_FILE) python3 - <<'PY'
	import os
	import pathlib
	import re

	target = pathlib.Path(os.environ["ENV_FILE"])
	template = pathlib.Path("backend/.env.example")
	if not target.exists():
	    if template.exists():
	        target.write_text(template.read_text())
	    else:
	        target.touch()

	text = target.read_text()
	if "USE_OPENAI" not in text:
	    text += "\nUSE_OPENAI=false\n"

	new_text = re.sub(r"^USE_OPENAI=.*$", f"USE_OPENAI={os.environ['STATE']}", text, flags=re.MULTILINE)
	target.write_text(new_text)
	print("USE_OPENAI set to", os.environ["STATE"])
	PY

test:
	@test -d $(VENV) || $(MAKE) setup
	. $(VENV)/bin/activate && pytest
