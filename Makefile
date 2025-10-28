.PHONY: setup backend ios dev

setup:
	python3 -m venv .venv && . .venv/bin/activate && pip install -r backend/requirements.txt

backend:
	. .venv/bin/activate && uvicorn backend.app.main:app --reload --port 8000

ios:
	open ios/AI_Notification_Agent.xcodeproj

dev:
	@echo "Run in two terminals:"
	@echo "1) make backend"
	@echo "2) make ios (then ⌘R in Xcode)"
