.DEFAULT_GOAL := help

MVN := $(shell command -v mvn 2>/dev/null || echo /home/aliube/.m2/wrapper/dists/apache-maven-3.10.0-bin/39q0qgmngdu329psje7bjgne79/apache-maven-3.10.0/bin/mvn)

.PHONY: help test test-unit test-arch test-integration up down reset logs ps

help: ## Tampilkan daftar perintah Makefile
	@echo "Payroll Management System — Makefile Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

test: ## Jalankan semua test (Unit + Boundary Arsitektur + Integration)
	$(MVN) -f backend/pom.xml test

test-unit: ## Jalankan Unit Test rumus kalkulator (tanpa DB)
	$(MVN) -f backend/pom.xml test -Dtest=PayrollCalculatorTest

test-arch: ## Jalankan test batas Modular Monolith & anti-cross-domain SQL join
	$(MVN) -f backend/pom.xml test -Dtest=ModularMonolithBoundaryTest

test-integration: ## Jalankan Integration Test End-to-End (memerlukan PostgreSQL & Redis aktif)
	$(MVN) -f backend/pom.xml test -Dtest=PayrollIntegrationTest

up: ## Build dan jalankan seluruh container Docker (postgres, redis, backend, frontend)
	docker compose up --build -d

down: ## Hentikan seluruh container Docker (data database tetap tersimpan)
	docker compose down

reset: ## Reset volume database dan bangun ulang seluruh stack Docker
	docker compose down -v
	docker compose up --build -d

logs: ## Pantau log container backend secara live
	docker compose logs -f backend

ps: ## Cek status seluruh container Docker
	docker compose ps
