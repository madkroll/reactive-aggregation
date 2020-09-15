#!/usr/bin/env bash

set -eo pipefail

# Requires Java 11
java -version

mvn gatling:test