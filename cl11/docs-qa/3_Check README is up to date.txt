2026-04-21T19:18:38.4657523Z ##[group]Run # Check if README mentions all major components
2026-04-21T19:18:38.4658447Z [36;1m# Check if README mentions all major components[0m
2026-04-21T19:18:38.4659182Z [36;1mif ! grep -q "apps/api-spring" README.md; then[0m
2026-04-21T19:18:38.4660058Z [36;1m  echo "::warning::README.md may be outdated - missing backend reference"[0m
2026-04-21T19:18:38.4660833Z [36;1mfi[0m
2026-04-21T19:18:38.4661334Z [36;1mif ! grep -q "apps/web-angular" README.md; then[0m
2026-04-21T19:18:38.4662183Z [36;1m  echo "::warning::README.md may be outdated - missing frontend reference"[0m
2026-04-21T19:18:38.4662939Z [36;1mfi[0m
2026-04-21T19:18:38.4663447Z [36;1mif ! grep -q "apps/analytics-dbt" README.md; then[0m
2026-04-21T19:18:38.4664286Z [36;1m  echo "::warning::README.md may be outdated - missing analytics reference"[0m
2026-04-21T19:18:38.4665015Z [36;1mfi[0m
2026-04-21T19:18:38.4776851Z shell: /usr/bin/bash -e {0}
2026-04-21T19:18:38.4777407Z env:
2026-04-21T19:18:38.4777797Z   JAVA_VERSION: 21
2026-04-21T19:18:38.4778282Z   NODE_VERSION: 20
2026-04-21T19:18:38.4778733Z   PYTHON_VERSION: 3.11
2026-04-21T19:18:38.4779184Z ##[endgroup]
