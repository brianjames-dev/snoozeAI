VENV ?= .venv
ENV_FILE ?= backend/.env

.PHONY: setup backend openai-on openai-off _toggle-openai

setup:
	python3 -m venv $(VENV)
	. $(VENV)/bin/activate && pip install -r backend/requirements.txt

backend:
	@test -d $(VENV) || $(MAKE) setup
	. $(VENV)/bin/activate && uvicorn backend.app.main:app --reload --port 8000

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
