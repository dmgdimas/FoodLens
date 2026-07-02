.PHONY: help up down rebuild logs backend-test backend-fmt health products calculate analyze

API_URL ?= http://localhost:8000
IMAGE ?= test.jpg

help:
	@echo "FoodLens project commands"
	@echo ""
	@echo "make up              - start all services"
	@echo "make down            - stop all services"
	@echo "make rebuild         - rebuild and start all services with clean database"
	@echo "make logs            - show docker compose logs"
	@echo "make backend-test    - run Go backend tests"
	@echo "make backend-fmt     - format Go backend code"
	@echo "make health          - check backend health"
	@echo "make products        - check products endpoint"
	@echo "make calculate       - check manual nutrition calculation"
	@echo "make analyze IMAGE=test.jpg - check image analysis endpoint"

up:
	docker compose up

down:
	docker compose down

rebuild:
	docker compose down -v
	docker compose up --build

logs:
	docker compose logs -f

backend-test:
	cd backend && go test ./...

backend-fmt:
	cd backend && go fmt ./...

health:
	curl $(API_URL)/health

products:
	curl $(API_URL)/api/v1/products

calculate:
	curl -X POST $(API_URL)/api/v1/calculate \
		-H "Content-Type: application/json" \
		-d '{"ml_class":"apple","weight_g":140}'

analyze:
	curl -X POST $(API_URL)/api/v1/analyze \
		-F "image=@$(IMAGE)"
