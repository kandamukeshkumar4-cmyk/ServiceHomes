.PHONY: help up backend frontend test lint clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## Start Docker infrastructure services (Postgres + MinIO)
	cd infra/docker && docker compose up -d db minio minio-init

down: ## Stop Docker services
	cd infra/docker && docker compose down

backend: ## Run Spring Boot backend (local profile, no Auth0 needed)
	cd apps/api-spring && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

backend-test: ## Run backend tests
	cd apps/api-spring && ./mvnw test

frontend: ## Run Angular frontend
	cd apps/web-angular && npm install && npm run start

frontend-build: ## Build Angular frontend
	cd apps/web-angular && npm install && npm run build

dbt-deps: ## Install dbt dependencies
	cd apps/analytics-dbt && dbt deps

dbt-build: ## Build dbt models
	cd apps/analytics-dbt && dbt build

dbt-test: ## Run dbt tests
	cd apps/analytics-dbt && dbt test

lint: ## Run all linters
	cd apps/web-angular && npm run lint

clean: ## Clean build artifacts
	cd apps/api-spring && ./mvnw clean
	cd apps/web-angular && rm -rf dist node_modules
	cd apps/analytics-dbt && rm -rf target dbt_packages
