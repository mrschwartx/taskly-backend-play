SBT = sbt
SEED_MAIN = com.taskly.backend.config.SeedMain
STAGE_BIN = target/universal/stage/bin/taskly-backend

.PHONY: env secret-key dev build run test verify seed clean

env:
	@test -f .env || { cp .env.example .env && echo "Created .env from .env.example"; }

secret-key:
	@openssl rand -base64 32

dev: env
	@set -a && . ./.env && set +a && $(SBT) run

build:
	$(SBT) stage

run: build env
	@set -a && . ./.env && set +a && $(STAGE_BIN)

test: env
	@set -a && . ./.env && set +a && $(SBT) test

verify: test

seed: build env
	@set -a && . ./.env && set +a && $(STAGE_BIN) -main $(SEED_MAIN)

clean:
	$(SBT) clean
